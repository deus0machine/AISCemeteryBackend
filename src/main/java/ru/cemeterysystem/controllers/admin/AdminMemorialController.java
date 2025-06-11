package ru.cemeterysystem.controllers.admin;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.Notification;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.NotificationRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.services.MemorialService;
import ru.cemeterysystem.dto.MemorialDTO;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/memorials")
@RequiredArgsConstructor
public class AdminMemorialController {
    private static final Logger log = LoggerFactory.getLogger(AdminMemorialController.class);

    private final MemorialRepository memorialRepository;
    private final UserRepository userRepository;
    private final MemorialService memorialService;
    private final NotificationRepository notificationRepository;

    @GetMapping
    public String memorialsList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            Model model) {
        
        // Определение сортировки
        Sort sortOrder;
        switch (sort) {
            case "oldest":
                sortOrder = Sort.by(Sort.Direction.ASC, "createdAt");
                break;
            case "az":
                sortOrder = Sort.by(Sort.Direction.ASC, "fio");
                break;
            case "za":
                sortOrder = Sort.by(Sort.Direction.DESC, "fio");
                break;
            case "newest":
            default:
                sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
        }
        
        PageRequest pageRequest = PageRequest.of(page, size, sortOrder);
        Page<Memorial> memorials;
        
        // Применение фильтров
        if (search != null && !search.isEmpty()) {
            if (status != null && !status.isEmpty()) {
                if ("pending_moderation".equals(status)) {
                    // Поиск мемориалов, ожидающих модерации
                    memorials = memorialRepository.findByFioContainingIgnoreCaseAndPublicationStatus(
                        search, Memorial.PublicationStatus.PENDING_MODERATION, pageRequest);
                } else {
                    boolean isPublic = "public".equals(status);
                    memorials = memorialRepository.findByFioContainingIgnoreCaseAndIsPublic(search, isPublic, pageRequest);
                }
            } else {
                memorials = memorialRepository.findByFioContainingIgnoreCase(search, pageRequest);
            }
        } else if (status != null && !status.isEmpty()) {
            if ("pending_moderation".equals(status)) {
                // Все мемориалы, ожидающие модерации
                memorials = memorialRepository.findByPublicationStatus(
                    Memorial.PublicationStatus.PENDING_MODERATION, pageRequest);
            } else {
                boolean isPublic = "public".equals(status);
                memorials = memorialRepository.findByIsPublic(isPublic, pageRequest);
            }
        } else {
            memorials = memorialRepository.findAll(pageRequest);
        }
        
        model.addAttribute("memorials", memorials);
        
        // Статистика для отображения (реальные данные)
        long totalMemorials = memorialRepository.count();
        long publicMemorials = memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PUBLISHED);
        long pendingModerationMemorials = memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PENDING_MODERATION);
        long recentMemorials = memorialRepository.countByCreatedAtAfter(LocalDateTime.now().minusMonths(1));
        
        model.addAttribute("totalMemorials", totalMemorials);
        model.addAttribute("publicMemorials", publicMemorials);
        model.addAttribute("pendingMemorials", pendingModerationMemorials);
        model.addAttribute("recentMemorials", recentMemorials);
        
        // Данные для графика по месяцам (реальные данные вместо заглушек)
        int[] monthlyData = new int[12];
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfYear = LocalDateTime.of(now.getYear(), 1, 1, 0, 0);
        
        for (int i = 0; i < 12; i++) {
            LocalDateTime monthStart = startOfYear.plusMonths(i);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            long count = memorialRepository.countByCreatedAtBetween(monthStart, monthEnd);
            monthlyData[i] = (int) count;
        }
        
        // Преобразуем массивы в списки для корректной передачи в Thymeleaf
        List<Integer> monthlyDataList = Arrays.stream(monthlyData).boxed().collect(Collectors.toList());
        model.addAttribute("monthlyData", monthlyDataList);
        
        // Данные для круговой диаграммы (реальные данные)
        long privateMemorials = memorialRepository.countByIsPublic(false) - pendingModerationMemorials;
        List<Integer> pieChartDataList = Arrays.asList(
            (int) publicMemorials,  // Опубликованные
            (int) privateMemorials, // Не опубликованные (приватные)
            (int) pendingModerationMemorials // Ожидают модерации
        );
        
        model.addAttribute("pieChartData", pieChartDataList);
        
        // Отладочный вывод
        log.info("=== СТАТИСТИКА МЕМОРИАЛОВ ===");
        log.info("Total: {}, Public: {}, Pending Moderation: {}, Recent: {}", 
                totalMemorials, publicMemorials, pendingModerationMemorials, recentMemorials);
        log.info("Monthly data: {}", monthlyDataList);
        log.info("Pie chart data: {}", pieChartDataList);
        
        return "admin/memorials";
    }
    
    @GetMapping("/{id}/edit")
    public String editMemorial(@PathVariable Long id, Model model) {
        Optional<Memorial> optionalMemorial = memorialRepository.findById(id);
        if (optionalMemorial.isEmpty()) {
            return "redirect:/admin/memorials?error=Memorial+not+found";
        }
        
        Memorial memorial = optionalMemorial.get();
        List<User> users = userRepository.findAll();
        
        model.addAttribute("memorial", memorial);
        model.addAttribute("users", users);
        
        return "admin/memorial-edit";
    }
    
    @PostMapping("/{id}/edit")
    public String updateMemorial(
            @PathVariable Long id,
            @ModelAttribute Memorial memorialUpdates,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            @RequestParam(value = "removePhoto", defaultValue = "false") boolean removePhoto,
            Model model) {
        
        Optional<Memorial> optionalMemorial = memorialRepository.findById(id);
        if (optionalMemorial.isEmpty()) {
            return "redirect:/admin/memorials?error=Memorial+not+found";
        }
        
        Memorial memorial = optionalMemorial.get();
        
        // Обновление данных мемориала
        // Убираем прямое редактирование fio - только через отдельные поля
        // memorial.setFio(memorialUpdates.getFio());
        
        // Обновляем отдельные поля ФИО
        memorial.setFirstName(memorialUpdates.getFirstName());
        memorial.setLastName(memorialUpdates.getLastName());
        memorial.setMiddleName(memorialUpdates.getMiddleName());
        
        memorial.setBirthDate(memorialUpdates.getBirthDate());
        memorial.setDeathDate(memorialUpdates.getDeathDate());
        memorial.setBiography(memorialUpdates.getBiography());
        memorial.setBurialLocation(memorialUpdates.getBurialLocation());
        memorial.setPublic(memorialUpdates.isPublic());
        
        // Обновление владельца мемориала
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            memorial.setUser(optionalUser.get());
        }
        
        // Обработка фотографии
        if (removePhoto) {
            memorial.setPhotoUrl(null);
        } else if (photoFile != null && !photoFile.isEmpty()) {
            String photoUrl = memorialService.uploadPhoto(id, photoFile);
            memorial.setPhotoUrl(photoUrl);
        }
        
        memorialRepository.save(memorial);
        return "redirect:/admin/memorials?successMessage=Memorial+updated+successfully";
    }
    
    @PostMapping("/delete")
    public String deleteMemorial(@RequestParam("memorialId") Long id) {
        Optional<Memorial> optionalMemorial = memorialRepository.findById(id);
        if (optionalMemorial.isPresent()) {
            memorialRepository.delete(optionalMemorial.get());
            return "redirect:/admin/memorials?successMessage=Memorial+deleted+successfully";
        }
        return "redirect:/admin/memorials?errorMessage=Memorial+not+found";
    }

    @PostMapping("/{id}/moderate")
    public String moderateMemorial(
            @PathVariable Long id,
            @RequestParam("action") String action,
            RedirectAttributes redirectAttributes) {
        
        // Получаем текущего администратора
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        User admin = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Проверяем, что пользователь действительно администратор
        if (admin.getRole() != User.Role.ADMIN) {
            redirectAttributes.addFlashAttribute("errorMessage", "Только администратор может модерировать мемориалы");
            return "redirect:/admin/memorials";
        }
        
        try {
            boolean approved = "approve".equals(action);
            memorialService.moderateMemorial(id, approved, admin, null);
            
            if (approved) {
                redirectAttributes.addFlashAttribute("successMessage", "Мемориал успешно опубликован");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Публикация мемориала отклонена");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при модерации мемориала: " + e.getMessage());
        }
        
        return "redirect:/admin/memorials";
    }
    
    /**
     * Обработчик GET-запроса для одобрения мемориала (удобен для вызова из представления)
     */
    @GetMapping("/{id}/approve")
    public String approveMemorial(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        // Получаем текущего администратора
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        User admin = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Проверяем, что пользователь действительно администратор
        if (admin.getRole() != User.Role.ADMIN) {
            redirectAttributes.addFlashAttribute("errorMessage", "Только администратор может одобрять мемориалы");
            return "redirect:/admin/memorials/" + id;
        }
        
        try {
            // Одобряем мемориал через сервис
            memorialService.moderateMemorial(id, true, admin, null);
            
            // Обновляем статус связанного уведомления о модерации
            List<Notification> moderationNotifications = notificationRepository.findByRelatedEntityIdAndType(
                id, Notification.NotificationType.MODERATION);
            
            for (Notification notification : moderationNotifications) {
                if (notification.getStatus() == Notification.NotificationStatus.PENDING) {
                    notification.setStatus(Notification.NotificationStatus.ACCEPTED);
                    notification.setRead(true);
                    notificationRepository.save(notification);
                    log.info("Обновлен статус уведомления ID={} для мемориала ID={} на ACCEPTED", 
                            notification.getId(), id);
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Мемориал успешно опубликован");
        } catch (Exception e) {
            log.error("Ошибка при публикации мемориала ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при публикации мемориала: " + e.getMessage());
        }
        
        return "redirect:/admin/memorials/" + id;
    }
    
    /**
     * Обработчик GET-запроса для отклонения мемориала (удобен для вызова из представления)
     */
    @GetMapping("/{id}/reject")
    public String rejectMemorial(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        // Перенаправляем на форму отклонения для ввода причины
        return "redirect:/admin/memorials/" + id + "/reject-form";
    }
    
    /**
     * Форма для ввода причины отклонения мемориала
     */
    @GetMapping("/{id}/reject-form")
    public String rejectMemorialForm(
            @PathVariable Long id,
            Model model) {
        
        Optional<Memorial> memorialOpt = memorialRepository.findById(id);
        if (memorialOpt.isEmpty()) {
            return "redirect:/admin/memorials?errorMessage=Memorial+not+found";
        }
        
        model.addAttribute("memorialId", id);
        model.addAttribute("memorialFio", memorialOpt.get().getFio());
        
        return "admin/memorial-reject";
    }
    
    /**
     * Обработчик POST-запроса для отклонения мемориала с указанием причины
     */
    @PostMapping("/{id}/reject")
    public String rejectMemorialPost(@PathVariable Long id, 
                                @RequestParam(name = "reason", required = false) String reason,
            RedirectAttributes redirectAttributes) {
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String login = authentication.getName();
            User currentUser = userRepository.findByLogin(login)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Отклоняем мемориал
            memorialService.moderateMemorial(id, false, currentUser, reason);
            
            redirectAttributes.addFlashAttribute("successMessage", "Мемориал отклонен");
        } catch (Exception e) {
            log.error("Ошибка при отклонении мемориала: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при отклонении мемориала: " + e.getMessage());
        }
        
        return "redirect:/admin/memorials/" + id;
    }

    @GetMapping("/{id}")
    public String viewMemorial(@PathVariable Long id, Model model) {
        log.info("=== АДМИН ПРОСМАТРИВАЕТ МЕМОРИАЛ ===");
        log.info("AdminMemorialController.viewMemorial: запрос на просмотр мемориала ID={}", id);
        
        Optional<Memorial> optionalMemorial = memorialRepository.findById(id);
        if (optionalMemorial.isEmpty()) {
            log.error("Мемориал ID={} не найден", id);
            return "redirect:/admin/memorials?error=Memorial+not+found";
        }
        
        Memorial memorial = optionalMemorial.get();
        log.info("Найден мемориал ID={}, ФИО='{}', changesUnderModeration={}, publicationStatus={}", 
                id, memorial.getFio(), memorial.isChangesUnderModeration(), memorial.getPublicationStatus());
        
        // Получаем текущего пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        log.info("Текущий пользователь: {}", login);
        
        User currentUser = userRepository.findByLogin(login).orElse(null);
        
        if (currentUser == null) {
            log.error("Пользователь {} не найден в базе данных", login);
            return "redirect:/login";
        }
        
        log.info("Найден пользователь: ID={}, роль={}", currentUser.getId(), currentUser.getRole());
        
        // Проверяем права доступа
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        if (!isAdmin) {
            log.error("Пользователь {} не является администратором!", currentUser.getLogin());
            return "redirect:/admin/memorials?error=Access+denied";
        }
        
        log.info("AdminMemorialController.viewMemorial: мемориал ID={}, changesUnderModeration={}, currentUser={}, role={}", 
                id, memorial.isChangesUnderModeration(), currentUser.getLogin(), currentUser.getRole());
        
        // Получаем мемориал через сервис, чтобы админ видел изменения на модерации
        MemorialDTO memorialDto = memorialService.getMemorialByIdForUser(id, currentUser);
        
        log.info("AdminMemorialController.viewMemorial: получен DTO для админа - ID={}, ФИО='{}', changesUnderModeration={}", 
                memorialDto.getId(), memorialDto.getFio(), memorialDto.isChangesUnderModeration());
        
        model.addAttribute("memorial", memorialDto);
        model.addAttribute("isAdmin", true);
        
        // Если мемориал находится на модерации, добавляем соответствующие параметры
        if (memorial.getPublicationStatus() == Memorial.PublicationStatus.PENDING_MODERATION) {
            model.addAttribute("needsModeration", true);
            log.info("УСТАНОВЛЕН ФЛАГ needsModeration=true для мемориала ID={}", id);
        } else {
            log.info("ФЛАГ needsModeration НЕ установлен, так как publicationStatus={}", memorial.getPublicationStatus());
        }
        
        // ИСПРАВЛЕНО: Используем DTO для проверки изменений на модерации
        if (memorialDto.isChangesUnderModeration()) {
            model.addAttribute("changesNeedModeration", true);
            log.info("УСТАНОВЛЕН ФЛАГ changesNeedModeration=true для мемориала ID={}", id);
        } else {
            log.info("ФЛАГ changesNeedModeration НЕ установлен для мемориала ID={}", id);
        }
        
        log.info("ИТОГОВЫЕ ФЛАГИ для admin/memorial-view: needsModeration={}, changesNeedModeration={}", 
                model.getAttribute("needsModeration"), 
                model.getAttribute("changesNeedModeration"));
        
        return "admin/memorial-view";
    }

    /**
     * Получает документ мемориала для просмотра администратором
     */
    @GetMapping("/{id}/document")
    public ResponseEntity<org.springframework.core.io.Resource> getMemorialDocument(@PathVariable Long id) {
        log.info("=== АДМИН ЗАПРАШИВАЕТ ДОКУМЕНТ ===");
        log.info("AdminMemorialController.getMemorialDocument: запрос документа мемориала ID={}", id);
        
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            log.info("Текущий админ: {}", currentUsername);
            
            User currentUser = userRepository.findByLogin(currentUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
            log.info("Найден пользователь: ID={}, роль={}", currentUser.getId(), currentUser.getRole());
            
            // Только администраторы могут просматривать документы в админке
            if (currentUser.getRole() != User.Role.ADMIN) {
                log.warn("Попытка доступа к документу мемориала не-администратором: {}", currentUsername);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Проверяем существование мемориала
            Memorial memorial = memorialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Мемориал не найден"));
            log.info("Мемориал найден: ID={}, ФИО='{}', documentUrl={}", 
                    memorial.getId(), memorial.getFio(), memorial.getDocumentUrl());
            
            // Проверяем наличие документа
            if (memorial.getDocumentUrl() == null || memorial.getDocumentUrl().trim().isEmpty()) {
                log.warn("У мемориала {} отсутствует документ", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Error", "Document not found")
                    .build();
            }
            
            log.info("Админ получает документ через MemorialService...");
            // Используем уже существующий метод из MemorialService
            return memorialService.getMemorialDocument(id);
            
        } catch (Exception e) {
            log.error("ОШИБКА при получении документа администратором для мемориала {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error", "Internal server error: " + e.getMessage())
                .build();
        } finally {
            log.info("=== КОНЕЦ ЗАПРОСА ДОКУМЕНТА АДМИНОМ ===");
        }
    }
    
    @PostMapping("/{id}/approve")
    public String approveMemorialPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("=== ОДОБРЕНИЕ ПУБЛИКАЦИИ МЕМОРИАЛА ===");
        log.info("approveMemorialPost: запрос на одобрение мемориала ID={}", id);
        
        try {
            // ПРОВЕРКА: получаем мемориал и проверяем, что он требует модерации
            Optional<Memorial> checkMemorial = memorialRepository.findById(id);
            if (checkMemorial.isPresent()) {
                Memorial mem = checkMemorial.get();
                log.warn("ВНИМАНИЕ! approveMemorialPost вызван для мемориала ID={} со статусом: " +
                        "publicationStatus={}, changesUnderModeration={}", 
                        id, mem.getPublicationStatus(), mem.isChangesUnderModeration());
                
                // Если мемориал уже опубликован И имеет изменения на модерации,
                // то это неправильный метод!
                if (mem.getPublicationStatus() == Memorial.PublicationStatus.PUBLISHED && 
                    mem.isChangesUnderModeration()) {
                    log.error("ОШИБКА! Для одобрения ИЗМЕНЕНИЙ нужно использовать /approve-changes, а не /approve!");
                    redirectAttributes.addFlashAttribute("errorMessage", 
                            "Неверный метод! Используйте кнопку 'Одобрить изменения' для модерации изменений.");
                    return "redirect:/admin/memorials/" + id;
                }
            }
            
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String login = authentication.getName();
            log.info("Текущий пользователь: {}", login);
            
            User currentUser = userRepository.findByLogin(login)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("Найден пользователь: ID={}, роль={}", currentUser.getId(), currentUser.getRole());
            
            // ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА: читаем мемориал до изменений
            Optional<Memorial> memorialBeforeOpt = memorialRepository.findById(id);
            if (memorialBeforeOpt.isPresent()) {
                Memorial memorialBefore = memorialBeforeOpt.get();
                log.info("СОСТОЯНИЕ ДО МОДЕРАЦИИ: ID={}, publicationStatus={}, isPublic={}", 
                        id, memorialBefore.getPublicationStatus(), memorialBefore.isPublic());
            } else {
                log.error("МЕМОРИАЛ ID={} НЕ НАЙДЕН В БД!", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Мемориал не найден");
                return "redirect:/admin/memorials/" + id;
            }
            
            log.info("Вызываем memorialService.moderateMemorial({}, true, {}, null)", id, currentUser.getLogin());
            
            // Одобряем мемориал
            MemorialDTO result = memorialService.moderateMemorial(id, true, currentUser, null);
            
            log.info("Результат moderateMemorial: ID={}, publicationStatus={}, isPublic={}", 
                    result.getId(), result.getPublicationStatus(), result.isPublic());
            
            // ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА: читаем мемориал ПОСЛЕ изменений
            Optional<Memorial> memorialAfterOpt = memorialRepository.findById(id);
            if (memorialAfterOpt.isPresent()) {
                Memorial memorialAfter = memorialAfterOpt.get();
                log.info("СОСТОЯНИЕ ПОСЛЕ МОДЕРАЦИИ: ID={}, publicationStatus={}, isPublic={}", 
                        id, memorialAfter.getPublicationStatus(), memorialAfter.isPublic());
                
                // Проверяем, действительно ли изменения применились
                if (memorialAfter.getPublicationStatus() != Memorial.PublicationStatus.PUBLISHED) {
                    log.error("ОШИБКА! Мемориал ID={} после одобрения имеет статус {}, а должен быть PUBLISHED!", 
                            id, memorialAfter.getPublicationStatus());
                }
                if (!memorialAfter.isPublic()) {
                    log.error("ОШИБКА! Мемориал ID={} после одобрения имеет isPublic={}, а должен быть true!", 
                            id, memorialAfter.isPublic());
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Мемориал успешно одобрен и опубликован");
        } catch (Exception e) {
            log.error("ОШИБКА при одобрении мемориала ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при одобрении мемориала: " + e.getMessage());
        }
        
        log.info("Перенаправляем на /admin/memorials/{}", id);
        return "redirect:/admin/memorials/" + id;
    }
    
    @PostMapping("/{id}/approve-changes")
    public String approveChanges(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("=== АДМИНИСТРАТОР ОДОБРЯЕТ ИЗМЕНЕНИЯ ===");
        log.info("Получен запрос на одобрение изменений мемориала ID={}", id);
        
        try {
            // ПРОВЕРКА: получаем мемориал и проверяем его состояние
            Optional<Memorial> checkMemorial = memorialRepository.findById(id);
            if (checkMemorial.isPresent()) {
                Memorial mem = checkMemorial.get();
                log.info("approveChanges: мемориал ID={} имеет статус: " +
                        "publicationStatus={}, changesUnderModeration={}, pendingChanges={}", 
                        id, mem.getPublicationStatus(), mem.isChangesUnderModeration(), mem.isPendingChanges());
                
                // Проверяем, что действительно есть изменения на модерации
                if (!mem.isChangesUnderModeration()) {
                    log.error("ОШИБКА! Мемориал ID={} не имеет изменений на модерации!", id);
                    redirectAttributes.addFlashAttribute("errorMessage", 
                            "У мемориала нет изменений, ожидающих модерации.");
                    return "redirect:/admin/memorials/" + id;
                }
            }
            
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String login = authentication.getName();
            log.info("Текущий пользователь: {}", login);
            
            User currentUser = userRepository.findByLogin(login)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("Найден пользователь: ID={}, роль={}", currentUser.getId(), currentUser.getRole());
            
            // Проверяем, что пользователь - администратор
            if (currentUser.getRole() != User.Role.ADMIN) {
                log.error("Пользователь {} не является администратором!", currentUser.getLogin());
                redirectAttributes.addFlashAttribute("errorMessage", "Только администратор может одобрять изменения");
                return "redirect:/admin/memorials/" + id;
            }
            
            log.info("Вызываем memorialService.approveChangesByAdmin({}, {})", id, currentUser.getLogin());
            
            // Используем специальный метод для администраторов
            memorialService.approveChangesByAdmin(id, currentUser);
            
            log.info("Изменения мемориала ID={} успешно одобрены администратором {}", id, currentUser.getLogin());
            redirectAttributes.addFlashAttribute("successMessage", "Изменения мемориала одобрены");
        } catch (Exception e) {
            log.error("ОШИБКА при одобрении изменений мемориала ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при одобрении изменений: " + e.getMessage());
        }
        
        log.info("Перенаправляем на /admin/memorials/{}", id);
        return "redirect:/admin/memorials/" + id;
    }
    
    @PostMapping("/{id}/reject-changes")
    public String rejectChanges(@PathVariable Long id, 
                               @RequestParam(name = "reason", required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String login = authentication.getName();
            User currentUser = userRepository.findByLogin(login)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Отклоняем изменения
            memorialService.rejectChanges(id, reason, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", "Изменения мемориала отклонены");
        } catch (Exception e) {
            log.error("Ошибка при отклонении изменений: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при отклонении изменений: " + e.getMessage());
        }
        
        return "redirect:/admin/memorials/" + id;
    }

    @PostMapping("/{id}/block")
    public String blockMemorial(@PathVariable Long id, 
                               @RequestParam(name = "reason", required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String login = authentication.getName();
            User currentUser = userRepository.findByLogin(login)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Проверяем, что пользователь - администратор
            if (currentUser.getRole() != User.Role.ADMIN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Только администратор может блокировать мемориалы");
                return "redirect:/admin/memorials/" + id;
            }
            
            // Получаем мемориал
            Memorial memorial = memorialRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Мемориал не найден"));
            
            // Проверяем, что мемориал еще не заблокирован
            if (memorial.isBlocked()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Мемориал уже заблокирован");
                return "redirect:/admin/memorials/" + id;
            }
            
            // Блокируем мемориал
            memorial.setBlocked(true);
            memorial.setBlockReason(reason != null ? reason : "Нарушение правил сообщества");
            memorial.setBlockedAt(java.time.LocalDateTime.now());
            memorial.setBlockedBy(currentUser);
            
            // Если мемориал был публичным, делаем его приватным
            if (memorial.isPublic()) {
                memorial.setPublic(false);
            }
            
            // Если мемориал был опубликован, меняем статус на REJECTED
            if (memorial.getPublicationStatus() == Memorial.PublicationStatus.PUBLISHED) {
                memorial.setPublicationStatus(Memorial.PublicationStatus.REJECTED);
            }
            
            memorialRepository.save(memorial);
            
            // Создаем уведомление для владельца мемориала
            createBlockNotificationForOwner(memorial, currentUser, reason);
            
            // Отправляем уведомление пользователю, подавшему жалобу (если есть)
            notifyReporterAboutAction(memorial, currentUser, true, reason);
            
            log.info("Мемориал ID={} заблокирован администратором {}: {}", 
                    id, currentUser.getLogin(), reason);
                    
            redirectAttributes.addFlashAttribute("successMessage", "Мемориал заблокирован");
        } catch (Exception e) {
            log.error("Ошибка при блокировке мемориала ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при блокировке мемориала: " + e.getMessage());
        }
        
        return "redirect:/admin/memorials/" + id;
    }

    @PostMapping("/{id}/unblock")
    public String unblockMemorial(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String login = authentication.getName();
            User currentUser = userRepository.findByLogin(login)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Проверяем, что пользователь - администратор
            if (currentUser.getRole() != User.Role.ADMIN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Только администратор может разблокировать мемориалы");
                return "redirect:/admin/memorials/" + id;
            }
            
            // Получаем мемориал
            Memorial memorial = memorialRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Мемориал не найден"));
            
            // Проверяем, что мемориал заблокирован
            if (!memorial.isBlocked()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Мемориал не заблокирован");
                return "redirect:/admin/memorials/" + id;
            }
            
            // Разблокируем мемориал
            memorial.setBlocked(false);
            memorial.setBlockReason(null);
            memorial.setBlockedAt(null);
            memorial.setBlockedBy(null);
            
            // Восстанавливаем статус публикации на DRAFT (владелец сам решит публиковать или нет)
            if (memorial.getPublicationStatus() == Memorial.PublicationStatus.REJECTED) {
                memorial.setPublicationStatus(Memorial.PublicationStatus.DRAFT);
            }
            
            memorialRepository.save(memorial);
            
            // Создаем уведомление для владельца мемориала
            createUnblockNotificationForOwner(memorial, currentUser);
            
            log.info("Мемориал ID={} разблокирован администратором {}", 
                    id, currentUser.getLogin());
                    
            redirectAttributes.addFlashAttribute("successMessage", "Мемориал разблокирован");
        } catch (Exception e) {
            log.error("Ошибка при разблокировке мемориала ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при разблокировке мемориала: " + e.getMessage());
        }
        
        return "redirect:/admin/memorials/" + id;
    }

    /**
     * Создает уведомление для владельца о блокировке мемориала
     */
    private void createBlockNotificationForOwner(Memorial memorial, User admin, String reason) {
        Notification notification = new Notification();
        notification.setTitle("Мемориал заблокирован");
        notification.setMessage(String.format(
            "Ваш мемориал \"%s\" был заблокирован администратором.\n\nПричина блокировки:\n%s\n\nДля получения дополнительной информации обратитесь в службу поддержки.",
            memorial.getFio(),
            reason != null ? reason : "Нарушение правил сообщества"
        ));
        notification.setUser(memorial.getCreatedBy());
        notification.setSender(admin);
        notification.setType(Notification.NotificationType.ADMIN_WARNING);
        notification.setStatus(Notification.NotificationStatus.INFO);
        notification.setRelatedEntityId(memorial.getId());
        notification.setRelatedEntityName(memorial.getFio());
        notification.setCreatedAt(java.time.LocalDateTime.now());
        notification.setRead(false);
        notification.setUrgent(true);
        
        notificationRepository.save(notification);
    }

    /**
     * Создает уведомление для владельца о разблокировке мемориала
     */
    private void createUnblockNotificationForOwner(Memorial memorial, User admin) {
        Notification notification = new Notification();
        notification.setTitle("Мемориал разблокирован");
        notification.setMessage(String.format(
            "Ваш мемориал \"%s\" был разблокирован администратором.\n\nТеперь вы можете снова редактировать мемориал и при желании сделать его публичным.",
            memorial.getFio()
        ));
        notification.setUser(memorial.getCreatedBy());
        notification.setSender(admin);
        notification.setType(Notification.NotificationType.ADMIN_INFO);
        notification.setStatus(Notification.NotificationStatus.INFO);
        notification.setRelatedEntityId(memorial.getId());
        notification.setRelatedEntityName(memorial.getFio());
        notification.setCreatedAt(java.time.LocalDateTime.now());
        notification.setRead(false);
        notification.setUrgent(false);
        
        notificationRepository.save(notification);
    }

    /**
     * Уведомляет пользователя, подавшего жалобу, о результате рассмотрения
     */
    private void notifyReporterAboutAction(Memorial memorial, User admin, boolean memorialBlocked, String blockReason) {
        // Находим уведомления о жалобах на этот мемориал
        List<Notification> reportNotifications = notificationRepository
                .findByRelatedEntityIdAndTypeAndStatus(
                        memorial.getId(),
                        Notification.NotificationType.MEMORIAL_REPORT, 
                        Notification.NotificationStatus.INFO
                );
        
        for (Notification reportNotification : reportNotifications) {
            if (reportNotification.getSender() != null) {
                User reporter = reportNotification.getSender();
                
                Notification responseNotification = new Notification();
                
                if (memorialBlocked) {
                    responseNotification.setTitle("Жалоба рассмотрена - приняты меры");
                    responseNotification.setMessage(String.format(
                        "По вашей жалобе на мемориал \"%s\" приняты меры. Мемориал был заблокирован администратором.\n\nСпасибо за вашу бдительность!",
                        memorial.getFio()
                    ));
                    responseNotification.setType(Notification.NotificationType.ADMIN_INFO);
                } else {
                    responseNotification.setTitle("Жалоба рассмотрена - отклонена");
                    responseNotification.setMessage(String.format(
                        "Ваша жалоба на мемориал \"%s\" была рассмотрена и отклонена администратором.\n\nМемориал не нарушает правила сообщества.",
                        memorial.getFio()
                    ));
                    responseNotification.setType(Notification.NotificationType.ADMIN_INFO);
                }
                
                responseNotification.setUser(reporter);
                responseNotification.setSender(admin);
                responseNotification.setStatus(Notification.NotificationStatus.INFO);
                responseNotification.setRelatedEntityId(memorial.getId());
                responseNotification.setRelatedEntityName(memorial.getFio());
                responseNotification.setCreatedAt(java.time.LocalDateTime.now());
                responseNotification.setRead(false);
                responseNotification.setUrgent(false);
                
                notificationRepository.save(responseNotification);
                
                // Помечаем исходное уведомление о жалобе как обработанное
                reportNotification.setStatus(Notification.NotificationStatus.PROCESSED);
                reportNotification.setRead(true);
                notificationRepository.save(reportNotification);
            }
        }
    }

    @PostMapping("/{id}/dismiss-report")
    public String dismissMemorialReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String login = authentication.getName();
            User currentUser = userRepository.findByLogin(login)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Проверяем, что пользователь - администратор
            if (currentUser.getRole() != User.Role.ADMIN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Только администратор может отклонять жалобы");
                return "redirect:/admin/memorials/" + id;
            }
            
            // Получаем мемориал
            Memorial memorial = memorialRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Мемориал не найден"));
            
            // Отправляем уведомление пользователю об отклонении жалобы
            notifyReporterAboutAction(memorial, currentUser, false, null);
            
            log.info("Жалоба на мемориал ID={} отклонена администратором {}", 
                    id, currentUser.getLogin());
                    
            redirectAttributes.addFlashAttribute("successMessage", "Жалоба отклонена");
        } catch (Exception e) {
            log.error("Ошибка при отклонении жалобы на мемориал ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при отклонении жалобы: " + e.getMessage());
        }
        
        return "redirect:/admin/notifications";
    }
} 