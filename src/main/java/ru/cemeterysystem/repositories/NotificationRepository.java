package ru.cemeterysystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId);
    
    // Находит уведомления, отправленные пользователем
    List<Notification> findBySenderId(Long senderId);
    
    // Находит уведомления определенного типа для пользователя
    List<Notification> findByUserIdAndType(Long userId, Notification.NotificationType type);
    
    // Находит непрочитанные уведомления пользователя
    List<Notification> findByUserIdAndReadIsFalse(Long userId);
    
    // Находит уведомления, ожидающие ответа
    List<Notification> findByUserIdAndStatus(Long userId, Notification.NotificationStatus status);
    
    // Находит уведомления для конкретного мемориала с определенным типом и статусом
    List<Notification> findByRelatedEntityIdAndTypeAndStatus(
        Long relatedEntityId, 
        Notification.NotificationType type, 
        Notification.NotificationStatus status
    );
    
    // Находит все уведомления, связанные с определенной сущностью
    List<Notification> findByRelatedEntityId(Long relatedEntityId);
    
    // Находит уведомления пользователя для определенной сущности, с определенным типом и статусом
    List<Notification> findByUserIdAndRelatedEntityIdAndTypeAndStatus(
        Long userId,
        Long relatedEntityId, 
        Notification.NotificationType type, 
        Notification.NotificationStatus status
    );
} 