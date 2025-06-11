package ru.cemeterysystem.aspects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.cemeterysystem.annotations.LogActivity;
import ru.cemeterysystem.models.SystemLog;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.SystemLogService;
import ru.cemeterysystem.services.UserService;
import ru.cemeterysystem.utils.IpAddressUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Аспект для автоматического логирования действий пользователей
 */
@Aspect
@Component
public class LoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    
    @Autowired
    private SystemLogService systemLogService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final SpelExpressionParser parser = new SpelExpressionParser();
    
    /**
     * Логирование после успешного выполнения метода
     */
    @AfterReturning(value = "@annotation(logActivity)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, LogActivity logActivity, Object result) {
        try {
            if (logActivity.onSuccessOnly()) {
                logAction(joinPoint, logActivity, result, null);
            }
        } catch (Exception e) {
            logger.error("Ошибка при логировании успешного действия", e);
        }
    }
    
    /**
     * Логирование после выброса исключения
     */
    @AfterThrowing(value = "@annotation(logActivity)", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, LogActivity logActivity, Throwable exception) {
        try {
            if (!logActivity.onSuccessOnly()) {
                logAction(joinPoint, logActivity, null, exception);
            }
        } catch (Exception e) {
            logger.error("Ошибка при логировании исключения", e);
        }
    }
    
    /**
     * Основной метод логирования
     */
    private void logAction(JoinPoint joinPoint, LogActivity logActivity, Object result, Throwable exception) {
        try {
            // Получаем текущего пользователя
            User currentUser = getCurrentUser();
            
            // Получаем HTTP запрос
            HttpServletRequest request = getCurrentRequest();
            
            // Обрабатываем описание с SpEL
            String description = processDescription(logActivity.description(), joinPoint, result, exception);
            
            // Получаем ID сущности
            Long entityId = getEntityId(logActivity.entityIdExpression(), joinPoint, result);
            
            // Создаем детали
            String details = null;
            if (logActivity.includeDetails()) {
                details = createDetails(joinPoint, result, exception);
            }
            
            // Определяем уровень важности
            SystemLog.Severity severity = exception != null ? SystemLog.Severity.ERROR : logActivity.severity();
            
            // Логируем действие
            systemLogService.logAction(
                logActivity.action(),
                logActivity.entityType(),
                entityId,
                description,
                details,
                currentUser,
                request != null ? IpAddressUtils.getClientIpAddress(request) : null,
                request != null ? request.getHeader("User-Agent") : null,
                severity
            );
            
        } catch (Exception e) {
            logger.error("Критическая ошибка при логировании действия", e);
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
            logger.debug("Не удалось получить текущего пользователя", e);
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
     * Обработка описания с SpEL выражениями
     */
    private String processDescription(String template, JoinPoint joinPoint, Object result, Throwable exception) {
        try {
            if (!StringUtils.hasText(template)) {
                return "Действие выполнено";
            }
            
            // Создаем контекст для SpEL
            EvaluationContext context = new StandardEvaluationContext();
            
            // Добавляем параметры метода
            Object[] args = joinPoint.getArgs();
            String[] paramNames = getParameterNames(joinPoint);
            
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            
            // Добавляем результат и исключение
            if (result != null) {
                context.setVariable("result", result);
            }
            if (exception != null) {
                context.setVariable("exception", exception);
            }
            
            // Парсим и выполняем выражение
            Expression expression = parser.parseExpression(template, new org.springframework.expression.common.TemplateParserContext());
            return expression.getValue(context, String.class);
            
        } catch (Exception e) {
            logger.warn("Ошибка при обработке описания: " + template, e);
            return template; // Возвращаем исходный шаблон
        }
    }
    
    /**
     * Получение ID сущности через SpEL
     */
    private Long getEntityId(String expression, JoinPoint joinPoint, Object result) {
        try {
            if (!StringUtils.hasText(expression)) {
                return null;
            }
            
            EvaluationContext context = new StandardEvaluationContext();
            
            // Добавляем параметры метода
            Object[] args = joinPoint.getArgs();
            String[] paramNames = getParameterNames(joinPoint);
            
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            
            if (result != null) {
                context.setVariable("result", result);
            }
            
            Expression expr = parser.parseExpression(expression);
            Object value = expr.getValue(context);
            
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value != null) {
                return Long.valueOf(value.toString());
            }
            
        } catch (Exception e) {
            logger.warn("Ошибка при получении ID сущности: " + expression, e);
        }
        
        return null;
    }
    
    /**
     * Создание детальной информации
     */
    private String createDetails(JoinPoint joinPoint, Object result, Throwable exception) {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // Информация о методе
            details.put("method", joinPoint.getSignature().toShortString());
            
            // Параметры (безопасные)
            Object[] args = joinPoint.getArgs();
            if (args.length > 0) {
                Map<String, Object> params = new HashMap<>();
                String[] paramNames = getParameterNames(joinPoint);
                
                for (int i = 0; i < args.length && i < paramNames.length; i++) {
                    Object arg = args[i];
                    // Не логируем пароли и чувствительные данные
                    if (isSensitiveParameter(paramNames[i], arg)) {
                        params.put(paramNames[i], "[HIDDEN]");
                    } else {
                        params.put(paramNames[i], sanitizeForLogging(arg));
                    }
                }
                details.put("parameters", params);
            }
            
            // Результат (если есть и не чувствительный)
            if (result != null && !isSensitiveData(result)) {
                details.put("result", sanitizeForLogging(result));
            }
            
            // Исключение
            if (exception != null) {
                details.put("exception", Map.of(
                    "type", exception.getClass().getSimpleName(),
                    "message", exception.getMessage()
                ));
            }
            
            return objectMapper.writeValueAsString(details);
            
        } catch (Exception e) {
            logger.warn("Ошибка при создании деталей", e);
            return "Детали недоступны";
        }
    }
    
    /**
     * Получение имен параметров метода
     */
    private String[] getParameterNames(JoinPoint joinPoint) {
        try {
            Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
            return Arrays.stream(method.getParameters())
                    .map(param -> param.getName())
                    .toArray(String[]::new);
        } catch (Exception e) {
            // Fallback - генерируем имена
            Object[] args = joinPoint.getArgs();
            String[] names = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                names[i] = "arg" + i;
            }
            return names;
        }
    }
    
    /**
     * Проверка на чувствительные параметры
     */
    private boolean isSensitiveParameter(String paramName, Object value) {
        String lowerName = paramName.toLowerCase();
        return lowerName.contains("password") || 
               lowerName.contains("pwd") || 
               lowerName.contains("token") ||
               lowerName.contains("key") ||
               (value instanceof String && ((String) value).length() > 1000); // Длинные строки
    }
    
    /**
     * Проверка на чувствительные данные
     */
    private boolean isSensitiveData(Object data) {
        if (data == null) return false;
        
        String className = data.getClass().getSimpleName().toLowerCase();
        return className.contains("password") || 
               className.contains("token") ||
               className.contains("key");
    }
    
    /**
     * Очистка данных для логирования
     */
    private Object sanitizeForLogging(Object data) {
        if (data == null) return null;
        
        // Ограничиваем размер строк
        if (data instanceof String) {
            String str = (String) data;
            return str.length() > 500 ? str.substring(0, 500) + "..." : str;
        }
        
        // Для сложных объектов возвращаем упрощенную информацию
        if (data.getClass().getPackage() != null && 
            data.getClass().getPackage().getName().startsWith("ru.cemeterysystem.models")) {
            try {
                return data.getClass().getSimpleName() + "@" + System.identityHashCode(data);
            } catch (Exception e) {
                return data.toString();
            }
        }
        
        return data;
    }
} 