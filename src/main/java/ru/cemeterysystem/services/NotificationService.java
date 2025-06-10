package ru.cemeterysystem.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.ArrayList;
import java.util.Arrays;
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
                .filter(notification -> !notification.getUser().getId().equals(notification.getSender().getId()))
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
    
    // Создать техническое уведомление для администраторов
    public NotificationDTO createTechnicalSupport(Long senderId, String message) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));
        
        // Найти всех администраторов
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        if (admins.isEmpty()) {
            throw new RuntimeException("Не найдено ни одного администратора");
        }
        
        // Создаем уведомление для каждого администратора
        Notification firstNotification = null;
        for (User admin : admins) {
            Notification notification = new Notification();
            notification.setTitle("Технические вопросы");
            notification.setMessage(message);
            notification.setUser(admin);
            notification.setSender(sender);
            notification.setType(Notification.NotificationType.TECHNICAL);
            notification.setStatus(Notification.NotificationStatus.INFO);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            notification.setUrgent(false);
            
            Notification saved = notificationRepository.save(notification);
            if (firstNotification == null) {
                firstNotification = saved;
            }
        }
        
        return convertToDTO(firstNotification);
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
                // Используем тот же тип MEMORIAL_OWNERSHIP для ответного уведомления
                responseNotification.setType(Notification.NotificationType.MEMORIAL_OWNERSHIP);
                responseNotification.setStatus(accept ? 
                    Notification.NotificationStatus.ACCEPTED : 
                    Notification.NotificationStatus.REJECTED);
                
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
    
    // --- Методы для административного интерфейса ---
    
    // Отметить все уведомления как прочитанные
    @Transactional
    public long markAllAsRead() {
        List<Notification> unreadNotifications = notificationRepository.findByReadFalse();
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
        return unreadNotifications.size();
    }
    
    // Отметить одно уведомление как прочитанное для админ-панели
    public boolean markAsReadAdmin(Long id) {
        try {
            Optional<Notification> notification = notificationRepository.findById(id);
            if (notification.isPresent()) {
                Notification notif = notification.get();
                notif.setRead(true);
                notificationRepository.save(notif);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage(), e);
            return false;
        }
    }
    
    // Создать административное уведомление
    @Transactional
    public Notification createNotification(String type, String title, String content, boolean urgent, String recipientType, Long[] recipientIds) {
        // Определяем тип уведомления
        Notification.NotificationType notificationType;
        switch (type) {
            case "admin_info":
                notificationType = Notification.NotificationType.INFO;
                break;
            case "mass_announcement":
                notificationType = Notification.NotificationType.MASS_ANNOUNCEMENT;
                break;
            case "admin_system":
                notificationType = Notification.NotificationType.ADMIN_SYSTEM;
                break;
            case "admin_warning":
                notificationType = Notification.NotificationType.ADMIN_WARNING;
                break;
            case "moderation":
                notificationType = Notification.NotificationType.MODERATION;
                break;
            case "technical":
                notificationType = Notification.NotificationType.TECHNICAL;
                break;
            case "user":
                notificationType = Notification.NotificationType.USER_REQUEST;
                break;
            case "system":
            default:
                notificationType = Notification.NotificationType.SYSTEM;
                break;
        }
        
        List<User> recipients = new ArrayList<>();
        
        // Определяем получателей в зависимости от типа
        if ("all_users".equals(recipientType)) {
            // Всем пользователям
            recipients = userRepository.findAll();
            log.info("Отправка уведомления всем пользователям: {} получателей", recipients.size());
        } else if ("all_admins".equals(recipientType)) {
            // Всем администраторам
            recipients = userRepository.findByRole(User.Role.ADMIN);
            log.info("Отправка уведомления всем администраторам: {} получателей", recipients.size());
        } else if ("specific_user".equals(recipientType) || "multiple_users".equals(recipientType)) {
            // Конкретным пользователям
        if (recipientIds != null && recipientIds.length > 0) {
                recipients = userRepository.findAllById(Arrays.asList(recipientIds));
                log.info("Отправка уведомления выбранным пользователям: {} получателей", recipients.size());
            } else {
                log.warn("Не указаны ID получателей для типа {}", recipientType);
                throw new RuntimeException("Не указаны получатели уведомления");
            }
        } else {
            log.warn("Неизвестный тип получателей: {}", recipientType);
            throw new RuntimeException("Неизвестный тип получателей: " + recipientType);
        }
        
        if (recipients.isEmpty()) {
            log.warn("Не найдено получателей для отправки уведомления");
            throw new RuntimeException("Не найдено получателей для отправки уведомления");
        }
        
        // Получаем текущего администратора как отправителя
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User admin = null;
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            admin = userRepository.findByLogin(username).orElse(null);
        }
        
        Notification firstNotification = null;
            
            // Создаем отдельные уведомления для каждого получателя
            for (User recipient : recipients) {
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setMessage(content);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            notification.setUrgent(urgent);
            notification.setType(notificationType);
            notification.setStatus(Notification.NotificationStatus.INFO);
            notification.setUser(recipient);
            notification.setSender(admin); // Устанавливаем отправителя
            
            Notification saved = notificationRepository.save(notification);
            if (firstNotification == null) {
                firstNotification = saved;
            }
            
            log.debug("Создано уведомление ID={} для пользователя ID={}", saved.getId(), recipient.getId());
        }
        
        log.info("Успешно создано {} уведомлений типа {} с заголовком '{}'", 
                recipients.size(), notificationType, title);
        
        return firstNotification; // Возвращаем первое созданное уведомление
    }

    @Transactional
    public long cleanupExcessNotifications() {
        try {
            // Получаем общее количество перед удалением
            long beforeCount = notificationRepository.count();
            
            // Получаем все прочитанные уведомления
            List<Notification> readNotifications = notificationRepository.findAll()
                .stream()
                .filter(n -> n.isRead() && 
                       (n.getStatus() == Notification.NotificationStatus.ACCEPTED || 
                        n.getStatus() == Notification.NotificationStatus.REJECTED || 
                        n.getStatus() == Notification.NotificationStatus.INFO))
                .collect(Collectors.toList());
                
            // Логируем количество прочитанных уведомлений
            log.info("Найдено {} прочитанных уведомлений для удаления", readNotifications.size());
            
            // Проверяем наличие тестового уведомления среди прочитанных
            List<Notification> keepNotifications = readNotifications.stream()
                .filter(n -> n.getTitle() != null && n.getTitle().equals("Мемориал опубликован") && 
                       n.getMessage() != null && n.getMessage().contains("Сергеев Иван Андреевич"))
                .collect(Collectors.toList());
                
            // Если найдено тестовое уведомление среди прочитанных, не удаляем его
            if (!keepNotifications.isEmpty()) {
                log.info("Найдено тестовое уведомление из DataLoader среди прочитанных - оно будет сохранено");
                
                // Получаем ID тестового уведомления, которое нужно сохранить
                List<Long> idsToKeep = keepNotifications.stream()
                    .map(Notification::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
                    
                // Удаляем все прочитанные, кроме тестового
                readNotifications.stream()
                    .filter(n -> !idsToKeep.contains(n.getId()))
                    .forEach(notificationRepository::delete);
            } else {
                // Удаляем все прочитанные уведомления
                readNotifications.forEach(notificationRepository::delete);
            }
            
            // Подсчитываем, сколько было удалено
            long afterCount = notificationRepository.count();
            long deletedCount = beforeCount - afterCount;
            
            log.info("Очистка уведомлений: удалено {} прочитанных уведомлений, оставлено {}", 
                    deletedCount, afterCount);
            
            return deletedCount;
        } catch (Exception e) {
            log.error("Ошибка при очистке уведомлений: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Ответить на техническое уведомление
     */
    @Transactional
    public boolean respondToTechnicalSupport(Long notificationId, String responseMessage) {
        try {
            log.info("Ответ на техническое уведомление ID={}", notificationId);
            
            Notification originalNotification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Уведомление не найдено"));
            
            // Проверяем, что это техническое уведомление
            if (originalNotification.getType() != Notification.NotificationType.TECHNICAL) {
                throw new RuntimeException("Можно отвечать только на технические уведомления");
            }
            
            // Получаем отправителя оригинального уведомления
            User originalSender = originalNotification.getSender();
            if (originalSender == null) {
                throw new RuntimeException("Не найден отправитель оригинального уведомления");
            }
            
            // Получаем текущего администратора (получателя оригинального уведомления)
            User admin = originalNotification.getUser();
            
            // Создаем ответное уведомление
            Notification responseNotification = new Notification();
            responseNotification.setTitle("Ответ на техническое обращение");
            responseNotification.setMessage(responseMessage);
            responseNotification.setUser(originalSender); // Отправляем пользователю
            responseNotification.setSender(admin); // От администратора
            responseNotification.setType(Notification.NotificationType.SYSTEM);
            responseNotification.setStatus(Notification.NotificationStatus.INFO);
            responseNotification.setCreatedAt(LocalDateTime.now());
            responseNotification.setRead(false);
            responseNotification.setUrgent(false);
            
            // Связываем с оригинальным уведомлением через relatedEntityId, но не указываем relatedEntityName
            responseNotification.setRelatedEntityId(originalNotification.getId());
            
            notificationRepository.save(responseNotification);
            
            // Отмечаем оригинальное уведомление как обработанное
            originalNotification.setStatus(Notification.NotificationStatus.PROCESSED);
            originalNotification.setRead(true);
            notificationRepository.save(originalNotification);
            
            log.info("Создан ответ на техническое уведомление для пользователя ID={}", originalSender.getId());
            return true;
        } catch (Exception e) {
            log.error("Ошибка при ответе на техническое уведомление: {}", e.getMessage(), e);
            return false;
        }
    }
} 