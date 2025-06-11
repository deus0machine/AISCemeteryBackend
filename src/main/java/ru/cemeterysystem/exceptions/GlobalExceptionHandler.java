package ru.cemeterysystem.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import ru.cemeterysystem.models.SystemLog;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.SystemLogService;
import ru.cemeterysystem.services.UserService;
import ru.cemeterysystem.utils.IpAddressUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений с логированием
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired
    private SystemLogService systemLogService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Обрабатывает исключения связанные с сериализацией/десериализацией JSON
     */
    @ExceptionHandler({JsonProcessingException.class, JsonMappingException.class})
    public ResponseEntity<Object> handleJsonProcessingException(Exception ex) {
        logger.error("Ошибка обработки JSON: {}", ex.getMessage(), ex);
        
        logException(ex, SystemLog.Severity.ERROR);
        
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Ошибка обработки данных: " + ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обрабатывает исключения нарушения целостности данных
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.error("Ошибка целостности данных: {}", ex.getMessage(), ex);
        
        logException(ex, SystemLog.Severity.ERROR);
        
        String message = ex.getMessage();
        if (message != null && message.contains("MEMORIAL_RELATIONS")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "MEMORIAL_IN_RELATIONS", "message", "Невозможно удалить мемориал, так как он связан в генеалогии."));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", message));
    }

    /**
     * Обрабатывает общие исключения времени выполнения
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception occurred", ex);
        
        logException(ex, SystemLog.Severity.ERROR);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера", "message", ex.getMessage()));
    }

    /**
     * Обрабатывает исключения доступа
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException ex) {
        logger.warn("Security exception occurred", ex);
        
        logException(ex, SystemLog.Severity.WARNING);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Доступ запрещен", "message", ex.getMessage()));
    }

    /**
     * Обрабатывает исключения с недопустимыми аргументами
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument exception occurred", ex);
        
        logException(ex, SystemLog.Severity.WARNING);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Некорректные данные", "message", ex.getMessage()));
    }

    /**
     * Универсальный обработчик всех остальных исключений
     * Автоматически определяет, возвращать JSON или HTML
     */
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected exception occurred", ex);
        
        logException(ex, SystemLog.Severity.CRITICAL);
        
        // Определяем тип запроса
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String requestURI = request.getRequestURI();
        
        // Если это API запрос или AJAX, возвращаем JSON
        if ("XMLHttpRequest".equals(requestedWith) || 
            (accept != null && accept.contains("application/json")) ||
            requestURI.startsWith("/api/")) {
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Неожиданная ошибка", "message", "Произошла критическая ошибка"));
        }
        
        // Иначе возвращаем HTML страницу ошибки
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("error", "Произошла ошибка");
        modelAndView.addObject("message", ex.getMessage());
        modelAndView.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return modelAndView;
    }

    /**
     * Логирование исключений
     */
    private void logException(Exception ex, SystemLog.Severity severity) {
        try {
            // Получаем текущего пользователя
            User currentUser = getCurrentUser();
            
            // Получаем HTTP запрос
            HttpServletRequest request = getCurrentRequest();
            
            // Создаем описание ошибки
            String description = String.format("Исключение: %s - %s", 
                ex.getClass().getSimpleName(), 
                ex.getMessage() != null ? ex.getMessage() : "Без сообщения");
            
            // Создаем детали ошибки
            String details = createExceptionDetails(ex, request);
            
            // Логируем асинхронно
            systemLogService.logAction(
                SystemLog.ActionType.SYSTEM,
                SystemLog.EntityType.SYSTEM,
                null,
                description,
                details,
                currentUser,
                request != null ? IpAddressUtils.getClientIpAddress(request) : null,
                request != null ? request.getHeader("User-Agent") : null,
                severity
            );
            
        } catch (Exception logEx) {
            logger.error("Ошибка при логировании исключения", logEx);
        }
    }

    /**
     * Получение текущего пользователя
     */
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String username = auth.getName();
                return userService.findByLogin(username).orElse(null);
            }
        } catch (Exception e) {
            logger.debug("Не удалось получить текущего пользователя при логировании исключения", e);
        }
        return null;
    }

    /**
     * Получение текущего HTTP запроса
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Создание детальной информации об исключении
     */
    private String createExceptionDetails(Exception ex, HttpServletRequest request) {
        StringBuilder details = new StringBuilder();
        
        // Информация об исключении
        details.append("Exception Type: ").append(ex.getClass().getName()).append("\n");
        details.append("Message: ").append(ex.getMessage() != null ? ex.getMessage() : "null").append("\n");
        
        // Информация о запросе
        if (request != null) {
            details.append("Request URL: ").append(request.getRequestURL()).append("\n");
            details.append("Method: ").append(request.getMethod()).append("\n");
            details.append("Query String: ").append(request.getQueryString() != null ? request.getQueryString() : "null").append("\n");
            details.append("User Agent: ").append(request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "null").append("\n");
            details.append("Referer: ").append(request.getHeader("Referer") != null ? request.getHeader("Referer") : "null").append("\n");
        }
        
        // Стек трейс (только первые несколько строк)
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace.length > 0) {
            details.append("Stack Trace (top 5):\n");
            int limit = Math.min(5, stackTrace.length);
            for (int i = 0; i < limit; i++) {
                details.append("  ").append(stackTrace[i].toString()).append("\n");
            }
        }
        
        return details.toString();
    }
} 