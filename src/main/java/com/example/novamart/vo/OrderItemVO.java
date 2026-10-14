package com.example.novamart.vo;

import com.example.novamart.entity.OrderItem;

import java.math.BigDecimal;

/** 订单明细项。 */
public record OrderItemVO(Long productId, String productName, BigDecimal price,
                          Integer quantity, BigDecimal subtotal) {

    public static OrderItemVO of(OrderItem item) {
        return new OrderItemVO(item.getProductId(), item.getProductName(), item.getPrice(),
                item.getQuantity(), item.getSubtotal());
    }
}
