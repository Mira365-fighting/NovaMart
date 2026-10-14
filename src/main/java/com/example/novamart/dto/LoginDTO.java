package com.example.novamart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** {@code POST /api/user/login} 的请求体。 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 可选的本地临时购物车。未登录时购物车存在 localStorage，
     * 登录时一起带上来，服务端在同一个事务里合并进数据库，用户不用重新加购。
     */
    @Valid
    private List<CartItemDTO> cartItems;
}
