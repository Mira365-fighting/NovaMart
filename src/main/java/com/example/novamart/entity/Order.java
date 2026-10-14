package com.example.novamart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * orders 表：订单主表。
 *
 * <p>表名用复数 orders —— order 是 MySQL 保留字，直接叫 order 每处 SQL 都得加反引号。
 *
 * <p>状态流转（数字必须和数据库里 status 字段的注释保持一致）：
 * <pre>
 *   0 待支付 --支付--> 1 已支付(待发货) --后台发货--> 2 已发货 --用户确认收货--> 3 已完成
 *   0/1 --用户取消--> 4 已取消（回滚库存）
 *   0/1 --后台关闭--> 5 已关闭（回滚库存）
 * </pre>
 * 4 和 5 的区别只是「谁发起的」：4 是用户自己取消，5 是后台强制关单，业务上都把库存加回去。
 */
@Data
@TableName("orders")
public class Order {

    public static final int STATUS_UNPAID = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_SHIPPED = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_CANCELLED = 4;
    public static final int STATUS_CLOSED = 5;

    private Long id;

    /** 订单号，对外展示用，不暴露自增 id */
    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime finishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 状态码 → 中文文案，前端直接显示，省得两边各维护一份映射 */
    public static String text(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_UNPAID -> "待支付";
            case STATUS_PAID -> "已支付";
            case STATUS_SHIPPED -> "已发货";
            case STATUS_COMPLETED -> "已完成";
            case STATUS_CANCELLED -> "已取消";
            case STATUS_CLOSED -> "已关闭";
            default -> "未知状态";
        };
    }
}
