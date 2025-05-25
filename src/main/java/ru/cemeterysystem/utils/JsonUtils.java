package ru.cemeterysystem.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Утилитный класс для работы с JSON
 */
@Component
public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    private final ObjectMapper objectMapper;

    public JsonUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Преобразует объект в JSON строку
     */
    public String toJson(@Nullable Object object) {
        if (object == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации объекта в JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка сериализации объекта", e);
        }
    }

    /**
     * Преобразует JSON строку в объект указанного класса
     */
    public <T> T fromJson(@Nullable String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Ошибка десериализации JSON в объект типа {}: {}", clazz.getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Ошибка десериализации JSON", e);
        }
    }

    /**
     * Преобразует JSON строку в объект указанного типа (для работы с дженериками)
     */
    public <T> T fromJson(@Nullable String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Ошибка десериализации JSON в объект: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка десериализации JSON", e);
        }
    }

    /**
     * Создает новый ObjectMapper с правильной конфигурацией для обработки дат Java 8
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }
} 