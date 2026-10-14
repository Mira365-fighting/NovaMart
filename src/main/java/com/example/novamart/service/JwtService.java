package com.example.novamart.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.novamart.common.exception.BusinessException;
import com.example.novamart.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;

/**
 * JWT 签发与校验。
 *
 * <p>Token 是无状态的：登录成功后前端拿到 Token，在有效期内带 Token 访问即可，
 * 不用再输账号密码，服务端也不用存 session。
 */
@Service
public class JwtService {

    private final Algorithm signingAlgorithm;
    private final JWTVerifier verifier;
    private final long expirationMs;

    public JwtService(@Value("${novamart.jwt.secret}") String secret,
                      @Value("${novamart.jwt.expiration}") long expirationMs) {
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
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("novamart.jwt.expiration 必须为正数");
        }

        this.signingAlgorithm = Algorithm.HMAC256(keyBytes);
        this.verifier = JWT.require(signingAlgorithm).build();
        this.expirationMs = expirationMs;
    }

    /** 签发 Token：subject 存 userId，exp = 当前时间 + 有效期 */
    public String createToken(long userId) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(Long.toString(userId))
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(expirationMs))
                .sign(signingAlgorithm);
    }

    /** 校验 Token 并取出 userId；签名错误或已过期统一抛「未登录」 */
    public long parseUserId(String token) {
        try {
            DecodedJWT decoded = verifier.verify(token);
            return Long.parseLong(decoded.getSubject());
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /** Token 有效期（秒），返回给前端 */
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
