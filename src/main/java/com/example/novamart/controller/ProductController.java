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

/** 商品接口，公开访问（不需要 Token）。 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** 分页浏览商品，keyword 可选，同时匹配商品名和分类 */
    @GetMapping("/page")
    public ApiResult<PageResult<ProductVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) String keyword) {
        return ApiResult.success(productService.page(pageNum, pageSize, keyword));
    }

    /** 商品详情 */
    @GetMapping("/{id}")
    public ApiResult<ProductVO> detail(@PathVariable Long id) {
        return ApiResult.success(productService.detail(id));
    }
}
