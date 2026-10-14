package com.example.novamart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * cart 表：购物车的一行，即「某用户买了某商品几个」。
 *
 * <p>类名是 CartItem 但表名是 cart，所以这里必须显式写 @TableName，
 * 否则 MyBatis-Plus 会去找 cart_item 表。
 */
@Data
@TableName("cart")
public class CartItem {

    private Long id;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
