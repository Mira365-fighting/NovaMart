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

    private Long id;

    private String username;

    /** BCrypt 哈希，不是明文；排除在 toString 之外，避免误打进日志 */
    @ToString.Exclude
    private String password;

    private String phone;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
