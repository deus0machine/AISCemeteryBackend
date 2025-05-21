package ru.cemeterysystem.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.dto.MemorialOwnershipRequestDTO;
import ru.cemeterysystem.dto.NotificationDTO;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.NotificationService;
import ru.cemeterysystem.services.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * Получает текущего аутентифицированного пользователя.
     *
     * @return текущий пользователь
     * @throws RuntimeException если пользователь не найден
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        return userService.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Получает все уведомления текущего пользователя
     */
    @GetMapping("/my")
    public List<NotificationDTO> getMyNotifications() {
        User user = getCurrentUser();
        return notificationService.getUserNotifications(user.getId());
    }

    /**
     * Получает все отправленные текущим пользователем уведомления
     */
    @GetMapping("/sent")
    public List<NotificationDTO> getSentNotifications() {
        User user = getCurrentUser();
        return notificationService.getSentNotifications(user.getId());
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
    public NotificationDTO respondToNotification(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> requestData) {
        boolean accept = requestData.get("accept");
        return notificationService.respondToNotification(id, accept);
    }

    /**
     * Отмечает уведомление как прочитанное
     */
    @PostMapping("/{id}/read")
    public NotificationDTO markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
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