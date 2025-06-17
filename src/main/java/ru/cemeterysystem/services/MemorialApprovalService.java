package ru.cemeterysystem.services;

import ru.cemeterysystem.dto.MemorialDTO;
import ru.cemeterysystem.models.User;

/**
 * Интерфейс для одобрения изменений мемориала.
 * Используется для разрыва циклической зависимости между NotificationService и MemorialService.
 */
public interface MemorialApprovalService {
    
    /**
     * Одобряет или отклоняет изменения в мемориале
     * 
     * @param memorialId ID мемориала
     * @param approve true для одобрения, false для отклонения
     * @param currentUser текущий пользователь
     * @return обновленный мемориал
     */
    MemorialDTO approveChanges(Long memorialId, boolean approve, User currentUser);
} 