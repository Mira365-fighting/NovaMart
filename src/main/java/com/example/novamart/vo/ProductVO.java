package com.example.novamart.vo;

import com.example.novamart.entity.Product;

import java.math.BigDecimal;

/**
 * 商品对外视图。实体不直接进出接口，所以用 VO 只暴露前端需要的字段。
 */
public record ProductVO(Long id, String name, String description, BigDecimal price,
                        Integer stock, String imageUrl, String category) {

    public static ProductVO of(Product product) {
        return new ProductVO(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStock(), product.getImageUrl(), product.getCategory());
    }
}
