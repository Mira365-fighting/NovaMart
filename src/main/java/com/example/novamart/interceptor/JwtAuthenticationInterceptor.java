package com.example.novamart.interceptor;

import com.example.novamart.common.exception.BusinessException;
import com.example.novamart.common.exception.ErrorCode;
import com.example.novamart.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器。
 *
 * <p>从请求头取 {@code Authorization: Bearer <token>}，校验通过后把 userId 塞进 request attribute，
 * Controller 用 {@code @RequestAttribute} 取出来用。前端传不了 userId，天然防越权。
 *
 * <p>校验失败直接抛 BusinessException —— 拦截器里抛出的异常同样会被
 * GlobalExceptionHandler 接住（DispatcherServlet 会走统一的异常解析器），
 * 所以这里不用手动往 response 里写 JSON，代码能短不少。
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    /** 校验通过后写入 request 的属性名，Controller 依赖它拿当前登录用户 */
    public static final String USER_ID_ATTRIBUTE = "userId";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        // 没有 Token 或不带 Bearer 前缀，视为未登录
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // 签名不对或已过期，JwtService 内部统一抛 UNAUTHORIZED
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        request.setAttribute(USER_ID_ATTRIBUTE, jwtService.parseUserId(token));
        return true;
    }
}
