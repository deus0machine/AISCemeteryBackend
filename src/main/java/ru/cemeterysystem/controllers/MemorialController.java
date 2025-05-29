package ru.cemeterysystem.controllers;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.cemeterysystem.dto.EditorRequestDTO;
import ru.cemeterysystem.dto.MemorialDTO;
import ru.cemeterysystem.dto.UserDTO;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.services.MemorialService;
import ru.cemeterysystem.services.UserService;

import java.util.List;
import java.util.Optional;

/**
 * REST контроллер для управления памятниками.
 * Обрабатывает запросы к API для получения, создания, обновления и удаления памятников,
 * а также для работы с фотографиями и настройками приватности.
 */
@RestController
@RequestMapping("/api/memorials")
@RequiredArgsConstructor
public class MemorialController {
    private static final Logger log = LoggerFactory.getLogger(MemorialController.class);
    
    private final MemorialService memorialService;
    private final UserService userService;
    private final MemorialRepository memorialRepository;
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
        return userService.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Получает список всех памятников.
     *
     * @return список всех памятников
     */
    @GetMapping
    public List<MemorialDTO> getAllMemorials() {
        return memorialService.getAllMemorials();
    }

    /**
     * Получает список памятников, принадлежащих текущему пользователю.
     *
     * @return список памятников текущего пользователя
     */
    @GetMapping("/my")
    public List<MemorialDTO> getMyMemorials() {
        log.info("=== ЗАПРОС МОИ МЕМОРИАЛЫ ===");
        User user = getCurrentUser();
        log.info("Получение 'моих мемориалов' для пользователя: {}", user.getLogin());
        
        List<MemorialDTO> memorials = memorialService.getMyMemorials(user.getId());
        log.info("Найдено {} мемориалов для пользователя {}", memorials.size(), user.getLogin());
        
        // Логируем статус каждого мемориала
        for (MemorialDTO memorial : memorials) {
            log.info("Мемориал ID={}, '{}', publicationStatus={}, isPublic={}, pendingChanges={}, changesUnderModeration={}", 
                    memorial.getId(), memorial.getFio(), memorial.getPublicationStatus(), 
                    memorial.isPublic(), memorial.isPendingChanges(), memorial.isChangesUnderModeration());
        }
        
        return memorials;
    }

    /**
     * Получает список публичных памятников.
     *
     * @return список публичных памятников
     */
    @GetMapping("/public")
    public List<MemorialDTO> getPublicMemorials() {
        return memorialService.getPublicMemorials();
    }

    /**
     * Получает памятник по его ID.
     *
     * @param id ID памятника
     * @return найденный памятник
     */
    @GetMapping("/{id}")
    public MemorialDTO getMemorialById(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        return memorialService.getMemorialByIdForUser(id, currentUser);
    }

    /**
     * Создает новый памятник.
     *
     * @param dto данные для создания памятника
     * @return созданный памятник
     */
    @PostMapping
    public MemorialDTO createMemorial(@RequestBody MemorialDTO dto) {
        User user = getCurrentUser();
        
        // При создании нового мемориала пользователь всегда является владельцем
        // но всё равно проверяем подписку для согласованности
        if ((dto.getMainLocation() != null || dto.getBurialLocation() != null) && 
            user.getHasSubscription() != Boolean.TRUE) {
            log.info("Создание мемориала с местоположением: пользователь={}, hasSubscription={}", 
                    user.getLogin(), user.getHasSubscription());
            
            throw new IllegalStateException("Для указания местоположения требуется подписка");
        }
        
        return memorialService.createMemorial(dto, user.getId());
    }

    /**
     * Обновляет информацию о памятнике.
     *
     * @param id ID памятника
     * @param dto данные для обновления памятника
     * @return обновленный памятник
     */
    @PutMapping("/{id}")
    public ResponseEntity<MemorialDTO> updateMemorial(@PathVariable Long id, @RequestBody MemorialDTO memorialDTO) {
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String login = authentication.getName();
            User user = userRepository.findByLogin(login)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Проверяем существование мемориала
            Memorial memorial = memorialRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Memorial not found"));
            
            // Проверяем, что пользователь имеет права на редактирование мемориала
            if (!memorial.getUser().equals(user) && !memorial.isEditor(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
            }
            
            // Проверяем, находится ли мемориал на модерации - добавлена более подробная проверка
            if (memorial.getPublicationStatus() == Memorial.PublicationStatus.PENDING_MODERATION) {
                log.error("Попытка обновить мемориал ID={} в статусе модерации пользователем ID={}", 
                        id, user.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header("X-Error-Type", "MEMORIAL_UNDER_MODERATION")
                    .header("X-Error-Message", "Невозможно обновить мемориал, находящийся на модерации")
                    .body(null);
            }
            
            MemorialDTO updatedMemorial = memorialService.updateMemorial(id, memorialDTO, user);
            return ResponseEntity.ok(updatedMemorial);
        } catch (Exception e) {
            log.error("Ошибка при обновлении мемориала ID={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error-Message", e.getMessage())
                .body(null);
        }
    }

    /**
     * Удаляет памятник по ID.
     *
     * @param id ID памятника
     * @return успешный ответ (HTTP 200)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMemorial(@PathVariable Long id) {
        memorialService.deleteMemorial(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Обновляет приватность памятника (публичный/непубличный).
     * 
     * @deprecated Используйте вместо этого метод sendForModeration для публикации
     * @param id ID памятника
     * @param isPublic новый статус приватности
     * @return успешный ответ (HTTP 200)
     */
    @Deprecated
    @PutMapping("/{id}/privacy")
    public ResponseEntity<Void> updateMemorialPrivacy(@PathVariable Long id,
                                                      @RequestBody boolean isPublic) {
        // Если пытаемся сделать мемориал публичным, проверяем наличие подписки
        if (isPublic) {
            User user = getCurrentUser();
            if (user.getHasSubscription() != Boolean.TRUE) {
                throw new IllegalStateException("Для публикации мемориала требуется подписка");
            }
        }
        
        memorialService.updateMemorialPrivacy(id, isPublic);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Отправляет мемориал на модерацию для публикации.
     *
     * @param id ID памятника
     * @return обновленный мемориал
     */
    @PostMapping("/{id}/send-for-moderation")
    public MemorialDTO sendForModeration(@PathVariable Long id) {
        User user = getCurrentUser();
        
        // Проверяем наличие подписки
        if (user.getHasSubscription() != Boolean.TRUE) {
            throw new IllegalStateException("Для публикации мемориала требуется подписка");
        }
        
        log.info("Отправка мемориала ID={} на модерацию пользователем {}", id, user.getLogin());
        return memorialService.sendForModeration(id, user);
    }

    /**
     * Отправляет изменения опубликованного мемориала на модерацию.
     *
     * @param id ID памятника
     * @return обновленный мемориал
     */
    @PostMapping("/{id}/send-changes-for-moderation")
    public MemorialDTO sendChangesForModeration(@PathVariable Long id) {
        User user = getCurrentUser();
        
        // Проверяем наличие подписки
        if (user.getHasSubscription() != Boolean.TRUE) {
            throw new IllegalStateException("Для отправки изменений на модерацию требуется подписка");
        }
        
        log.info("Отправка изменений мемориала ID={} на модерацию пользователем {}", id, user.getLogin());
        return memorialService.sendChangesForModeration(id, user);
    }

    /**
     * Загружает фото для памятника.
     *
     * @param id ID памятника
     * @param file файл изображения
     * @return URL загруженного фото
     */
    @PostMapping("/{id}/photo")
    public String uploadPhoto(@PathVariable Long id,
                              @RequestParam("photo") MultipartFile file) {
        return memorialService.uploadPhoto(id, file);
    }

    /**
     * Ищет памятники по различным параметрам.
     *
     * @param query строка поиска по названию или описанию памятника
     * @param location местоположение памятника
     * @param startDate дата начала периода для поиска
     * @param endDate дата окончания периода для поиска
     * @param isPublic фильтр по публичности
     * @return список найденных памятников
     */
    @GetMapping("/search")
    public List<MemorialDTO> searchMemorials(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Boolean isPublic
    ) {
        return memorialService.searchMemorials(query, location, startDate, endDate, isPublic);
    }
    
    /**
     * Получает список редакторов мемориала
     * 
     * @param id ID мемориала
     * @return список пользователей-редакторов
     */
    @GetMapping("/{id}/editors")
    public List<UserDTO> getMemorialEditors(@PathVariable Long id) {
        log.info("Получение редакторов для мемориала с ID: {}", id);
        return memorialService.getMemorialEditors(id);
    }
    
    /**
     * Добавляет или удаляет редактора мемориала
     * 
     * @param id ID мемориала
     * @param request запрос с информацией о редакторе и действии
     * @return обновленный мемориал
     */
    @PostMapping("/{id}/editors")
    public MemorialDTO manageEditor(
            @PathVariable Long id,
            @RequestBody EditorRequestDTO request) {
        log.info("Управление редактором для мемориала с ID: {}, действие: {}, пользователь: {}", 
                id, request.getAction(), request.getUserId());
        
        User currentUser = getCurrentUser();
        return memorialService.manageEditor(id, request, currentUser);
    }
    
    /**
     * Получает список мемориалов, ожидающих подтверждения изменений
     * 
     * @return список мемориалов с изменениями
     */
    @GetMapping("/edited")
    public List<MemorialDTO> getEditedMemorials() {
        User currentUser = getCurrentUser();
        log.info("Получение мемориалов с изменениями для пользователя: {}", currentUser.getLogin());
        return memorialService.getEditedMemorials(currentUser);
    }
    
    /**
     * Получает информацию о ожидающих изменениях мемориала
     * 
     * @param id ID мемориала
     * @return мемориал с информацией о ожидающих изменениях
     */
    @GetMapping("/{id}/pending-changes")
    public MemorialDTO getMemorialPendingChanges(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        log.info("Получение информации о ожидающих изменениях мемориала с ID: {}", id);
        return memorialService.getMemorialPendingChanges(id, currentUser);
    }
    
    /**
     * Подтверждает или отклоняет изменения в мемориале
     * 
     * @param id ID мемориала
     * @param request объект с полем approve: true для подтверждения, false для отклонения
     * @return обновленный мемориал
     */
    @PostMapping("/{id}/approve-changes")
    public MemorialDTO approveChanges(
            @PathVariable Long id,
            @RequestBody ApproveChangesRequest request) {
        User currentUser = getCurrentUser();
        log.info("Подтверждение изменений мемориала с ID: {}, решение: {}", id, request.isApprove());
        return memorialService.approveChanges(id, request.isApprove(), currentUser);
    }

    /**
     * Диагностический эндпоинт для проверки редакторов всех мемориалов
     * 
     * @return строка с отчетом о редакторах
     */
    @GetMapping("/debug/editors")
    public ResponseEntity<String> debugEditors() {
        StringBuilder report = new StringBuilder();
        List<Memorial> allMemorials = memorialService.getAllMemorialsWithEditors();
        
        report.append("Всего мемориалов: ").append(allMemorials.size()).append("\n\n");
        
        for (Memorial memorial : allMemorials) {
            report.append("Мемориал ID: ").append(memorial.getId())
                  .append(", '").append(memorial.getFio()).append("'\n")
                  .append("  Владелец: ").append(memorial.getCreatedBy().getLogin())
                  .append(" (ID: ").append(memorial.getCreatedBy().getId()).append(")\n");
            
            if (memorial.getEditors() != null && !memorial.getEditors().isEmpty()) {
                report.append("  Редакторы (").append(memorial.getEditors().size()).append("):\n");
                for (User editor : memorial.getEditors()) {
                    report.append("    - ").append(editor.getLogin())
                          .append(" (ID: ").append(editor.getId()).append(")\n");
                }
            } else {
                report.append("  Редакторы: нет\n");
            }
            
            report.append("\n");
        }
        
        return ResponseEntity.ok(report.toString());
    }

    /**
     * Класс для запроса подтверждения/отклонения изменений
     */
    public static class ApproveChangesRequest {
        private boolean approve;
        
        public boolean isApprove() {
            return approve;
        }
        
        public void setApprove(boolean approve) {
            this.approve = approve;
        }
    }

    /**
     * Одобряет публикацию мемориала (для администраторов).
     *
     * @param id ID памятника
     * @return обновленный мемориал
     */
    @PostMapping("/{id}/approve")
    public MemorialDTO approveMemorial(@PathVariable Long id) {
        User user = getCurrentUser();
        
        // Проверяем права администратора
        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Только администраторы могут одобрять публикацию мемориалов");
        }
        
        log.info("Одобрение публикации мемориала ID={} администратором {}", id, user.getLogin());
        return memorialService.moderateMemorial(id, true, user, null);
    }
    
    /**
     * Отклоняет публикацию мемориала с указанием причины (для администраторов).
     *
     * @param id ID памятника
     * @param reason причина отклонения
     * @return обновленный мемориал
     */
    @PostMapping("/{id}/reject")
    public MemorialDTO rejectMemorial(@PathVariable Long id, @RequestBody String reason) {
        User user = getCurrentUser();
        
        // Проверяем права администратора
        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Только администраторы могут отклонять публикацию мемориалов");
        }
        
        log.info("Отклонение публикации мемориала ID={} администратором {}", id, user.getLogin());
        return memorialService.moderateMemorial(id, false, user, reason);
    }
    
    /**
     * Одобряет изменения опубликованного мемориала (для администраторов).
     *
     * @param id ID памятника
     * @return обновленный мемориал
     */
    @PostMapping("/{id}/admin/approve-changes")
    public MemorialDTO approveChangesByAdmin(@PathVariable Long id) {
        User user = getCurrentUser();
        
        // Проверяем права администратора
        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Только администраторы могут одобрять изменения мемориалов");
        }
        
        log.info("Одобрение изменений мемориала ID={} администратором {}", id, user.getLogin());
        return memorialService.approveChangesByAdmin(id, user);
    }
    
    /**
     * Отклоняет изменения опубликованного мемориала с указанием причины (для администраторов).
     * Мемориал остается опубликованным с исходными данными.
     *
     * @param id ID памятника
     * @param reason причина отклонения
     * @return обновленный мемориал
     */
    @PostMapping("/{id}/admin/reject-changes")
    public MemorialDTO rejectChangesByAdmin(@PathVariable Long id, @RequestBody String reason) {
        User user = getCurrentUser();
        
        // Проверяем права администратора
        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Только администраторы могут отклонять изменения мемориалов");
        }
        
        log.info("Отклонение изменений мемориала ID={} администратором {}", id, user.getLogin());
        return memorialService.rejectChanges(id, reason, user);
    }
}
