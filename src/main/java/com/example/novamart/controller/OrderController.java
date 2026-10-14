package com.example.novamart.controller;

import com.example.novamart.common.result.ApiResult;
import com.example.novamart.security.UserContext;
import com.example.novamart.service.OrderService;
import com.example.novamart.vo.OrderDetailVO;
import com.example.novamart.vo.OrderVO;
import com.example.novamart.vo.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台订单接口，全部需要登录。
 *
 * <p>状态流转只开放「用户能自己做的」三件事：支付、取消、确认收货。
 * 发货和关闭是后台的操作，在 /api/admin/order/**。
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 从购物车提交订单：扣库存、生成订单、清空购物车，整体一个事务 */
    @PostMapping("/submit")
    public ApiResult<OrderDetailVO> submit() {
        return ApiResult.success(orderService.submit(UserContext.requireUserId()));
    }

    /** 我的订单分页，可按状态筛选 */
    @GetMapping("/page")
    public ApiResult<PageResult<OrderVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) Integer status) {
        return ApiResult.success(orderService.page(UserContext.requireUserId(), status, pageNum, pageSize));
    }

    /** 订单详情（含明细） */
    @GetMapping("/{id}")
    public ApiResult<OrderDetailVO> detail(@PathVariable long id) {
        return ApiResult.success(orderService.detail(UserContext.requireUserId(), id));
    }

    /** 支付（模拟）：0 待支付 → 1 已支付 */
    @PutMapping("/{id}/pay")
    public ApiResult<Void> pay(@PathVariable long id) {
        orderService.pay(UserContext.requireUserId(), id);
        return ApiResult.success();
    }

    /** 取消订单：0 待支付 → 4 已取消，回滚库存 */
    @PutMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable long id) {
        orderService.cancel(UserContext.requireUserId(), id);
        return ApiResult.success();
    }

    /** 确认收货：2 已发货 → 3 已完成 */
    @PutMapping("/{id}/confirm")
    public ApiResult<Void> confirm(@PathVariable long id) {
        orderService.confirm(UserContext.requireUserId(), id);
        return ApiResult.success();
    }
}
