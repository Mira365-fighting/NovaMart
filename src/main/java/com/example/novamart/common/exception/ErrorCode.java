package com.example.novamart.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。
 *
 * <p>每个错误码带上对应的 HTTP 状态码：HTTP 状态码只表达传输层结果（200/400/401/404/409/500），
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

    // 认证类
    UNAUTHORIZED(40101, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),

    // 冲突类
    CONFLICT(40901, "数据冲突", HttpStatus.CONFLICT),

    // 系统类
    INTERNAL_ERROR(50001, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR);

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
