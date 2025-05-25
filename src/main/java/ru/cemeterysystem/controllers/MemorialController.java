package ru.cemeterysystem.controllers;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.cemeterysystem.dto.EditorRequestDTO;
import ru.cemeterysystem.dto.MemorialDTO;
import ru.cemeterysystem.dto.UserDTO;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.services.MemorialService;
import ru.cemeterysystem.services.UserService;

import java.util.List;

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
        User user = getCurrentUser();
        return memorialService.getMyMemorials(user.getId());
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
    public MemorialDTO updateMemorial(@PathVariable Long id,
                                      @RequestBody MemorialDTO dto) {
        User user = getCurrentUser();
        
        // Получаем информацию о правах пользователя на мемориал
        MemorialDTO existingMemorial = memorialService.getMemorialByIdForUser(id, user);
        
        // Получаем флаги из DTO
        boolean isEditor = existingMemorial.isEditor();
        boolean isOwner = existingMemorial.getCreatedBy() != null && 
                          existingMemorial.getCreatedBy().getId().equals(user.getId());
        
        log.info("Обновление мемориала ID={}: пользователь={}, isEditor={}, isOwner={}, hasSubscription={}", 
                id, user.getLogin(), isEditor, isOwner, user.getHasSubscription());
        
        // Проверяем наличие подписки, если указаны местоположения и пользователь не является редактором
        if ((dto.getMainLocation() != null || dto.getBurialLocation() != null) && 
            user.getHasSubscription() != Boolean.TRUE && !isEditor && !isOwner) {
            log.warn("Отказано в обновлении местоположения: пользователь не имеет подписки и не является редактором");
            throw new IllegalStateException("Для указания местоположения требуется подписка");
        }
        
        return memorialService.updateMemorial(id, dto);
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
     * @param id ID памятника
     * @param isPublic новый статус приватности
     * @return успешный ответ (HTTP 200)
     */
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
}
