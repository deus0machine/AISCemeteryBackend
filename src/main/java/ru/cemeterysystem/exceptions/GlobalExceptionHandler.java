package ru.cemeterysystem.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений для всего приложения
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Обрабатывает исключения связанные с сериализацией/десериализацией JSON
     */
    @ExceptionHandler({JsonProcessingException.class, JsonMappingException.class})
    public ResponseEntity<Object> handleJsonProcessingException(Exception ex) {
        log.error("Ошибка обработки JSON: {}", ex.getMessage(), ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Ошибка обработки данных: " + ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обрабатывает исключения нарушения целостности данных
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Ошибка целостности данных: {}", ex.getMessage(), ex);
        
        String message = ex.getMessage();
        if (message != null && message.contains("MEMORIAL_RELATIONS")) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "MEMORIAL_IN_RELATIONS");
            response.put("message", "Невозможно удалить мемориал, так как он связан в генеалогии.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        Map<String, String> response = new HashMap<>();
        response.put("error", "SERVER_ERROR");
        response.put("message", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Обрабатывает общие исключения времени выполнения
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex) {
        log.error("Ошибка выполнения: {}", ex.getMessage(), ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Обрабатывает все остальные исключения
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex) {
        log.error("Непредвиденная ошибка: {}", ex.getMessage(), ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Произошла непредвиденная ошибка");
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 