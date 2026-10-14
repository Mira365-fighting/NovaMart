package com.example.novamart.config;
import com.github.pagehelper.PageInterceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * MyBatis-Plus 配置。
 *
 * <p>这里只做一件事：把 PageHelper 的分页插件挂到 MyBatis 上。
 * MyBatis-Plus 的自动配置会收集容器里所有 {@link org.apache.ibatis.plugin.Interceptor} Bean，
 * 统一挂到 SqlSessionFactory 上，所以声明成 Bean 就够了，不需要 pagehelper 的 spring-boot-starter。
 * （不引那个 starter 是为了避开它顺带拉进来的 mybatis-spring-boot-starter，避免和 MyBatis-Plus 的自动配置打架。）
 */
@Configuration
public class MyBatisPlusConfiguration {

    @Bean
    public PageInterceptor pageInterceptor() {
        PageInterceptor interceptor = new PageInterceptor();
        Properties properties = new Properties();
        // 指定 MySQL 方言，PageHelper 据此生成 LIMIT 语句
        properties.setProperty("helperDialect", "mysql");
        // 页码越界时自动纠正到第一页/最后一页，而不是返回空列表
        properties.setProperty("reasonable", "true");
        interceptor.setProperties(properties);
        return interceptor;
    }
}
