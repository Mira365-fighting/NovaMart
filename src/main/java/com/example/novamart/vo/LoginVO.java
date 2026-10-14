package com.example.novamart.vo;

import com.example.novamart.entity.User;

/**
 * 登录 / 刷新 Token 成功后的返回体。
 *
 * @param userId       用户 id
 * @param username     登录名
 * @param role         USER / ADMIN，前端据此决定要不要显示后台入口
 * @param accessToken  短 Token，2 小时，每个请求放在 {@code Authorization: Bearer <accessToken>}
 * @param refreshToken 长 Token，7 天，accessToken 过期后用它换新的一对 Token
 * @param tokenType    固定 Bearer，前端不用硬编码
 * @param expiresIn    accessToken 有效期（秒）
 */
public record LoginVO(Long userId, String username, String role,
                      String accessToken, String refreshToken,
                      String tokenType, Long expiresIn) {

    public static LoginVO of(User user, String accessToken, String refreshToken, long accessExpiresInSeconds) {
        return new LoginVO(user.getId(), user.getUsername(), user.getRole(),
                accessToken, refreshToken, "Bearer", accessExpiresInSeconds);
    }
}
