package com.example.novamart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * orders 表：订单主表。
 *
 * <p>表名用复数 orders —— order 是 MySQL 保留字，直接叫 order 每处 SQL 都得加反引号。
 */
@Data
@TableName("orders")
public class Order {

    /** 0 = 待支付 */
    public static final int STATUS_UNPAID = 0;
    /** 1 = 已支付 */
    public static final int STATUS_PAID = 1;
    /** 2 = 已取消 */
    public static final int STATUS_CANCELLED = 2;

    private Long id;

    /** 订单号，对外展示用，不暴露自增 id */
    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    /** 0 = 待支付，1 = 已支付，2 = 已取消 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
