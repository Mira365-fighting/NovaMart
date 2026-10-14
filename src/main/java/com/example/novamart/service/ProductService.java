package com.example.novamart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.novamart.common.exception.BusinessException;
import com.example.novamart.common.exception.ErrorCode;
import com.example.novamart.entity.Product;
import com.example.novamart.mapper.ProductMapper;
import com.example.novamart.util.PageUtil;
import com.example.novamart.vo.PageResult;
import com.example.novamart.vo.ProductVO;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 前台商品浏览（公开接口，不需要登录）。
 *
 * <p>详情走 Redis 缓存，列表不走 —— 列表条件组合多、命中率低，缓存它性价比不高。
 */
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductCacheService productCacheService;

    public ProductService(ProductMapper productMapper, ProductCacheService productCacheService) {
        this.productMapper = productMapper;
        this.productCacheService = productCacheService;
    }

    /**
     * 分页浏览在售商品。
     *
     * <p>分页交给 PageHelper：startPage 之后的第一条查询会被自动改写加上 LIMIT，
     * 并额外执行一次 COUNT，所以不用再手写 count 语句和 offset。
     *
     * @param keyword  按商品名模糊匹配，可不传
     * @param category 按分类精确匹配，可不传
     */
    public PageResult<ProductVO> page(int pageNum, int pageSize, String keyword, String category) {
        PageUtil.start(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, Product.STATUS_ON_SALE)
                .like(StringUtils.hasText(keyword), Product::getName, keyword)
                .eq(StringUtils.hasText(category), Product::getCategory, category)
                .orderByAsc(Product::getId);

        List<Product> products = productMapper.selectList(wrapper);
        return PageResult.of(new PageInfo<>(products), ProductVO::of);
    }

    /** 分类列表，给前台的分类筛选用 */
    public List<String> categories() {
        return productMapper.selectCategories();
    }

    /**
     * 商品详情，带缓存。
     *
     * <p>流程：查缓存 → 命中直接返回 / 命中空值缓存说明确实不存在（直接抛，挡住穿透）
     * → 都没命中就回源数据库，然后写缓存（查不到则写空值缓存）。
     */
    public ProductVO detail(Long id) {
        Optional<ProductVO> cached = productCacheService.get(id);
        if (cached.isPresent()) {
            return cached.get();
        }

        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != Product.STATUS_ON_SALE) {
            // 不存在的商品也缓存起来，避免有人拿随机 id 一直刷数据库
            productCacheService.putNull(id);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        ProductVO productVO = ProductVO.of(product);
        productCacheService.put(productVO);
        return productVO;
    }

    /** 取在售商品，不存在或已下架直接抛异常；购物车模块也复用这个校验 */
    public Product requireOnSale(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != Product.STATUS_ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    /** 按 id 批量取商品，返回 id -> 商品 的映射，供购物车一次性组装数据 */
    public Map<Long, Product> mapByIds(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productMapper.selectByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
