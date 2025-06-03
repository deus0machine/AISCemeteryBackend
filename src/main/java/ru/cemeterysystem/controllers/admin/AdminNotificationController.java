package ru.cemeterysystem.controllers.admin;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.Notification;
import ru.cemeterysystem.repositories.NotificationRepository;
import ru.cemeterysystem.services.NotificationService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {
    private static final Logger log = LoggerFactory.getLogger(AdminNotificationController.class);
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @GetMapping
    public String notificationsList(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "type", required = false) String type,
            Model model) {
        
        // Подсчет уведомлений для статистики
        long totalNotifications = notificationRepository.count();
        long urgentNotifications = notificationRepository.countByUrgentTrue();
        long processedNotifications = notificationRepository.countByReadTrue();
        long recentNotifications = notificationRepository.countByCreatedAtAfter(LocalDateTime.now().minusMonths(1));
        
        model.addAttribute("totalNotifications", totalNotifications);
        model.addAttribute("urgentNotifications", urgentNotifications);
        model.addAttribute("processedNotifications", processedNotifications);
        model.addAttribute("recentNotifications", recentNotifications);
        
        // Получаем и фильтруем уведомления
        List<Notification> notificationList = notificationRepository.findAll();
        
        // Применяем фильтры
        if (search != null && !search.isEmpty()) {
            notificationList = notificationList.stream()
                .filter(n -> (n.getTitle() != null && n.getTitle().toLowerCase().contains(search.toLowerCase())) || 
                            (n.getMessage() != null && n.getMessage().toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());
        }
        
        if (status != null && !status.isEmpty()) {
            switch (status) {
                case "read":
                    notificationList = notificationList.stream().filter(Notification::isRead).collect(Collectors.toList());
                    break;
                case "unread":
                    notificationList = notificationList.stream().filter(n -> !n.isRead()).collect(Collectors.toList());
                    break;
                case "urgent":
                    notificationList = notificationList.stream().filter(Notification::isUrgent).collect(Collectors.toList());
                    break;
            }
        }
        
        if (type != null && !type.isEmpty()) {
            Notification.NotificationType notificationType;
            switch (type) {
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
                    notificationType = Notification.NotificationType.SYSTEM;
                    break;
                default:
                    notificationType = null;
            }
            
            if (notificationType != null) {
                final Notification.NotificationType finalType = notificationType;
                notificationList = notificationList.stream()
                    .filter(n -> n.getType() == finalType)
                    .collect(Collectors.toList());
            }
        }
        
        model.addAttribute("notificationList", notificationList);
        
        // Данные для графиков на основе реальных данных
        Map<String, Object> chartData = new HashMap<>();
        String[] days = new String[]{"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        int[] values = new int[7];
        
        // Подсчитываем количество уведомлений по дням недели
        for (Notification notification : notificationList) {
            if (notification.getCreatedAt() != null) {
                int dayOfWeek = notification.getCreatedAt().getDayOfWeek().getValue() - 1; // 0 = Monday
                if (dayOfWeek >= 0 && dayOfWeek < 7) {
                    values[dayOfWeek]++;
                }
            }
        }
        
        chartData.put("days", days);
        chartData.put("values", values);
        
        // Подсчитываем уведомления по типам
        Map<String, Integer> typeCountMap = new HashMap<>();
        typeCountMap.put("Модерация контента", 0);
        typeCountMap.put("Технические вопросы", 0);
        typeCountMap.put("Запросы пользователей", 0);
        typeCountMap.put("Системные", 0);
        
        for (Notification notification : notificationList) {
            if (notification.getType() != null) {
                switch (notification.getType()) {
                    case MODERATION:
                        typeCountMap.put("Модерация контента", typeCountMap.get("Модерация контента") + 1);
                        break;
                    case TECHNICAL:
                        typeCountMap.put("Технические вопросы", typeCountMap.get("Технические вопросы") + 1);
                        break;
                    case USER_REQUEST:
                        typeCountMap.put("Запросы пользователей", typeCountMap.get("Запросы пользователей") + 1);
                        break;
                    case SYSTEM:
                    case MEMORIAL_EDIT:
                    case MEMORIAL_OWNERSHIP:
                    default:
                        typeCountMap.put("Системные", typeCountMap.get("Системные") + 1);
                        break;
                }
            }
        }
        
        Map<String, Object> pieChartData = new HashMap<>();
        pieChartData.put("labels", typeCountMap.keySet().toArray(new String[0]));
        pieChartData.put("values", typeCountMap.values().toArray());
        
        model.addAttribute("chartData", chartData);
        model.addAttribute("pieChartData", pieChartData);
        
        return "admin/notifications";
    }
    
    @PostMapping("/mark-read")
    @ResponseBody
    public Map<String, Object> markAllAsRead() {
        long updatedCount = notificationService.markAllAsRead();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("updatedCount", updatedCount);
        
        return response;
    }
    
    @PostMapping("/mark-read/{id}")
    @ResponseBody
    public Map<String, Object> markAsRead(@PathVariable Long id) {
        boolean success = notificationService.markAsReadAdmin(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        
        return response;
    }
    
    @PostMapping("/create")
    @ResponseBody
    public Map<String, Object> createNotification(
            @RequestParam("type") String type,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "urgent", defaultValue = "false") boolean urgent,
            @RequestParam("recipientType") String recipientType,
            @RequestParam(value = "sendEmail", defaultValue = "false") boolean sendEmail,
            @RequestParam(value = "recipientIds", required = false) Long[] recipientIds) {
        
        try {
        Notification notification = notificationService.createNotification(
                    type, title, content, urgent, recipientType, recipientIds);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", notification != null);
        if (notification != null) {
            response.put("notificationId", notification.getId());
                response.put("message", "Уведомление успешно создано и отправлено");
            } else {
                response.put("message", "Ошибка создания уведомления");
            }
            
            // TODO: Реализовать отправку email-уведомлений если sendEmail = true
            if (sendEmail) {
                // Здесь можно добавить логику отправки email
                log.info("Запрос на отправку email-уведомления (функция не реализована)");
        }
        
        return response;
        } catch (Exception e) {
            log.error("Ошибка при создании уведомления: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }
    
    @PostMapping("/settings")
    @ResponseBody
    public Map<String, Object> saveSettings(
            @RequestParam("autoDeletion") String autoDeletion,
            @RequestParam(value = "emailNotification", defaultValue = "false") boolean emailNotification,
            @RequestParam(value = "emailUrgentOnly", defaultValue = "false") boolean emailUrgentOnly,
            @RequestParam("digestFrequency") String digestFrequency) {
        
        // В реальном приложении здесь был бы код для сохранения настроек
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        
        return response;
    }
    
    @PostMapping("/cleanup")
    @ResponseBody
    public Map<String, Object> cleanupNotifications() {
        long deletedCount = notificationService.cleanupExcessNotifications();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("deletedCount", deletedCount);
        
        return response;
    }
    
    /**
     * Ответ на техническое уведомление
     */
    @PostMapping("/technical-response")
    @ResponseBody
    public Map<String, Object> respondToTechnical(
            @RequestParam("notificationId") Long notificationId,
            @RequestParam("response") String response) {
        
        try {
            boolean success = notificationService.respondToTechnicalSupport(notificationId, response);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", success);
            
            if (!success) {
                responseData.put("message", "Не удалось отправить ответ");
            }
            
            return responseData;
        } catch (Exception e) {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", false);
            responseData.put("message", e.getMessage());
            return responseData;
        }
    }
} 