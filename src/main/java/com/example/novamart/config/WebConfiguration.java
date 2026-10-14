package com.example.novamart.config;

import com.example.novamart.interceptor.JwtAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置。
 *
 * <p>用 WebMvcConfigurer（而不是继承 WebMvcConfigurationSupport）扩展 MVC，
 * 这样只追加拦截器，Spring Boot 的默认配置不会被覆盖掉。
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    public WebConfiguration(JwtAuthenticationInterceptor jwtAuthenticationInterceptor) {
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
    }

    /**
     * 购物车和订单必须登录后才能访问；
     * 商品浏览、注册、登录是公开接口，所以不加进来。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/api/cart/**", "/api/order/**");
    }
}
