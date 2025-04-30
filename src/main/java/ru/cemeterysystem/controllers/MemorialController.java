package ru.cemeterysystem.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.cemeterysystem.dto.MemorialDTO;
import ru.cemeterysystem.models.User;
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
        return memorialService.getMemorialById(id);
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
}
