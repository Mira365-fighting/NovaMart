package com.example.novamart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.novamart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

/** 购物车表的通用 CRUD 由 BaseMapper 提供。 */
@Mapper
public interface CartMapper extends BaseMapper<CartItem> {
}
