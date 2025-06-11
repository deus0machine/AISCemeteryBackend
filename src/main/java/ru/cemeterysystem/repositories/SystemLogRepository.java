package ru.cemeterysystem.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.SystemLog;
import ru.cemeterysystem.models.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    
    // Поиск логов по пользователю
    Page<SystemLog> findByUser(User user, Pageable pageable);
    
    // Поиск логов по типу действия
    Page<SystemLog> findByActionType(SystemLog.ActionType actionType, Pageable pageable);
    
    // Поиск логов по типу сущности
    Page<SystemLog> findByEntityType(SystemLog.EntityType entityType, Pageable pageable);
    
    // Поиск логов по уровню важности
    Page<SystemLog> findBySeverity(SystemLog.Severity severity, Pageable pageable);
    
    // Поиск логов по диапазону дат
    Page<SystemLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Поиск логов по описанию (содержит текст)
    Page<SystemLog> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);
    
    // Поиск логов по IP адресу
    Page<SystemLog> findByIpAddress(String ipAddress, Pageable pageable);
    
    // Комплексный поиск
    @Query("SELECT sl FROM SystemLog sl WHERE " +
           "(:actionType IS NULL OR sl.actionType = :actionType) AND " +
           "(:entityType IS NULL OR sl.entityType = :entityType) AND " +
           "(:severity IS NULL OR sl.severity = :severity) AND " +
           "(:user IS NULL OR sl.user = :user) AND " +
           "(:startDate IS NULL OR sl.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR sl.createdAt <= :endDate) AND " +
           "(:searchText IS NULL OR LOWER(sl.description) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    Page<SystemLog> findLogsWithFilters(
            @Param("actionType") SystemLog.ActionType actionType,
            @Param("entityType") SystemLog.EntityType entityType,
            @Param("severity") SystemLog.Severity severity,
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("searchText") String searchText,
            Pageable pageable
    );
    
    // Получить последние действия (для dashboard)
    List<SystemLog> findTop10ByOrderByCreatedAtDesc();
    
    // Получить последние 5 действий (для dashboard)
    List<SystemLog> findTop5ByOrderByCreatedAtDesc();
    
    // Подсчет логов по типу действия за период
    @Query("SELECT COUNT(sl) FROM SystemLog sl WHERE sl.actionType = :actionType AND sl.createdAt >= :startDate")
    Long countByActionTypeAndCreatedAtAfter(@Param("actionType") SystemLog.ActionType actionType, 
                                           @Param("startDate") LocalDateTime startDate);
    
    // Подсчет всех логов за период
    @Query("SELECT COUNT(sl) FROM SystemLog sl WHERE sl.createdAt >= :startDate")
    Long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);
    
    // Подсчет логов по уровню важности за период
    @Query("SELECT COUNT(sl) FROM SystemLog sl WHERE sl.severity = :severity AND sl.createdAt >= :startDate")
    Long countBySeverityAndCreatedAtAfter(@Param("severity") SystemLog.Severity severity, 
                                         @Param("startDate") LocalDateTime startDate);
    
    // Получить топ пользователей по активности
    @Query("SELECT sl.user, COUNT(sl) FROM SystemLog sl WHERE sl.user IS NOT NULL " +
           "AND sl.createdAt >= :startDate GROUP BY sl.user ORDER BY COUNT(sl) DESC")
    List<Object[]> findTopActiveUsers(@Param("startDate") LocalDateTime startDate, Pageable pageable);
    
    // Статистика по дням (простой подход)
    @Query("SELECT sl.createdAt, COUNT(sl) FROM SystemLog sl " +
           "WHERE sl.createdAt >= :startDate GROUP BY sl.createdAt ORDER BY sl.createdAt")
    List<Object[]> getActivityByDays(@Param("startDate") LocalDateTime startDate);
    
    // Удаление старых логов
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
} 