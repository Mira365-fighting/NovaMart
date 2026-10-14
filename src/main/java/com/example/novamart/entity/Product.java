package com.example.novamart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** product 表：在售商品。 */
@Data
@TableName("product")
public class Product {

    /** status = 1 表示在售 */
    public static final int STATUS_ON_SALE = 1;

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String imageUrl;

    private String category;

    /** 1 = 在售，0 = 下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
