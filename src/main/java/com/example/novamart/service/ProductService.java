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
import java.util.function.Function;
import java.util.stream.Collectors;

/** 商品浏览。这两个接口不需要登录。 */
@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * 分页浏览在售商品，keyword 同时匹配商品名和分类，可不传。
     *
     * <p>分页交给 PageHelper：startPage 之后的第一条查询会被自动改写加上 LIMIT，
     * 并额外执行一次 COUNT，所以不用再手写 count 语句和 offset。
     */
    public PageResult<ProductVO> page(int pageNum, int pageSize, String keyword) {
        PageUtil.start(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, Product.STATUS_ON_SALE)
                // 有关键字才拼这段条件，括号保证 OR 不会污染前面的 status 判断
                .and(StringUtils.hasText(keyword),
                        condition -> condition.like(Product::getName, keyword)
                                .or().like(Product::getCategory, keyword))
                .orderByAsc(Product::getId);

        List<Product> products = productMapper.selectList(wrapper);
        return PageResult.of(new PageInfo<>(products), ProductVO::of);
    }

    /** 商品详情 */
    public ProductVO detail(Long id) {
        return ProductVO.of(requireOnSale(id));
    }

    /** 取在售商品，不存在或已下架直接抛异常；购物车模块也复用这个校验。 */
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
