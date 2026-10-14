package com.example.novamart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.novamart.common.exception.BusinessException;
import com.example.novamart.common.exception.ErrorCode;
import com.example.novamart.dto.CartItemDTO;
import com.example.novamart.entity.CartItem;
import com.example.novamart.entity.Product;
import com.example.novamart.mapper.CartMapper;
import com.example.novamart.mapper.ProductMapper;
import com.example.novamart.vo.CartVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 购物车。
 *
 * <p>MySQL 是唯一数据源，不做缓存，读写都直接落库，逻辑保持最简。
 * 所有方法都要求 userId，而这个 userId 来自 Token（过滤器校验后放进 ThreadLocal），前端无法伪造。
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ProductService productService;

    public CartService(CartMapper cartMapper, ProductMapper productMapper, ProductService productService) {
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.productService = productService;
    }

    /** 查看购物车，返回带商品信息的列表 */
    public List<CartVO> list(long userId) {
        List<CartItem> items = cartMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
        return assemble(items);
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
        save(userId, dto.getProductId(), existing, targetQuantity);
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

    /** 删除购物车中的某一行（本来就没有则报错提示） */
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

    /**
     * 合并「未登录期间的本地购物车」。
     *
     * <p>用户没登录时购物车存在浏览器 localStorage，登录时一起提交上来，
     * 这里按「同商品数量相加」并入数据库，并统一受库存上限约束。
     *
     * <p>两条容错原则（本地那份可能存了好几天，不能因为一行脏数据就让登录失败）：
     * <ul>
     *   <li>商品已删除 / 已下架 / 无库存 → 跳过这一行；</li>
     *   <li>数量累加超过库存 → 取库存上限。</li>
     * </ul>
     *
     * @return 合并后的完整购物车
     */
    @Transactional
    public List<CartVO> merge(long userId, List<CartItemDTO> items) {
        for (CartItemDTO dto : items) {
            Product product = productMapper.selectById(dto.getProductId());
            if (product == null || product.getStatus() == null
                    || product.getStatus() != Product.STATUS_ON_SALE) {
                log.debug("合并购物车：跳过已下架或不存在的商品 productId={}", dto.getProductId());
                continue;
            }

            int stock = product.getStock() == null ? 0 : product.getStock();
            if (stock <= 0) {
                log.debug("合并购物车：跳过无库存的商品 productId={}", dto.getProductId());
                continue;
            }

            CartItem existing = findByUserAndProduct(userId, dto.getProductId());
            int mergedQuantity = (existing == null ? 0 : existing.getQuantity()) + dto.getQuantity();
            save(userId, dto.getProductId(), existing, Math.min(mergedQuantity, stock));
        }
        return list(userId);
    }

    /** 新增或更新购物车行，insert / update 的分支只写在这里 */
    private void save(long userId, long productId, CartItem existing, int quantity) {
        if (existing == null) {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            cartMapper.insert(item);
        } else {
            existing.setQuantity(quantity);
            cartMapper.updateById(existing);
        }
    }

    /** 购物车行 + 商品信息组装成 VO；商品已被删除的历史行直接跳过 */
    private List<CartVO> assemble(List<CartItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        // 一次把所有商品查出来，避免逐行查库
        List<Long> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<Long, Product> productById = productService.mapByIds(productIds);

        return items.stream()
                .filter(item -> productById.containsKey(item.getProductId()))
                .map(item -> CartVO.of(item, productById.get(item.getProductId())))
                .toList();
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
