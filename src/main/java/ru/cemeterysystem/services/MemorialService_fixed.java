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
public class MemorialService_fixed {
    private static final Logger log = LoggerFactory.getLogger(MemorialService.class);
    
    private final MemorialRepository memorialRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final MemorialMapper memorialMapper;
    private final UserMapper userMapper;
    private final NotificationRepository notificationRepository;

    /**
     * Подтверждает или отклоняет изменения в мемориале
     * 
     * @param memorialId ID мемориала
     * @param approve true для подтверждения, false для отклонения
     * @param currentUser текущий пользователь
     * @return обновленный мемориал
     */
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
                // Проверяем наличие ожидающего фото
                if (memorial.getPendingPhotoUrl() != null && !memorial.getPendingPhotoUrl().isEmpty()) {
                    log.info("Найдено ожидающее фото для мемориала ID={}: {}", 
                            memorialId, memorial.getPendingPhotoUrl());
                    
                    // Удаляем старое фото, если есть
                    if (memorial.getPhotoUrl() != null) {
                        fileStorageService.deleteFile(memorial.getPhotoUrl());
                    }
                    
                    // Применяем новое фото
                    memorial.setPhotoUrl(memorial.getPendingPhotoUrl());
                    memorial.setPendingPhotoUrl(null);
                    
                    log.info("Новое фото успешно применено для мемориала ID={}", memorialId);
                }
                
                // При подтверждении изменений применяем proposedChanges
                if (memorial.getProposedChanges() != null && !memorial.getProposedChanges().isEmpty()) {
                    log.info("Применяем предложенные изменения для мемориала ID={}", memorialId);
                    
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    // Регистрируем модуль для сериализации Java 8 date/time типов
                    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    
                    // Логируем JSON для отладки
                    log.info("JSON с предложенными изменениями: {}", memorial.getProposedChanges());
                    
                    try {
                        // Пытаемся десериализовать предложенные изменения
                        MemorialDTO proposedDto = objectMapper.readValue(memorial.getProposedChanges(), MemorialDTO.class);
                        log.info("Успешно десериализованы предложенные изменения: {}", proposedDto.getFio());
                        
                        // Делаем резервную копию текущего состояния мемориала перед применением изменений
                        String oldFio = memorial.getFio();
                        String oldBiography = memorial.getBiography();
                        
                        // Явно проверяем и логируем биографию перед обновлением
                        log.info("Биография до обновления: '{}', биография в proposedDto: '{}'", 
                                 oldBiography, proposedDto.getBiography());
                        
                        // Применяем предложенные изменения
                        updateMemorialFromDTO(memorial, proposedDto);
                        
                        // Особая обработка для биографии
                        if (proposedDto.getBiography() != null) {
                            log.info("Явно устанавливаем биографию: '{}'", proposedDto.getBiography());
                            memorial.setBiography(proposedDto.getBiography());
                        }
                        
                        // Принудительно сбрасываем флаг pending
                        memorial.setPendingChanges(false);
                        memorial.setPreviousState(null);
                        memorial.setProposedChanges(null);
                        
                        // Немедленно сохраняем, чтобы примененные изменения точно сохранились
                        memorial = memorialRepository.saveAndFlush(memorial);
                        
                        log.info("Успешно применены предложенные изменения для мемориала ID={}: старое имя: {}, новое имя: {}, биография: '{}'", 
                                memorialId, oldFio, memorial.getFio(), memorial.getBiography());
                        
                        // Проверяем, что изменения применены (для биографии)
                        if (proposedDto.getBiography() != null && !proposedDto.getBiography().equals(memorial.getBiography())) {
                            log.warn("ПРЕДУПРЕЖДЕНИЕ: Биография не была правильно обновлена! Ожидается: '{}', Получено: '{}'", 
                                    proposedDto.getBiography(), memorial.getBiography());
                            
                            // Повторная попытка обновления биографии
                            memorial.setBiography(proposedDto.getBiography());
                            memorial = memorialRepository.saveAndFlush(memorial);
                            log.info("Повторное обновление биографии: '{}'", memorial.getBiography());
                        }
                    } catch (Exception e) {
                        log.error("Ошибка при десериализации предложенных изменений: {}", e.getMessage(), e);
                        
                        // Пытаемся восстановить базовые поля из JSON
                        try {
                            // Получаем JSON как Map
                            // Обеспечиваем поддержку Java 8 date/time типов
                            objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                            objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                            
                            Map<String, Object> fieldsMap = objectMapper.readValue(
                                memorial.getProposedChanges(), 
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                            );
                            
                            // Обновляем отдельные поля
                            if (fieldsMap.containsKey("fio")) {
                                memorial.setFio((String) fieldsMap.get("fio"));
                            }
                            
                            if (fieldsMap.containsKey("birthDate")) {
                                memorial.setBirthDate(LocalDate.parse((String) fieldsMap.get("birthDate")));
                            }
                            
                            if (fieldsMap.containsKey("deathDate") && fieldsMap.get("deathDate") != null) {
                                memorial.setDeathDate(LocalDate.parse((String) fieldsMap.get("deathDate")));
                            } else {
                                memorial.setDeathDate(null);
                            }
                            
                            if (fieldsMap.containsKey("biography")) {
                                Object biographyObj = fieldsMap.get("biography");
                                String biography = biographyObj != null ? biographyObj.toString() : null;
                                log.info("Устанавливаем биографию из JSON: '{}', тип объекта: {}", 
                                    biography, biographyObj != null ? biographyObj.getClass().getName() : "null");
                                memorial.setBiography(biography);
                            } else {
                                log.info("Поле biography не найдено в JSON");
                            }
                            
                            if (fieldsMap.containsKey("isPublic")) {
                                memorial.setPublic((Boolean) fieldsMap.get("isPublic"));
                            }
                            
                            // Принудительно сбрасываем флаг pending
                            memorial.setPendingChanges(false);
                            memorial.setPreviousState(null);
                            memorial.setProposedChanges(null);
                            
                            // Сохраняем изменения
                            memorial = memorialRepository.saveAndFlush(memorial);
                            
                            log.info("Применены базовые поля из JSON для мемориала ID={}", memorialId);
                        } catch (Exception ex) {
                            log.error("Не удалось восстановить данные из JSON: {}", ex.getMessage(), ex);
                        }
                    }
                } else {
                    log.warn("Предложенные изменения не найдены для мемориала ID={}", memorialId);
                }
            } catch (Exception e) {
                log.error("Ошибка при применении предложенных изменений: {}", e.getMessage(), e);
            }
            
            // Сбрасываем флаги и очищаем временные данные
            log.info("Сбрасываем флаги ожидания изменений для мемориала ID={}", memorialId);
            memorial.setPendingChanges(false);
            memorial.setPreviousState(null);
            memorial.setProposedChanges(null);
            
            // Немедленно сохраняем изменения в базе данных
            memorial = memorialRepository.saveAndFlush(memorial);
            log.info("Сохранен обновленный мемориал с примененными изменениями, ID={}, pendingChanges={}", 
                     memorial.getId(), memorial.isPendingChanges());
            
            // Создаем уведомление для редактора о том, что изменения приняты
            if (memorial.getLastEditorId() != null) {
                User editor = userRepository.findById(memorial.getLastEditorId())
                    .orElse(null);
                
                if (editor != null) {
                    Notification notification = new Notification();
                    notification.setTitle("Изменения в мемориале приняты");
                    notification.setMessage("Владелец " + currentUser.getFio() + " принял ваши изменения в мемориале \"" 
                            + memorial.getFio() + "\".");
                    notification.setUser(editor);
                    notification.setSender(currentUser);
                    notification.setType(Notification.NotificationType.MEMORIAL_EDIT);
                    notification.setStatus(Notification.NotificationStatus.ACCEPTED);
                    notification.setRelatedEntityId(memorial.getId());
                    notification.setRelatedEntityName(memorial.getFio());
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setRead(false);
                    
                    notificationRepository.save(notification);
                    log.info("Создано уведомление о принятии изменений для пользователя ID={}", editor.getId());
                }
            }
        } else {
            log.info("Отклонены изменения для мемориала ID={}", memorialId);
            
            // При отклонении изменений ничего не делаем с самим мемориалом,
            // так как изменения не были применены
            
            // Сбрасываем флаги и очищаем временные данные
            log.info("Сбрасываем флаги ожидания изменений для мемориала ID={} (отклонение)", memorialId);
            memorial.setPendingChanges(false);
            memorial.setPreviousState(null);
            memorial.setProposedChanges(null);
            
            // Немедленно сохраняем изменения в базе данных
            memorial = memorialRepository.saveAndFlush(memorial);
            log.info("Сохранен мемориал с отклоненными изменениями, ID={}, pendingChanges={}", 
                     memorial.getId(), memorial.isPendingChanges());
            
            // Создаем уведомление для редактора о том, что изменения отклонены
            if (memorial.getLastEditorId() != null) {
                User editor = userRepository.findById(memorial.getLastEditorId())
                    .orElse(null);
                
                if (editor != null) {
                    Notification notification = new Notification();
                    notification.setTitle("Изменения в мемориале отклонены");
                    notification.setMessage("Владелец " + currentUser.getFio() + " отклонил ваши изменения в мемориале \"" 
                            + memorial.getFio() + "\".");
                    notification.setUser(editor);
                    notification.setSender(currentUser);
                    notification.setType(Notification.NotificationType.MEMORIAL_EDIT);
                    notification.setStatus(Notification.NotificationStatus.REJECTED);
                    notification.setRelatedEntityId(memorial.getId());
                    notification.setRelatedEntityName(memorial.getFio());
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setRead(false);
                    
                    notificationRepository.save(notification);
                    log.info("Создано уведомление о отклонении изменений для пользователя ID={}", editor.getId());
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
} 