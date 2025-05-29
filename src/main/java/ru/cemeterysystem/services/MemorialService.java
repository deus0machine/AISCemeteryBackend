package ru.cemeterysystem.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.cemeterysystem.dto.EditorRequestDTO;
import ru.cemeterysystem.dto.MemorialDTO;
import ru.cemeterysystem.dto.UserDTO;
import ru.cemeterysystem.mappers.MemorialMapper;
import ru.cemeterysystem.mappers.UserMapper;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.Notification;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.NotificationRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.services.FileStorageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemorialService {
    private static final Logger log = LoggerFactory.getLogger(MemorialService.class);
    
    private final MemorialRepository memorialRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final MemorialMapper memorialMapper;
    private final UserMapper userMapper;
    private final NotificationRepository notificationRepository;

    public List<MemorialDTO> getAllMemorials() {
        return memorialRepository.findAll().stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
    }

    public List<MemorialDTO> getMyMemorials(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        log.info("Получение мемориалов для пользователя: id={}, login={}", user.getId(), user.getLogin());
        
        // Получаем мемориалы, где пользователь является создателем
        List<Memorial> ownedMemorials = memorialRepository.findByCreatedBy(user);
        log.info("Найдено мемориалов, где пользователь является создателем: {}", ownedMemorials.size());
        
        // Получаем мемориалы, где пользователь является редактором - метод 1
        List<Memorial> editedMemorials = memorialRepository.findByEditorsContaining(user);
        log.info("Найдено мемориалов, где пользователь является редактором (метод 1): {}", editedMemorials.size());
        
        // Получаем мемориалы, где пользователь является редактором - метод 2 через JPQL
        List<Memorial> editedMemorials2 = memorialRepository.findMemorialsWhereUserIsEditor(user.getId());
        log.info("Найдено мемориалов, где пользователь является редактором (метод 2): {}", editedMemorials2.size());
        
        // Логируем данные о всех мемориалах в системе, чтобы понять структуру связей
        log.info("Проверка всех мемориалов в системе на наличие редакторов...");
        List<Memorial> allMemorialsInSystem = memorialRepository.findAll();
        log.info("Всего мемориалов в системе: {}", allMemorialsInSystem.size());
        
        for (Memorial m : allMemorialsInSystem) {
            // Проверяем все мемориалы и их редакторов
            if (m.getEditors() != null && !m.getEditors().isEmpty()) {
                log.info("Мемориал ID: {}, '{}', Владелец: {}, Количество редакторов: {}", 
                        m.getId(), m.getFio(), m.getCreatedBy().getLogin(), m.getEditors().size());
                
                for (User editor : m.getEditors()) {
                    log.info("  - Редактор: id={}, login={}", editor.getId(), editor.getLogin());
                    
                    // Если это редактор текущего пользователя - отмечаем особо
                    if (editor.getId().equals(user.getId())) {
                        log.info("  >>> НАЙДЕН ПОЛЬЗОВАТЕЛЬ {} КАК РЕДАКТОР МЕМОРИАЛА {}!", 
                                user.getLogin(), m.getId());
                        
                        // Проверяем, почему этот мемориал мог не попасть в результаты поиска
                        if (!editedMemorials.contains(m)) {
                            log.warn("  !!! Этот мемориал НЕ был найден методом findByEditorsContaining!");
                        }
                        if (!editedMemorials2.contains(m)) {
                            log.warn("  !!! Этот мемориал НЕ был найден методом findMemorialsWhereUserIsEditor!");
                        }
                        
                        // Если мемориал не найден ни одним методом, добавляем его вручную
                        if (!editedMemorials.contains(m) && !editedMemorials2.contains(m)) {
                            log.info("  +++ Добавляем мемориал вручную в список редактируемых");
                            editedMemorials.add(m);
                        }
                    }
                }
            }
        }
        
        // Объединяем результаты обоих методов
        for (Memorial memorial : editedMemorials2) {
            if (!editedMemorials.contains(memorial)) {
                editedMemorials.add(memorial);
                log.info("Добавлен мемориал в список редакторов (из метода 2): {}", memorial.getId());
            }
        }
        
        // Объединяем списки и удаляем дубликаты
        List<Memorial> allMemorials = new ArrayList<>(ownedMemorials);
        for (Memorial memorial : editedMemorials) {
            if (!allMemorials.contains(memorial)) {
                allMemorials.add(memorial);
                log.info("Добавлен мемориал ID: {} в итоговый список (пользователь - редактор)", memorial.getId());
            }
        }
        
        log.info("Общее количество мемориалов для отображения: {}", allMemorials.size());
        
        List<MemorialDTO> result = allMemorials.stream()
            .map(memorial -> {
                MemorialDTO dto = memorialMapper.toDTO(memorial);
                
                // Установим флаг isEditor для каждого мемориала
                boolean isOwner = memorial.getCreatedBy().equals(user);
                boolean isEditor = memorial.isEditor(user);
                
                if (isOwner) {
                    // Пользователь является основным владельцем
                    dto.setEditor(false);
                    log.info("Мемориал ID: {} - пользователь является владельцем", memorial.getId());
                } else if (isEditor) {
                    // Пользователь является редактором
                    dto.setEditor(true);
                    log.info("Мемориал ID: {} - пользователь является редактором", memorial.getId());
                } else {
                    log.warn("!!! Странная ситуация: мемориал ID: {} - пользователь не владелец и не редактор!", 
                           memorial.getId());
                }
                
                return dto;
            })
            .collect(Collectors.toList());
        
        log.info("Возвращаем {} мемориалов для пользователя {}", result.size(), user.getLogin());
        return result;
    }

    public List<MemorialDTO> getPublicMemorials() {
        // Получаем текущего пользователя (если авторизован)
        User userResult = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getPrincipal().equals("anonymousUser")) {
                String login = authentication.getName();
                userResult = userRepository.findByLogin(login).orElse(null);
                
                if (userResult != null) {
                    log.info("getPublicMemorials: Текущий пользователь: ID={}, login={}", 
                            userResult.getId(), userResult.getLogin());
                } else {
                    log.warn("getPublicMemorials: Не удалось найти пользователя по логину: {}", login);
                }
            } else {
                log.info("getPublicMemorials: Пользователь не аутентифицирован");
            }
        } catch (Exception e) {
            log.warn("getPublicMemorials: Не удалось получить текущего пользователя: {}", e.getMessage());
        }
        
        // Создаем final переменную для использования в лямбда-выражении
        final User currentUser = userResult;
        
        List<Memorial> publicMemorials = memorialRepository.findByIsPublicTrue();
        log.info("getPublicMemorials: Найдено {} публичных мемориалов", publicMemorials.size());
        
        return publicMemorials.stream()
            .map(memorial -> {
                // Проверяем, является ли пользователь владельцем или редактором
                boolean isOwner = currentUser != null && memorial.getCreatedBy().equals(currentUser);
                boolean isEditor = false;
                
                // Проверяем, есть ли текущий пользователь в списке редакторов
                if (currentUser != null && memorial.getEditors() != null) {
                    // Выводим список редакторов для отладки
                    List<Long> editorIds = memorial.getEditors().stream()
                        .map(User::getId)
                        .collect(Collectors.toList());
                    
                    log.info("getPublicMemorials: Мемориал ID={} имеет {} редакторов: {}", 
                           memorial.getId(), editorIds.size(), editorIds);
                           
                    // Проверяем каждого редактора и сравниваем ID
                    for (User editor : memorial.getEditors()) {
                        log.info("getPublicMemorials: Проверка редактора ID={} для мемориала ID={}, текущий пользователь ID={}", 
                                editor.getId(), memorial.getId(), currentUser.getId());
                        
                        if (editor.getId().equals(currentUser.getId())) {
                            isEditor = true;
                            log.info("getPublicMemorials: НАЙДЕНО СОВПАДЕНИЕ! Пользователь ID={} является редактором мемориала ID={}", 
                                    currentUser.getId(), memorial.getId());
                            break;
                        }
                    }
                    
                    log.info("getPublicMemorials: Результат проверки: мемориал ID={}, пользователь={}, is_editor={}", 
                           memorial.getId(), currentUser.getLogin(), isEditor);
                }
                
                // Если мемориал имеет ожидающие изменения и пользователь не владелец/редактор
                if (memorial.isPendingChanges() && !isOwner && !isEditor) {
                                    try {
                    if (memorial.getPreviousState() != null && !memorial.getPreviousState().isEmpty()) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        MemorialDTO previousDto = objectMapper.readValue(memorial.getPreviousState(), MemorialDTO.class);
                            
                            // Обновляем ID и редакторов, которые не хранятся в previousState
                            previousDto.setId(memorial.getId());
                            previousDto.setEditorIds(memorial.getEditors().stream()
                                .map(User::getId)
                                .collect(Collectors.toList()));
                            
                            // Устанавливаем флаг редактора на основе нашей проверки
                            previousDto.setEditor(isEditor);
                            
                            // Устанавливаем флаг pendingChanges в false для внешних пользователей
                            previousDto.setPendingChanges(false);
                            
                            return previousDto;
                        }
                    } catch (Exception e) {
                        log.error("Ошибка при восстановлении предыдущего состояния мемориала: {}", e.getMessage());
                    }
                }
                
                // В обычном случае возвращаем текущее состояние
                MemorialDTO dto = memorialMapper.toDTO(memorial);
                
                // Устанавливаем флаг редактора на основе нашей проверки
                dto.setEditor(isEditor);
                
                // Показываем флаг pendingChanges только владельцу и редакторам
                if (!isOwner && !isEditor) {
                    dto.setPendingChanges(false);
                }
                
                log.info("getPublicMemorials: Для мемориала ID={} установлен флаг is_editor={}", memorial.getId(), isEditor);
                
                return dto;
            })
            .collect(Collectors.toList());
    }

    public MemorialDTO getMemorialById(Long id) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        MemorialDTO dto = memorialMapper.toDTO(memorial);
        return dto;
    }

    public MemorialDTO getMemorialByIdForUser(Long id, User currentUser) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        log.info("getMemorialByIdForUser: Запрос мемориала ID={} для пользователя ID={}, login={}", 
                id, currentUser != null ? currentUser.getId() : "null", 
                currentUser != null ? currentUser.getLogin() : "null");
        
        // Проверяем, является ли пользователь владельцем или редактором
        boolean isOwner = currentUser != null && memorial.getCreatedBy().equals(currentUser);
        boolean isEditor = false;
        
        // Проверяем, есть ли текущий пользователь в списке редакторов
        if (currentUser != null && memorial.getEditors() != null) {
            // Выводим список редакторов для отладки
            List<Long> editorIds = memorial.getEditors().stream()
                .map(User::getId)
                .collect(Collectors.toList());
            
            log.info("getMemorialByIdForUser: Мемориал ID={} имеет {} редакторов: {}", 
                   id, editorIds.size(), editorIds);
                   
            // Проверяем каждого редактора и сравниваем ID
            for (User editor : memorial.getEditors()) {
                log.info("getMemorialByIdForUser: Проверка редактора ID={} для мемориала ID={}, текущий пользователь ID={}", 
                        editor.getId(), memorial.getId(), currentUser.getId());
                
                if (editor.getId().equals(currentUser.getId())) {
                    isEditor = true;
                    log.info("getMemorialByIdForUser: НАЙДЕНО СОВПАДЕНИЕ! Пользователь ID={} является редактором мемориала ID={}", 
                            currentUser.getId(), memorial.getId());
                    break;
                }
            }
            
            log.info("getMemorialByIdForUser: Проверка статуса редактора: мемориал ID={}, пользователь={}, is_editor={}, в списке редакторов: {}", 
                   memorial.getId(), currentUser.getLogin(), isEditor, editorIds);
        }
        
        // Если есть ожидающие изменения и текущий пользователь не владелец и не редактор,
        // возвращаем предыдущее состояние мемориала
        if (memorial.isPendingChanges() && !isOwner && !isEditor) {
            try {
                if (memorial.getPreviousState() != null && !memorial.getPreviousState().isEmpty()) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    MemorialDTO previousDto = objectMapper.readValue(memorial.getPreviousState(), MemorialDTO.class);
                    
                    // Обновляем ID и редакторов, которые не хранятся в previousState
                    previousDto.setId(memorial.getId());
                    previousDto.setEditorIds(memorial.getEditors().stream()
                        .map(User::getId)
                        .collect(Collectors.toList()));
                    
                    // Устанавливаем флаг редактора на основе нашей проверки
                    previousDto.setEditor(isEditor);
                    
                    // Устанавливаем флаг pendingChanges в false для внешних пользователей
                    previousDto.setPendingChanges(false);
                    
                    return previousDto;
                }
            } catch (Exception e) {
                log.error("Ошибка при восстановлении предыдущего состояния мемориала: {}", e.getMessage());
            }
        }
        
        MemorialDTO dto = memorialMapper.toDTO(memorial);
        
        // Устанавливаем флаг редактора на основе нашей проверки
        dto.setEditor(isEditor);
        
        // Показываем флаг pendingChanges только владельцу и редакторам
        if (!isOwner && !isEditor) {
            dto.setPendingChanges(false);
        }
        
        log.info("getMemorialByIdForUser: Возвращается мемориал ID={} с флагом is_editor={}", memorial.getId(), isEditor);
        
        return dto;
    }

    public MemorialDTO createMemorial(MemorialDTO dto, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        validateMemorialDates(LocalDate.parse(dto.getBirthDate()), 
                            dto.getDeathDate() != null ? LocalDate.parse(dto.getDeathDate()) : null);

        Memorial memorial = new Memorial();
        updateMemorialFromDTO(memorial, dto);
        memorial.setCreatedBy(user);
        memorial.setUser(user);

        return memorialMapper.toDTO(memorialRepository.save(memorial));
    }

    /**
     * Обновляет существующий мемориал
     * @param id ID мемориала
     * @param memorialDTO данные для обновления
     * @param user пользователь, выполняющий обновление
     * @return обновленный мемориал
     */
    @Transactional
    public MemorialDTO updateMemorial(Long id, MemorialDTO memorialDTO, User user) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
            
        // Проверяем права доступа
        if (!memorial.getCreatedBy().equals(user) && !memorial.isEditor(user)) {
            throw new RuntimeException("Unauthorized access");
        }
        
        // Проверяем статус мемориала - строгая проверка на модерацию
        if (memorial.getPublicationStatus() == Memorial.PublicationStatus.PENDING_MODERATION) {
            log.error("Попытка обновить мемориал ID={} находящийся на модерации. Пользователь ID={}, логин={}",
                    id, user.getId(), user.getLogin());
            throw new RuntimeException("Cannot edit memorial that is under moderation");
        }
        
        boolean isEditor = !memorial.getCreatedBy().equals(user) && memorial.isEditor(user);
        log.info("Обновление мемориала ID={}, пользователь={} (ID={}), isEditor={}",
                id, user.getLogin(), user.getId(), isEditor);
        
        // Обновляем поля мемориала
        if (isEditor) {
            // Если редактор - сохраняем как предложенные изменения
            updateMemorialPendingFields(memorial, memorialDTO, user);
        } else {
            // Если владелец - применяем изменения сразу
            updateMemorialFields(memorial, memorialDTO);
        }
        
        Memorial updatedMemorial = memorialRepository.save(memorial);
        return memorialMapper.toDTO(updatedMemorial);
    }
    
    /**
     * Обновляет поля мемориала из DTO для владельца (прямое обновление)
     */
    private void updateMemorialFields(Memorial memorial, MemorialDTO dto) {
        if (dto.getFio() != null) {
            memorial.setFio(dto.getFio());
        }
        
                if (dto.getBirthDate() != null) {
            memorial.setBirthDate(LocalDate.parse(dto.getBirthDate()));
                }
                
                if (dto.getDeathDate() != null) {
            memorial.setDeathDate(LocalDate.parse(dto.getDeathDate()));
                } else {
            memorial.setDeathDate(null);
                }
                
        if (dto.getBiography() != null) {
            memorial.setBiography(dto.getBiography());
        }
        
                if (dto.getMainLocation() != null) {
            memorial.setMainLocation(dto.getMainLocation());
                }
                
                if (dto.getBurialLocation() != null) {
            memorial.setBurialLocation(dto.getBurialLocation());
        }
        
        memorial.setPublic(dto.isPublic());
        
        // Если было изменение через редактора, сбрасываем ожидающие изменения
        memorial.setPendingChanges(false);
        memorial.setPendingBiography(null);
        memorial.setPendingBirthDate(null);
        memorial.setPendingDeathDate(null);
        memorial.setPendingMainLocation(null);
        memorial.setPendingBurialLocation(null);
        memorial.setPendingPhotoUrl(null);
        memorial.setLastEditorId(null);
    }
    
    /**
     * Обновляет поля мемориала из DTO для редактора (в ожидающие изменения)
     */
    private void updateMemorialPendingFields(Memorial memorial, MemorialDTO dto, User editor) {
        boolean hasChanges = false;
        
        if (dto.getFio() != null && !dto.getFio().equals(memorial.getFio())) {
            // ФИО не может быть изменено редактором, только владельцем
            log.warn("Редактор пытается изменить ФИО мемориала, это разрешено только владельцу");
        }
        
        if (dto.getBirthDate() != null) {
            LocalDate birthDate = LocalDate.parse(dto.getBirthDate());
            if (!birthDate.equals(memorial.getBirthDate())) {
                memorial.setPendingBirthDate(birthDate);
                hasChanges = true;
            }
        }
        
        if (dto.getDeathDate() != null) {
            LocalDate deathDate = LocalDate.parse(dto.getDeathDate());
            if (memorial.getDeathDate() == null || !deathDate.equals(memorial.getDeathDate())) {
                memorial.setPendingDeathDate(deathDate);
                hasChanges = true;
            }
        } else if (memorial.getDeathDate() != null) {
            memorial.setPendingDeathDate(null);
            hasChanges = true;
        }
        
        if (dto.getBiography() != null && !dto.getBiography().equals(memorial.getBiography())) {
            memorial.setPendingBiography(dto.getBiography());
            hasChanges = true;
        }
        
        if (dto.getMainLocation() != null && !dto.getMainLocation().equals(memorial.getMainLocation())) {
            memorial.setPendingMainLocation(dto.getMainLocation());
            hasChanges = true;
        }
        
        if (dto.getBurialLocation() != null && !dto.getBurialLocation().equals(memorial.getBurialLocation())) {
            memorial.setPendingBurialLocation(dto.getBurialLocation());
            hasChanges = true;
        }
        
        // Публичность может быть изменена только владельцем
        
        if (hasChanges) {
            memorial.setPendingChanges(true);
            memorial.setLastEditorId(editor.getId());
            
            // Сохраняем предыдущее состояние, если еще не сохранено
            if (memorial.getPreviousState() == null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    
                    MemorialDTO currentDto = memorialMapper.toDTO(memorial);
                    memorial.setPreviousState(objectMapper.writeValueAsString(currentDto));
            } catch (Exception e) {
                    log.error("Ошибка при сохранении предыдущего состояния мемориала: {}", e.getMessage(), e);
                }
            }
            
            // Создаем уведомление для владельца
            createPendingChangesNotification(memorial, editor);
        }
    }

    public List<MemorialDTO> searchMemorials(String query, String location, String startDate,
                                        String endDate, Boolean isPublic) {
        return memorialRepository.search(query, location, startDate, endDate, isPublic).stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
    }

    public List<MemorialDTO> findByFio(String fio) {
        return memorialRepository.findByFio(fio).stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
    }

    public List<MemorialDTO> findByUserId(Long userId) {
        return memorialRepository.findByUser_Id(userId).stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
    }

    private void updateMemorialFromDTO(Memorial memorial, MemorialDTO dto) {
        memorial.setFio(dto.getFio());
        memorial.setBirthDate(LocalDate.parse(dto.getBirthDate()));
        if (dto.getDeathDate() != null) {
            memorial.setDeathDate(LocalDate.parse(dto.getDeathDate()));
        }
        memorial.setBiography(dto.getBiography());
        memorial.setMainLocation(dto.getMainLocation());
        memorial.setBurialLocation(dto.getBurialLocation());
        memorial.setPublic(dto.isPublic());
        memorial.setTreeId(dto.getTreeId());
    }

    private void validateMemorialDates(LocalDate birthDate, LocalDate deathDate) {
        if (deathDate != null) {
            if (deathDate.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Дата смерти не может быть позже сегодняшнего дня");
            }
            if (deathDate.isBefore(birthDate)) {
                throw new IllegalArgumentException("Дата смерти не может быть раньше даты рождения");
            }
        }
    }
    
    /**
     * Получает список редакторов мемориала
     * 
     * @param memorialId ID мемориала
     * @return список пользователей-редакторов
     */
    public List<UserDTO> getMemorialEditors(Long memorialId) {
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        log.info("Получение редакторов для мемориала ID={}, количество редакторов: {}", 
                memorialId, memorial.getEditors().size());
        
        return memorial.getEditors().stream()
            .map(userMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Управление редакторами мемориала
     * 
     * @param memorialId ID мемориала
     * @param request запрос с информацией о редакторе и действии
     * @param currentUser текущий пользователь
     * @return обновленный мемориал
     */
    @Transactional
    public MemorialDTO manageEditor(Long memorialId, EditorRequestDTO request, User currentUser) {
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        // Проверяем права доступа - только владелец может управлять редакторами
        if (!memorial.getCreatedBy().equals(currentUser)) {
            throw new RuntimeException("Only memorial owner can manage editors");
        }
        
        User editor = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("Editor user not found"));
        
        String action = request.getAction();
        
        if ("add".equals(action)) {
            // Добавляем редактора
            memorial.addEditor(editor);
            log.info("Добавлен редактор ID={} к мемориалу ID={}", editor.getId(), memorialId);
        } else if ("remove".equals(action)) {
            // Удаляем редактора
            memorial.removeEditor(editor);
            log.info("Удален редактор ID={} из мемориала ID={}", editor.getId(), memorialId);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
        
        // Выполняем завершающее сохранение и проверяем, что флаги сброшены правильно
        memorial = memorialRepository.save(memorial);
        MemorialDTO resultDto = memorialMapper.toDTO(memorial);
        
        log.info("Завершено подтверждение/отклонение изменений мемориала ID={}, финальное состояние pendingChanges={}", 
                memorial.getId(), memorial.isPendingChanges());
        
        return resultDto;
    }
    
    /**
     * Получает мемориалы, ожидающие подтверждения изменений
     * 
     * @param currentUser текущий пользователь
     * @return список мемориалов с изменениями
     */
    public List<MemorialDTO> getEditedMemorials(User currentUser) {
        List<Memorial> memorialsWithPendingChanges = new ArrayList<>();
        
        // Получаем мемориалы, владельцем которых является текущий пользователь
        List<Memorial> ownedMemorials = memorialRepository.findByCreatedBy(currentUser);
        
        // Фильтруем только те, которые имеют ожидающие изменения
        List<Memorial> editedOwnedMemorials = ownedMemorials.stream()
            .filter(Memorial::isPendingChanges)
            .collect(Collectors.toList());
        
        memorialsWithPendingChanges.addAll(editedOwnedMemorials);
        
        // Получаем мемориалы, где пользователь является редактором
        List<Memorial> editedMemorials = memorialRepository.findByEditorsContaining(currentUser);
        
        // Фильтруем только те, которые имеют ожидающие изменения
        List<Memorial> editedAsEditorMemorials = editedMemorials.stream()
            .filter(Memorial::isPendingChanges)
            .collect(Collectors.toList());
        
        memorialsWithPendingChanges.addAll(editedAsEditorMemorials);
        
        return memorialsWithPendingChanges.stream()
            .map(memorial -> {
                MemorialDTO dto = memorialMapper.toDTO(memorial);
                boolean isEditor = !memorial.getCreatedBy().equals(currentUser);
                dto.setEditor(isEditor);
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Получает информацию о ожидающих изменениях мемориала
     * Возвращает мемориал с заполненными полями pending* если есть ожидающие изменения
     *
     * @param id ID мемориала
     * @param currentUser текущий пользователь
     * @return DTO мемориала с информацией о ожидающих изменениях
     */
    public MemorialDTO getMemorialPendingChanges(Long id, User currentUser) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
            
        // Проверяем права доступа
        boolean isOwner = memorial.getCreatedBy().equals(currentUser);
        boolean isEditor = memorial.isEditor(currentUser);
        
        if (!isOwner && !isEditor) {
            throw new RuntimeException("Нет прав для просмотра ожидающих изменений");
        }
        
        log.info("Получение ожидающих изменений для мемориала ID={}, пользователь={}, isOwner={}, isEditor={}",
                id, currentUser.getLogin(), isOwner, isEditor);
                
        MemorialDTO dto = memorialMapper.toDTO(memorial);
        dto.setEditor(isEditor);
        
        // Возвращаем DTO с информацией об ожидающих изменениях
        // Поля pendingPhotoUrl и другие pending* поля уже должны быть заполнены маппером
        
        return dto;
    }
    
    @Transactional
    public MemorialDTO approveChanges(Long memorialId, boolean approve, User currentUser) {
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        // Проверяем права доступа - только владелец может подтверждать изменения
        if (!memorial.getCreatedBy().equals(currentUser)) {
            throw new RuntimeException("Only memorial owner can approve changes");
        }
        
        if (!memorial.isPendingChanges()) {
            throw new RuntimeException("Memorial has no pending changes");
        }
        
        if (approve) {
            log.info("Подтверждены изменения для мемориала ID={}", memorialId);
            
            try {
                // Применяем ожидающие изменения
                
                // 1. Обрабатываем фото
                if (memorial.getPendingPhotoUrl() != null && !memorial.getPendingPhotoUrl().isEmpty()) {
                    log.info("Применяем ожидающее фото для мемориала ID={}", memorialId);
                    
                    // Удаляем старое фото, если есть
                    if (memorial.getPhotoUrl() != null) {
                        fileStorageService.deleteFile(memorial.getPhotoUrl());
                    }
                    
                    // Применяем новое фото
                    memorial.setPhotoUrl(memorial.getPendingPhotoUrl());
                    memorial.setPendingPhotoUrl(null);
                }
                
                // 2. Обрабатываем биографию
                if (memorial.getPendingBiography() != null) {
                    log.info("Применяем ожидающую биографию для мемориала ID={}", memorialId);
                    memorial.setBiography(memorial.getPendingBiography());
                    memorial.setPendingBiography(null);
                }
                
                // 3. Обрабатываем даты
                if (memorial.getPendingBirthDate() != null) {
                    log.info("Применяем ожидающую дату рождения для мемориала ID={}", memorialId);
                    memorial.setBirthDate(memorial.getPendingBirthDate());
                    memorial.setPendingBirthDate(null);
                }
                
                if (memorial.getPendingDeathDate() != null) {
                    log.info("Применяем ожидающую дату смерти для мемориала ID={}", memorialId);
                    memorial.setDeathDate(memorial.getPendingDeathDate());
                    memorial.setPendingDeathDate(null);
                }
                
                // 4. Обрабатываем местоположения
                if (memorial.getPendingMainLocation() != null) {
                    log.info("Применяем ожидающее основное местоположение для мемориала ID={}", memorialId);
                    memorial.setMainLocation(memorial.getPendingMainLocation());
                    memorial.setPendingMainLocation(null);
                }
                
                if (memorial.getPendingBurialLocation() != null) {
                    log.info("Применяем ожидающее место захоронения для мемориала ID={}", memorialId);
                    memorial.setBurialLocation(memorial.getPendingBurialLocation());
                    memorial.setPendingBurialLocation(null);
                }
                
                // Если есть и старое поле proposedChanges, тоже обрабатываем его для совместимости
                if (memorial.getProposedChanges() != null && !memorial.getProposedChanges().isEmpty()) {
                    log.info("Найдены также предложенные изменения в старом формате, применяем их");
                    
                    // Здесь оставляем старый код для обратной совместимости
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    
                    log.info("JSON с предложенными изменениями: {}", memorial.getProposedChanges());
                    
                    try {
                        MemorialDTO proposedDto = objectMapper.readValue(memorial.getProposedChanges(), MemorialDTO.class);
                        log.info("Успешно десериализованы предложенные изменения: {}", proposedDto.getFio());
                        
                        // Применяем предложенные изменения
                        updateMemorialFromDTO(memorial, proposedDto);
                    } catch (Exception e) {
                        log.error("Ошибка при десериализации предложенных изменений: {}", e.getMessage(), e);
                    }
                }
                
                // Сбрасываем флаг ожидающих изменений и все временные данные
                memorial.setPendingChanges(false);
                memorial.setPreviousState(null);
                memorial.setProposedChanges(null);
                
                // Сохраняем изменения
                memorial = memorialRepository.saveAndFlush(memorial);
                log.info("Успешно применены все ожидающие изменения для мемориала ID={}", memorialId);
                
                // Создаем уведомление для редактора о том, что изменения приняты
                if (memorial.getLastEditorId() != null) {
                    User editor = userRepository.findById(memorial.getLastEditorId())
                        .orElse(null);
                    
                    if (editor != null) {
                        Notification notification = new Notification();
                        notification.setUser(editor);
                        notification.setSender(currentUser);
                        notification.setTitle("Изменения в мемориале приняты");
                        notification.setMessage("Ваши изменения для мемориала \"" + memorial.getFio() + "\" были одобрены.");
                        notification.setType(Notification.NotificationType.MEMORIAL_EDIT);
                        notification.setStatus(Notification.NotificationStatus.ACCEPTED);
                        notification.setRelatedEntityId(memorial.getId());
                        notification.setRelatedEntityName(memorial.getFio());
                        notification.setCreatedAt(LocalDateTime.now());
                        notification.setRead(false);
                        
                        notificationRepository.save(notification);
                        log.info("Создано уведомление для редактора ID={} о принятии изменений", editor.getId());
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка при применении ожидающих изменений: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка при применении изменений: " + e.getMessage());
            }
        } else {
            log.info("Отклонены изменения для мемориала ID={}", memorialId);
            
            // При отклонении изменений очищаем все временные данные
            memorial.setPendingPhotoUrl(null);
            memorial.setPendingBiography(null);
            memorial.setPendingBirthDate(null);
            memorial.setPendingDeathDate(null);
            memorial.setPendingMainLocation(null);
            memorial.setPendingBurialLocation(null);
            memorial.setPendingChanges(false);
            memorial.setPreviousState(null);
            memorial.setProposedChanges(null);
            
            // Сохраняем изменения
            memorial = memorialRepository.saveAndFlush(memorial);
            log.info("Сохранен мемориал с отклоненными изменениями, ID={}", memorial.getId());
            
            // Создаем уведомление для редактора о том, что изменения отклонены
            if (memorial.getLastEditorId() != null) {
                User editor = userRepository.findById(memorial.getLastEditorId())
                    .orElse(null);
                
                if (editor != null) {
                    Notification notification = new Notification();
                    notification.setUser(editor);
                    notification.setSender(currentUser);
                    notification.setTitle("Изменения в мемориале отклонены");
                    notification.setMessage("Ваши изменения для мемориала \"" + memorial.getFio() + "\" были отклонены.");
                    notification.setType(Notification.NotificationType.MEMORIAL_EDIT);
                    notification.setStatus(Notification.NotificationStatus.REJECTED);
                    notification.setRelatedEntityId(memorial.getId());
                    notification.setRelatedEntityName(memorial.getFio());
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setRead(false);
                    
                    notificationRepository.save(notification);
                    log.info("Создано уведомление для редактора ID={} о отклонении изменений", editor.getId());
                }
            }
            
            memorial.setLastEditorId(null);
        }
        
        // Еще раз принудительно сбрасываем флаг ожидания изменений
        memorial.setPendingChanges(false);
        
        // Выполняем завершающее сохранение и проверяем флаги
        memorial = memorialRepository.saveAndFlush(memorial);
        log.info("Завершено подтверждение/отклонение изменений мемориала ID={}, финальное состояние pendingChanges={}", 
                memorial.getId(), memorial.isPendingChanges());
        
        // Для обновления кэша и состояния в БД, перезагружаем мемориал из репозитория
        memorial = memorialRepository.findById(memorial.getId())
            .orElseThrow(() -> new RuntimeException("Memorial not found after save"));
        
        // Последняя проверка - если pendingChanges все еще true после всех обновлений, сбрасываем еще раз
        if (memorial.isPendingChanges()) {
            log.warn("ВНИМАНИЕ! После сохранения мемориал ID={} все еще имеет флаг pendingChanges=true", memorial.getId());
            // Принудительно обновляем флаг еще раз, если он все еще true
            memorial.setPendingChanges(false);
            memorial = memorialRepository.saveAndFlush(memorial);
            log.info("Принудительно сброшен флаг pendingChanges для мемориала ID={}", memorial.getId());
        }
        
        return memorialMapper.toDTO(memorial);
    }

    /**
     * Получает список всех мемориалов с загруженными редакторами
     * 
     * @return список всех мемориалов
     */
    public List<Memorial> getAllMemorialsWithEditors() {
        return memorialRepository.findAll();
    }

    @Transactional
    public String uploadPhoto(Long id, MultipartFile file) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        // Получаем текущего пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        User currentUser = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Проверяем, является ли пользователь редактором или владельцем
        boolean isEditor = memorial.isEditor(currentUser);
        boolean isOwner = memorial.getCreatedBy().equals(currentUser);
        
        log.info("Загрузка фото для мемориала ID={}: пользователь={}, isEditor={}, isOwner={}", 
                id, currentUser.getLogin(), isEditor, isOwner);
        
        // Сохраняем фото в хранилище (в любом случае)
        String photoUrl = fileStorageService.storeFile(file);
        
        // Если пользователь - редактор (но не владелец)
        if (isEditor && !isOwner) {
            log.info("Загрузка фото редактором. ID мемориала: {}, редактор: {}", id, currentUser.getLogin());
            
            try {
                // Проверяем, есть ли уже ожидающее изменение фото
                boolean hasPendingPhotoAlready = memorial.getPendingPhotoUrl() != null && !memorial.getPendingPhotoUrl().isEmpty();
                
                // Сохраняем текущее состояние фото (если есть)
                String oldPhotoUrl = memorial.getPhotoUrl();
                
                // Сохраняем информацию о новом фото в pendingPhotoUrl
                memorial.setPendingPhotoUrl(photoUrl);
                
                // Устанавливаем флаг ожидания изменений
                memorial.setPendingChanges(true);
                
                // Запоминаем, кто внес изменения
                memorial.setLastEditorId(currentUser.getId());
                
                // Если previousState еще не сохранен (первое изменение)
                if (memorial.getPreviousState() == null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    
                    // Преобразуем мемориал в DTO и сохраняем как previousState
                    MemorialDTO currentDto = memorialMapper.toDTO(memorial);
                    String previousState = objectMapper.writeValueAsString(currentDto);
                    memorial.setPreviousState(previousState);
                }
                
                // Формируем текст изменения фото
                StringBuilder photoChangeDetails = new StringBuilder();
                photoChangeDetails.append("- Загружено новое фото:\n");
                photoChangeDetails.append("  URL фото: ").append(photoUrl).append("\n\n");
                
                // Проверяем, есть ли уже уведомление о ожидающих изменениях для этого мемориала
                List<Notification> existingNotifications = notificationRepository.findByRelatedEntityIdAndTypeAndStatus(
                    memorial.getId(), 
                    Notification.NotificationType.MEMORIAL_EDIT, 
                    Notification.NotificationStatus.PENDING
                );
                
                Notification notification;
                
                if (!existingNotifications.isEmpty()) {
                    // Обновляем существующее уведомление
                    notification = existingNotifications.get(0);
                    
                    // Обновляем сообщение, сохраняя существующие изменения
                    String currentMessage = notification.getMessage();
                    
                    if (currentMessage.contains("=== ИЗМЕНЕНИЯ ===")) {
                        // Находим позицию начала списка изменений и добавляем новую информацию о фото
                        int changeStartPos = currentMessage.indexOf("=== ИЗМЕНЕНИЯ ===") + "=== ИЗМЕНЕНИЯ ===\n".length();
                        
                        // Сохраняем существующее содержимое и добавляем информацию о фото в начало
                        String existingChanges = currentMessage.substring(changeStartPos);
                        String newMessage = currentMessage.substring(0, changeStartPos) + 
                                          photoChangeDetails.toString() + 
                                          existingChanges;
                        
                        notification.setMessage(newMessage);
                    } else {
                        // Если формат сообщения отличается, создаем новое сообщение целиком
                        String newMessage = "Редактор " + currentUser.getFio() +
                            " внес изменения в мемориал \"" + memorial.getFio() +
                            "\". Требуется ваше подтверждение.\n\n=== ИЗМЕНЕНИЯ ===\n" + 
                            photoChangeDetails.toString();
                        
                        notification.setMessage(newMessage);
                    }
                    
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setRead(false);
                    
                    log.info("Обновлено существующее уведомление ID={} для мемориала ID={}", 
                            notification.getId(), memorial.getId());
                } else {
                    // Создаем новое уведомление
                    notification = new Notification();
                    notification.setTitle("Изменения в мемориале");
                    
                    String message = "Редактор " + currentUser.getFio() +
                        " внес изменения в мемориал \"" + memorial.getFio() +
                        "\". Требуется ваше подтверждение.\n\n=== ИЗМЕНЕНИЯ ===\n" + 
                        photoChangeDetails.toString();
                    
                    notification.setMessage(message);
                    notification.setUser(memorial.getCreatedBy());
                    notification.setSender(currentUser);
                    notification.setType(Notification.NotificationType.MEMORIAL_EDIT);
                    notification.setStatus(Notification.NotificationStatus.PENDING);
                    notification.setRelatedEntityId(memorial.getId());
                    notification.setRelatedEntityName(memorial.getFio());
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setRead(false);
                    
                    log.info("Создано новое уведомление о изменении мемориала ID={} для владельца ID={}", 
                            memorial.getId(), memorial.getCreatedBy().getId());
                }
                
                // Сохраняем уведомление
                notification = notificationRepository.save(notification);
                log.info("Сохранено уведомление ID={} для мемориала ID={}", 
                        notification.getId(), memorial.getId());
                
                // Создаем уведомление для редактора о том, что его изменения ожидают подтверждения
                // Проверяем, есть ли уже уведомление для редактора
                List<Notification> existingEditorNotifications = notificationRepository.findByUserIdAndRelatedEntityIdAndTypeAndStatus(
                    currentUser.getId(),
                    memorial.getId(),
                    Notification.NotificationType.MEMORIAL_EDIT,
                    Notification.NotificationStatus.INFO
                );
                
                if (!existingEditorNotifications.isEmpty()) {
                    // Если уже есть уведомление для редактора, не создаем новое
                    log.info("Уведомление для редактора ID={} уже существует, не создаем новое", currentUser.getId());
                } else {
                    // Получаем системного пользователя
                    User systemUser = userRepository.findByLogin("system")
                            .orElseGet(() -> {
                                User system = new User();
                                system.setLogin("system");
                                system.setFio("Система");
                                system.setDateOfRegistration(new Date());
                                system.setPassword("system_password");
                                system.setRole(User.Role.USER);
                                system.setHasSubscription(false);
                                return userRepository.save(system);
                            });
                    
                    // Создаем информационное уведомление для редактора
                    Notification editorNotification = new Notification();
                    editorNotification.setTitle("Ваши изменения ожидают подтверждения");
                    editorNotification.setMessage("Вы внесли изменения в мемориал \"" + memorial.getFio() + 
                            "\". Изменения будут видны после подтверждения владельцем.");
                    
                    // Получатель - редактор (текущий пользователь)
                    editorNotification.setUser(currentUser);
                    
                    // Отправитель - система
                    editorNotification.setSender(systemUser);
                    
                    editorNotification.setType(Notification.NotificationType.MEMORIAL_EDIT);
                    editorNotification.setStatus(Notification.NotificationStatus.INFO); // Информационное уведомление
                    editorNotification.setRelatedEntityId(memorial.getId());
                    editorNotification.setRelatedEntityName(memorial.getFio());
                    editorNotification.setCreatedAt(LocalDateTime.now());
                    editorNotification.setRead(false);
                    
                    // Сохраняем уведомление для редактора
                    Notification savedEditorNotification = notificationRepository.save(editorNotification);
                    log.info("Создано информационное уведомление ID={} для редактора ID={} о изменениях", 
                            savedEditorNotification.getId(), currentUser.getId());
                }
                
                // Сохраняем мемориал
                memorialRepository.save(memorial);
                
                log.info("Сохранены ожидающие изменения фото для мемориала ID={}", memorial.getId());
                
                // Возвращаем URL, но фактически он еще не применен к мемориалу
                return photoUrl;
                
            } catch (Exception e) {
                log.error("Ошибка при обработке фото от редактора: {}", e.getMessage(), e);
                
                // В случае ошибки всё равно возвращаем URL, чтобы не терять фото
                return photoUrl;
            }
        } else {
            // Если владелец - применяем изменения сразу
            if (memorial.getPhotoUrl() != null) {
                fileStorageService.deleteFile(memorial.getPhotoUrl());
            }
            
            // Применяем новое фото
            memorial.setPhotoUrl(photoUrl);
            
            // Сбрасываем pendingPhotoUrl, если было
            memorial.setPendingPhotoUrl(null);
            
            memorialRepository.save(memorial);
            return photoUrl;
        }
    }

    /**
     * Удаляет мемориал по ID
     *
     * @param id ID мемориала для удаления
     */
    @Transactional
    public void deleteMemorial(Long id) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        // Получаем текущего пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        User currentUser = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Проверяем права доступа - только владелец может удалить мемориал
        if (!memorial.getCreatedBy().equals(currentUser)) {
            throw new RuntimeException("Only memorial owner can delete memorial");
        }
        
        // Удаляем фото, если есть
        if (memorial.getPhotoUrl() != null) {
            fileStorageService.deleteFile(memorial.getPhotoUrl());
        }
        
        // Удаляем ожидающее фото, если есть
        if (memorial.getPendingPhotoUrl() != null) {
            fileStorageService.deleteFile(memorial.getPendingPhotoUrl());
        }
        
        // Удаляем уведомления, связанные с этим мемориалом
        List<Notification> notifications = notificationRepository.findByRelatedEntityId(memorial.getId());
        if (!notifications.isEmpty()) {
            notificationRepository.deleteAll(notifications);
            log.info("Удалено {} уведомлений, связанных с мемориалом ID={}", notifications.size(), id);
        }
        
        // Удаляем мемориал
        memorialRepository.delete(memorial);
        log.info("Удален мемориал ID={}", id);
    }

    /**
     * Обновляет приватность мемориала (публичный/непубличный)
     *
     * @param id ID мемориала
     * @param isPublic новый статус приватности
     */
    @Transactional
    public void updateMemorialPrivacy(Long id, boolean isPublic) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        // Получаем текущего пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        User currentUser = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Проверяем права доступа - только владелец может изменять приватность
        if (!memorial.getCreatedBy().equals(currentUser)) {
            throw new RuntimeException("Only memorial owner can update privacy settings");
        }
        
        memorial.setPublic(isPublic);
        memorialRepository.save(memorial);
        
        log.info("Обновлена приватность мемориала ID={}, isPublic={}", id, isPublic);
    }

    /**
     * Отправляет мемориал на модерацию
     * @param id ID мемориала
     * @param user текущий пользователь
     * @return обновленный мемориал
     */
    @Transactional
    public MemorialDTO sendForModeration(Long id, User user) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        // Проверяем, что пользователь является владельцем или редактором
        if (!memorial.getCreatedBy().equals(user) && !memorial.isEditor(user)) {
            throw new RuntimeException("Unauthorized access");
        }
        
        // Устанавливаем статус "На модерации"
        memorial.setPublicationStatus(Memorial.PublicationStatus.PENDING_MODERATION);
        
        Memorial savedMemorial = memorialRepository.save(memorial);
        
        // Создаем уведомление для администраторов
        createModerationNotification(savedMemorial, user);
        
        log.info("Мемориал ID={} отправлен на модерацию пользователем ID={}", id, user.getId());
        
        return memorialMapper.toDTO(savedMemorial);
    }
    
    /**
     * Создает уведомление о запросе на модерацию мемориала
     */
    private void createModerationNotification(Memorial memorial, User sender) {
        // Находим всех администраторов
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        
        for (User admin : admins) {
            Notification notification = new Notification();
            notification.setUser(admin);
            notification.setSender(sender);
            notification.setTitle("Запрос на публикацию мемориала");
            notification.setMessage("Пользователь '" + sender.getFio() + "' запрашивает публикацию мемориала '" + memorial.getFio() + "'");
            notification.setType(Notification.NotificationType.MODERATION);
            notification.setStatus(Notification.NotificationStatus.PENDING);
            notification.setRead(false);
            notification.setUrgent(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRelatedEntityId(memorial.getId());
            notification.setRelatedEntityName(memorial.getFio());
            
            notificationRepository.save(notification);
            log.info("Создано уведомление о модерации для администратора {}", admin.getFio());
        }

        // ВАЖНО: Это отдельное информационное уведомление для пользователя
        // Оно имеет тип SYSTEM, чтобы не отображаться в списке модерации у администраторов
        Notification senderNotification = new Notification();
        senderNotification.setUser(sender);
        senderNotification.setSender(sender); // Самому себе
        senderNotification.setTitle("Мемориал отправлен на модерацию");
        senderNotification.setMessage("Ваш мемориал '" + memorial.getFio() + "' отправлен на модерацию и будет опубликован после одобрения администратором.");
        senderNotification.setType(Notification.NotificationType.SYSTEM); // Меняем тип на SYSTEM для отделения от уведомлений модерации
        senderNotification.setStatus(Notification.NotificationStatus.INFO); // Информационное - не требует действий
        senderNotification.setRead(false);
        senderNotification.setUrgent(false);
        senderNotification.setCreatedAt(LocalDateTime.now());
        senderNotification.setRelatedEntityId(memorial.getId());
        senderNotification.setRelatedEntityName(memorial.getFio());
        
        notificationRepository.save(senderNotification);
        log.info("Создано информационное уведомление для отправителя {} о начале модерации", sender.getFio());
    }
    
    /**
     * Обрабатывает решение администратора по публикации мемориала
     * @param id ID мемориала
     * @param approved одобрено или отклонено
     * @param admin администратор, принявший решение
     * @return обновленный мемориал
     */
    @Transactional
    public MemorialDTO moderateMemorial(Long id, boolean approved, User admin) {
        // Проверяем, что пользователь является администратором
        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Unauthorized access");
        }
        
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        if (approved) {
            // Публикуем мемориал
            memorial.setPublicationStatus(Memorial.PublicationStatus.PUBLISHED);
            memorial.setPublic(true);
        } else {
            // Отклоняем публикацию
            memorial.setPublicationStatus(Memorial.PublicationStatus.REJECTED);
            memorial.setPublic(false);
        }
        
        Memorial savedMemorial = memorialRepository.save(memorial);
        
        // Создаем уведомление для владельца мемориала
        createModerationResponseNotification(savedMemorial, admin, approved);
        
        log.info("Модерация мемориала ID={}: {}", id, approved ? "одобрен" : "отклонен");
        
        return memorialMapper.toDTO(savedMemorial);
    }
    
    /**
     * Создает уведомление о решении по модерации мемориала
     */
    private void createModerationResponseNotification(Memorial memorial, User admin, boolean approved) {
        User owner = memorial.getCreatedBy();
        
        Notification notification = new Notification();
        notification.setUser(owner);
        notification.setSender(admin);
        
        if (approved) {
            notification.setTitle("Мемориал опубликован");
            notification.setMessage("Ваш мемориал '" + memorial.getFio() + "' был одобрен и опубликован на сайте.");
            notification.setStatus(Notification.NotificationStatus.ACCEPTED);
        } else {
            notification.setTitle("Мемориал не опубликован");
            notification.setMessage("Ваш мемориал '" + memorial.getFio() + "' не был одобрен администратором и не будет опубликован на сайте.");
            notification.setStatus(Notification.NotificationStatus.REJECTED);
        }
        
        notification.setType(Notification.NotificationType.MODERATION);
        notification.setRead(false);
        notification.setUrgent(true); // Делаем уведомление срочным для привлечения внимания
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRelatedEntityId(memorial.getId());
        notification.setRelatedEntityName(memorial.getFio());
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Создано уведомление ID={} о результате модерации для владельца {} ({})", 
                savedNotification.getId(), owner.getFio(), owner.getId());
    }

    /**
     * Создает уведомление о предложенных изменениях от редактора
     */
    private void createPendingChangesNotification(Memorial memorial, User editor) {
        User owner = memorial.getCreatedBy();
        
        // Создаем уведомление для владельца о предложенных изменениях
        Notification notification = new Notification();
        notification.setUser(owner);
        notification.setSender(editor);
        notification.setTitle("Предложены изменения в мемориале");
        notification.setMessage("Редактор " + editor.getFio() + " предложил изменения в мемориале '" 
                + memorial.getFio() + "'. Требуется ваше подтверждение.");
        notification.setType(Notification.NotificationType.MEMORIAL_EDIT);
        notification.setStatus(Notification.NotificationStatus.PENDING);
        notification.setRead(false);
        notification.setUrgent(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRelatedEntityId(memorial.getId());
        notification.setRelatedEntityName(memorial.getFio());
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Создано уведомление ID={} о предложенных изменениях для владельца {} ({})", 
                savedNotification.getId(), owner.getFio(), owner.getId());
        
        // Создаем информационное уведомление для редактора
        Notification editorNotification = new Notification();
        editorNotification.setUser(editor);
        editorNotification.setSender(editor); // Самому себе
        editorNotification.setTitle("Изменения предложены");
        editorNotification.setMessage("Ваши изменения в мемориале '" + memorial.getFio() 
                + "' предложены владельцу и будут применены после его подтверждения.");
        editorNotification.setType(Notification.NotificationType.MEMORIAL_EDIT);
        editorNotification.setStatus(Notification.NotificationStatus.INFO); // Для исходящих - INFO
        editorNotification.setRead(false);
        editorNotification.setUrgent(false);
        editorNotification.setCreatedAt(LocalDateTime.now());
        editorNotification.setRelatedEntityId(memorial.getId());
        editorNotification.setRelatedEntityName(memorial.getFio());
        
        Notification savedEditorNotification = notificationRepository.save(editorNotification);
        log.info("Создано уведомление ID={} для редактора {} о предложенных изменениях", 
                savedEditorNotification.getId(), editor.getFio());
    }
}