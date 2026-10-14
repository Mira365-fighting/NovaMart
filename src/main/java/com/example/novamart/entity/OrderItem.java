package com.example.novamart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * order_item 表：订单明细。
 *
 * <p>productName 和 price 是下单那一刻的快照，商品之后改名改价都不影响历史订单。
 */
@Data
@TableName("order_item")
public class OrderItem {

    private Long id;

    private Long orderId;

    private Long productId;

    /** 下单时的商品名快照 */
    private String productName;

    /** 下单时的单价快照 */
    private BigDecimal price;

    private Integer quantity;

    /** 小计 = price * quantity */
    private BigDecimal subtotal;
}
