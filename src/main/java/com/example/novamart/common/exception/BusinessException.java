package com.example.novamart.common.exception;

/**
 * 业务异常。
 *
 * <p>继承 RuntimeException（非受检异常），业务代码里直接 {@code throw new BusinessException(...)}，
 * 不需要每一层 try-catch，最终由 GlobalExceptionHandler 统一转成 ApiResult。
 *
 * <p>携带结构化的 {@link ErrorCode}，而不是散落在各处的魔法字符串。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 直接用错误码自带的提示 */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 同一个错误码在不同场景需要更具体的提示时用这个 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
