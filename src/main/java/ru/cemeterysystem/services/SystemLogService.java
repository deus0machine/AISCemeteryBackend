package ru.cemeterysystem.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.models.SystemLog;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.SystemLogRepository;
import ru.cemeterysystem.utils.IpAddressUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;
    private static final Logger logger = LoggerFactory.getLogger(SystemLogService.class);

    /**
     * Асинхронное создание лога
     */
    @Async("logExecutor")
    public void logAction(SystemLog.ActionType actionType, SystemLog.EntityType entityType, 
                         String description, User user) {
        SystemLog log = new SystemLog(actionType, entityType, description, user);
        systemLogRepository.save(log);
    }

    /**
     * Асинхронное создание лога с ID сущности
     */
    @Async("logExecutor")
    public void logAction(SystemLog.ActionType actionType, SystemLog.EntityType entityType, 
                         Long entityId, String description, User user) {
        SystemLog log = new SystemLog(actionType, entityType, entityId, description, user);
        systemLogRepository.save(log);
    }

    /**
     * Асинхронное создание лога с уровнем важности
     */
    @Async("logExecutor")
    public void logAction(SystemLog.ActionType actionType, SystemLog.EntityType entityType, 
                         String description, User user, SystemLog.Severity severity) {
        SystemLog log = new SystemLog(actionType, entityType, description, user, severity);
        systemLogRepository.save(log);
    }

    /**
     * Асинхронное создание лога с ID сущности и уровнем важности
     */
    @Async("logExecutor")
    public void logAction(SystemLog.ActionType actionType, SystemLog.EntityType entityType, 
                         Long entityId, String description, User user, SystemLog.Severity severity) {
        SystemLog log = new SystemLog(actionType, entityType, entityId, description, user);
        log.setSeverity(severity);
        systemLogRepository.save(log);
    }

    /**
     * Создание лога с полной информацией о запросе
     */
    @Async("logExecutor")
    public void logActionWithRequest(SystemLog.ActionType actionType, SystemLog.EntityType entityType, 
                                   String description, User user, HttpServletRequest request) {
        SystemLog log = new SystemLog(actionType, entityType, description, user);
        
        if (request != null) {
            log.setIpAddress(IpAddressUtils.getClientIpAddress(request));
            log.setUserAgent(request.getHeader("User-Agent"));
        }
        
        systemLogRepository.save(log);
    }

    /**
     * Создание лога с полной информацией (все параметры)
     */
    @Async("logExecutor")
    public void logAction(SystemLog.ActionType actionType, SystemLog.EntityType entityType, 
                         Long entityId, String description, String details, User user, 
                         String ipAddress, String userAgent, SystemLog.Severity severity) {
        SystemLog log = new SystemLog(actionType, entityType, entityId, description, user);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setSeverity(severity);
        systemLogRepository.save(log);
    }

    /**
     * Получение логов с фильтрацией и пагинацией
     * Используем простые методы Spring Data вместо сложных запросов
     */
    public Page<SystemLog> getLogsWithFilters(SystemLog.ActionType actionType,
                                            SystemLog.EntityType entityType,
                                            SystemLog.Severity severity,
                                            User user,
                                            LocalDateTime startDate,
                                            LocalDateTime endDate,
                                            String searchText,
                                            int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // TODO: Поиск по searchText временно отключен - нужно исправить тип поля description в БД
        if (searchText != null && !searchText.trim().isEmpty()) {
            logger.warn("Поиск по тексту '{}' игнорируется - поле description имеет неправильный тип в БД", searchText);
        }
        
        // Используем простые методы в зависимости от фильтров
        if (actionType != null) {
            return systemLogRepository.findByActionTypeOrderByCreatedAtDesc(actionType, pageable);
        } else if (entityType != null) {
            return systemLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);
        } else if (severity != null) {
            return systemLogRepository.findBySeverityOrderByCreatedAtDesc(severity, pageable);
        } else if (user != null) {
            return systemLogRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        } else if (startDate != null && endDate != null) {
            return systemLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate, pageable);
        } else {
            // Если нет фильтров, возвращаем все логи
            return systemLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    /**
     * Получение всех логов с пагинацией
     */
    public Page<SystemLog> getAllLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findAll(pageable);
    }

    /**
     * Получение лога по ID
     */
    public SystemLog getLogById(Long id) {
        return systemLogRepository.findById(id).orElse(null);
    }

    /**
     * Получение последних действий для dashboard
     */
    public List<SystemLog> getRecentActions() {
        return systemLogRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * Получение статистики логов
     */
    public Map<String, Object> getLogStatistics() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime weekAgo = now.minusWeeks(1);
        LocalDateTime monthAgo = now.minusMonths(1);

        // Статистика за сегодня
        stats.put("todayTotal", systemLogRepository.countByCreatedAtAfter(today));
        stats.put("todayErrors", systemLogRepository.countBySeverityAndCreatedAtAfter(SystemLog.Severity.ERROR, today));
        stats.put("todayCritical", systemLogRepository.countBySeverityAndCreatedAtAfter(SystemLog.Severity.CRITICAL, today));

        // Статистика за неделю
        stats.put("weekTotal", systemLogRepository.countByCreatedAtAfter(weekAgo));
        stats.put("weekLogins", systemLogRepository.countByActionTypeAndCreatedAtAfter(SystemLog.ActionType.LOGIN, weekAgo));

        // Статистика за месяц
        stats.put("monthTotal", systemLogRepository.countByCreatedAtAfter(monthAgo));

        // Топ пользователей за неделю
        Pageable topUsers = PageRequest.of(0, 5);
        List<Object[]> activeUsers = systemLogRepository.findTopActiveUsers(weekAgo, topUsers);
        stats.put("topActiveUsers", activeUsers);

        // Активность по дням за последние 7 дней
        try {
            List<Object[]> dailyActivity = systemLogRepository.getActivityByDays(weekAgo);
            // Группируем по дням для простоты (так как GROUP BY sl.createdAt может дать много записей)
            Map<String, Long> groupedActivity = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (Object[] row : dailyActivity) {
                LocalDateTime dateTime = (LocalDateTime) row[0];
                Long count = (Long) row[1];
                String dayKey = dateTime.format(formatter);
                groupedActivity.merge(dayKey, count, Long::sum);
            }
            
            List<Map<String, Object>> processedActivity = new ArrayList<>();
            for (Map.Entry<String, Long> entry : groupedActivity.entrySet()) {
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("day", entry.getKey());
                dayData.put("count", entry.getValue());
                processedActivity.add(dayData);
            }
            
            stats.put("dailyActivity", processedActivity);
        } catch (Exception e) {
            // Если статистика по дням не работает, создаем пустой список
            stats.put("dailyActivity", new ArrayList<>());
        }

        return stats;
    }

    /**
     * Очистка старых логов
     */
    @Transactional
    public int cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        long countBefore = systemLogRepository.count();
        systemLogRepository.deleteByCreatedAtBefore(cutoffDate);
        long countAfter = systemLogRepository.count();
        
        int deletedCount = (int) (countBefore - countAfter);
        
        // Логируем операцию очистки
        logAction(SystemLog.ActionType.SYSTEM, SystemLog.EntityType.SYSTEM, 
                 "Очистка старых логов: удалено " + deletedCount + " записей", null, SystemLog.Severity.INFO);
        
        return deletedCount;
    }

    /**
     * Методы для быстрого логирования типовых действий
     */
    
    public void logUserLogin(User user, HttpServletRequest request) {
        logActionWithRequest(SystemLog.ActionType.LOGIN, SystemLog.EntityType.USER, 
                           "Пользователь вошел в систему: " + user.getFio(), user, request);
    }

    public void logUserLogout(User user) {
        logAction(SystemLog.ActionType.LOGOUT, SystemLog.EntityType.USER, 
                 "Пользователь вышел из системы: " + user.getFio(), user);
    }

    public void logUserRegistration(User user) {
        logAction(SystemLog.ActionType.CREATE, SystemLog.EntityType.USER, 
                 "Зарегистрирован новый пользователь: " + user.getFio(), user);
    }

    public void logMemorialCreation(Long memorialId, String memorialName, User user) {
        logAction(SystemLog.ActionType.CREATE, SystemLog.EntityType.MEMORIAL, memorialId,
                 "Создано новое захоронение: " + memorialName, user);
    }

    public void logMemorialUpdate(Long memorialId, String memorialName, User user) {
        logAction(SystemLog.ActionType.UPDATE, SystemLog.EntityType.MEMORIAL, memorialId,
                 "Обновлено захоронение: " + memorialName, user);
    }

    public void logMemorialDelete(Long memorialId, String memorialName, User user) {
        logAction(SystemLog.ActionType.DELETE, SystemLog.EntityType.MEMORIAL, memorialId,
                 "Удалено захоронение: " + memorialName, user, SystemLog.Severity.WARNING);
    }

    public void logReportGeneration(String reportType, User user) {
        logAction(SystemLog.ActionType.EXPORT, SystemLog.EntityType.REPORT,
                 "Сгенерирован отчет: " + reportType, user);
    }

    public void logError(String errorDescription, User user) {
        logAction(SystemLog.ActionType.SYSTEM, SystemLog.EntityType.SYSTEM,
                 "Ошибка: " + errorDescription, user, SystemLog.Severity.ERROR);
    }

    public void logCriticalError(String errorDescription, User user) {
        logAction(SystemLog.ActionType.SYSTEM, SystemLog.EntityType.SYSTEM,
                 "Критическая ошибка: " + errorDescription, user, SystemLog.Severity.CRITICAL);
    }
} 