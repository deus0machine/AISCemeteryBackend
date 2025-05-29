package ru.cemeterysystem.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.dto.MemorialOwnershipRequestDTO;
import ru.cemeterysystem.dto.NotificationDTO;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.services.NotificationService;
import ru.cemeterysystem.services.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * Получает текущего аутентифицированного пользователя.
     *
     * @return текущий пользователь
     * @throws RuntimeException если пользователь не найден
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Получает все уведомления текущего пользователя
     */
    @GetMapping
    public List<NotificationDTO> getUserNotifications() {
        User currentUser = getCurrentUser();
        List<NotificationDTO> notifications = notificationService.getUserNotifications(currentUser.getId());
        
        // Если пользователь админ, фильтруем уведомления, чтобы не показывать самоуведомления пользователей
        if (currentUser.getRole() == User.Role.ADMIN) {
            return notifications.stream()
                .filter(notification -> {
                    // Исключаем уведомления, где:
                    // 1. Отправитель и получатель - один и тот же человек (не админ)
                    // 2. Тип уведомления - SYSTEM или INFO (не требует действий)
                    return !(notification.getSenderId() != null && 
                           notification.getReceiverId() != null &&
                           notification.getSenderId().equals(notification.getReceiverId()) &&
                           !notification.getSenderId().equals(currentUser.getId()) &&
                           (notification.getType().equals("SYSTEM") || 
                            notification.getStatus().equals("INFO")));
                })
                .collect(Collectors.toList());
        }
        
        return notifications;
    }

    /**
     * Получает все отправленные текущим пользователем уведомления
     */
    @GetMapping("/sent")
    public List<NotificationDTO> getSentNotifications() {
        User currentUser = getCurrentUser();
        return notificationService.getSentNotifications(currentUser.getId());
    }

    /**
     * Создает запрос на совместное владение мемориалом
     */
    @PostMapping("/memorial-ownership")
    public NotificationDTO createMemorialOwnershipRequest(
            @RequestBody MemorialOwnershipRequestDTO requestDTO) {
        User sender = getCurrentUser();
        Long receiverId = Long.parseLong(requestDTO.getReceiverId());
        Long memorialId = Long.parseLong(requestDTO.getMemorialId());
        String message = requestDTO.getMessage();

        return notificationService.createMemorialOwnershipRequest(
                sender.getId(), receiverId, memorialId, message);
    }

    /**
     * Отвечает на запрос (принимает или отклоняет)
     */
    @PostMapping("/{id}/respond")
    public ResponseEntity<Map<String, Object>> respondToNotification(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        
        Boolean accept = request.get("accept");
        if (accept == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Missing 'accept' parameter");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            NotificationDTO notification = notificationService.respondToNotification(id, accept);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", notification);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Отмечает уведомление как прочитанное
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id) {
        NotificationDTO notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    /**
     * Удаляет уведомление
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok().build();
    }
} 