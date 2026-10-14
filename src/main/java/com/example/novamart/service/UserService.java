package com.example.novamart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.novamart.common.exception.BusinessException;
import com.example.novamart.common.exception.ErrorCode;
import com.example.novamart.dto.LoginDTO;
import com.example.novamart.dto.RegisterDTO;
import com.example.novamart.entity.User;
import com.example.novamart.mapper.UserMapper;
import com.example.novamart.vo.LoginVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户的注册与登录。 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** 注册，返回新用户 id。写操作加事务，出错整体回滚。 */
    @Transactional
    public Long register(RegisterDTO dto) {
        // 数据库唯一索引才是真正的防线，这里只是给用户一个友好的提前提示
        if (findByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone())) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "手机号已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        // BCrypt 加密后入库，明文密码永远不落库
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        userMapper.insert(user);
        return user.getId();
    }

    /** 登录，校验通过后签发 Token */
    public LoginVO login(LoginDTO dto) {
        User user = findByUsername(dto.getUsername());
        // 用户不存在和密码错误返回同一句提示，防止用户名探测
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtService.createToken(user.getId());
        return LoginVO.of(user, token, jwtService.getExpirationSeconds());
    }

    /** selectOne 查不到会返回 null，正好用来判断用户是否存在 */
    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
