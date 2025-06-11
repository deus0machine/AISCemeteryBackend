package ru.cemeterysystem.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.cemeterysystem.models.SystemLog;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.SystemLogService;
import ru.cemeterysystem.services.UserService;
import ru.cemeterysystem.utils.IpAddressUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Слушатель событий аутентификации
 */
@Component
public class AuthenticationEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationEventListener.class);
    
    @Autowired
    private SystemLogService systemLogService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Обработка successful login
     */
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        logAuthenticationEvent(event, "Успешный вход в систему", SystemLog.ActionType.LOGIN, SystemLog.Severity.INFO);
    }
    
    /**
     * Обработка interactive login (например, через форму)
     */
    @EventListener
    public void handleInteractiveAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
        logAuthenticationEvent(event, "Интерактивный вход в систему", SystemLog.ActionType.LOGIN, SystemLog.Severity.INFO);
    }
    
    /**
     * Обработка failed login
     */
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        try {
            String username = event.getAuthentication().getName();
            HttpServletRequest request = getCurrentRequest();
            
            String description = String.format("Неудачная попытка входа для пользователя: %s. Причина: %s", 
                    username, event.getException().getMessage());
            
            systemLogService.logAction(
                SystemLog.ActionType.LOGIN,
                SystemLog.EntityType.USER,
                null,
                description,
                String.format("Exception: %s", event.getException().getClass().getSimpleName()),
                null, // Пользователь не аутентифицирован
                request != null ? IpAddressUtils.getClientIpAddress(request) : null,
                request != null ? request.getHeader("User-Agent") : null,
                SystemLog.Severity.WARNING
            );
            
        } catch (Exception e) {
            logger.error("Ошибка при логировании неудачного входа", e);
        }
    }
    
    /**
     * Обработка logout (если используется LogoutSuccessEvent)
     */
    @EventListener
    public void handleLogoutSuccess(LogoutSuccessEvent event) {
        try {
            Authentication auth = event.getAuthentication();
            User user = null;
            
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                user = userService.findByLogin(username).orElse(null);
            }
            
            HttpServletRequest request = getCurrentRequest();
            
            String description = user != null ? 
                String.format("Пользователь %s вышел из системы", user.getFio()) :
                "Выход из системы";
            
            systemLogService.logAction(
                SystemLog.ActionType.LOGOUT,
                SystemLog.EntityType.USER,
                user != null ? user.getId() : null,
                description,
                null,
                user,
                request != null ? IpAddressUtils.getClientIpAddress(request) : null,
                request != null ? request.getHeader("User-Agent") : null,
                SystemLog.Severity.INFO
            );
            
        } catch (Exception e) {
            logger.error("Ошибка при логировании выхода", e);
        }
    }
    
    /**
     * Общий метод для логирования событий аутентификации
     */
    private void logAuthenticationEvent(AbstractAuthenticationEvent event, String baseDescription, 
                                      SystemLog.ActionType actionType, SystemLog.Severity severity) {
        try {
            Authentication auth = event.getAuthentication();
            String username = auth.getName();
            
            User user = userService.findByLogin(username).orElse(null);
            HttpServletRequest request = getCurrentRequest();
            
            String description = user != null ? 
                String.format("%s: %s", baseDescription, user.getFio()) :
                String.format("%s: %s", baseDescription, username);
            
            systemLogService.logAction(
                actionType,
                SystemLog.EntityType.USER,
                user != null ? user.getId() : null,
                description,
                String.format("Authentication type: %s", auth.getClass().getSimpleName()),
                user,
                request != null ? IpAddressUtils.getClientIpAddress(request) : null,
                request != null ? request.getHeader("User-Agent") : null,
                severity
            );
            
        } catch (Exception e) {
            logger.error("Ошибка при логировании события аутентификации", e);
        }
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
} 