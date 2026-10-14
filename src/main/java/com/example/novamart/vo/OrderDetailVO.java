package com.example.novamart.vo;

import com.example.novamart.entity.Order;
import com.example.novamart.entity.OrderItem;

import java.util.List;

/** 订单详情：订单头 + 明细列表。 */
public record OrderDetailVO(OrderVO order, List<OrderItemVO> items) {

    public static OrderDetailVO of(Order order, List<OrderItem> items) {
        return new OrderDetailVO(OrderVO.of(order), items.stream().map(OrderItemVO::of).toList());
    }
}
