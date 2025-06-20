package ru.cemeterysystem.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    // ID объекта, к которому относится уведомление (мемориал, дерево и т.д.)
    @Column(name = "related_entity_id")
    private Long relatedEntityId;
    
    // Имя объекта, к которому относится уведомление (например, ФИО мемориала)
    @Column(name = "related_entity_name", length = 255)
    private String relatedEntityName;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean read = false;
    
    // Флаг срочности уведомления
    @Column(nullable = false)
    private boolean urgent = false;

    public enum NotificationType {
        INFO,                     // Информационное уведомление
        MEMORIAL_OWNERSHIP,       // Запрос на совместное владение мемориалом
        TREE_ACCESS_REQUEST,      // Запрос на доступ к дереву
        MEMORIAL_COMMENT,         // Комментарий к мемориалу
        ANNIVERSARY,              // Годовщина
        SYSTEM,                   // Системное уведомление
        MEMORIAL_EDIT,            // Уведомление о изменении мемориала
        MODERATION,               // Уведомление о модерации контента
        CHANGES_REJECTED,         // Уведомление об отклонении изменений мемориала
        TECHNICAL,                // Техническое уведомление
        USER_REQUEST,             // Запрос от пользователя
        MEMORIAL_MODERATION,      // Новый мемориал на модерации
        CHANGES_MODERATION,       // Изменения мемориала на модерации
        MEMORIAL_APPROVED,        // Мемориал одобрен администратором
        MEMORIAL_REJECTED,        // Мемориал отклонен администратором
        ADMIN_INFO,               // Информация от администратора
        MASS_ANNOUNCEMENT,        // Массовое оповещение о важном
        ADMIN_SYSTEM,             // Административное системное уведомление 
        ADMIN_WARNING,            // Предупреждение от администратора
        MEMORIAL_REPORT,          // Жалоба на мемориал
        FAMILY_TREE_MODERATION    // Модерация семейного дерева
    }
    
    public enum NotificationStatus {
        PENDING,    // Ожидает ответа
        ACCEPTED,   // Принято
        REJECTED,   // Отклонено
        INFO,       // Информационное (не требует ответа)
        PROCESSED   // Обработано (для технических уведомлений)
    }
} 