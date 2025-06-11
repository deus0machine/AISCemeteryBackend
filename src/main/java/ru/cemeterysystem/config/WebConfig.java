package ru.cemeterysystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ru.cemeterysystem.interceptors.ActivityInterceptor;

/**
 * Конфигурация веб-приложения
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private ActivityInterceptor activityInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activityInterceptor)
                .addPathPatterns("/**") // Применяем ко всем URL
                .excludePathPatterns(
                    "/css/**", 
                    "/js/**", 
                    "/images/**", 
                    "/static/**", 
                    "/favicon.ico",
                    "/error",
                    "/actuator/**"
                ); // Исключаем статические ресурсы
    }
} 