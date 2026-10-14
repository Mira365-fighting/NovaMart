package com.example.novamart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** {@code POST /api/cart/add} 和 {@code PUT /api/cart/update} 共用的请求体。 */
@Data
public class CartItemDTO {

    @NotNull(message = "商品 id 不能为空")
    @Positive(message = "商品 id 必须为正数")
    private Long productId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 999, message = "数量不能超过 999")
    private Integer quantity;
}
