package ru.cemeterysystem.controllers.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.cemeterysystem.models.SystemLog;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.services.SystemLogService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogsController {

    private final SystemLogService systemLogService;
    private final UserRepository userRepository;

    /**
     * Главная страница логов
     */
    @GetMapping
    public String logsPage(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String actionType,
                          @RequestParam(required = false) String entityType,
                          @RequestParam(required = false) String severity,
                          @RequestParam(required = false) Long userId,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                          @RequestParam(required = false) String searchText,
                          Model model) {

        // Конвертируем строковые параметры в enum'ы
        SystemLog.ActionType actionTypeEnum = null;
        if (actionType != null && !actionType.isEmpty()) {
            try {
                actionTypeEnum = SystemLog.ActionType.valueOf(actionType);
            } catch (IllegalArgumentException e) {
                // Игнорируем неверные значения
            }
        }

        SystemLog.EntityType entityTypeEnum = null;
        if (entityType != null && !entityType.isEmpty()) {
            try {
                entityTypeEnum = SystemLog.EntityType.valueOf(entityType);
            } catch (IllegalArgumentException e) {
                // Игнорируем неверные значения
            }
        }

        SystemLog.Severity severityEnum = null;
        if (severity != null && !severity.isEmpty()) {
            try {
                severityEnum = SystemLog.Severity.valueOf(severity);
            } catch (IllegalArgumentException e) {
                // Игнорируем неверные значения
            }
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // Получаем логи с фильтрацией
        Page<SystemLog> logs = systemLogService.getLogsWithFilters(
            actionTypeEnum, entityTypeEnum, severityEnum, user,
            startDate, endDate, searchText, page, size
        );

        // Добавляем данные в модель
        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logs.getTotalPages());
        model.addAttribute("totalElements", logs.getTotalElements());

        // Параметры фильтров для сохранения состояния
        model.addAttribute("actionType", actionType);
        model.addAttribute("entityType", entityType);
        model.addAttribute("severity", severity);
        model.addAttribute("userId", userId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("searchText", searchText);
        model.addAttribute("size", size);

        // Данные для выпадающих списков
        model.addAttribute("actionTypes", Arrays.asList(SystemLog.ActionType.values()));
        model.addAttribute("entityTypes", Arrays.asList(SystemLog.EntityType.values()));
        model.addAttribute("severities", Arrays.asList(SystemLog.Severity.values()));
        
        // Получаем всех пользователей для фильтра
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);

        // Получаем статистику
        Map<String, Object> statistics = systemLogService.getLogStatistics();
        model.addAttribute("statistics", statistics);

        return "admin/logs";
    }

    /**
     * Просмотр детальной информации о логе
     */
    @GetMapping("/{id}")
    public String viewLog(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Ищем лог по ID через репозиторий
            SystemLog log = systemLogService.getLogById(id);
            
            if (log == null) {
                redirectAttributes.addFlashAttribute("error", "Запись журнала не найдена");
                return "redirect:/admin/logs";
            }

            model.addAttribute("log", log);
            return "admin/log-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при загрузке записи журнала: " + e.getMessage());
            return "redirect:/admin/logs";
        }
    }

    /**
     * Очистка старых логов
     */
    @PostMapping("/cleanup")
    public String cleanupLogs(@RequestParam(defaultValue = "30") int daysToKeep,
                             RedirectAttributes redirectAttributes) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        try {
            int deletedCount = systemLogService.cleanupOldLogs(daysToKeep);
            
            redirectAttributes.addFlashAttribute("success", 
                "Успешно удалено " + deletedCount + " старых записей журнала");
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Ошибка при очистке журнала: " + e.getMessage());
        }

        return "redirect:/admin/logs";
    }

    /**
     * Экспорт логов (будущая функциональность)
     */
    @GetMapping("/export")
    public String exportLogs(@RequestParam(required = false) String format,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                            RedirectAttributes redirectAttributes) {
        
        // Заглушка для будущей реализации экспорта
        redirectAttributes.addFlashAttribute("info", 
            "Функция экспорта журналов находится в разработке");
            
        return "redirect:/admin/logs";
    }

    /**
     * API endpoint для получения логов в JSON формате (для AJAX запросов)
     */
    @GetMapping("/api")
    @ResponseBody
    public Page<SystemLog> getLogsApi(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String actionType,
                                     @RequestParam(required = false) String entityType,
                                     @RequestParam(required = false) String severity,
                                     @RequestParam(required = false) Long userId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                     @RequestParam(required = false) String searchText) {

        SystemLog.ActionType actionTypeEnum = null;
        if (actionType != null && !actionType.isEmpty()) {
            try {
                actionTypeEnum = SystemLog.ActionType.valueOf(actionType);
            } catch (IllegalArgumentException ignored) {}
        }

        SystemLog.EntityType entityTypeEnum = null;
        if (entityType != null && !entityType.isEmpty()) {
            try {
                entityTypeEnum = SystemLog.EntityType.valueOf(entityType);
            } catch (IllegalArgumentException ignored) {}
        }

        SystemLog.Severity severityEnum = null;
        if (severity != null && !severity.isEmpty()) {
            try {
                severityEnum = SystemLog.Severity.valueOf(severity);
            } catch (IllegalArgumentException ignored) {}
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        return systemLogService.getLogsWithFilters(
            actionTypeEnum, entityTypeEnum, severityEnum, user,
            startDate, endDate, searchText, page, size
        );
    }

    /**
     * API endpoint для получения статистики
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public Map<String, Object> getStatistics() {
        return systemLogService.getLogStatistics();
    }
} 