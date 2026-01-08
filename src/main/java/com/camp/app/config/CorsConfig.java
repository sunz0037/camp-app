package com.camp.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS跨域配置
 * 允许前端（localhost:3000）访问后端API
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 允许的源（前端地址）- Spring Boot 3.x使用allowedOriginPatterns
                .allowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000")
                // 允许的HTTP方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // 允许的请求头
                .allowedHeaders("*")
                // 允许发送Cookie和认证信息
                .allowCredentials(true)
                // 预检请求的缓存时间（秒）
                .maxAge(3600);
    }

    /**
     * CORS过滤器（备用方案）
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的源（Spring Boot 3.x使用allowedOriginPatterns）
        config.addAllowedOriginPattern("http://localhost:3000");
        config.addAllowedOriginPattern("http://127.0.0.1:3000");
        
        // 允许的HTTP方法
        config.addAllowedMethod("*");
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 允许发送Cookie
        config.setAllowCredentials(true);
        
        // 预检请求缓存时间
        config.setMaxAge(3600L);
        
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}

