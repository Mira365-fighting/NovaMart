package com.example.novamart.common.exception;

import com.example.novamart.common.result.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器。
 *
 * <p>有了它，Controller 和 Service 里一行 try-catch 都不用写，
 * 抛什么异常就返回什么格式的 JSON。分五层兜：
 * <ol>
 *   <li>业务异常：HTTP 状态码取 ErrorCode 里配的，业务码原样返回</li>
 *   <li>权限不足（{@code @PreAuthorize} 校验失败）→ 403</li>
 *   <li>参数类异常：@Valid 校验失败、JSON 解析不了、路径参数类型不对 → 400</li>
 *   <li>数据库唯一键冲突 → 409</li>
 *   <li>其它未预料的异常 → 500，只回通用文案，不把堆栈暴露给前端</li>
 * </ol>
 *
 * <p>注意第 2 条必须显式写：{@code AccessDeniedException} 是 RuntimeException，
 * 不单独处理的话会被最后的 Exception 兜底吞成 500，前端就看不到 403 了。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：HTTP 状态码跟 ErrorCode 走，响应体统一 ApiResult */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResult.error(errorCode.getCode(), exception.getMessage()));
    }

    /**
     * 权限不足。
     *
     * <p>「没登录」在过滤器层就被拦下（走 RestSecurityHandlers 返 401），
     * 能走到这里的是「登录了但角色不够」——比如普通用户访问后台接口。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiResult.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
    }

    /** 参数问题统一返回 400，取第一条校验失败的提示给前端 */
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResult<Void>> handleBadRequest(Exception exception) {
        String message = exception instanceof MethodArgumentNotValidException validationException
                ? validationException.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage()).orElse("参数错误")
                : "参数错误";
        return ResponseEntity.badRequest()
                .body(ApiResult.error(ErrorCode.INVALID_ARGUMENT.getCode(), message));
    }

    /** 数据库唯一索引冲突（用户名/手机号重复等）→ 409 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResult<Void>> handleDuplicateKey(DuplicateKeyException exception) {
        log.warn("唯一键冲突：{}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.error(ErrorCode.CONFLICT.getCode(), ErrorCode.CONFLICT.getMessage()));
    }

    /** 兜底：没预料到的异常记日志，对外只说「系统异常」 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception exception) {
        log.error("未处理的异常", exception);
        return ResponseEntity.internalServerError()
                .body(ApiResult.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
