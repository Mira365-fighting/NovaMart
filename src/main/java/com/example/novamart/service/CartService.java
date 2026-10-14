package com.example.novamart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.novamart.common.exception.BusinessException;
import com.example.novamart.common.exception.ErrorCode;
import com.example.novamart.dto.CartItemDTO;
import com.example.novamart.entity.CartItem;
import com.example.novamart.entity.Product;
import com.example.novamart.mapper.CartMapper;
import com.example.novamart.vo.CartVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 购物车。
 *
 * <p>MySQL 是唯一数据源，不做缓存，读写都直接落库，逻辑保持最简。
 * 所有方法都要求 userId，而这个 userId 来自 Token（拦截器解析后写入 request），前端无法伪造。
 */
@Service
public class CartService {

    private final CartMapper cartMapper;
    private final ProductService productService;

    public CartService(CartMapper cartMapper, ProductService productService) {
        this.cartMapper = cartMapper;
        this.productService = productService;
    }

    /** 查看购物车，返回带商品信息的列表 */
    public List<CartVO> list(long userId) {
        List<CartItem> items = cartMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
        if (items.isEmpty()) {
            return List.of();
        }

        // 一次把所有商品查出来，避免逐行查库
        List<Long> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<Long, Product> productById = productService.mapByIds(productIds);

        // 商品已被删除的历史行直接跳过
        return items.stream()
                .filter(item -> productById.containsKey(item.getProductId()))
                .map(item -> CartVO.of(item, productById.get(item.getProductId())))
                .toList();
    }

    /**
     * 加入购物车：已经在车里就累加数量，否则新增一行。
     * 写操作返回更新后的整份购物车，前端直接渲染，省一次请求。
     */
    @Transactional
    public List<CartVO> add(long userId, CartItemDTO dto) {
        Product product = productService.requireOnSale(dto.getProductId());
        CartItem existing = findByUserAndProduct(userId, dto.getProductId());

        int targetQuantity = (existing == null ? 0 : existing.getQuantity()) + dto.getQuantity();
        requireEnoughStock(product, targetQuantity);

        if (existing == null) {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(product.getId());
            item.setQuantity(targetQuantity);
            cartMapper.insert(item);
        } else {
            existing.setQuantity(targetQuantity);
            cartMapper.updateById(existing);
        }
        return list(userId);
    }

    /** 把某一行改成指定数量（覆盖，不是累加） */
    @Transactional
    public List<CartVO> updateQuantity(long userId, CartItemDTO dto) {
        Product product = productService.requireOnSale(dto.getProductId());
        CartItem existing = findByUserAndProduct(userId, dto.getProductId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.CART_NOT_FOUND);
        }
        requireEnoughStock(product, dto.getQuantity());

        existing.setQuantity(dto.getQuantity());
        cartMapper.updateById(existing);
        return list(userId);
    }

    /** 删除购物车中的某一行（幂等：本来就没有则报错提示） */
    @Transactional
    public List<CartVO> remove(long userId, long productId) {
        int deleted = cartMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId));
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.CART_NOT_FOUND);
        }
        return list(userId);
    }

    /** 清空购物车。删 0 行也算成功，天然幂等。 */
    @Transactional
    public void clear(long userId) {
        cartMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
    }

    /** (user_id, product_id) 上有唯一索引，所以最多只会有一行 */
    private CartItem findByUserAndProduct(long userId, long productId) {
        return cartMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId));
    }

    /** 下单前的库存校验，避免把明显超卖的数量放进购物车 */
    private static void requireEnoughStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "「" + product.getName() + "」库存不足，仅剩 " + product.getStock() + " 件");
        }
    }
}
