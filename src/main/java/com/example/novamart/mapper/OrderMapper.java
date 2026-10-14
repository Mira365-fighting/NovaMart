package com.example.novamart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.novamart.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/** 订单主表。 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
