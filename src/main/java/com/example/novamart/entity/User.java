package com.example.novamart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * user 表：注册用户。
 *
 * <p>表名和字段名靠 MyBatis-Plus 的默认规则映射（类名首字母小写 → 表名，
 * 驼峰属性 → 下划线字段），所以字段上基本不用写注解。
 * 主键类型在 application.yml 里全局配了 id-type: auto，这里也不用标 @TableId。
 */
@Data
@TableName("user")
public class User {

    /** 普通用户：只能用前台接口 */
    public static final String ROLE_USER = "USER";
    /** 管理员：可以访问 /api/admin/** */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 1 = 正常 */
    public static final int STATUS_ENABLED = 1;
    /** 0 = 已禁用，登录和带 Token 访问都会被拒 */
    public static final int STATUS_DISABLED = 0;

    private Long id;

    private String username;

    /** BCrypt 哈希，不是明文；排除在 toString 之外，避免误打进日志 */
    @ToString.Exclude
    private String password;

    private String phone;

    /** USER / ADMIN，决定 Spring Security 里是 ROLE_USER 还是 ROLE_ADMIN */
    private String role;

    /** 1 = 正常，0 = 已禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isEnabled() {
        return status != null && status == STATUS_ENABLED;
    }

    /** 角色 → Spring Security 的权限名（hasRole('ADMIN') 要的其实是 ROLE_ADMIN） */
    public String toAuthority() {
        return "ROLE_" + (isAdmin() ? ROLE_ADMIN : ROLE_USER);
    }
}
