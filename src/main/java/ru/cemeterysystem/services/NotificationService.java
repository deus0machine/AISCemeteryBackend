package ru.cemeterysystem.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MemorialRepository memorialRepository;
    private final MemorialService memorialService;

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
    @Transactional
    public NotificationDTO respondToNotification(Long notificationId, boolean accept) {
        log.info("Обработка ответа на уведомление ID={}, принято={}", notificationId, accept);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Уведомление не найдено"));
        
        notification.setStatus(accept 
                ? Notification.NotificationStatus.ACCEPTED 
                : Notification.NotificationStatus.REJECTED);
        notification.setRead(true);
        
        // Обработка запроса на совместное владение мемориалом
        if (accept && notification.getType() == Notification.NotificationType.MEMORIAL_OWNERSHIP && 
            notification.getRelatedEntityId() != null) {
            
            Long memorialId = notification.getRelatedEntityId();
            User sender = notification.getSender();
            
            log.info("Добавление пользователя ID={} как редактора мемориала ID={}", 
                    sender.getId(), memorialId);
            
            try {
                Memorial memorial = memorialRepository.findById(memorialId)
                        .orElseThrow(() -> new RuntimeException("Мемориал не найден"));
                
                // Добавляем отправителя запроса как редактора мемориала
                memorial.addEditor(sender);
                memorialRepository.save(memorial);
                
                log.info("Успешно добавлен редактор. Пользователь ID={} теперь редактор мемориала ID={}", 
                        sender.getId(), memorialId);
                
                // Проверяем, действительно ли редактор был добавлен
                Memorial savedMemorial = memorialRepository.findById(memorialId).orElse(null);
                if (savedMemorial != null) {
                    boolean isEditor = savedMemorial.getEditors().contains(sender);
                    log.info("Проверка: пользователь {} является редактором мемориала: {}", 
                            sender.getId(), isEditor);
                    
                    if (!isEditor) {
                        log.warn("Пользователь не был добавлен как редактор после сохранения!");
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка при добавлении редактора к мемориалу: {}", e.getMessage(), e);
                throw new RuntimeException("Не удалось добавить редактора: " + e.getMessage());
            }
        }
        // Обработка уведомлений об изменении мемориала редактором
        else if (notification.getType() == Notification.NotificationType.MEMORIAL_EDIT && 
                 notification.getRelatedEntityId() != null) {
            log.info("Обработка ответа на уведомление о редактировании мемориала ID={}", 
                     notification.getRelatedEntityId());
                
            // Получаем текущего пользователя и мемориал
            User currentUser = notification.getUser(); // Получатель уведомления (владелец)
            Long memorialId = notification.getRelatedEntityId();
            
            try {
                log.info("Вызываем MemorialService.approveChanges для мемориала ID={}, approve={}", 
                         memorialId, accept);
                memorialService.approveChanges(memorialId, accept, currentUser);
                log.info("Успешно применены/отклонены изменения мемориала ID={}", memorialId);
            } catch (Exception e) {
                log.error("Ошибка при обработке изменений мемориала: {}", e.getMessage(), e);
            }
        }
        
        Notification updated = notificationRepository.save(notification);
        
        // Создаем встречное уведомление для отправителя
        if (notification.getSender() != null && notification.getRelatedEntityId() != null) {
            User sender = notification.getSender();
            User receiver = notification.getUser();
            
            Notification responseNotification = new Notification();
            responseNotification.setUser(sender);
            responseNotification.setSender(receiver);
            responseNotification.setRelatedEntityId(notification.getRelatedEntityId());
            responseNotification.setRelatedEntityName(notification.getRelatedEntityName());
            responseNotification.setRead(false);
            responseNotification.setCreatedAt(LocalDateTime.now());
            
            // Установка заголовка и сообщения в зависимости от типа и статуса
            if (notification.getType() == Notification.NotificationType.MEMORIAL_OWNERSHIP) {
                responseNotification.setType(Notification.NotificationType.SYSTEM);
                responseNotification.setStatus(Notification.NotificationStatus.INFO);
                
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
            } 
            // Обработка ответов на уведомления о редактировании
            else if (notification.getType() == Notification.NotificationType.MEMORIAL_EDIT) {
                responseNotification.setType(Notification.NotificationType.MEMORIAL_EDIT);
                responseNotification.setStatus(accept ? 
                    Notification.NotificationStatus.ACCEPTED : 
                    Notification.NotificationStatus.REJECTED);
                
                if (accept) {
                    responseNotification.setTitle("Изменения в мемориале приняты");
                    responseNotification.setMessage(
                        String.format("Ваши изменения в мемориале \"%s\" были приняты владельцем %s", 
                        notification.getRelatedEntityName(), receiver.getFio()));
                } else {
                    responseNotification.setTitle("Изменения в мемориале отклонены");
                    responseNotification.setMessage(
                        String.format("Ваши изменения в мемориале \"%s\" были отклонены владельцем %s", 
                        notification.getRelatedEntityName(), receiver.getFio()));
                }
            }
            
            notificationRepository.save(responseNotification);
            log.info("Создано ответное уведомление ID={} для пользователя ID={}", 
                     responseNotification.getId(), sender.getId());
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
        
        // Настраиваем данные отправителя и получателя
        if (notification.getSender() != null) {
            dto.setSenderId(notification.getSender().getId());
            dto.setSenderName(notification.getSender().getFio());
            
            // Добавляем информацию о получателе (для исходящих уведомлений)
            dto.setReceiverId(notification.getUser().getId());
            dto.setReceiverName(notification.getUser().getFio());
        }
        
        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setRelatedEntityName(notification.getRelatedEntityName());
        
        // Логируем преобразование для отладки
        log.debug("Преобразовано уведомление ID={}, Тип={}, От: {} -> Кому: {}", 
                notification.getId(), notification.getType(),
                notification.getSender() != null ? notification.getSender().getId() : "null",
                notification.getUser().getId());
        
        return dto;
    }
} 