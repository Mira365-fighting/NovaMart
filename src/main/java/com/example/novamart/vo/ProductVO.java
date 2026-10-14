package com.example.novamart.vo;

import com.example.novamart.entity.Product;

import java.math.BigDecimal;

/**
 * 商品对外视图。实体不直接进出接口，所以用 VO 只暴露前端需要的字段。
 *
 * <p>status 同时给后台管理用（前台只返回在售商品，所以它恒为 1）。
 */
public record ProductVO(Long id, String name, String description, BigDecimal price,
                        Integer stock, String imageUrl, String category, Integer status) {

    public static ProductVO of(Product product) {
        return new ProductVO(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStock(), product.getImageUrl(),
                product.getCategory(), product.getStatus());
    }
}
