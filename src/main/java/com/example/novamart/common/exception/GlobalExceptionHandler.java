package com.example.novamart.common.exception;

import com.example.novamart.common.result.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.RedisConnectionFailureException;
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

    /**
     * Redis 不可用 → 503。
     *
     * <p>和 v2.0 的处理方式不同：那时候所有 Redis 调用点都自己 catch 掉了，
     * 请求照常返回。v3.0 的秒杀链路里，「Redis 挂了」是<b>必须让调用方知道</b>的信号 ——
     * 因为库存就在 Redis 上，读不到库存就不能放请求过去。
     * 所以 SeckillStockService 会把它翻译成 {@code REDIS_UNAVAILABLE} 交给业务分支，
     * 而这里兜的是「有人忘了处理、直接让异常冒出来」的情况。
     *
     * <p>返回 503 而不是 500：这是「暂时不可用、重试可能就好」，不是代码 bug，
     * 网关和监控看到 503 会按可用性问题处理（比如触发重试或熔断），比 500 更准确。
     */
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ApiResult<Void>> handleRedisUnavailable(RedisConnectionFailureException exception) {
        log.error("Redis 不可用：{}", exception.getMessage());
        return ResponseEntity.status(ErrorCode.SERVICE_UNAVAILABLE.getHttpStatus())
                .body(ApiResult.error(ErrorCode.SERVICE_UNAVAILABLE.getCode(),
                        ErrorCode.SERVICE_UNAVAILABLE.getMessage()));
    }

    /** RabbitMQ 不可用 → 503。原因同上：属于「暂时不可用」，不是业务错误 */
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiResult<Void>> handleAmqpUnavailable(AmqpException exception) {
        log.error("RabbitMQ 不可用：{}", exception.getMessage());
        return ResponseEntity.status(ErrorCode.SERVICE_UNAVAILABLE.getHttpStatus())
                .body(ApiResult.error(ErrorCode.SERVICE_UNAVAILABLE.getCode(),
                        ErrorCode.SERVICE_UNAVAILABLE.getMessage()));
    }

    /** 兜底：没预料到的异常记日志，对外只说「系统异常」 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception exception) {
        log.error("未处理的异常", exception);
        return ResponseEntity.internalServerError()
                .body(ApiResult.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
