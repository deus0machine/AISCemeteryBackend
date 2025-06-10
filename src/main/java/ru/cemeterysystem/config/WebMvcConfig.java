package ru.cemeterysystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.util.List;

/**
 * Конфигурация Spring MVC с настройками для Jackson и форматированием дат
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public WebMvcConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Настраивает HTTP конвертеры сообщений, устанавливая кастомный ObjectMapper
     * для обработки JSON
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Заменяем стандартный MappingJackson2HttpMessageConverter на наш
        for (int i = 0; i < converters.size(); i++) {
            HttpMessageConverter<?> converter = converters.get(i);
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                converters.set(i, new MappingJackson2HttpMessageConverter(objectMapper));
                break;
            }
        }
    }
    
    /**
     * Добавляет форматтеры для конвертации типов данных
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatterForFieldType(LocalDate.class, new DateFormatter("yyyy-MM-dd"));
    }
    
    /**
     * Настраивает простые контроллеры представлений для Thymeleaf страниц
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/access-denied").setViewName("access-denied");
    }
    
    /**
     * Настраивает обработчики статических ресурсов
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Статические ресурсы должны быть явно определены, чтобы не конфликтовали с контроллерами
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
                
        // Использовать более низкий приоритет для ресурсов, чтобы контроллеры имели преимущество
        registry.setOrder(2); // Более низкий приоритет, чем у контроллеров
    }
} 