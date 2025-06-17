package ru.cemeterysystem.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.Notification;

import java.time.LocalDateTime;
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
    
    // Находит уведомления для конкретного мемориала с определенным типом
    List<Notification> findByRelatedEntityIdAndType(
        Long relatedEntityId,
        Notification.NotificationType type
    );
    
    // Находит уведомления пользователя для определенной сущности, с определенным типом и статусом
    List<Notification> findByUserIdAndRelatedEntityIdAndTypeAndStatus(
        Long userId,
        Long relatedEntityId, 
        Notification.NotificationType type, 
        Notification.NotificationStatus status
    );
    
    // Методы для админ-панели уведомлений
    
    // Находит все непрочитанные уведомления
    List<Notification> findByReadFalse();
    
    // Подсчитывает количество срочных уведомлений
    long countByUrgentTrue();
    
    // Подсчитывает количество прочитанных уведомлений
    long countByReadTrue();
    
    // Подсчитывает количество уведомлений после указанной даты
    long countByCreatedAtAfter(LocalDateTime date);
    
    // Подсчёт уведомлений по статусу
    long countByStatus(Notification.NotificationStatus status);
    
    // Подсчёт уведомлений по прочтению
    long countByRead(boolean read);
    
    // Методы для пагинации в админ-панели
    
    // Все уведомления с пагинацией, отсортированные по дате создания (новые сверху)
    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Поиск по заголовку или сообщению с пагинацией
    Page<Notification> findByTitleContainingIgnoreCaseOrMessageContainingIgnoreCaseOrderByCreatedAtDesc(
        String title, String message, Pageable pageable);
    
    // Фильтрация по статусу прочтения с пагинацией
    Page<Notification> findByReadOrderByCreatedAtDesc(boolean read, Pageable pageable);
    
    // Фильтрация по срочности с пагинацией
    Page<Notification> findByUrgentOrderByCreatedAtDesc(boolean urgent, Pageable pageable);
    
    // Фильтрация по типу с пагинацией
    Page<Notification> findByTypeOrderByCreatedAtDesc(Notification.NotificationType type, Pageable pageable);
} 