package com.example.novamart.controller;

import com.example.novamart.common.result.ApiResult;
import com.example.novamart.dto.CartItemDTO;
import com.example.novamart.dto.CartMergeDTO;
import com.example.novamart.security.UserContext;
import com.example.novamart.service.CartService;
import com.example.novamart.vo.CartVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口，全部需要登录（SecurityConfiguration 里 /api/cart/** 要求 authenticated）。
 *
 * <p>userId 从 {@link UserContext}（拦截器写入的 ThreadLocal）取，前端不传也传不了。
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
    public ApiResult<List<CartVO>> list() {
        return ApiResult.success(cartService.list(UserContext.requireUserId()));
    }

    /** 加入购物车：已有该商品则累加数量 */
    @PostMapping("/add")
    public ApiResult<List<CartVO>> add(@Valid @RequestBody CartItemDTO dto) {
        return ApiResult.success(cartService.add(UserContext.requireUserId(), dto));
    }

    /** 修改数量（覆盖为指定值，不是累加） */
    @PutMapping("/update")
    public ApiResult<List<CartVO>> update(@Valid @RequestBody CartItemDTO dto) {
        return ApiResult.success(cartService.updateQuantity(UserContext.requireUserId(), dto));
    }

    /** 删除购物车中的某个商品 */
    @DeleteMapping("/{productId}")
    public ApiResult<List<CartVO>> remove(@PathVariable long productId) {
        return ApiResult.success(cartService.remove(UserContext.requireUserId(), productId));
    }

    /**
     * 清空购物车。路径不带子资源，DELETE /api/cart 就是对「购物车」整体删除。
     */
    @DeleteMapping
    public ApiResult<Void> clear() {
        cartService.clear(UserContext.requireUserId());
        return ApiResult.success();
    }

    /**
     * 合并「未登录期间的本地临时购物车」。
     * 登录接口本身也能合并（请求体带 cartItems），这个接口是给「已经登录着、
     * 但浏览器里还留着旧本地购物车」的场景兜底用的。
     */
    @PostMapping("/merge")
    public ApiResult<List<CartVO>> merge(@Valid @RequestBody CartMergeDTO dto) {
        return ApiResult.success(cartService.merge(UserContext.requireUserId(), dto.getItems()));
    }
}
