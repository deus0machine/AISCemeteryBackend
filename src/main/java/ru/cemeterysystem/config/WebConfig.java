package ru.cemeterysystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
                    "/favicon.svg",
                    "/error",
                    "/actuator/**"
                ); // Исключаем статические ресурсы
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Обработка favicon
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400); // Кэшируем на 24 часа
                
        registry.addResourceHandler("/favicon.svg")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400);
                
        // Обработка других статических ресурсов
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400);
    }
} 