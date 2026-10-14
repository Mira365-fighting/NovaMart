package com.example.novamart.vo;

import com.example.novamart.entity.User;

/**
 * 登录成功后的返回体。
 *
 * @param token     签好名的 JWT，之后每个请求放在 {@code Authorization: Bearer <token>} 里
 * @param tokenType 固定 Bearer，前端不用硬编码
 * @param expiresIn 有效期（秒）
 */
public record LoginVO(Long userId, String username, String token, String tokenType, Long expiresIn) {

    public static LoginVO of(User user, String token, long expiresInSeconds) {
        return new LoginVO(user.getId(), user.getUsername(), token, "Bearer", expiresInSeconds);
    }
}
