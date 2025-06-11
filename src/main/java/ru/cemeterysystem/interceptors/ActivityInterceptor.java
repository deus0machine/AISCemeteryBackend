package ru.cemeterysystem.interceptors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import ru.cemeterysystem.models.SystemLog;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.SystemLogService;
import ru.cemeterysystem.services.UserService;
import ru.cemeterysystem.utils.IpAddressUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Интерсептор для отслеживания активности пользователей
 */
@Component
public class ActivityInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityInterceptor.class);
    
    @Autowired
    private SystemLogService systemLogService;
    
    @Autowired
    private UserService userService;
    
    // URL-ы, которые мы хотим отслеживать
    private static final Set<String> TRACKED_PATHS = new HashSet<>(Arrays.asList(
        "/memorials", "/memorial", "/family-trees", "/family-tree", 
        "/admin", "/profile", "/subscription", "/search"
    ));
    
    // URL-ы, которые мы НЕ отслеживаем (статические ресурсы, служебные)
    private static final Set<String> IGNORED_PATHS = new HashSet<>(Arrays.asList(
        "/css", "/js", "/images", "/static", "/favicon.ico", "/api/logs",
        "/login", "/logout", "/error", "/actuator"
    ));
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Логируем только для аутентифицированных пользователей
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return true;
            }
            
            String requestURI = request.getRequestURI();
            String method = request.getMethod();
            
            // Проверяем, нужно ли отслеживать этот запрос
            if (!shouldTrackRequest(requestURI, method)) {
                return true;
            }
            
            // Получаем пользователя
            User user = getCurrentUser(auth);
            if (user == null) {
                return true;
            }
            
            // Определяем тип действия и сущности
            ActivityInfo activityInfo = determineActivity(requestURI, method);
            
            if (activityInfo != null) {
                // Создаем описание
                String description = createDescription(activityInfo, requestURI, method, user);
                
                // Логируем асинхронно
                systemLogService.logAction(
                    activityInfo.actionType,
                    activityInfo.entityType,
                    null, // ID сущности определяется позже через аннотации
                    description,
                    String.format("HTTP %s %s", method, requestURI),
                    user,
                    IpAddressUtils.getClientIpAddress(request),
                    request.getHeader("User-Agent"),
                    SystemLog.Severity.INFO
                );
            }
            
        } catch (Exception e) {
            logger.debug("Ошибка при логировании активности пользователя", e);
        }
        
        return true;
    }
    
    /**
     * Проверяем, нужно ли отслеживать данный запрос
     */
    private boolean shouldTrackRequest(String requestURI, String method) {
        // Игнорируем статические ресурсы и служебные URL
        for (String ignoredPath : IGNORED_PATHS) {
            if (requestURI.startsWith(ignoredPath)) {
                return false;
            }
        }
        
        // Отслеживаем только GET запросы для просмотров (POST/PUT/DELETE будут через аннотации)
        if (!"GET".equals(method)) {
            return false;
        }
        
        // Проверяем, находится ли URL в списке отслеживаемых
        return TRACKED_PATHS.stream().anyMatch(requestURI::startsWith);
    }
    
    /**
     * Определяем тип активности по URL
     */
    private ActivityInfo determineActivity(String requestURI, String method) {
        if (requestURI.startsWith("/memorial")) {
            return new ActivityInfo(SystemLog.ActionType.VIEW, SystemLog.EntityType.MEMORIAL);
        } else if (requestURI.startsWith("/family-tree")) {
            return new ActivityInfo(SystemLog.ActionType.VIEW, SystemLog.EntityType.FAMILY_TREE);
        } else if (requestURI.startsWith("/admin")) {
            return new ActivityInfo(SystemLog.ActionType.VIEW, SystemLog.EntityType.SYSTEM);
        } else if (requestURI.startsWith("/profile")) {
            return new ActivityInfo(SystemLog.ActionType.VIEW, SystemLog.EntityType.USER);
        } else if (requestURI.startsWith("/search")) {
            return new ActivityInfo(SystemLog.ActionType.VIEW, SystemLog.EntityType.SYSTEM);
        } else if (requestURI.startsWith("/subscription")) {
            return new ActivityInfo(SystemLog.ActionType.VIEW, SystemLog.EntityType.USER);
        }
        
        return null;
    }
    
    /**
     * Создаем описание действия
     */
    private String createDescription(ActivityInfo activityInfo, String requestURI, String method, User user) {
        StringBuilder description = new StringBuilder();
        
        switch (activityInfo.entityType) {
            case MEMORIAL:
                if (requestURI.contains("/memorial/")) {
                    description.append("Просмотр мемориала");
                } else {
                    description.append("Просмотр списка мемориалов");
                }
                break;
            case FAMILY_TREE:
                if (requestURI.contains("/family-tree/")) {
                    description.append("Просмотр семейного древа");
                } else {
                    description.append("Просмотр списка семейных древ");
                }
                break;
            case SYSTEM:
                if (requestURI.startsWith("/admin")) {
                    description.append("Доступ к административной панели");
                } else if (requestURI.startsWith("/search")) {
                    description.append("Поиск по системе");
                }
                break;
            case USER:
                if (requestURI.startsWith("/profile")) {
                    description.append("Просмотр профиля пользователя");
                } else if (requestURI.startsWith("/subscription")) {
                    description.append("Просмотр информации о подписке");
                }
                break;
            default:
                description.append("Просмотр страницы: ").append(requestURI);
        }
        
        return description.toString();
    }
    
    /**
     * Получение текущего пользователя
     */
    private User getCurrentUser(Authentication auth) {
        try {
            String username = auth.getName();
            return userService.findByLogin(username).orElse(null);
        } catch (Exception e) {
            logger.debug("Не удалось получить пользователя", e);
            return null;
        }
    }
    
    /**
     * Внутренний класс для хранения информации об активности
     */
    private static class ActivityInfo {
        final SystemLog.ActionType actionType;
        final SystemLog.EntityType entityType;
        
        ActivityInfo(SystemLog.ActionType actionType, SystemLog.EntityType entityType) {
            this.actionType = actionType;
            this.entityType = entityType;
        }
    }
} 