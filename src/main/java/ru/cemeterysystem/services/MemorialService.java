package ru.cemeterysystem.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.cemeterysystem.dto.EditorRequestDTO;
import ru.cemeterysystem.dto.MemorialDTO;
import ru.cemeterysystem.dto.PagedResponse;
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

    // Новые методы с пагинацией
    
    /**
     * Получает список всех мемориалов с пагинацией
     */
    public PagedResponse<MemorialDTO> getAllMemorials(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Memorial> memorialPage = memorialRepository.findAll(pageable);
        
        List<MemorialDTO> memorialDTOs = memorialPage.getContent().stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
            
        return PagedResponse.of(memorialDTOs, page, size, memorialPage.getTotalElements());
    }
    
    /**
     * Получает список мемориалов пользователя с пагинацией
     */
    public PagedResponse<MemorialDTO> getMyMemorials(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        log.info("Получение мемориалов с пагинацией для пользователя: id={}, login={}, страница={}, размер={}", 
                user.getId(), user.getLogin(), page, size);
        
        // Получаем все мемориалы пользователя (без пагинации для фильтрации)
        List<Memorial> ownedMemorials = memorialRepository.findByCreatedBy(user);
        List<Memorial> editedMemorials = memorialRepository.findByEditorsContaining(user);
        List<Memorial> editedMemorials2 = memorialRepository.findMemorialsWhereUserIsEditor(user.getId());
        
        // Объединяем результаты
        List<Memorial> allMemorials = new ArrayList<>(ownedMemorials);
        for (Memorial memorial : editedMemorials) {
            if (!allMemorials.contains(memorial)) {
                allMemorials.add(memorial);
            }
        }
        for (Memorial memorial : editedMemorials2) {
            if (!allMemorials.contains(memorial)) {
                allMemorials.add(memorial);
            }
        }
        
        // Сортируем по дате создания (новые первыми)
        allMemorials.sort((m1, m2) -> {
            if (m1.getCreatedAt() == null && m2.getCreatedAt() == null) return 0;
            if (m1.getCreatedAt() == null) return 1;
            if (m2.getCreatedAt() == null) return -1;
            return m2.getCreatedAt().compareTo(m1.getCreatedAt());
        });
        
        // Применяем пагинацию вручную
        long totalElements = allMemorials.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allMemorials.size());
        
        List<Memorial> pageMemorials = startIndex < allMemorials.size() ? 
            allMemorials.subList(startIndex, endIndex) : new ArrayList<>();
        
        List<MemorialDTO> result = pageMemorials.stream()
            .map(memorial -> {
                MemorialDTO dto = memorialMapper.toDTO(memorial);
                
                boolean isOwner = memorial.getCreatedBy().equals(user);
                boolean isEditor = memorial.isEditor(user);
                
                dto.setEditor(!isOwner && isEditor);
                
                return dto;
            })
            .collect(Collectors.toList());
        
        log.info("Возвращаем {} мемориалов на странице {} из {} для пользователя {}", 
                result.size(), page + 1, (int) Math.ceil((double) totalElements / size), user.getLogin());
        
        return PagedResponse.of(result, page, size, totalElements);
    }
    
    /**
     * Получает список публичных мемориалов с пагинацией
     */
    public PagedResponse<MemorialDTO> getPublicMemorials(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Memorial> memorialPage = memorialRepository.findByIsPublicTrueAndPublicationStatusAndIsBlockedFalse(
                Memorial.PublicationStatus.PUBLISHED, pageable);
        
        // Получаем текущего пользователя (если авторизован)
        User currentUser = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getPrincipal().equals("anonymousUser")) {
                String login = authentication.getName();
                currentUser = userRepository.findByLogin(login).orElse(null);
            }
        } catch (Exception e) {
            log.warn("getPublicMemorials: Не удалось получить текущего пользователя: {}", e.getMessage());
        }
        
        final User finalCurrentUser = currentUser;
        
        List<MemorialDTO> memorialDTOs = memorialPage.getContent().stream()
            .map(memorial -> {
                // Проверяем, является ли пользователь владельцем или редактором
                boolean isOwner = finalCurrentUser != null && memorial.getCreatedBy().equals(finalCurrentUser);
                boolean isEditor = false;
                
                if (finalCurrentUser != null && memorial.getEditors() != null) {
                    for (User editor : memorial.getEditors()) {
                        if (editor.getId().equals(finalCurrentUser.getId())) {
                            isEditor = true;
                            break;
                        }
                    }
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
                            
                            previousDto.setId(memorial.getId());
                            previousDto.setEditorIds(memorial.getEditors().stream()
                                .map(User::getId)
                                .collect(Collectors.toList()));
                            previousDto.setEditor(isEditor);
                            previousDto.setPendingChanges(false);
                            
                            return previousDto;
                        }
                    } catch (Exception e) {
                        log.error("Ошибка при получении предыдущего состояния мемориала {}: {}", memorial.getId(), e.getMessage());
                    }
                }
                
                MemorialDTO dto = memorialMapper.toDTO(memorial);
                dto.setEditor(isEditor);
                return dto;
            })
            .collect(Collectors.toList());
            
        log.info("getPublicMemorials: Возвращаем {} публичных мемориалов на странице {} из {}", 
                memorialDTOs.size(), page + 1, memorialPage.getTotalPages());
        
        return PagedResponse.of(memorialDTOs, page, size, memorialPage.getTotalElements());
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
        
        MemorialDTO dto = memorialMapper.toDTO(memorial);
        
        // Определяем права доступа пользователя
        boolean isOwner = memorial.getCreatedBy().equals(currentUser);
        boolean isEditor = memorial.isEditor(currentUser);
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        
        log.info("getMemorialByIdForUser ID={}: пользователь={}, роль={}, isOwner={}, isEditor={}, isAdmin={}, changesUnderModeration={}", 
                id, currentUser.getLogin(), currentUser.getRole(), isOwner, isEditor, isAdmin, memorial.isChangesUnderModeration());
        
        // Если администратор просматривает мемориал с изменениями на модерации,
        // показываем ему мемориал с примененными изменениями для принятия решения
        if (isAdmin && memorial.isChangesUnderModeration()) {
            log.info("АДМИН ПРОСМАТРИВАЕТ МЕМОРИАЛ С ИЗМЕНЕНИЯМИ: ID={}, pending поля существуют: fio={}, bio={}, photo={}", 
                    id, memorial.getPendingFio() != null, memorial.getPendingBiography() != null, memorial.getPendingPhotoUrl() != null);
            
            // Создаем DTO с примененными изменениями для предварительного просмотра
            MemorialDTO previewDto = memorialMapper.toDTO(memorial);
            
            // Применяем все pending изменения для предварительного просмотра
            if (memorial.getPendingFio() != null) {
                log.info("Применяем pending ФИО: '{}' -> '{}'", previewDto.getFio(), memorial.getPendingFio());
                previewDto.setFio(memorial.getPendingFio());
            }
            if (memorial.getPendingPhotoUrl() != null) {
                log.info("Применяем pending фото: '{}' -> '{}'", previewDto.getPhotoUrl(), memorial.getPendingPhotoUrl());
                previewDto.setPhotoUrl(memorial.getPendingPhotoUrl());
            }
            if (memorial.getPendingBiography() != null) {
                log.info("Применяем pending биографию: '{}' -> '{}'", 
                        previewDto.getBiography() != null ? previewDto.getBiography().substring(0, Math.min(50, previewDto.getBiography().length())) : "null", 
                        memorial.getPendingBiography().substring(0, Math.min(50, memorial.getPendingBiography().length())));
                previewDto.setBiography(memorial.getPendingBiography());
            }
            if (memorial.getPendingBirthDate() != null) {
                log.info("Применяем pending дату рождения: '{}' -> '{}'", previewDto.getBirthDate(), memorial.getPendingBirthDate().toString());
                previewDto.setBirthDate(memorial.getPendingBirthDate().toString());
            }
            if (memorial.getPendingDeathDate() != null) {
                log.info("Применяем pending дату смерти: '{}' -> '{}'", previewDto.getDeathDate(), memorial.getPendingDeathDate().toString());
                previewDto.setDeathDate(memorial.getPendingDeathDate().toString());
            }
            if (memorial.getPendingIsPublic() != null) {
                log.info("Применяем pending публичность: '{}' -> '{}'", previewDto.isPublic(), memorial.getPendingIsPublic());
                previewDto.setPublic(memorial.getPendingIsPublic());
            }
            if (memorial.getPendingMainLocation() != null) {
                log.info("Применяем pending основное местоположение");
                previewDto.setMainLocation(memorial.getPendingMainLocation());
            }
            if (memorial.getPendingBurialLocation() != null) {
                log.info("Применяем pending место захоронения");
                previewDto.setBurialLocation(memorial.getPendingBurialLocation());
            }
            
            // Показываем флаг изменений на модерации
            previewDto.setChangesUnderModeration(true);
            previewDto.setEditor(isEditor);
            
            log.info("ВОЗВРАЩАЕТСЯ ПРЕДВАРИТЕЛЬНЫЙ ПРОСМОТР С ИЗМЕНЕНИЯМИ ДЛЯ АДМИНА: ФИО='{}', биография='{}'", 
                    previewDto.getFio(), 
                    previewDto.getBiography() != null ? previewDto.getBiography().substring(0, Math.min(50, previewDto.getBiography().length())) : "null");
            return previewDto;
        }
        
        // Если пользователь НЕ является владельцем/админом, и изменения на модерации - 
        // скрываем индикатор changesUnderModeration и не показываем pending поля
        if (!isOwner && !isAdmin && memorial.isChangesUnderModeration()) {
            dto.setChangesUnderModeration(false); // Скрываем индикатор от обычных пользователей
            // Очищаем pending поля, чтобы обычные пользователи не видели изменения на модерации
            dto.setPendingPhotoUrl(null);
            dto.setPendingBiography(null);
            dto.setPendingBirthDate(null);
            dto.setPendingDeathDate(null);
            dto.setPendingMainLocation(null);
            dto.setPendingBurialLocation(null);
        }
        
        // Устанавливаем флаг редактора для текущего пользователя
        dto.setEditor(isEditor);
        
        log.info("Мемориал ID={} получен пользователем {} (владелец: {}, редактор: {}, админ: {})", 
                id, currentUser.getLogin(), isOwner, isEditor, isAdmin);
        
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
        
        // Проверяем, находятся ли изменения мемориала на модерации
        if (memorial.isChangesUnderModeration()) {
            log.error("Попытка обновить мемориал ID={} с изменениями на модерации. Пользователь ID={}, логин={}",
                    id, user.getId(), user.getLogin());
            throw new RuntimeException("Cannot edit memorial with changes under moderation");
        }
        
        boolean isOwner = memorial.getCreatedBy().equals(user);
        boolean isEditor = !isOwner && memorial.isEditor(user);
        
        log.info("Обновление мемориала ID={}, пользователь={} (ID={}), isOwner={}, isEditor={}, статус={}",
                id, user.getLogin(), user.getId(), isOwner, isEditor, memorial.getPublicationStatus());
        
        // Определяем, нужно ли сохранять изменения как pending
        boolean shouldUsePendingFields = false;
        
        if (isEditor) {
            // Редакторы всегда сохраняют в pending поля
            shouldUsePendingFields = true;
        } else if (isOwner) {
            // Владельцы используют pending поля только для опубликованных мемориалов
            shouldUsePendingFields = memorial.getPublicationStatus() == Memorial.PublicationStatus.PUBLISHED;
        }
        
        if (shouldUsePendingFields) {
            log.info("Сохранение изменений в pending поля для мемориала ID={}", id);
            // Сохраняем как предложенные изменения (для редакторов или для опубликованных мемориалов)
            updateMemorialPendingFields(memorial, memorialDTO, user);
        } else {
            log.info("Прямое применение изменений для мемориала ID={}", id);
            // Прямое применение изменений (для неопубликованных мемориалов владельца)
            updateMemorialFields(memorial, memorialDTO);
        }
        
        Memorial updatedMemorial = memorialRepository.save(memorial);
        return memorialMapper.toDTO(updatedMemorial);
    }
    
    /**
     * Обновляет поля мемориала из DTO для владельца (прямое обновление)
     */
    private void updateMemorialFields(Memorial memorial, MemorialDTO dto) {
        // Убираем возможность редактирования fio напрямую - только через отдельные поля
        // if (dto.getFio() != null) {
        //     memorial.setFio(dto.getFio());
        // }
        
        // Обновление отдельных полей ФИО
        if (dto.getFirstName() != null) {
            memorial.setFirstName(dto.getFirstName());
        }
        
        if (dto.getLastName() != null) {
            memorial.setLastName(dto.getLastName());
        }
        
        if (dto.getMiddleName() != null) {
            memorial.setMiddleName(dto.getMiddleName());
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
        memorial.setPendingFio(null);
        memorial.setPendingFirstName(null);
        memorial.setPendingLastName(null);
        memorial.setPendingMiddleName(null);
        memorial.setPendingBiography(null);
        memorial.setPendingBirthDate(null);
        memorial.setPendingDeathDate(null);
        memorial.setPendingIsPublic(null);
        memorial.setPendingMainLocation(null);
        memorial.setPendingBurialLocation(null);
        memorial.setLastEditorId(null);
    }
    
    /**
     * Обновляет поля мемориала из DTO для редактора или владельца (в ожидающие изменения)
     */
    private void updateMemorialPendingFields(Memorial memorial, MemorialDTO dto, User editor) {
        boolean hasChanges = false;
        boolean isOwner = memorial.getCreatedBy().equals(editor);
        
        // Убираем возможность изменения ФИО напрямую - только через отдельные поля
        // ФИО формируется автоматически из отдельных полей
        
        // Обновление отдельных полей ФИО в pending режиме
        if (dto.getFirstName() != null && !dto.getFirstName().equals(memorial.getFirstName())) {
            memorial.setPendingFirstName(dto.getFirstName());
            hasChanges = true;
        }
        
        if (dto.getLastName() != null && !dto.getLastName().equals(memorial.getLastName())) {
            memorial.setPendingLastName(dto.getLastName());
            hasChanges = true;
        }
        
        if (dto.getMiddleName() != null && !dto.getMiddleName().equals(memorial.getMiddleName())) {
            memorial.setPendingMiddleName(dto.getMiddleName());
            hasChanges = true;
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
        if (isOwner && dto.isPublic() != memorial.isPublic()) {
            memorial.setPendingIsPublic(dto.isPublic());
            hasChanges = true;
        }
        
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
            
            // Если владелец редактирует опубликованный мемориал, то он должен сам отправить на модерацию
            // Если редактор - создаем уведомление для владельца
            if (!isOwner) {
            createPendingChangesNotification(memorial, editor);
            } else {
                log.info("Владелец отредактировал опубликованный мемориал ID={}. Требуется отправка на модерацию.", memorial.getId());
            }
        }
    }

    public List<MemorialDTO> searchMemorials(String query, String location, String startDate,
                                        String endDate, Boolean isPublic) {
        // Вызываем новый метод с пагинацией для обратной совместимости
        PagedResponse<MemorialDTO> pagedResponse = searchMemorials(query, location, startDate, endDate, isPublic, 0, 1000);
        return pagedResponse.getContent();
    }
    
    /**
     * Ищет мемориалы по различным параметрам с пагинацией
     */
    public PagedResponse<MemorialDTO> searchMemorials(String query, String location, String startDate,
                                        String endDate, Boolean isPublic, int page, int size) {
        List<Memorial> allResults = memorialRepository.search(query, location, startDate, endDate, isPublic);
        
        // Применяем пагинацию вручную
        long totalElements = allResults.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allResults.size());
        
        List<Memorial> pageResults = startIndex < allResults.size() ? 
            allResults.subList(startIndex, endIndex) : new ArrayList<>();
        
        List<MemorialDTO> memorialDTOs = pageResults.stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
            
        return PagedResponse.of(memorialDTOs, page, size, totalElements);
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
        // Убираем возможность установки fio напрямую - только через отдельные поля
        // memorial.setFio(dto.getFio());
        
        // Обновление отдельных полей ФИО
        memorial.setFirstName(dto.getFirstName());
        memorial.setLastName(dto.getLastName());
        memorial.setMiddleName(dto.getMiddleName());
        
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
     * Исключает мемориалы с изменениями на модерации у админа
     * 
     * @param currentUser текущий пользователь
     * @return список мемориалов с изменениями
     */
    public List<MemorialDTO> getEditedMemorials(User currentUser) {
        List<Memorial> memorialsWithPendingChanges = new ArrayList<>();
        
        // Получаем мемориалы, владельцем которых является текущий пользователь
        List<Memorial> ownedMemorials = memorialRepository.findByCreatedBy(currentUser);
        
        // Фильтруем только те, которые имеют ожидающие изменения И НЕ находятся на модерации у админа
        List<Memorial> editedOwnedMemorials = ownedMemorials.stream()
            .filter(memorial -> memorial.isPendingChanges() && !memorial.isChangesUnderModeration())
            .collect(Collectors.toList());
        
        memorialsWithPendingChanges.addAll(editedOwnedMemorials);
        
        // Получаем мемориалы, где пользователь является редактором
        List<Memorial> editedMemorials = memorialRepository.findByEditorsContaining(currentUser);
        
        // Фильтруем только те, которые имеют ожидающие изменения И НЕ находятся на модерации у админа
        List<Memorial> editedAsEditorMemorials = editedMemorials.stream()
            .filter(memorial -> memorial.isPendingChanges() && !memorial.isChangesUnderModeration())
            .collect(Collectors.toList());
        
        memorialsWithPendingChanges.addAll(editedAsEditorMemorials);
        
        log.info("Получено {} мемориалов с ожидающими изменениями для пользователя {} (исключая модерацию админом)", 
                memorialsWithPendingChanges.size(), currentUser.getLogin());
        
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
        
        boolean isPublished = memorial.getPublicationStatus() == Memorial.PublicationStatus.PUBLISHED;
        
        if (approve) {
            log.info("Владелец принимает изменения для мемориала ID={}, isPublished={}", memorialId, isPublished);
            
            if (isPublished) {
                // Для опубликованных мемориалов отправляем изменения на модерацию админу
                log.info("Опубликованный мемориал ID={}: отправляем изменения на модерацию админу", memorialId);
                
                // Устанавливаем флаг "Изменения на модерации у админа"
                memorial.setChangesUnderModeration(true);
                memorial = memorialRepository.save(memorial);
                
                // Создаем уведомление для администраторов о необходимости модерации изменений
                createChangesModerationNotification(memorial, currentUser);
                
                // Создаем уведомление для редактора о том, что изменения приняты владельцем и отправлены на модерацию
                Long savedLastEditorId = memorial.getLastEditorId();
                if (savedLastEditorId != null) {
                    User editor = userRepository.findById(savedLastEditorId).orElse(null);
                    if (editor != null) {
                        Notification notification = new Notification();
                        notification.setUser(editor);
                        notification.setSender(currentUser);
                        notification.setTitle("Изменения приняты и отправлены на модерацию");
                        notification.setMessage(String.format(
                            "Ваши изменения для мемориала \"%s\" были приняты владельцем и отправлены на модерацию администратору.",
                            memorial.getFio()
                        ));
                        notification.setType(Notification.NotificationType.SYSTEM);
                        notification.setStatus(Notification.NotificationStatus.INFO);
                        notification.setRelatedEntityId(memorial.getId());
                        notification.setRelatedEntityName(memorial.getFio());
                        notification.setCreatedAt(LocalDateTime.now());
                        notification.setRead(false);
                        
                        notificationRepository.save(notification);
                        log.info("Создано уведомление для редактора ID={} о принятии изменений и отправке на модерацию", editor.getId());
                    }
                }
                
                log.info("Изменения мемориала ID={} приняты владельцем и отправлены на модерацию админу", memorialId);
            } else {
                // Для неопубликованных мемориалов применяем изменения сразу (старая логика)
                log.info("Неопубликованный мемориал ID={}: применяем изменения сразу", memorialId);
            
            try {
                // Применяем ожидающие изменения
                
                // 1. Обрабатываем отдельные поля ФИО
                if (memorial.getPendingFirstName() != null) {
                    log.info("Применяем ожидающее имя для мемориала ID={}", memorialId);
                    memorial.setFirstName(memorial.getPendingFirstName());
                    memorial.setPendingFirstName(null);
                }
                
                if (memorial.getPendingLastName() != null) {
                    log.info("Применяем ожидающую фамилию для мемориала ID={}", memorialId);
                    memorial.setLastName(memorial.getPendingLastName());
                    memorial.setPendingLastName(null);
                }
                
                if (memorial.getPendingMiddleName() != null) {
                    log.info("Применяем ожидающее отчество для мемориала ID={}", memorialId);
                    memorial.setMiddleName(memorial.getPendingMiddleName());
                    memorial.setPendingMiddleName(null);
                }
                
                // 2. Обрабатываем ФИО (для совместимости)
                if (memorial.getPendingFio() != null) {
                    log.info("Применяем ожидающее ФИО для мемориала ID={}", memorialId);
                    memorial.setFio(memorial.getPendingFio());
                    memorial.setPendingFio(null);
                }
                
                // 3. Обрабатываем фото
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
                
                    // 4. Обрабатываем биографию
                if (memorial.getPendingBiography() != null) {
                    log.info("Применяем ожидающую биографию для мемориала ID={}", memorialId);
                    memorial.setBiography(memorial.getPendingBiography());
                    memorial.setPendingBiography(null);
                }
                
                    // 5. Обрабатываем даты
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
                
                    // 6. Обрабатываем публичность
                    if (memorial.getPendingIsPublic() != null) {
                        log.info("Применяем ожидающую публичность для мемориала ID={}", memorialId);
                        memorial.setPublic(memorial.getPendingIsPublic());
                        memorial.setPendingIsPublic(null);
                    }
                    
                    // 7. Обрабатываем местоположения
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
                
                // Сбрасываем флаг ожидающих изменений и все временные данные
                memorial.setPendingChanges(false);
                memorial.setPreviousState(null);
                memorial.setProposedChanges(null);
                
                // Сохраняем изменения
                memorial = memorialRepository.saveAndFlush(memorial);
                    log.info("Успешно применены все ожидающие изменения для неопубликованного мемориала ID={}", memorialId);
                
                // Создаем уведомление для редактора о том, что изменения приняты
                    Long savedLastEditorId = memorial.getLastEditorId();
                    if (savedLastEditorId != null) {
                        User editor = userRepository.findById(savedLastEditorId).orElse(null);
                    if (editor != null) {
                        Notification notification = new Notification();
                        notification.setUser(editor);
                        notification.setSender(currentUser);
                        notification.setTitle("Изменения в мемориале приняты");
                            notification.setMessage("Ваши изменения для мемориала \"" + memorial.getFio() + "\" были приняты владельцем.");
                            notification.setType(Notification.NotificationType.SYSTEM);
                            notification.setStatus(Notification.NotificationStatus.INFO);
                        notification.setRelatedEntityId(memorial.getId());
                        notification.setRelatedEntityName(memorial.getFio());
                        notification.setCreatedAt(LocalDateTime.now());
                        notification.setRead(false);
                        
                        notificationRepository.save(notification);
                        log.info("Создано уведомление для редактора ID={} о принятии изменений", editor.getId());
                    }
                }
                    
                    // Очищаем lastEditorId
                    memorial.setLastEditorId(null);
            } catch (Exception e) {
                log.error("Ошибка при применении ожидающих изменений: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка при применении изменений: " + e.getMessage());
                }
            }
        } else {
            log.info("Отклонены изменения для мемориала ID={}", memorialId);
            
            // При отклонении изменений очищаем все временные данные
            // Удаляем pending фото из хранилища, если есть
            if (memorial.getPendingPhotoUrl() != null) {
                log.info("Удаляем pending фото из хранилища при отклонении владельцем: {}", memorial.getPendingPhotoUrl());
                fileStorageService.deleteFile(memorial.getPendingPhotoUrl());
            }
            
            memorial.setPendingPhotoUrl(null);
            memorial.setPendingFio(null);
            memorial.setPendingBiography(null);
            memorial.setPendingBirthDate(null);
            memorial.setPendingDeathDate(null);
            memorial.setPendingIsPublic(null);
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
                    notification.setMessage("Ваши изменения для мемориала \"" + memorial.getFio() + "\" были отклонены владельцем.");
                    notification.setType(Notification.NotificationType.SYSTEM);
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
        
        // Еще раз принудительно сбрасываем флаг ожидания изменений (только для неопубликованных или отклоненных)
        if (!isPublished || !approve) {
        memorial.setPendingChanges(false);
        }
        
        // Выполняем завершающее сохранение и проверяем флаги
        memorial = memorialRepository.saveAndFlush(memorial);
        log.info("Завершено подтверждение/отклонение изменений мемориала ID={}, финальное состояние pendingChanges={}, changesUnderModeration={}", 
                memorial.getId(), memorial.isPendingChanges(), memorial.isChangesUnderModeration());
        
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
        boolean isPublished = memorial.getPublicationStatus() == Memorial.PublicationStatus.PUBLISHED;
        
        log.info("Загрузка фото для мемориала ID={}: пользователь={}, isEditor={}, isOwner={}, isPublished={}", 
                id, currentUser.getLogin(), isEditor, isOwner, isPublished);
        
        // Проверяем права доступа
        if (!isOwner && !isEditor) {
            throw new RuntimeException("Нет прав для загрузки фото");
        }
        
        // Сохраняем фото в хранилище (в любом случае)
        String photoUrl = fileStorageService.storeFile(file);
        
        // Определяем, нужно ли использовать pending поля
        boolean shouldUsePendingFields = false;
        
        if (isEditor && !isOwner) {
            // Редакторы всегда используют pending поля
            shouldUsePendingFields = true;
        } else if (isOwner && isPublished) {
            // Владельцы опубликованных мемориалов тоже используют pending поля
            shouldUsePendingFields = true;
        }
        
        if (shouldUsePendingFields) {
            log.info("Сохранение фото в pending поле для мемориала ID={}", id);
            // Сохраняем как pending изменение
                memorial.setPendingPhotoUrl(photoUrl);
                memorial.setPendingChanges(true);
                memorial.setLastEditorId(currentUser.getId());
                
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
                
            // Если редактор - создаем уведомление для владельца
            if (isEditor && !isOwner) {
                createPendingChangesNotification(memorial, currentUser);
                } else {
                log.info("Владелец изменил фото опубликованного мемориала ID={}. Требуется отправка на модерацию.", memorial.getId());
            }
            
                memorialRepository.save(memorial);
            log.info("Фото сохранено как pending изменение для мемориала ID={}", id);
                return photoUrl;
        } else {
            log.info("Прямое применение фото для неопубликованного мемориала ID={}", id);
            // Прямое применение (для неопубликованных мемориалов владельца)
            if (memorial.getPhotoUrl() != null) {
                fileStorageService.deleteFile(memorial.getPhotoUrl());
            }
            
            memorial.setPhotoUrl(photoUrl);
            memorial.setPendingPhotoUrl(null);
            
            memorialRepository.save(memorial);
            log.info("Фото напрямую применено для мемориала ID={}", id);
            return photoUrl;
        }
    }

    @Transactional
    public String uploadDocument(Long id, MultipartFile file) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        // Получаем текущего пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        User currentUser = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Проверяем права доступа - только владелец может загружать документы
        if (!memorial.getCreatedBy().equals(currentUser)) {
            throw new RuntimeException("Only memorial owner can upload documents");
        }
        
        // Проверяем, что у пользователя есть подписка
        if (currentUser.getHasSubscription() != Boolean.TRUE) {
            throw new RuntimeException("Document upload requires subscription");
        }
        
        // Валидация файла
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        // Проверка типа файла
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && 
                                    !contentType.startsWith("image/"))) {
            throw new RuntimeException("Only PDF and image files are allowed");
        }
        
        // Проверка размера файла (максимум 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("File size must not exceed 10MB");
        }
        
        log.info("Загрузка документа для мемориала ID={}: пользователь={}, размер={}, тип={}", 
                id, currentUser.getLogin(), file.getSize(), contentType);
        
        // Сохраняем документ в хранилище
        String documentUrl = fileStorageService.storeFile(file);
        
        // Удаляем старый документ, если есть
        if (memorial.getDocumentUrl() != null) {
            try {
                fileStorageService.deleteFile(memorial.getDocumentUrl());
            } catch (Exception e) {
                log.warn("Не удалось удалить старый документ: {}", e.getMessage());
            }
        }
        
        // Сохраняем URL документа
        memorial.setDocumentUrl(documentUrl);
        memorialRepository.save(memorial);
        
        log.info("Документ загружен для мемориала ID={}, URL: {}", id, documentUrl);
        return documentUrl;
    }

    /**
     * Получает документ мемориала для просмотра или скачивания
     */
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getMemorialDocument(Long id) {
        log.info("MemorialService.getMemorialDocument: начало получения документа для мемориала {}", id);
        
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> {
                log.error("MemorialService.getMemorialDocument: мемориал {} не найден", id);
                return new RuntimeException("Memorial not found");
            });
        
        log.info("MemorialService.getMemorialDocument: мемориал найден, documentUrl={}", memorial.getDocumentUrl());
        
        if (memorial.getDocumentUrl() == null || memorial.getDocumentUrl().trim().isEmpty()) {
            log.error("MemorialService.getMemorialDocument: у мемориала {} отсутствует документ", id);
            throw new RuntimeException("Document not found");
        }
        
        log.info("MemorialService.getMemorialDocument: передаем запрос в FileStorageService для URL: {}", memorial.getDocumentUrl());
        
        try {
            org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> result = fileStorageService.getFile(memorial.getDocumentUrl());
            log.info("MemorialService.getMemorialDocument: документ успешно получен из FileStorageService, статус: {}", result.getStatusCode());
            return result;
        } catch (Exception e) {
            log.error("MemorialService.getMemorialDocument: ошибка при получении файла: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Проверяет наличие документа у мемориала
     */
    public boolean hasDocument(Long id) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        return memorial.getDocumentUrl() != null && !memorial.getDocumentUrl().isEmpty();
    }

    /**
     * Проверяет права доступа пользователя к просмотру мемориала
     */
    public boolean hasViewAccess(Long memorialId, Long userId) {
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Владелец всегда имеет доступ
        if (memorial.getCreatedBy().equals(user)) {
            return true;
        }
        
        // Редакторы имеют доступ
        if (memorial.isEditor(user)) {
            return true;
        }
        
        // Для опубликованных мемориалов - доступ есть у всех
        if (memorial.isPublic() && memorial.getPublicationStatus() == Memorial.PublicationStatus.PUBLISHED) {
            return true;
        }
        
        return false;
    }

    /**
     * Проверяет права доступа пользователя к редактированию мемориала
     */
    public boolean hasEditAccess(Long memorialId, Long userId) {
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Владелец всегда имеет доступ
        if (memorial.getCreatedBy().equals(user)) {
            return true;
        }
        
        // Редакторы имеют доступ
        return memorial.isEditor(user);
    }

    @Transactional
    public void deleteMemorial(Long id) {
        // ... существующий код удаления мемориала ...
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
        
        // Удаляем мемориал
        memorialRepository.delete(memorial);
        log.info("Удален мемориал ID={}", id);
    }

    @Transactional
    public void updateMemorialPrivacy(Long id, boolean isPublic) {
        // ... существующий код обновления приватности ...
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        memorial.setPublic(isPublic);
        memorialRepository.save(memorial);
        
        log.info("Обновлена приватность мемориала ID={}, isPublic={}", id, isPublic);
    }

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
    
    @Transactional
    public MemorialDTO sendChangesForModeration(Long id, User user) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        // Проверяем, что пользователь является владельцем
        if (!memorial.getCreatedBy().equals(user)) {
            throw new RuntimeException("Only memorial owner can send changes for moderation");
        }
        
        // Проверяем, что мемориал опубликован
        if (memorial.getPublicationStatus() != Memorial.PublicationStatus.PUBLISHED) {
            throw new RuntimeException("Only published memorials can have changes sent for moderation");
        }
        
        // Проверяем, что изменения не находятся уже на модерации
        if (memorial.isChangesUnderModeration()) {
            throw new RuntimeException("Memorial changes are already under moderation");
        }
        
        // Устанавливаем флаг "Изменения на модерации"
        memorial.setChangesUnderModeration(true);
        
        Memorial savedMemorial = memorialRepository.save(memorial);
        
        // Создаем уведомление для администраторов о необходимости модерации изменений
        createChangesModerationNotification(savedMemorial, user);
        
        log.info("Изменения мемориала ID={} отправлены на модерацию пользователем ID={}", id, user.getId());
        
        return memorialMapper.toDTO(savedMemorial);
    }

    private void createModerationNotification(Memorial memorial, User sender) {
        try {
        // Находим всех администраторов
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        
        for (User admin : admins) {
            Notification notification = new Notification();
                notification.setType(Notification.NotificationType.MODERATION);
            notification.setUser(admin);
            notification.setSender(sender);
            notification.setTitle("Запрос на публикацию мемориала");
                
                String message = String.format(
                    "Пользователь %s отправил мемориал \"%s\" на модерацию.\n\n" +
                    "Требуется ваше решение об одобрении или отклонении публикации.",
                    sender.getFio() != null ? sender.getFio() : sender.getLogin(),
                    memorial.getFio()
                );
                
                notification.setMessage(message);
            notification.setStatus(Notification.NotificationStatus.PENDING);
            notification.setRelatedEntityId(memorial.getId());
            notification.setRelatedEntityName(memorial.getFio());
                notification.setRead(false);
                notification.setCreatedAt(LocalDateTime.now());
            
            notificationRepository.save(notification);
                log.info("Уведомление о модерации создано для администратора {}", admin.getLogin());
            }
        } catch (Exception e) {
            log.error("Ошибка при создании уведомления о модерации: {}", e.getMessage(), e);
        }
    }

    private void createChangesModerationNotification(Memorial memorial, User owner) {
        try {
            // Находим всех администраторов
            List<User> admins = userRepository.findByRole(User.Role.ADMIN);
            
            for (User admin : admins) {
                Notification notification = new Notification();
                notification.setType(Notification.NotificationType.MODERATION);
                notification.setUser(admin);
                notification.setSender(owner);
                notification.setTitle("Изменения мемориала на модерации");
                
                String message = String.format(
                    "Пользователь %s отправил изменения мемориала \"%s\" на модерацию.\n\n" +
                    "Требуется ваше решение об одобрении или отклонении изменений.",
                    owner.getFio() != null ? owner.getFio() : owner.getLogin(),
                    memorial.getFio()
                );
                
                notification.setMessage(message);
                notification.setStatus(Notification.NotificationStatus.PENDING);
                notification.setRelatedEntityId(memorial.getId());
                notification.setRelatedEntityName(memorial.getFio());
                notification.setRead(false);
                notification.setCreatedAt(LocalDateTime.now());
                
                notificationRepository.save(notification);
                log.info("Уведомление о модерации изменений создано для администратора {}", admin.getLogin());
            }
        } catch (Exception e) {
            log.error("Ошибка при создании уведомления о модерации изменений: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public MemorialDTO moderateMemorial(Long id, boolean approved, User admin, String reason) {
        log.info("=== МОДЕРАЦИЯ МЕМОРИАЛА ===");
        log.info("moderateMemorial: ID={}, approved={}, admin={}, reason='{}'", id, approved, admin.getLogin(), reason);
        
        // Проверяем, что пользователь является администратором
        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Unauthorized access");
        }
        
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        log.info("Найден мемориал ID={}, текущий статус: {}, isPublic: {}", 
                id, memorial.getPublicationStatus(), memorial.isPublic());
        
        if (approved) {
            // Публикуем мемориал
            log.info("ОДОБРЯЕМ мемориал ID={}: устанавливаем PUBLISHED и isPublic=true", id);
            memorial.setPublicationStatus(Memorial.PublicationStatus.PUBLISHED);
            memorial.setPublic(true);
        } else {
            // Отклоняем публикацию
            log.info("ОТКЛОНЯЕМ мемориал ID={}: устанавливаем REJECTED и isPublic=false, причина: '{}'", id, reason);
            memorial.setPublicationStatus(Memorial.PublicationStatus.REJECTED);
            memorial.setPublic(false);
        }
        
        log.info("Сохраняем мемориал ID={} с новым статусом: {}, isPublic: {}", 
                id, memorial.getPublicationStatus(), memorial.isPublic());
        
        Memorial savedMemorial = memorialRepository.save(memorial);
        
        log.info("Мемориал ID={} сохранен в БД! Финальный статус: {}, isPublic: {}", 
                id, savedMemorial.getPublicationStatus(), savedMemorial.isPublic());
        
        // Создаем уведомление для владельца мемориала
        createModerationResponseNotification(savedMemorial, admin, approved, reason);
        
        log.info("Модерация мемориала ID={}: {}", id, approved ? "одобрен" : "отклонен");
        
        MemorialDTO result = memorialMapper.toDTO(savedMemorial);
        log.info("Возвращаем DTO: ID={}, publicationStatus={}, isPublic={}", 
                result.getId(), result.getPublicationStatus(), result.isPublic());
        
        return result;
    }

    private void createModerationResponseNotification(Memorial memorial, User admin, boolean approved, String reason) {
        try {
        log.info("=== СОЗДАНИЕ УВЕДОМЛЕНИЯ О РЕЗУЛЬТАТЕ МОДЕРАЦИИ ===");
        log.info("createModerationResponseNotification: ID={}, approved={}, reason='{}'", memorial.getId(), approved, reason);
        
        Notification notification = new Notification();
            notification.setUser(memorial.getCreatedBy());
        notification.setSender(admin);
        
        if (approved) {
                notification.setType(Notification.NotificationType.SYSTEM);
                notification.setStatus(Notification.NotificationStatus.INFO); // Информационное уведомление
                notification.setTitle("Мемориал одобрен");
                notification.setMessage(String.format(
                    "Ваш мемориал \"%s\" был одобрен администратором и опубликован.\n\n" +
                    "Теперь он доступен для просмотра всем пользователям.",
                    memorial.getFio()
                ));
        } else {
                notification.setType(Notification.NotificationType.SYSTEM);
                notification.setStatus(Notification.NotificationStatus.INFO); // Информационное уведомление
                notification.setTitle("Мемориал отклонен");
                
                String message = String.format(
                    "Ваш мемориал \"%s\" был отклонен администратором.\n\n",
                    memorial.getFio()
                );
                
                // Добавляем причину отклонения, если она указана
                if (reason != null && !reason.trim().isEmpty()) {
                    log.info("Добавляем причину отклонения в сообщение: '{}'", reason.trim());
                    message += String.format("Причина отклонения: %s\n\n", reason.trim());
                } else {
                    log.warn("Причина отклонения пуста или не указана: reason='{}'", reason);
                }
                
                message += "Вы можете внести изменения и повторно отправить его на модерацию.";
                
                notification.setMessage(message);
                log.info("Финальное сообщение уведомления: '{}'", message);
            }
            
            notification.setRelatedEntityId(memorial.getId());
            notification.setRelatedEntityName(memorial.getFio());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
            
            notificationRepository.save(notification);
            log.info("Уведомление о результате модерации ({}) создано для пользователя {}", 
                    approved ? "одобрение" : "отклонение", memorial.getCreatedBy().getLogin());
        } catch (Exception e) {
            log.error("Ошибка при создании уведомления о результате модерации: {}", e.getMessage(), e);
        }
    }

    private void createPendingChangesNotification(Memorial memorial, User editor) {
        try {
            Notification notification = new Notification();
            notification.setType(Notification.NotificationType.MEMORIAL_EDIT);
            notification.setUser(memorial.getCreatedBy());
            notification.setSender(editor);
            notification.setTitle("Предложены изменения к мемориалу");
            
            String message = String.format(
                "Пользователь %s предложил изменения к вашему мемориалу \"%s\".\n\n" +
                "Просмотрите предложенные изменения и одобрите или отклоните их.",
                editor.getFio() != null ? editor.getFio() : editor.getLogin(),
                memorial.getFio()
            );
            
            notification.setMessage(message);
        notification.setRelatedEntityId(memorial.getId());
        notification.setRelatedEntityName(memorial.getFio());
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            
            notificationRepository.save(notification);
            log.info("Уведомление о предложенных изменениях создано для владельца {}", memorial.getCreatedBy().getLogin());
        } catch (Exception e) {
            log.error("Ошибка при создании уведомления о предложенных изменениях: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public MemorialDTO rejectChanges(Long id, String reason, User admin) {
        log.info("=== ОТКЛОНЕНИЕ ИЗМЕНЕНИЙ МЕМОРИАЛА ===");
        log.info("rejectChanges: ID={}, admin={}, reason='{}'", id, admin.getLogin(), reason);
        
        Memorial memorial = memorialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        // Проверяем права доступа - только администраторы
        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Только администраторы могут отклонять изменения");
        }
        
        // Проверяем, что изменения действительно на модерации
        if (!memorial.isChangesUnderModeration()) {
            throw new RuntimeException("У мемориала нет изменений на модерации");
        }
        
        // Очищаем все pending поля и снимаем флаг модерации
        memorial.setChangesUnderModeration(false);
        
        // Сохраняем ID редактора перед очисткой для создания уведомлений
        Long savedLastEditorId = memorial.getLastEditorId();
        
        // Удаляем pending фото из хранилища, если есть
        if (memorial.getPendingPhotoUrl() != null) {
            log.info("Удаляем pending фото из хранилища: {}", memorial.getPendingPhotoUrl());
            fileStorageService.deleteFile(memorial.getPendingPhotoUrl());
        }
        
        memorial.setPendingPhotoUrl(null);
        memorial.setPendingFio(null);
        memorial.setPendingBiography(null);
        memorial.setPendingBirthDate(null);
        memorial.setPendingDeathDate(null);
        memorial.setPendingIsPublic(null);
        memorial.setPendingMainLocation(null);
        memorial.setPendingBurialLocation(null);
        memorial.setPendingChanges(false);
        memorial.setPreviousState(null);
        memorial.setProposedChanges(null);
        memorial.setLastEditorId(null);
        
        // ВАЖНО: НЕ изменяем publicationStatus - мемориал остается опубликованным
        
        memorial = memorialRepository.save(memorial);
        
        // Отправляем уведомления об отклонении изменений
        createChangesRejectionNotification(memorial, admin, reason, savedLastEditorId);
        
        log.info("Изменения мемориала ID={} отклонены администратором {}", id, admin.getLogin());
        return memorialMapper.toDTO(memorial);
    }

    private void createChangesRejectionNotification(Memorial memorial, User admin, String reason, Long savedLastEditorId) {
        try {
        log.info("=== СОЗДАНИЕ УВЕДОМЛЕНИЙ ОБ ОТКЛОНЕНИИ ИЗМЕНЕНИЙ ===");
        log.info("createChangesRejectionNotification: ID={}, reason='{}', editorId={}", memorial.getId(), reason, savedLastEditorId);
        
        // 1. Создаем уведомление для владельца мемориала
        Notification ownerNotification = new Notification();
        ownerNotification.setType(Notification.NotificationType.SYSTEM);
        ownerNotification.setStatus(Notification.NotificationStatus.INFO);
        ownerNotification.setUser(memorial.getCreatedBy());
        ownerNotification.setSender(admin);
        ownerNotification.setTitle("Изменения мемориала отклонены");
        
        String ownerMessage = String.format(
            "Изменения вашего мемориала \"%s\" были отклонены администратором.\n\n",
            memorial.getFio()
        );
        
        // Добавляем причину отклонения, если она указана
        if (reason != null && !reason.trim().isEmpty()) {
            log.info("Добавляем причину отклонения изменений в сообщение: '{}'", reason.trim());
            ownerMessage += String.format("Причина отклонения: %s\n\n", reason.trim());
        } else {
            log.warn("Причина отклонения изменений пуста или не указана: reason='{}'", reason);
        }
        
        ownerMessage += "Вы можете внести изменения и повторно отправить их на модерацию.";
        
        ownerNotification.setMessage(ownerMessage);
        ownerNotification.setRelatedEntityId(memorial.getId());
        ownerNotification.setRelatedEntityName(memorial.getFio());
        ownerNotification.setRead(false);
        ownerNotification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(ownerNotification);
        log.info("Уведомление об отклонении изменений создано для владельца {} с причиной: {}", 
                memorial.getCreatedBy().getLogin(), reason != null ? reason : "не указана");
        
        // 2. Создаем уведомление для редактора, если он есть
        if (savedLastEditorId != null) {
            User editor = userRepository.findById(savedLastEditorId).orElse(null);
            if (editor != null) {
                Notification editorNotification = new Notification();
                editorNotification.setType(Notification.NotificationType.SYSTEM);
                editorNotification.setStatus(Notification.NotificationStatus.INFO);
                editorNotification.setUser(editor);
                editorNotification.setSender(admin);
                editorNotification.setTitle("Ваши изменения мемориала отклонены");
                
                String editorMessage = String.format(
                    "Ваши изменения для мемориала \"%s\" были отклонены администратором.\n\n",
                    memorial.getFio()
                );
                
                // Добавляем причину отклонения, если она указана
                if (reason != null && !reason.trim().isEmpty()) {
                    editorMessage += String.format("Причина отклонения: %s\n\n", reason.trim());
                }
                
                editorMessage += "Вы можете внести новые изменения и повторно отправить их владельцу на рассмотрение.";
                
                editorNotification.setMessage(editorMessage);
                editorNotification.setRelatedEntityId(memorial.getId());
                editorNotification.setRelatedEntityName(memorial.getFio());
                editorNotification.setRead(false);
                editorNotification.setCreatedAt(LocalDateTime.now());
                
                notificationRepository.save(editorNotification);
                log.info("Уведомление об отклонении изменений создано для редактора {} с причиной: {}", 
                        editor.getLogin(), reason != null ? reason : "не указана");
            } else {
                log.warn("Редактор с ID={} не найден для создания уведомления об отклонении", savedLastEditorId);
            }
        } else {
            log.info("Редактор не указан, уведомление создано только для владельца");
        }
        
        } catch (Exception e) {
            log.error("Ошибка при создании уведомлений об отклонении изменений: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public MemorialDTO approveChangesByAdmin(Long memorialId, User admin) {
        log.info("Администратор {} одобряет изменения мемориала ID={}", admin.getLogin(), memorialId);
        
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        // Проверяем права доступа - только администраторы
        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Только администраторы могут одобрять изменения");
        }
        
        // Проверяем, что изменения действительно на модерации
        if (!memorial.isChangesUnderModeration()) {
            throw new RuntimeException("У мемориала нет изменений на модерации");
        }
        
        try {
            log.info("Применяем изменения для мемориала ID={} администратором", memorialId);
            
            // Применяем ожидающие изменения
            
            // 1. Обрабатываем отдельные поля ФИО
            if (memorial.getPendingFirstName() != null) {
                log.info("Применяем ожидающее имя для мемориала ID={}", memorialId);
                memorial.setFirstName(memorial.getPendingFirstName());
                memorial.setPendingFirstName(null);
            }
            
            if (memorial.getPendingLastName() != null) {
                log.info("Применяем ожидающую фамилию для мемориала ID={}", memorialId);
                memorial.setLastName(memorial.getPendingLastName());
                memorial.setPendingLastName(null);
            }
            
            if (memorial.getPendingMiddleName() != null) {
                log.info("Применяем ожидающее отчество для мемориала ID={}", memorialId);
                memorial.setMiddleName(memorial.getPendingMiddleName());
                memorial.setPendingMiddleName(null);
            }
            
            // 2. Обрабатываем ФИО (для совместимости)
            if (memorial.getPendingFio() != null) {
                log.info("Применяем ожидающее ФИО для мемориала ID={}", memorialId);
                memorial.setFio(memorial.getPendingFio());
                memorial.setPendingFio(null);
            }
            
            // 3. Обрабатываем фото
            if (memorial.getPendingPhotoUrl() != null && !memorial.getPendingPhotoUrl().isEmpty()) {
                log.info("Применяем pending фото для мемориала ID={}", memorialId);
                
                // Удаляем старое фото, если есть
                if (memorial.getPhotoUrl() != null) {
                    fileStorageService.deleteFile(memorial.getPhotoUrl());
                }
                
                // Применяем новое фото
                memorial.setPhotoUrl(memorial.getPendingPhotoUrl());
                memorial.setPendingPhotoUrl(null);
            }
            
            // 4. Обрабатываем биографию
            if (memorial.getPendingBiography() != null) {
                log.info("Применяем pending биографию для мемориала ID={}", memorialId);
                memorial.setBiography(memorial.getPendingBiography());
                memorial.setPendingBiography(null);
            }
            
            // 5. Обрабатываем даты
            if (memorial.getPendingBirthDate() != null) {
                log.info("Применяем pending дату рождения для мемориала ID={}", memorialId);
                memorial.setBirthDate(memorial.getPendingBirthDate());
                memorial.setPendingBirthDate(null);
            }
            
            if (memorial.getPendingDeathDate() != null) {
                log.info("Применяем pending дату смерти для мемориала ID={}", memorialId);
                memorial.setDeathDate(memorial.getPendingDeathDate());
                memorial.setPendingDeathDate(null);
            }
            
            // 6. Обрабатываем публичность
            if (memorial.getPendingIsPublic() != null) {
                log.info("Применяем pending публичность для мемориала ID={}", memorialId);
                memorial.setPublic(memorial.getPendingIsPublic());
                memorial.setPendingIsPublic(null);
            }
            
            // 7. Обрабатываем местоположения
            if (memorial.getPendingMainLocation() != null) {
                log.info("Применяем pending основное местоположение для мемориала ID={}", memorialId);
                memorial.setMainLocation(memorial.getPendingMainLocation());
                memorial.setPendingMainLocation(null);
            }
            
            if (memorial.getPendingBurialLocation() != null) {
                log.info("Применяем pending место захоронения для мемориала ID={}", memorialId);
                memorial.setBurialLocation(memorial.getPendingBurialLocation());
                memorial.setPendingBurialLocation(null);
            }
            
            // Очищаем флаги и временные данные
            memorial.setChangesUnderModeration(false);
            memorial.setPendingChanges(false);
            memorial.setPreviousState(null);
            memorial.setProposedChanges(null);
            
            // Сохраняем изменения
            memorial = memorialRepository.saveAndFlush(memorial);
            log.info("Успешно применены все ожидающие изменения для мемориала ID={} администратором", memorialId);
            
            // Сохраняем lastEditorId перед созданием уведомлений
            Long savedLastEditorId = memorial.getLastEditorId();
            
            // Создаем уведомление для владельца о том, что изменения приняты
            if (savedLastEditorId != null) {
                User editor = userRepository.findById(savedLastEditorId)
                    .orElse(null);
                
                if (editor != null) {
                    Notification notification = new Notification();
                    notification.setUser(editor);
                    notification.setSender(admin);
                    notification.setTitle("Изменения в мемориале приняты");
                    notification.setMessage("Ваши изменения для мемориала \"" + memorial.getFio() + "\" были одобрены администратором.");
                    notification.setType(Notification.NotificationType.SYSTEM);
                    notification.setStatus(Notification.NotificationStatus.INFO); // Информационное уведомление
        notification.setRelatedEntityId(memorial.getId());
        notification.setRelatedEntityName(memorial.getFio());
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setRead(false);
                    
                    notificationRepository.save(notification);
                    log.info("Создано уведомление для редактора ID={} о принятии изменений администратором", editor.getId());
                }
            }
            
            // Также создаем уведомление для владельца мемориала
            Notification ownerNotification = new Notification();
            ownerNotification.setUser(memorial.getCreatedBy());
            ownerNotification.setSender(admin);
            ownerNotification.setTitle("Изменения в мемориале одобрены");
            ownerNotification.setMessage("Изменения в вашем мемориале \"" + memorial.getFio() + "\" были одобрены администратором.");
            ownerNotification.setType(Notification.NotificationType.SYSTEM);
            ownerNotification.setStatus(Notification.NotificationStatus.INFO); // Информационное уведомление
            ownerNotification.setRelatedEntityId(memorial.getId());
            ownerNotification.setRelatedEntityName(memorial.getFio());
            ownerNotification.setCreatedAt(LocalDateTime.now());
            ownerNotification.setRead(false);
            
            notificationRepository.save(ownerNotification);
            log.info("Создано уведомление для владельца ID={} о одобрении изменений администратором", memorial.getCreatedBy().getId());
            
            // Теперь очищаем lastEditorId
            memorial.setLastEditorId(null);
            
        } catch (Exception e) {
            log.error("Ошибка при применении ожидающих изменений администратором: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при применении изменений: " + e.getMessage());
        }
        
        // Финальное сохранение и проверка флагов
        memorial = memorialRepository.saveAndFlush(memorial);
        log.info("Завершено одобрение изменений мемориала ID={} администратором, финальное состояние pendingChanges={}, changesUnderModeration={}", 
                memorial.getId(), memorial.isPendingChanges(), memorial.isChangesUnderModeration());
        
        return memorialMapper.toDTO(memorial);
    }

    // Старые методы для обратной совместимости
    
    public List<MemorialDTO> getMyMemorials(Long userId) {
        // Вызываем новый метод с пагинацией и возвращаем первую страницу большого размера
        PagedResponse<MemorialDTO> pagedResponse = getMyMemorials(userId, 0, 1000);
        return pagedResponse.getContent();
    }

    public List<MemorialDTO> getPublicMemorials() {
        // Вызываем новый метод с пагинацией и возвращаем первую страницу большого размера
        PagedResponse<MemorialDTO> pagedResponse = getPublicMemorials(0, 1000);
        return pagedResponse.getContent();
    }
}