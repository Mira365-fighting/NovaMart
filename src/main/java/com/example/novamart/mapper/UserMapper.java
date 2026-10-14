package com.example.novamart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.novamart.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** 继承 BaseMapper 就自动拥有 insert / selectById / updateById / deleteById 等单表方法。 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
