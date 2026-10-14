package com.example.novamart.common.result;

/**
 * 统一响应体：所有接口都返回 {@code {code, message, data}}，前端只需要认这一种格式。
 *
 * <p>用 record 代替普通 POJO —— 编译器自动生成全参构造、getter（同名方法）、equals、hashCode、toString，
 * 这种「只存数据没有行为」的结构正合适。
 *
 * @param code    0 表示成功，非 0 是业务错误码（见 {@link com.example.novamart.common.exception.ErrorCode}）
 * @param message 提示信息，成功固定为 success
 * @param data    业务数据，没有就是 null
 */
public record ApiResult<T>(int code, String message, T data) {

    /** 成功码，和 ErrorCode.SUCCESS 保持一致 */
    private static final int SUCCESS_CODE = 0;

    /** 成功并携带数据 */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(SUCCESS_CODE, "success", data);
    }

    /** 成功但没有返回数据 */
    public static <T> ApiResult<T> success() {
        return new ApiResult<>(SUCCESS_CODE, "success", null);
    }

    /** 失败，携带业务错误码和提示 */
    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
