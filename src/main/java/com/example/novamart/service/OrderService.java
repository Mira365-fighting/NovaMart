package com.example.novamart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.novamart.common.exception.BusinessException;
import com.example.novamart.common.exception.ErrorCode;
import com.example.novamart.entity.Order;
import com.example.novamart.entity.OrderItem;
import com.example.novamart.entity.Product;
import com.example.novamart.mapper.OrderItemMapper;
import com.example.novamart.mapper.OrderMapper;
import com.example.novamart.mapper.ProductMapper;
import com.example.novamart.util.PageUtil;
import com.example.novamart.vo.CartVO;
import com.example.novamart.vo.OrderDetailVO;
import com.example.novamart.vo.OrderVO;
import com.example.novamart.vo.PageResult;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 订单：从购物车下单、查询、取消。 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartService cartService;
    private final ProductMapper productMapper;

    public OrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                        CartService cartService, ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartService = cartService;
        this.productMapper = productMapper;
    }

    /**
     * 从购物车提交订单：扣库存 → 写订单和明细 → 清空购物车。
     * 四步必须在同一个事务里，任何一步失败都整体回滚，不能出现「库存扣了但订单没生成」。
     */
    @Transactional
    public OrderDetailVO submit(long userId) {
        List<CartVO> cartItems = cartService.list(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_NOT_FOUND, "购物车是空的，先加商品再下单");
        }

        // 1. 逐件扣库存，同时组装明细
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>(cartItems.size());
        for (CartVO cartItem : cartItems) {
            deductStock(cartItem.productId(), cartItem.quantity());

            OrderItem item = new OrderItem();
            item.setProductId(cartItem.productId());
            // 商品名和单价存快照，之后商品改价改名不影响这张订单
            item.setProductName(cartItem.productName());
            item.setPrice(cartItem.price());
            item.setQuantity(cartItem.quantity());
            item.setSubtotal(cartItem.subtotal());
            orderItems.add(item);

            totalAmount = totalAmount.add(cartItem.subtotal());
        }

        // 2. 写订单主表（insert 后自增 id 会回填到 order.id）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(Order.STATUS_UNPAID);
        orderMapper.insert(order);

        // 3. 写订单明细
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 4. 清空购物车
        cartService.clear(userId);
        return detail(userId, order.getId());
    }

    /** 我的订单分页，分页同样交给 PageHelper */
    public PageResult<OrderVO> page(long userId, int pageNum, int pageSize) {
        PageUtil.start(pageNum, pageSize);
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getId));
        return PageResult.of(new PageInfo<>(orders), OrderVO::of);
    }

    /** 订单详情，带明细 */
    public OrderDetailVO detail(long userId, long orderId) {
        Order order = requireOwnOrder(userId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        return OrderDetailVO.of(order, items);
    }

    /** 取消订单：只有待支付的能取消，取消时把库存加回去 */
    @Transactional
    public void cancel(long userId, long orderId) {
        Order order = requireOwnOrder(userId, orderId);
        if (order.getStatus() == null || order.getStatus() != Order.STATUS_UNPAID) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有待支付的订单可以取消");
        }

        order.setStatus(Order.STATUS_CANCELLED);
        orderMapper.updateById(order);

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        items.forEach(item -> restoreStock(item.getProductId(), item.getQuantity()));
    }

    /** 查订单并校验归属，防止拿到别人的订单 id 越权访问 */
    private Order requireOwnOrder(long userId, long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 扣库存。
     * SET stock = stock - n 且 WHERE stock >= n —— 判断和扣减在同一条 SQL 里完成，
     * 并发下单时不会把库存扣成负数（影响行数为 0 说明库存不够）。
     */
    private void deductStock(Long productId, int quantity) {
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - " + quantity);
        if (productMapper.update(null, wrapper) == 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "商品 " + productId + " 库存不足，下单失败");
        }
    }

    /** 取消订单时回滚库存 */
    private void restoreStock(Long productId, int quantity) {
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("stock = stock + " + quantity));
    }

    /** 订单号 = 毫秒级时间 + 4 位随机数；order_no 上的唯一索引兜底防重 */
    private static String generateOrderNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
