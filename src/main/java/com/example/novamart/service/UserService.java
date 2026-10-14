package com.example.novamart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.novamart.common.exception.BusinessException;
import com.example.novamart.common.exception.ErrorCode;
import com.example.novamart.dto.CartItemDTO;
import com.example.novamart.dto.LoginDTO;
import com.example.novamart.dto.RegisterDTO;
import com.example.novamart.entity.User;
import com.example.novamart.mapper.UserMapper;
import com.example.novamart.security.JwtPayload;
import com.example.novamart.vo.LoginVO;
import com.example.novamart.vo.MeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** 用户：注册、登录、刷新 Token、退出登录、查当前用户。 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CartService cartService;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService,
                       TokenBlacklistService tokenBlacklistService, CartService cartService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.cartService = cartService;
    }

    /** 注册，返回新用户 id。默认是普通用户、状态正常。 */
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
        user.setRole(User.ROLE_USER);
        user.setStatus(User.STATUS_ENABLED);
        userMapper.insert(user);
        return user.getId();
    }

    /**
     * 登录：校验凭证 → 合并未登录期间的本地临时购物车 → 签发一对 Token。
     *
     * <p>合并放在登录里，用户从「游客」变成「登录用户」的那一步顺手就把购物车带过来了，
     * 前端不用再发一次请求、也不会出现「加过的商品不见了」。
     */
    @Transactional
    public LoginVO login(LoginDTO dto) {
        User user = findByUsername(dto.getUsername());
        // 用户不存在和密码错误返回同一句提示，防止用户名探测
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        List<CartItemDTO> cartItems = dto.getCartItems();
        if (cartItems != null && !cartItems.isEmpty()) {
            cartService.merge(user.getId(), cartItems);
        }
        return issueTokens(user);
    }

    /**
     * 用 refreshToken 换一对新 Token。
     *
     * <p>同时做一次<b>轮换</b>：旧的 refreshToken 用完立刻拉黑。
     * 这样 refreshToken 一旦被窃取并重复使用，第二次就会失败，风险窗口小很多。
     */
    @Transactional
    public LoginVO refresh(String refreshToken) {
        JwtPayload payload = jwtService.parseRefresh(refreshToken);
        if (tokenBlacklistService.isBlacklisted(payload.jti())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        User user = userMapper.selectById(payload.userId());
        if (user == null || !user.isEnabled()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        tokenBlacklistService.blacklist(payload);
        return issueTokens(user);
    }

    /**
     * 退出登录：把 Token 写进 Redis 黑名单。
     *
     * <p>JWT 是无状态的，服务端没法「撤回」已经签发出去的 Token，
     * 所以只能记一笔黑名单，之后每个请求都会查一次。
     *
     * <p>这里对 Token 无效/已过期的情况一律忽略 —— 登出要幂等，重复调用不该报错。
     */
    public void logout(String authorizationHeader, String refreshToken) {
        blacklistQuietly(authorizationHeader, true);
        blacklistQuietly(refreshToken, false);
    }

    /** 当前登录用户信息 */
    public MeVO me(long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return MeVO.of(user);
    }

    private LoginVO issueTokens(User user) {
        JwtService.TokenPair tokenPair = jwtService.createTokens(user);
        return LoginVO.of(user, tokenPair.accessToken(), tokenPair.refreshToken(),
                jwtService.getAccessExpirationSeconds());
    }

    /** 拉黑一个 Token；无效/过期/为空都只是跳过，不让登出失败 */
    private void blacklistQuietly(String tokenWithPrefix, boolean accessToken) {
        if (!StringUtils.hasText(tokenWithPrefix)) {
            return;
        }
        try {
            String token = tokenWithPrefix.startsWith(BEARER_PREFIX)
                    ? tokenWithPrefix.substring(BEARER_PREFIX.length()).trim()
                    : tokenWithPrefix.trim();
            JwtPayload payload = accessToken ? jwtService.parseAccess(token) : jwtService.parseRefresh(token);
            tokenBlacklistService.blacklist(payload);
        } catch (RuntimeException exception) {
            log.debug("登出时该 Token 已无效，忽略：{}", exception.getMessage());
        }
    }

    /** selectOne 查不到会返回 null，正好用来判断用户是否存在 */
    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
