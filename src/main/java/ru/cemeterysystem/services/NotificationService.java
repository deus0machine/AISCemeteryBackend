package ru.cemeterysystem.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.Notification;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.NotificationRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.dto.NotificationDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MemorialRepository memorialRepository;

    // Получить все уведомления пользователя
    public List<NotificationDTO> getUserNotifications(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    // Получить отправленные пользователем уведомления
    public List<NotificationDTO> getSentNotifications(Long userId) {
        return notificationRepository.findBySenderId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    // Создать запрос на совместное владение мемориалом
    public NotificationDTO createMemorialOwnershipRequest(Long senderId, Long receiverId, Long memorialId, String message) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Получатель не найден"));
        Memorial memorial = memorialRepository.findById(memorialId)
                .orElseThrow(() -> new RuntimeException("Мемориал не найден"));
        
        Notification notification = new Notification();
        notification.setTitle("Запрос на совместное владение мемориалом");
        notification.setMessage(message);
        notification.setUser(receiver);
        notification.setSender(sender);
        notification.setType(Notification.NotificationType.MEMORIAL_OWNERSHIP);
        notification.setStatus(Notification.NotificationStatus.PENDING);
        notification.setRelatedEntityId(memorialId);
        notification.setRelatedEntityName(memorial.getFio());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        
        Notification saved = notificationRepository.save(notification);
        return convertToDTO(saved);
    }
    
    // Ответить на запрос уведомления
    public NotificationDTO respondToNotification(Long notificationId, boolean accept) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Уведомление не найдено"));
        
        notification.setStatus(accept 
                ? Notification.NotificationStatus.ACCEPTED 
                : Notification.NotificationStatus.REJECTED);
        notification.setRead(true);
        
        Notification updated = notificationRepository.save(notification);
        
        // Создаем встречное уведомление для отправителя
        if (notification.getSender() != null && notification.getRelatedEntityId() != null) {
            User sender = notification.getSender();
            User receiver = notification.getUser();
            
            Notification responseNotification = new Notification();
            responseNotification.setUser(sender);
            responseNotification.setSender(receiver);
            responseNotification.setType(Notification.NotificationType.SYSTEM);
            responseNotification.setStatus(Notification.NotificationStatus.INFO);
            responseNotification.setRead(false);
            responseNotification.setCreatedAt(LocalDateTime.now());
            responseNotification.setRelatedEntityId(notification.getRelatedEntityId());
            responseNotification.setRelatedEntityName(notification.getRelatedEntityName());
            
            // Установка заголовка и сообщения
            if (accept) {
                responseNotification.setTitle("Запрос на совместное владение принят");
                responseNotification.setMessage(
                    String.format("Ваш запрос на совместное владение мемориалом \"%s\" был принят пользователем %s", 
                    notification.getRelatedEntityName(), receiver.getFio()));
            } else {
                responseNotification.setTitle("Запрос на совместное владение отклонен");
                responseNotification.setMessage(
                    String.format("Ваш запрос на совместное владение мемориалом \"%s\" был отклонен пользователем %s", 
                    notification.getRelatedEntityName(), receiver.getFio()));
            }
            
            notificationRepository.save(responseNotification);
        }
        
        return convertToDTO(updated);
    }
    
    // Отметить уведомление как прочитанное
    public NotificationDTO markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Уведомление не найдено"));
        
        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        return convertToDTO(updated);
    }
    
    // Удалить уведомление
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
    
    // Конвертировать в DTO
    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType().name());
        dto.setStatus(notification.getStatus().name());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt().toString());
        dto.setUserId(notification.getUser().getId());
        
        if (notification.getSender() != null) {
            dto.setSenderId(notification.getSender().getId());
            dto.setSenderName(notification.getSender().getFio());
        }
        
        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setRelatedEntityName(notification.getRelatedEntityName());
        
        return dto;
    }
} 