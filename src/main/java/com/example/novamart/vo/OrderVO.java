package com.example.novamart.vo;

import com.example.novamart.entity.Order;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单列表项（不含明细）。 */
public record OrderVO(Long id, String orderNo, BigDecimal totalAmount,
                      Integer status, String statusText,
                      // spring.jackson.date-format 只作用于 java.util.Date，
                      // LocalDateTime 要自己指定格式，否则会输出 ISO 的 2026-10-14T15:03:33
                      @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime,
                      @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime payTime,
                      @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime shipTime,
                      @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime finishTime) {

    public static OrderVO of(Order order) {
        return new OrderVO(order.getId(), order.getOrderNo(), order.getTotalAmount(),
                order.getStatus(), Order.text(order.getStatus()),
                order.getCreateTime(), order.getPayTime(), order.getShipTime(), order.getFinishTime());
    }
}
