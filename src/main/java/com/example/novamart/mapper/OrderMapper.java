package com.example.novamart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.novamart.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/** 订单主表。 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 后台仪表盘的累计销售额。
     * 只统计 1=已支付 2=已发货 3=已完成，已取消(4)/已关闭(5) 的订单不计入营业额。
     */
    @Select("SELECT IFNULL(SUM(total_amount), 0) FROM orders WHERE status IN (1, 2, 3)")
    BigDecimal sumPaidAmount();
}
