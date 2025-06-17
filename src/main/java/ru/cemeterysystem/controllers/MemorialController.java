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
import ru.cemeterysystem.models.Notification;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.repositories.NotificationRepository;
import ru.cemeterysystem.services.MemorialService;
import ru.cemeterysystem.services.UserService;

import java.util.List;
import java.util.Map;
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
    private final NotificationRepository notificationRepository;

    /**
     * Получает текущего аутентифицированного пользователя.
     *
     * @return текущий пользователь
     * @throws RuntimeException если пользователь не найден
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("getCurrentUser: authentication = {}", authentication);
        log.info("getCurrentUser: principal = {}", authentication != null ? authentication.getPrincipal() : "null");
        log.info("getCurrentUser: authenticated = {}", authentication != null ? authentication.isAuthenticated() : "null");
        
        if (authentication == null) {
            log.error("getCurrentUser: Authentication is null!");
            throw new RuntimeException("Authentication is null");
        }
        
        String login = authentication.getName();
        log.info("getCurrentUser: извлеченный login = {}", login);
        
        if (login == null || login.trim().isEmpty()) {
            log.error("getCurrentUser: Login is null or empty!");
            throw new RuntimeException("Login is null or empty");
        }
        
        User user = userService.findByLogin(login)
                .orElseThrow(() -> {
                    log.error("getCurrentUser: Пользователь с login '{}' не найден в базе данных", login);
                    return new RuntimeException("User not found: " + login);
                });
        
        log.info("getCurrentUser: найден пользователь: id={}, login={}, role={}", 
                user.getId(), user.getLogin(), user.getRole());
        return user;
    }

    /**
     * Получает список всех памятников с пагинацией.
     *
     * @param page номер страницы (начиная с 0)
     * @param size размер страницы
     * @return пагинированный список всех памятников
     */
    @GetMapping
    public ru.cemeterysystem.dto.PagedResponse<MemorialDTO> getAllMemorials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return memorialService.getAllMemorials(page, size);
    }

    /**
     * Получает список памятников, принадлежащих текущему пользователю с пагинацией.
     *
     * @param page номер страницы (начиная с 0)
     * @param size размер страницы
     * @return пагинированный список памятников текущего пользователя
     */
    @GetMapping("/my")
    public ru.cemeterysystem.dto.PagedResponse<MemorialDTO> getMyMemorials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("=== ЗАПРОС МОИ МЕМОРИАЛЫ С ПАГИНАЦИЕЙ ===");
        User user = getCurrentUser();
        log.info("Получение 'моих мемориалов' для пользователя: {} (страница {}, размер {})", 
                user.getLogin(), page, size);
        
        ru.cemeterysystem.dto.PagedResponse<MemorialDTO> response = memorialService.getMyMemorials(user.getId(), page, size);
        log.info("Найдено {} мемориалов на странице {} из {} для пользователя {}", 
                response.getContent().size(), page + 1, response.getTotalPages(), user.getLogin());
        
        return response;
    }

    /**
     * Получает список публичных памятников с пагинацией.
     *
     * @param page номер страницы (начиная с 0)
     * @param size размер страницы
     * @return пагинированный список публичных памятников
     */
    @GetMapping("/public")
    public ru.cemeterysystem.dto.PagedResponse<MemorialDTO> getPublicMemorials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return memorialService.getPublicMemorials(page, size);
    }

    /**
     * Получает документ мемориала для просмотра или скачивания.
     * Доступно для владельцев, редакторов и администраторов.
     *
     * @param id ID мемориала
     * @return ResponseEntity с содержимым документа
     */
    @GetMapping("/{id}/document")
    public ResponseEntity<org.springframework.core.io.Resource> getMemorialDocument(@PathVariable Long id) {
        log.info("=== НАЧАЛО ЗАПРОСА ДОКУМЕНТА ===");
        log.info("Запрошен документ для мемориала с ID: {}", id);
        
        try {
            User user = getCurrentUser();
            log.info("Текущий пользователь: login={}, id={}, role={}", 
                    user.getLogin(), user.getId(), user.getRole());
            
            // Проверяем существование мемориала
            log.info("Проверяем существование мемориала с ID: {}", id);
            Memorial memorial = memorialRepository.findById(id).orElse(null);
            if (memorial == null) {
                log.error("Мемориал с ID {} не найден", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            log.info("Мемориал найден: id={}, fio={}, owner={}, documentUrl={}", 
                    memorial.getId(), memorial.getFio(), 
                    memorial.getUser().getLogin(), memorial.getDocumentUrl());
            
            // Проверяем наличие документа
            if (memorial.getDocumentUrl() == null || memorial.getDocumentUrl().trim().isEmpty()) {
                log.warn("У мемориала {} отсутствует документ", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Error", "Document not found")
                    .build();
            }
            log.info("URL документа: {}", memorial.getDocumentUrl());
            
            // Проверяем права доступа к мемориалу
            log.info("Проверяем права доступа...");
            boolean isOwner = memorial.getUser().getId().equals(user.getId());
            log.info("Является ли пользователь владельцем: {}", isOwner);
            
            boolean isEditor = memorial.getEditors() != null && 
                             memorial.getEditors().stream().anyMatch(editor -> editor.getId().equals(user.getId()));
            log.info("Является ли пользователь редактором: {}", isEditor);
            
            boolean isAdmin = user.getRole() == User.Role.ADMIN;
            log.info("Является ли пользователь администратором: {}", isAdmin);
            
            boolean hasAccess = isOwner || isEditor || isAdmin;
            log.info("Общий результат проверки доступа: {}", hasAccess);
            
            if (!hasAccess) {
                log.warn("ОТКАЗ В ДОСТУПЕ к документу мемориала {} для пользователя {}", id, user.getLogin());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header("X-Error", "Access denied")
                    .build();
            }
            
            log.info("Доступ разрешен, получаем документ из сервиса...");
            // Получаем документ
            ResponseEntity<org.springframework.core.io.Resource> result = memorialService.getMemorialDocument(id);
            log.info("Документ успешно получен, статус ответа: {}", result.getStatusCode());
            return result;
            
        } catch (Exception e) {
            log.error("КРИТИЧЕСКАЯ ОШИБКА при получении документа мемориала {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error", "Internal server error: " + e.getMessage())
                .build();
        } finally {
            log.info("=== КОНЕЦ ЗАПРОСА ДОКУМЕНТА ===");
        }
    }

    /**
     * Проверяет наличие документа у мемориала.
     *
     * @param id ID мемориала
     * @return true если документ есть, false если нет
     */
    @GetMapping("/{id}/has-document")
    public boolean hasDocument(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            log.info("Проверка наличия документа мемориала {} пользователем {}", id, user.getLogin());
            
            // Проверяем права доступа к мемориалу
            boolean hasAccess = memorialService.hasViewAccess(id, user.getId()) || 
                               user.getRole() == User.Role.ADMIN;
            
            if (!hasAccess) {
                log.warn("Отказ в доступе к информации о документе мемориала {} для пользователя {}", id, user.getLogin());
                return false;
            }
            
            return memorialService.hasDocument(id);
        } catch (Exception e) {
            log.error("Ошибка при проверке наличия документа мемориала {}: {}", id, e.getMessage());
            return false;
        }
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
     * Получает мемориалы пользователя, доступные для добавления в конкретное дерево
     * Исключает мемориалы, которые уже находятся в других деревьях
     * Для редакторов исключает совместные мемориалы (где пользователь не владелец)
     * 
     * @param familyTreeId ID дерева
     * @return список доступных мемориалов
     */
    @GetMapping("/available-for-tree/{familyTreeId}")
    public List<MemorialDTO> getAvailableMemorialsForTree(@PathVariable Long familyTreeId) {
        User user = getCurrentUser();
        log.info("Получение доступных мемориалов для дерева ID={} пользователем {}", familyTreeId, user.getLogin());
        return memorialService.getAvailableMemorialsForTree(user.getId(), familyTreeId);
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
     * Загружает документ, подтверждающий существование человека.
     * Доступно только пользователям с подпиской.
     *
     * @param id ID памятника
     * @param file файл документа
     * @return URL загруженного документа
     */
    @PostMapping("/{id}/document")
    public String uploadDocument(@PathVariable Long id,
                                 @RequestParam("document") MultipartFile file) {
        User user = getCurrentUser();
        
        // Проверяем наличие подписки
        if (user.getHasSubscription() != Boolean.TRUE) {
            throw new IllegalStateException("Для загрузки документов требуется подписка");
        }
        
        log.info("Загрузка документа для мемориала ID={} пользователем {}", id, user.getLogin());
        return memorialService.uploadDocument(id, file);
    }

    /**
     * Удаляет документ мемориала.
     *
     * @param id ID мемориала
     * @return ResponseEntity с результатом операции
     */
    @DeleteMapping("/{id}/document")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        log.info("=== НАЧАЛО УДАЛЕНИЯ ДОКУМЕНТА ===");
        log.info("Удаление документа для мемориала с ID: {}", id);
        
        try {
            User user = getCurrentUser();
            log.info("Пользователь: {}", user.getLogin());
            
            memorialService.deleteDocument(id, user);
            log.info("Документ успешно удален для мемориала: {}", id);
            log.info("=== КОНЕЦ УДАЛЕНИЯ ДОКУМЕНТА ===");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("ОШИБКА при удалении документа для мемориала {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ищет памятники по различным параметрам с пагинацией.
     *
     * @param query строка поиска по названию или описанию памятника
     * @param location местоположение памятника
     * @param startDate дата начала периода для поиска
     * @param endDate дата окончания периода для поиска
     * @param isPublic фильтр по публичности
     * @param page номер страницы (начиная с 0)
     * @param size размер страницы
     * @return пагинированный список найденных памятников
     */
    @GetMapping("/search")
    public ru.cemeterysystem.dto.PagedResponse<MemorialDTO> searchMemorials(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return memorialService.searchMemorials(query, location, startDate, endDate, isPublic, page, size);
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
     * Удаляет редактора из мемориала (только для владельца)
     * 
     * @param id ID мемориала
     * @param editorId ID редактора для удаления
     * @return обновленный мемориал
     */
    @DeleteMapping("/{id}/editors/{editorId}")
    public MemorialDTO removeEditor(
            @PathVariable Long id,
            @PathVariable Long editorId) {
        log.info("Удаление редактора ID={} из мемориала ID={}", editorId, id);
        
        User currentUser = getCurrentUser();
        
        // Создаем запрос на удаление редактора
        EditorRequestDTO request = new EditorRequestDTO();
        request.setUserId(editorId);
        request.setAction("remove");
        
        return memorialService.manageEditor(id, request, currentUser);
    }
    
    /**
     * Отказ от права редактирования мемориала (для редактора)
     * 
     * @param id ID мемориала
     * @return обновленный мемориал
     */
    @PostMapping("/{id}/editors/resign")
    public MemorialDTO resignFromEditing(@PathVariable Long id) {
        log.info("Отказ от редактирования мемориала ID={}", id);
        
        User currentUser = getCurrentUser();
        
        // Создаем запрос на удаление себя из редакторов
        EditorRequestDTO request = new EditorRequestDTO();
        request.setUserId(currentUser.getId());
        request.setAction("remove");
        
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

    /**
     * Создает жалобу на мемориал
     *
     * @param id ID мемориала
     * @param request данные жалобы
     * @return ответ с результатом обработки жалобы
     */
    @PostMapping("/{id}/report")
    public ru.cemeterysystem.dto.MemorialReportResponseDTO reportMemorial(
            @PathVariable Long id, 
            @RequestBody ru.cemeterysystem.dto.MemorialReportRequestDTO request) {
        
        User user = getCurrentUser();
        
        // Получаем мемориал и проверяем его существование
        Memorial memorial = memorialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Мемориал не найден"));
        
        // Проверяем, что мемориал публичный
        if (!memorial.isPublic()) {
            return new ru.cemeterysystem.dto.MemorialReportResponseDTO(
                "error", 
                "Жалобы можно подавать только на публичные мемориалы"
            );
        }
        
        // Проверяем, что пользователь не является владельцем мемориала
        if (memorial.getCreatedBy().getId().equals(user.getId())) {
            return new ru.cemeterysystem.dto.MemorialReportResponseDTO(
                "error", 
                "Нельзя подавать жалобу на собственный мемориал"
            );
        }
        
        // Валидируем причину жалобы
        if (request.getReason() == null || request.getReason().trim().length() < 10) {
            return new ru.cemeterysystem.dto.MemorialReportResponseDTO(
                "error", 
                "Причина жалобы должна содержать минимум 10 символов"
            );
        }
        
        try {
            // Создаем уведомления для всех администраторов
            createReportNotificationForAdmins(user, memorial, request.getReason());
            
            log.info("Создана жалоба на мемориал ID={} от пользователя {}: {}", 
                    id, user.getLogin(), request.getReason());
                    
            return new ru.cemeterysystem.dto.MemorialReportResponseDTO(
                "success", 
                "Жалоба отправлена администратору"
            );
            
        } catch (Exception e) {
            log.error("Ошибка при создании жалобы на мемориал ID={}: {}", id, e.getMessage(), e);
            return new ru.cemeterysystem.dto.MemorialReportResponseDTO(
                "error", 
                "Ошибка при отправке жалобы: " + e.getMessage()
            );
        }
    }

    /**
     * Создает уведомления о жалобе для всех администраторов
     */
    private void createReportNotificationForAdmins(User reporter, Memorial memorial, String reason) {
        // Находим всех администраторов
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        
        if (admins.isEmpty()) {
            log.warn("Не найдено ни одного администратора для отправки жалобы");
            throw new RuntimeException("Не найдено ни одного администратора");
        }
        
        // Создаем уведомление для каждого администратора
        for (User admin : admins) {
            Notification notification = new Notification();
            notification.setTitle("Жалоба на мемориал");
            notification.setMessage(String.format(
                "Пользователь %s (%s) подал жалобу на мемориал \"%s\".\n\nПричина жалобы:\n%s",
                reporter.getFio() != null ? reporter.getFio() : reporter.getLogin(),
                reporter.getLogin(),
                memorial.getFio(),
                reason
            ));
            notification.setUser(admin);
            notification.setSender(reporter);
            notification.setType(Notification.NotificationType.MEMORIAL_REPORT);
            notification.setStatus(Notification.NotificationStatus.INFO);
            notification.setRelatedEntityId(memorial.getId());
            notification.setRelatedEntityName(memorial.getFio());
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notification.setRead(false);
            notification.setUrgent(true); // Жалобы требуют внимания
            
            // Сохраняем уведомление
            notificationRepository.save(notification);
        }
        
        log.info("Созданы уведомления о жалобе для {} администраторов", admins.size());
    }

    /**
     * Расширенный поиск мемориалов по различным критериям
     * 
     * @param query общий поисковый запрос (ФИО)
     * @param firstName имя
     * @param lastName фамилия
     * @param middleName отчество
     * @param birthDateFrom дата рождения от
     * @param birthDateTo дата рождения до
     * @param deathDateFrom дата смерти от
     * @param deathDateTo дата смерти до
     * @param location местоположение (адрес)
     * @param isPublic публичность
     * @param sortBy поле для сортировки
     * @param sortDirection направление сортировки
     * @param page номер страницы
     * @param size размер страницы
     * @return пагинированный список найденных памятников
     */
    @GetMapping("/search/advanced")
    public ru.cemeterysystem.dto.PagedResponse<MemorialDTO> advancedSearchMemorials(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String middleName,
            @RequestParam(required = false) String birthDateFrom,
            @RequestParam(required = false) String birthDateTo,
            @RequestParam(required = false) String deathDateFrom,
            @RequestParam(required = false) String deathDateTo,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Advanced search: query={}, firstName={}, lastName={}, middleName={}, location={}", 
                query, firstName, lastName, middleName, location);
        
        return memorialService.advancedSearchMemorials(
            query, firstName, lastName, middleName,
            birthDateFrom, birthDateTo, deathDateFrom, deathDateTo,
            location, isPublic, sortBy, sortDirection, page, size
        );
    }

    /**
     * Быстрый поиск мемориалов (автодополнение)
     * 
     * @param query поисковый запрос
     * @param limit максимальное количество результатов
     * @return список найденных памятников
     */
    @GetMapping("/search/quick")
    public List<MemorialDTO> quickSearchMemorials(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Quick search: query={}, limit={}", query, limit);
        return memorialService.quickSearchMemorials(query, limit);
    }

    /**
     * Поиск мемориалов по годовщинам (дни рождения/смерти)
     * 
     * @param type тип годовщины (birth/death)
     * @param month месяц (1-12)
     * @param day день (1-31)
     * @param page номер страницы
     * @param size размер страницы
     * @return пагинированный список мемориалов с годовщинами
     */
    @GetMapping("/search/anniversaries")
    public ru.cemeterysystem.dto.PagedResponse<MemorialDTO> searchAnniversaries(
            @RequestParam String type,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Anniversary search: type={}, month={}, day={}", type, month, day);
        return memorialService.searchAnniversaries(type, month, day, page, size);
    }

    /**
     * Поиск мемориалов с использованием DTO фильтров
     * 
     * @param searchDTO объект с параметрами поиска
     * @return пагинированный список найденных памятников
     */
    @PostMapping("/search/filter")
    public ru.cemeterysystem.dto.PagedResponse<MemorialDTO> searchMemorialsWithFilter(
            @RequestBody ru.cemeterysystem.dto.MemorialSearchDTO searchDTO
    ) {
        log.info("Search with filter DTO: {}", searchDTO);
        
        return memorialService.advancedSearchMemorials(
            searchDTO.getQuery(), 
            searchDTO.getFirstName(), 
            searchDTO.getLastName(), 
            searchDTO.getMiddleName(),
            searchDTO.getBirthDateFrom(), 
            searchDTO.getBirthDateTo(), 
            searchDTO.getDeathDateFrom(), 
            searchDTO.getDeathDateTo(),
            searchDTO.getLocation(), 
            searchDTO.getIsPublic(), 
            searchDTO.getSortBy(), 
            searchDTO.getSortDirection(), 
            searchDTO.getPage(), 
            searchDTO.getSize()
        );
    }

    /**
     * Получает статистику поиска
     * 
     * @return статистика по мемориалам
     */
    @GetMapping("/search/stats")
    public Map<String, Object> getSearchStats() {
        return memorialService.getSearchStats();
    }

    /**
     * Получает мемориалы в заданных географических границах для оптимизации карты
     * 
     * @param minLat минимальная широта
     * @param maxLat максимальная широта  
     * @param minLng минимальная долгота
     * @param maxLng максимальная долгота
     * @param page номер страницы
     * @param size размер страницы
     * @return пагинированный список мемориалов в заданных границах
     */
    @GetMapping("/search/bounds")
    public ru.cemeterysystem.dto.PagedResponse<MemorialDTO> getMemorialsInBounds(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLng,
            @RequestParam double maxLng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        log.info("Поиск мемориалов в границах: minLat={}, maxLat={}, minLng={}, maxLng={}, page={}, size={}", 
                minLat, maxLat, minLng, maxLng, page, size);
        
        return memorialService.getMemorialsInBounds(minLat, maxLat, minLng, maxLng, page, size);
    }
}
