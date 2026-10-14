package com.example.novamart.config;

import com.example.novamart.interceptor.UserContextInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Spring MVC 配置。
 *
 * <p>用 WebMvcConfigurer（而不是继承 WebMvcConfigurationSupport）扩展 MVC，
 * 这样只追加拦截器，Spring Boot 的默认配置不会被覆盖掉。
 *
 * <p>注意：这里的拦截器**不再做认证**。认证已经交给 Spring Security 的
 * {@code JwtAuthenticationFilter} 了，拦截器只负责把当前用户放进 ThreadLocal。
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    public WebConfiguration(UserContextInterceptor userContextInterceptor) {
        this.userContextInterceptor = userContextInterceptor;
    }

    @Value("${novamart.cors.allowed-origins}")
    private String allowedOrigins;

    /** 把当前登录用户放进 ThreadLocal，请求结束由拦截器自己清理 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor).addPathPatterns("/api/**");
    }

    /**
     * 跨域配置。前端 dev server（Vite 5173）直连后端时需要；
     * 生产环境由后端托管打包好的静态页，属同源，这段其实用不上。
     * Spring Security 的 {@code cors()} 会自动找这个 Bean。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // 允许携带 Authorization 头 / Cookie
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
