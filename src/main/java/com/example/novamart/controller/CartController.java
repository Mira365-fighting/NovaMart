package com.example.novamart.controller;

import com.example.novamart.common.result.ApiResult;
import com.example.novamart.dto.CartItemDTO;
import com.example.novamart.interceptor.JwtAuthenticationInterceptor;
import com.example.novamart.service.CartService;
import com.example.novamart.vo.CartVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口，全部需要登录（由 JwtAuthenticationInterceptor 拦 /api/cart/**）。
 *
 * <p>userId 一律从 Token 解析后写进 request 的 attribute 里取，前端不传也传不了。
 * 增删改都返回更新后的完整购物车，前端可以直接渲染，省掉一次查询请求。
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** 购物车列表 */
    @GetMapping("/list")
    public ApiResult<List<CartVO>> list(
            @RequestAttribute(JwtAuthenticationInterceptor.USER_ID_ATTRIBUTE) long userId) {
        return ApiResult.success(cartService.list(userId));
    }

    /** 加入购物车：已有该商品则累加数量 */
    @PostMapping("/add")
    public ApiResult<List<CartVO>> add(
            @RequestAttribute(JwtAuthenticationInterceptor.USER_ID_ATTRIBUTE) long userId,
            @Valid @RequestBody CartItemDTO dto) {
        return ApiResult.success(cartService.add(userId, dto));
    }

    /** 修改数量（覆盖为指定值，不是累加） */
    @PutMapping("/update")
    public ApiResult<List<CartVO>> update(
            @RequestAttribute(JwtAuthenticationInterceptor.USER_ID_ATTRIBUTE) long userId,
            @Valid @RequestBody CartItemDTO dto) {
        return ApiResult.success(cartService.updateQuantity(userId, dto));
    }

    /** 删除购物车中的某个商品 */
    @DeleteMapping("/{productId}")
    public ApiResult<List<CartVO>> remove(
            @RequestAttribute(JwtAuthenticationInterceptor.USER_ID_ATTRIBUTE) long userId,
            @PathVariable long productId) {
        return ApiResult.success(cartService.remove(userId, productId));
    }

    /** 清空购物车。路径不带子资源，DELETE /api/cart 就是对「购物车」整体删除。 */
    @DeleteMapping
    public ApiResult<Void> clear(
            @RequestAttribute(JwtAuthenticationInterceptor.USER_ID_ATTRIBUTE) long userId) {
        cartService.clear(userId);
        return ApiResult.success();
    }
}
