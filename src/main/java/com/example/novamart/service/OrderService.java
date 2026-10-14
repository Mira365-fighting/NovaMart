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
import com.example.novamart.util.BizAssert;
import com.example.novamart.util.OrderNoGenerator;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单。
 *
 * <p>本类持有整个订单状态机（前台取消/支付/确认收货，后台发货/关闭都走这里），
 * 前后台的「能查到哪些订单」分在各自的 Service 里，但「状态怎么变」只有一处实现。
 */
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

    // ------------------------------------------------------------------ 下单

    /**
     * 从购物车提交订单：加行锁 → 扣库存 → 写订单和明细 → 清空购物车。
     *
     * <p>四步必须在同一个事务里（{@code @Transactional}），任何一步失败都整体回滚，
     * 不会出现「库存扣了但订单没生成」这种脏数据。
     *
     * <p>库存用<b>数据库行锁</b>（SELECT ... FOR UPDATE）控制并发：
     * 先按 id 升序把这些商品行锁住，然后再判断库存、扣减。
     * 并发下单时，第二个事务会在这句 SELECT 上卡住等锁，
     * 等它拿到锁时读到的已经是扣减后的库存，所以不会超卖。
     */
    @Transactional
    public OrderDetailVO submit(long userId) {
        List<CartVO> cartItems = cartService.list(userId);
        BizAssert.isTrue(!cartItems.isEmpty(), ErrorCode.CART_NOT_FOUND, "购物车是空的，先加商品再下单");

        // 1. 加行锁。SQL 里带了 ORDER BY id，并发事务按同样顺序申请锁，不会循环等待成死锁。
        List<Long> productIds = cartItems.stream()
                .map(CartVO::productId)
                .distinct()
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = productMapper.selectByIdsForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 2. 持锁逐个判断并扣减库存，同时组装明细
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>(cartItems.size());
        for (CartVO cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.productId());
            BizAssert.notNull(product, ErrorCode.PRODUCT_NOT_FOUND);
            BizAssert.isTrue(product.getStatus() != null && product.getStatus() == Product.STATUS_ON_SALE,
                    ErrorCode.PRODUCT_NOT_FOUND,
                    "「" + cartItem.productName() + "」已下架，请先移出购物车");
            BizAssert.isTrue(product.getStock() >= cartItem.quantity(),
                    ErrorCode.INVALID_ARGUMENT,
                    () -> "「" + product.getName() + "」库存不足，仅剩 " + product.getStock() + " 件");
            deductStock(product.getId(), cartItem.quantity());

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

        // 3. 写订单主表（insert 后自增 id 会回填到 order.id）
        Order order = new Order();
        order.setOrderNo(OrderNoGenerator.next());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(Order.STATUS_UNPAID);
        orderMapper.insert(order);

        // 4. 写订单明细
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 5. 清空购物车
        cartService.clear(userId);
        return detail(userId, order.getId());
    }

    // ------------------------------------------------------------------ 前台查询与状态流转

    /** 我的订单分页，可按状态筛选；分页交给 PageHelper */
    public PageResult<OrderVO> page(long userId, Integer status, int pageNum, int pageSize) {
        PageUtil.start(pageNum, pageSize);
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getId));
        return PageResult.of(new PageInfo<>(orders), OrderVO::of);
    }

    /** 订单详情，带明细 */
    public OrderDetailVO detail(long userId, long orderId) {
        return OrderDetailVO.of(requireOwnOrder(userId, orderId), itemsOf(orderId));
    }

    /** 支付（本项目的模拟支付）：0 待支付 → 1 已支付 */
    @Transactional
    public void pay(long userId, long orderId) {
        requireOwnOrder(userId, orderId);
        transition(orderId, Order.STATUS_UNPAID, Order.STATUS_PAID,
                "只有待支付的订单可以支付",
                wrapper -> wrapper.set(Order::getPayTime, LocalDateTime.now()));
    }

    /** 取消订单：0 待支付 → 4 已取消，并把库存加回去 */
    @Transactional
    public void cancel(long userId, long orderId) {
        requireOwnOrder(userId, orderId);
        transition(orderId, Order.STATUS_UNPAID, Order.STATUS_CANCELLED,
                "只有待支付的订单可以取消", wrapper -> {
                });
        restoreStockOf(orderId);
    }

    /** 确认收货：2 已发货 → 3 已完成 */
    @Transactional
    public void confirm(long userId, long orderId) {
        requireOwnOrder(userId, orderId);
        transition(orderId, Order.STATUS_SHIPPED, Order.STATUS_COMPLETED,
                "只有已发货的订单可以确认收货",
                wrapper -> wrapper.set(Order::getFinishTime, LocalDateTime.now()));
    }

    // ------------------------------------------------------------------ 后台状态流转（查询在 AdminOrderService）

    /** 后台发货：1 已支付 → 2 已发货 */
    @Transactional
    public void ship(long orderId) {
        requireOrder(orderId);
        transition(orderId, Order.STATUS_PAID, Order.STATUS_SHIPPED,
                "只有已支付(待发货)的订单可以发货",
                wrapper -> wrapper.set(Order::getShipTime, LocalDateTime.now()));
    }

    /** 后台关闭订单：0/1 → 5 已关闭，并把库存加回去 */
    @Transactional
    public void close(long orderId) {
        requireOrder(orderId);
        LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .in(Order::getStatus, Order.STATUS_UNPAID, Order.STATUS_PAID)
                .set(Order::getStatus, Order.STATUS_CLOSED);
        if (orderMapper.update(null, wrapper) == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ILLEGAL, "只有待支付或已支付的订单可以关闭");
        }
        restoreStockOf(orderId);
    }

    /** 后台看订单详情，不校验归属 */
    public OrderDetailVO detailForAdmin(long orderId) {
        return OrderDetailVO.of(requireOrder(orderId), itemsOf(orderId));
    }

    // ------------------------------------------------------------------ 内部工具

    /**
     * 状态流转统一走「带条件的 UPDATE」：
     * {@code UPDATE orders SET status = ? WHERE id = ? AND status = ?}
     *
     * <p>影响行数为 0 就说明状态已经被别的请求改掉了（比如并发点了两次取消、重复发货），
     * 直接报错。这样不用「先 SELECT 判断再 UPDATE」，天然避免了并发下的重复流转。
     */
    private void transition(long orderId, int fromStatus, int toStatus,
                            String failMessage, Consumer<LambdaUpdateWrapper<Order>> extraSet) {
        LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, fromStatus)
                .set(Order::getStatus, toStatus);
        extraSet.accept(wrapper);
        if (orderMapper.update(null, wrapper) == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ILLEGAL, failMessage);
        }
    }

    /**
     * 扣库存。
     * 虽然外面已经持有行锁了，这里仍然把条件写进 SQL（stock >= n），
     * 数据库层再兜一道底，任何情况下都不会把库存扣成负数。
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

    /** 取消 / 关闭订单时把明细里的数量加回库存 */
    private void restoreStockOf(long orderId) {
        for (OrderItem item : itemsOf(orderId)) {
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, item.getProductId())
                    .setSql("stock = stock + " + item.getQuantity()));
        }
    }

    private List<OrderItem> itemsOf(long orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    /** 查订单并校验归属，防止拿到别人的订单 id 越权访问 */
    private Order requireOwnOrder(long userId, long orderId) {
        Order order = orderMapper.selectById(orderId);
        BizAssert.isTrue(order != null && order.getUserId() == userId, ErrorCode.ORDER_NOT_FOUND);
        return order;
    }

    private Order requireOrder(long orderId) {
        return BizAssert.notNull(orderMapper.selectById(orderId), ErrorCode.ORDER_NOT_FOUND);
    }
}
