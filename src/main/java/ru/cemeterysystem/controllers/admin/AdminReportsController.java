package ru.cemeterysystem.controllers.admin;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.models.Notification;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.repositories.NotificationRepository;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import ru.cemeterysystem.services.ReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportsController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminReportsController.class);
    
    private final MemorialRepository memorialRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FamilyTreeRepository familyTreeRepository;
    private final ReportService reportService;
    
    /**
     * Главная страница отчётов
     */
    @GetMapping
    public String reportsPage(Model model) {
        log.info("Загрузка страницы отчётов");
        
        // Добавляем базовую статистику для отображения на странице
        Map<String, Object> stats = new HashMap<>();
        
        // Статистика мемориалов
        long totalMemorials = memorialRepository.count();
        long publishedMemorials = memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PUBLISHED);
        long pendingMemorials = memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PENDING_MODERATION);
        long rejectedMemorials = memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.REJECTED);
        long privateMemorials = totalMemorials - publishedMemorials - pendingMemorials - rejectedMemorials;
        
        stats.put("totalMemorials", totalMemorials);
        stats.put("publishedMemorials", publishedMemorials);
        stats.put("pendingMemorials", pendingMemorials);
        stats.put("rejectedMemorials", rejectedMemorials);
        stats.put("privateMemorials", privateMemorials);
        
        // Статистика пользователей
        long totalUsers = userRepository.count();
        long adminUsers = userRepository.countByRole(User.Role.ADMIN);
        long regularUsers = userRepository.countByRole(User.Role.USER);
        long subscribedUsers = userRepository.countByHasSubscription(true);
        
        stats.put("totalUsers", totalUsers);
        stats.put("adminUsers", adminUsers);
        stats.put("regularUsers", regularUsers);
        stats.put("subscribedUsers", subscribedUsers);
        
        // Статистика уведомлений
        long totalNotifications = notificationRepository.count();
        long pendingNotifications = notificationRepository.countByStatus(Notification.NotificationStatus.PENDING);
        long unreadNotifications = notificationRepository.countByRead(false);
        
        stats.put("totalNotifications", totalNotifications);
        stats.put("pendingNotifications", pendingNotifications);
        stats.put("unreadNotifications", unreadNotifications);
        
        // Статистика семейных деревьев
        long totalFamilyTrees = familyTreeRepository.count();
        long publishedTrees = familyTreeRepository.countByPublicationStatus(FamilyTree.PublicationStatus.PUBLISHED);
        long pendingTrees = familyTreeRepository.countByPublicationStatus(FamilyTree.PublicationStatus.PENDING_MODERATION);
        
        stats.put("totalFamilyTrees", totalFamilyTrees);
        stats.put("publishedTrees", publishedTrees);
        stats.put("pendingTrees", pendingTrees);
        
        model.addAttribute("stats", stats);
        model.addAttribute("currentDate", LocalDate.now());
        
        return "admin/reports";
    }
    
    /**
     * Генерация отчёта по мемориалам в PDF
     */
    @GetMapping("/memorials/pdf")
    public ResponseEntity<byte[]> generateMemorialsPdfReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        
        log.info("Генерация PDF отчёта по мемориалам: startDate={}, endDate={}, status={}", startDate, endDate, status);
        
        try {
            byte[] pdfBytes = reportService.generateMemorialsPdfReport(startDate, endDate, status);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "memorials-report.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            log.error("Ошибка генерации PDF отчёта по мемориалам: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Генерация отчёта по мемориалам в Excel
     */
    @GetMapping("/memorials/excel")
    public ResponseEntity<byte[]> generateMemorialsExcelReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        
        log.info("Генерация Excel отчёта по мемориалам: startDate={}, endDate={}, status={}", startDate, endDate, status);
        
        try {
            byte[] excelBytes = reportService.generateMemorialsExcelReport(startDate, endDate, status);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", "memorials-report.csv");
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
                    
        } catch (Exception e) {
            log.error("Ошибка генерации Excel отчёта по мемориалам: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Генерация отчёта по пользователям в PDF
     */
    @GetMapping("/users/pdf")
    public ResponseEntity<byte[]> generateUsersPdfReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String role) {
        
        log.info("Генерация PDF отчёта по пользователям: startDate={}, endDate={}, role={}", startDate, endDate, role);
        
        try {
            byte[] pdfBytes = reportService.generateUsersPdfReport(startDate, endDate, role);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "users-report.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            log.error("Ошибка генерации PDF отчёта по пользователям: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Генерация отчёта по пользователям в Excel
     */
    @GetMapping("/users/excel")
    public ResponseEntity<byte[]> generateUsersExcelReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String role) {
        
        log.info("Генерация Excel отчёта по пользователям: startDate={}, endDate={}, role={}", startDate, endDate, role);
        
        try {
            byte[] excelBytes = reportService.generateUsersExcelReport(startDate, endDate, role);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", "users-report.csv");
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
                    
        } catch (Exception e) {
            log.error("Ошибка генерации Excel отчёта по пользователям: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Генерация сводного отчёта в PDF
     */
    @GetMapping("/summary")
    public ResponseEntity<byte[]> generateSummaryPdfReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Генерация сводного PDF отчёта: startDate={}, endDate={}", startDate, endDate);
        
        try {
            byte[] pdfBytes = reportService.generateSummaryPdfReport(startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "summary-report.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            log.error("Ошибка генерации сводного PDF отчёта: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Генерация отчёта по модерации в PDF
     */
    @GetMapping("/moderation")
    public ResponseEntity<byte[]> generateModerationPdfReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Генерация PDF отчёта по модерации: startDate={}, endDate={}", startDate, endDate);
        
        try {
            byte[] pdfBytes = reportService.generateModerationPdfReport(startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "moderation-report.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            log.error("Ошибка генерации PDF отчёта по модерации: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Генерация быстрых отчетов
     */
    @GetMapping("/quick/{type}")
    public ResponseEntity<byte[]> generateQuickReport(@PathVariable String type) {
        
        log.info("Генерация быстрого отчёта: type={}", type);
        
        try {
            StringBuilder content = new StringBuilder();
            
            switch (type) {
                case "pending-memorials":
                    long pendingCount = memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PENDING_MODERATION);
                    content.append("Количество мемориалов на модерации: ").append(pendingCount).append("\n");
                    content.append("Период: ").append("2025-05-10 - 2025-06-11").append("\n");
                    content.append("Всего мемориалов: ").append(memorialRepository.count()).append("\n");
                    break;
                    
                case "new-users":
                    LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
                    Date lastMonthDate = Date.from(lastMonth.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    long newUsersCount = userRepository.countByDateOfRegistrationAfter(lastMonthDate);
                    content.append("Новых пользователей за последний месяц: ").append(newUsersCount).append("\n");
                    content.append("Всего пользователей: ").append(userRepository.count()).append("\n");
                    content.append("Активных пользователей: ").append(userRepository.countByRole(User.Role.USER)).append("\n");
                    break;
                    
                case "popular-memorials":
                    long publishedCount = memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PUBLISHED);
                    content.append("Опубликованных мемориалов: ").append(publishedCount).append("\n");
                    content.append("На модерации: ").append(memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PENDING_MODERATION)).append("\n");
                    content.append("Всего мемориалов: ").append(memorialRepository.count()).append("\n");
                    break;
                    
                case "subscription-stats":
                    long subscribedUsers = userRepository.countByHasSubscription(true);
                    long totalUsers = userRepository.count();
                    content.append("Пользователей с подпиской: ").append(subscribedUsers).append("\n");
                    content.append("Общее количество пользователей: ").append(totalUsers).append("\n");
                    content.append("Процент пользователей с подпиской: ").append(totalUsers > 0 ? Math.round((double)subscribedUsers / totalUsers * 100) : 0).append("%\n");
                    break;
                    
                default:
                    content.append("Неизвестный тип отчёта: ").append(type).append("\n");
            }
            
            byte[] pdfBytes = reportService.generateQuickPdfReport(type, content.toString());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", type + "-report.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            log.error("Ошибка генерации быстрого отчёта: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 