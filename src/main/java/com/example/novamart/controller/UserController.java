package com.example.novamart.controller;

import com.example.novamart.common.result.ApiResult;
import com.example.novamart.dto.LoginDTO;
import com.example.novamart.dto.LogoutDTO;
import com.example.novamart.dto.RefreshTokenDTO;
import com.example.novamart.dto.RegisterDTO;
import com.example.novamart.security.UserContext;
import com.example.novamart.service.UserService;
import com.example.novamart.vo.LoginVO;
import com.example.novamart.vo.MeVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户与登录态接口。
 *
 * <p>注册 / 登录 / 刷新 Token 是公开接口（在 SecurityConfiguration 里放行），
 * 登出和「我的信息」需要带 Token。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 注册，返回新用户 id */
    @PostMapping("/register")
    public ApiResult<Long> register(@Valid @RequestBody RegisterDTO dto) {
        return ApiResult.success(userService.register(dto));
    }

    /**
     * 登录，返回 accessToken(2h) + refreshToken(7d)。
     * 请求体里可以带 cartItems，把未登录期间的本地购物车一起合并进来。
     */
    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return ApiResult.success(userService.login(dto));
    }

    /** 用 refreshToken 换一对新 Token（旧的 refreshToken 会被轮换掉） */
    @PostMapping("/refresh")
    public ApiResult<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return ApiResult.success(userService.refresh(dto.refreshToken()));
    }

    /**
     * 退出登录：把 Token 写进 Redis 黑名单。
     * 没带 refreshToken 也能调，只是长 Token 会一直有效到自然过期。
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) LogoutDTO dto) {
        userService.logout(authorization, dto == null ? null : dto.refreshToken());
        return ApiResult.success();
    }

    /** 当前登录用户信息，userId 从 ThreadLocal 取（拦截器写入），前端传不了 */
    @GetMapping("/me")
    public ApiResult<MeVO> me() {
        return ApiResult.success(userService.me(UserContext.requireUserId()));
    }
}
