package com.example.novamart.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。
 *
 * <p>每个错误码带上对应的 HTTP 状态码：HTTP 状态码只表达传输层结果（200/400/401/403/404/409/500），
 * 业务区分交给 code，这样网关、监控、浏览器都能正确识别，前端也能精确判断。
 */
public enum ErrorCode {

    // 成功
    SUCCESS(0, "success", HttpStatus.OK),

    // 参数类
    INVALID_ARGUMENT(40001, "参数错误", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(40002, "参数校验失败", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(40003, "用户不存在", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(40004, "用户名已存在", HttpStatus.CONFLICT),
    PRODUCT_NOT_FOUND(40005, "商品不存在或已下架", HttpStatus.NOT_FOUND),
    CART_NOT_FOUND(40006, "购物车中不存在该商品", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(40007, "订单不存在", HttpStatus.NOT_FOUND),
    ORDER_STATUS_ILLEGAL(40008, "当前订单状态不允许该操作", HttpStatus.CONFLICT),

    // 秒杀类（v3.0）
    SECKILL_NOT_FOUND(40009, "秒杀活动不存在", HttpStatus.NOT_FOUND),
    SECKILL_NOT_STARTED(40010, "秒杀还没开始，请稍候", HttpStatus.CONFLICT),
    SECKILL_ENDED(40011, "秒杀已结束", HttpStatus.CONFLICT),
    SECKILL_SOLD_OUT(40012, "手慢了，商品已被抢光", HttpStatus.CONFLICT),
    SECKILL_DUPLICATE(40013, "您已经抢到过了，每人限购 1 件", HttpStatus.CONFLICT),
    SECKILL_ORDER_NOT_FOUND(40014, "秒杀订单不存在", HttpStatus.NOT_FOUND),
    SECKILL_ORDER_STATUS_ILLEGAL(40015, "当前秒杀订单状态不允许该操作", HttpStatus.CONFLICT),
    SECKILL_NOT_READY(40016, "秒杀活动正在准备中，请稍后重试", HttpStatus.CONFLICT),

    // 认证类
    UNAUTHORIZED(40101, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    USER_DISABLED(40102, "账号已被禁用，请联系管理员", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(40103, "登录状态已失效，请重新登录", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED(40104, "登录状态已失效，请重新登录", HttpStatus.UNAUTHORIZED),

    // 授权类
    FORBIDDEN(40301, "无权限访问", HttpStatus.FORBIDDEN),

    // 冲突类
    CONFLICT(40901, "数据冲突", HttpStatus.CONFLICT),

    // 系统类
    INTERNAL_ERROR(50001, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR),
    /** Redis / RabbitMQ 这类外部依赖不可用：不是业务错，重试可能就好 */
    SERVICE_UNAVAILABLE(50301, "服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
