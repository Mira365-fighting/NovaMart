package com.example.novamart.controller;

import com.example.novamart.common.result.ApiResult;
import com.example.novamart.service.ProductService;
import com.example.novamart.vo.PageResult;
import com.example.novamart.vo.ProductVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 前台商品接口，全部公开（不需要 Token）。后台的商品管理在 /api/admin/product/**。 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 分页浏览在售商品。
     * 所有列表接口统一用 pageNum / pageSize 两个分页参数（由 PageHelper 实现）。
     */
    @GetMapping("/page")
    public ApiResult<PageResult<ProductVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String category) {
        return ApiResult.success(productService.page(pageNum, pageSize, keyword, category));
    }

    /** 分类列表，给前台分类筛选用。注意这条要放在 /{id} 之前理解：字面量路径优先级更高。 */
    @GetMapping("/categories")
    public ApiResult<List<String>> categories() {
        return ApiResult.success(productService.categories());
    }

    /** 商品详情（走 Redis 缓存） */
    @GetMapping("/{id}")
    public ApiResult<ProductVO> detail(@PathVariable Long id) {
        return ApiResult.success(productService.detail(id));
    }
}
