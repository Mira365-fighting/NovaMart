package com.example.novamart.vo;

import com.example.novamart.entity.CartItem;
import com.example.novamart.entity.Product;

import java.math.BigDecimal;

/**
 * 购物车的一行：商品信息 + 数量 + 小计。
 *
 * @param productId   商品 id
 * @param productName 商品名
 * @param imageUrl    封面图
 * @param price       单价
 * @param quantity    数量
 * @param subtotal    小计 = price * quantity，服务端算好，前端不做金额运算
 * @param stock       当前库存，方便前端提前提示
 */
public record CartVO(Long productId, String productName, String imageUrl, BigDecimal price,
                     Integer quantity, BigDecimal subtotal, Integer stock) {

    /** 由「购物车行 + 商品」组装 */
    public static CartVO of(CartItem item, Product product) {
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartVO(product.getId(), product.getName(), product.getImageUrl(),
                product.getPrice(), item.getQuantity(), subtotal, product.getStock());
    }
}
