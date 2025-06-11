package ru.cemeterysystem.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "system_logs")
public class SystemLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;
    
    @Column(name = "entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;
    
    @Column(name = "entity_id")
    private Long entityId;
    
    @Column(name = "description", nullable = false)
    private String description;
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    public enum ActionType {
        CREATE,     // Создание
        UPDATE,     // Обновление  
        DELETE,     // Удаление
        LOGIN,      // Вход в систему
        LOGOUT,     // Выход из системы
        VIEW,       // Просмотр
        APPROVE,    // Одобрение
        REJECT,     // Отклонение
        BLOCK,      // Блокировка
        UNBLOCK,    // Разблокировка
        EXPORT,     // Экспорт данных
        IMPORT,     // Импорт данных
        BACKUP,     // Резервное копирование
        RESTORE,    // Восстановление
        SYSTEM      // Системное действие
    }
    
    public enum EntityType {
        USER,           // Пользователь
        MEMORIAL,       // Мемориал
        FAMILY_TREE,    // Семейное древо
        NOTIFICATION,   // Уведомление
        REPORT,         // Отчет
        SYSTEM,         // Система
        SETTINGS,       // Настройки
        FILE,           // Файл
        BACKUP          // Резервная копия
    }
    
    public enum Severity {
        INFO,       // Информационное
        WARNING,    // Предупреждение
        ERROR,      // Ошибка
        CRITICAL    // Критическое
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (severity == null) {
            severity = Severity.INFO;
        }
    }
    
    // Конструктор для быстрого создания логов
    public SystemLog(ActionType actionType, EntityType entityType, String description, User user) {
        this.actionType = actionType;
        this.entityType = entityType;
        this.description = description;
        this.user = user;
        this.severity = Severity.INFO;
        this.createdAt = LocalDateTime.now();
    }
    
    public SystemLog(ActionType actionType, EntityType entityType, Long entityId, String description, User user) {
        this(actionType, entityType, description, user);
        this.entityId = entityId;
    }
    
    public SystemLog(ActionType actionType, EntityType entityType, String description, User user, Severity severity) {
        this(actionType, entityType, description, user);
        this.severity = severity;
    }
} 