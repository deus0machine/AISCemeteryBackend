package ru.cemeterysystem.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
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
} 