package com.example.novamart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.novamart.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/** 商品表的通用 CRUD 由 BaseMapper 提供，复杂条件用 LambdaQueryWrapper 拼。 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
