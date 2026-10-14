package com.example.novamart.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.novamart.common.exception.BusinessException;
import com.example.novamart.common.exception.ErrorCode;
import com.example.novamart.entity.User;
import com.example.novamart.security.JwtPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * JWT 签发与校验：一次登录发两个 Token。
 *
 * <ul>
 *   <li><b>accessToken</b> 短（默认 2 小时）—— 每个请求带着它，泄露了影响面也小；</li>
 *   <li><b>refreshToken</b> 长（默认 7 天）—— 只在 accessToken 过期后拿去换新的一对 Token，
 *       用户不用重新输账号密码。</li>
 * </ul>
 *
 * <p>两个 Token 靠 {@code typ} 声明区分，校验器也绑定了 typ，
 * 所以拿 refreshToken 当 accessToken 用会直接校验失败，反之亦然。
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final Algorithm algorithm;
    /** 校验器自带 typ 判断，避免两个 Token 混用 */
    private final JWTVerifier accessVerifier;
    private final JWTVerifier refreshVerifier;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(@Value("${novamart.jwt.secret}") String secret,
                      @Value("${novamart.jwt.access-expiration}") long accessExpirationMs,
                      @Value("${novamart.jwt.refresh-expiration}") long refreshExpirationMs) {
        // 密钥必须是 Base64 编码的、长度至少 256bit（32 字节）
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("novamart.jwt.secret 必须是 Base64 编码", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("novamart.jwt.secret 解码后至少需要 256bit");
        }
        if (accessExpirationMs <= 0 || refreshExpirationMs <= 0) {
            throw new IllegalArgumentException("Token 有效期必须为正数");
        }
        if (refreshExpirationMs <= accessExpirationMs) {
            throw new IllegalArgumentException("refreshToken 有效期必须长于 accessToken");
        }

        this.algorithm = Algorithm.HMAC256(keyBytes);
        this.accessVerifier = JWT.require(algorithm).withClaim(CLAIM_TYPE, TYPE_ACCESS).build();
        this.refreshVerifier = JWT.require(algorithm).withClaim(CLAIM_TYPE, TYPE_REFRESH).build();
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /** 登录 / 刷新时签发一对 Token */
    public TokenPair createTokens(User user) {
        Instant now = Instant.now();
        return new TokenPair(
                create(user, TYPE_ACCESS, now, accessExpirationMs),
                create(user, TYPE_REFRESH, now, refreshExpirationMs));
    }

    /** 校验 accessToken，签名不对 / 已过期 / 类型不对都抛「未登录」 */
    public JwtPayload parseAccess(String token) {
        return parse(token, accessVerifier, ErrorCode.UNAUTHORIZED);
    }

    /** 校验 refreshToken，失败时提示「登录状态已失效，请重新登录」 */
    public JwtPayload parseRefresh(String token) {
        return parse(token, refreshVerifier, ErrorCode.REFRESH_TOKEN_INVALID);
    }

    /** accessToken 有效期（秒），返回给前端 */
    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    private String create(User user, String type, Instant now, long ttlMs) {
        return JWT.create()
                .withSubject(Long.toString(user.getId()))
                // jti 是退出登录时写黑名单的键
                .withJWTId(UUID.randomUUID().toString())
                .withClaim(CLAIM_TYPE, type)
                .withClaim(CLAIM_ROLE, user.getRole())
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(ttlMs))
                .sign(algorithm);
    }

    private JwtPayload parse(String token, JWTVerifier verifier, ErrorCode errorCode) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            return new JwtPayload(Long.parseLong(jwt.getSubject()),
                    jwt.getClaim(CLAIM_ROLE).asString(),
                    jwt.getId(),
                    jwt.getExpiresAtAsInstant());
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            throw new BusinessException(errorCode);
        }
    }

    /** 一对 Token。accessToken 给每个请求用，refreshToken 只用来换新 Token。 */
    public record TokenPair(String accessToken, String refreshToken) {
    }
}
