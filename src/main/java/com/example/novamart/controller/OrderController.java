package com.example.novamart.controller;

import com.example.novamart.common.result.ApiResult;
import com.example.novamart.interceptor.JwtAuthenticationInterceptor;
import com.example.novamart.service.OrderService;
import com.example.novamart.vo.OrderDetailVO;
import com.example.novamart.vo.OrderVO;
import com.example.novamart.vo.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 订单接口，全部需要登录（拦截器拦 /api/order/**）。 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 从购物车提交订单：扣库存、生成订单、清空购物车 */
    @PostMapping("/submit")
    public ApiResult<OrderDetailVO> submit(
            @RequestAttribute(JwtAuthenticationInterceptor.USER_ID_ATTRIBUTE) long userId) {
        return ApiResult.success(orderService.submit(userId));
    }

    /** 我的订单分页 */
    @GetMapping("/page")
    public ApiResult<PageResult<OrderVO>> page(
            @RequestAttribute(JwtAuthenticationInterceptor.USER_ID_ATTRIBUTE) long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.success(orderService.page(userId, pageNum, pageSize));
    }

    /** 订单详情（含明细） */
    @GetMapping("/{id}")
    public ApiResult<OrderDetailVO> detail(
            @RequestAttribute(JwtAuthenticationInterceptor.USER_ID_ATTRIBUTE) long userId,
            @PathVariable long id) {
        return ApiResult.success(orderService.detail(userId, id));
    }

    /** 取消订单（仅待支付可取消，并回滚库存） */
    @PutMapping("/{id}/cancel")
    public ApiResult<Void> cancel(
            @RequestAttribute(JwtAuthenticationInterceptor.USER_ID_ATTRIBUTE) long userId,
            @PathVariable long id) {
        orderService.cancel(userId, id);
        return ApiResult.success();
    }
}
