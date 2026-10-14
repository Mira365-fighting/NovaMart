package com.example.novamart.controller;

import com.example.novamart.common.result.ApiResult;
import com.example.novamart.dto.LoginDTO;
import com.example.novamart.dto.RegisterDTO;
import com.example.novamart.service.UserService;
import com.example.novamart.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口，公开访问（不需要 Token）。
 * Controller 只做「收参数 + 调 Service + 包结果」，业务逻辑全在 Service 里。
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

    /** 登录，成功后返回 JWT，之后一段时间内不用再输账号密码 */
    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return ApiResult.success(userService.login(dto));
    }
}
