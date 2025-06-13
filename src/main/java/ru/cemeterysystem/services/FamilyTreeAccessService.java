package ru.cemeterysystem.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.FamilyTreeAccess;
import ru.cemeterysystem.models.FamilyTreeDraft;
import ru.cemeterysystem.models.Notification;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.FamilyTreeAccessRepository;
import ru.cemeterysystem.repositories.FamilyTreeDraftRepository;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import ru.cemeterysystem.repositories.NotificationRepository;
import ru.cemeterysystem.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FamilyTreeAccessService {
    private static final Logger logger = LoggerFactory.getLogger(FamilyTreeAccessService.class);
    
    private final FamilyTreeAccessRepository accessRepository;
    private final FamilyTreeRepository familyTreeRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FamilyTreeDraftRepository draftRepository;

    @Autowired
    public FamilyTreeAccessService(
            FamilyTreeAccessRepository accessRepository,
            FamilyTreeRepository familyTreeRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            FamilyTreeDraftRepository draftRepository) {
        this.accessRepository = accessRepository;
        this.familyTreeRepository = familyTreeRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.draftRepository = draftRepository;
    }

    @Transactional
    public FamilyTreeAccess grantAccess(Long familyTreeId, Long userId, FamilyTreeAccess.AccessLevel accessLevel, Long grantedById) {
        FamilyTree tree = familyTreeRepository.findById(familyTreeId)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!hasAccess(familyTreeId, grantedById, FamilyTreeAccess.AccessLevel.ADMIN)) {
            throw new RuntimeException("Insufficient permissions to grant access");
        }

        if (accessRepository.existsByFamilyTreeIdAndUserId(familyTreeId, userId)) {
            throw new RuntimeException("Access already granted for this user");
        }

        FamilyTreeAccess access = new FamilyTreeAccess();
        access.setFamilyTree(tree);
        access.setUser(user);
        access.setAccessLevel(accessLevel);
        access.setGrantedById(grantedById);

        return accessRepository.save(access);
    }

    @Transactional
    public FamilyTreeAccess updateAccess(Long familyTreeId, Long userId, FamilyTreeAccess.AccessLevel newAccessLevel, Long updatedById) {
        if (!hasAccess(familyTreeId, updatedById, FamilyTreeAccess.AccessLevel.ADMIN)) {
            throw new RuntimeException("Insufficient permissions to update access");
        }

        FamilyTreeAccess access = accessRepository.findByFamilyTreeIdAndUserId(familyTreeId, userId)
                .orElseThrow(() -> new RuntimeException("Access not found"));

        access.setAccessLevel(newAccessLevel);
        return accessRepository.save(access);
    }

    @Transactional
    public void revokeAccess(Long familyTreeId, Long userId, Long revokedById) {
        if (!hasAccess(familyTreeId, revokedById, FamilyTreeAccess.AccessLevel.ADMIN)) {
            throw new RuntimeException("Insufficient permissions to revoke access");
        }

        if (!accessRepository.existsByFamilyTreeIdAndUserId(familyTreeId, userId)) {
            throw new RuntimeException("Access not found");
        }
        accessRepository.deleteByFamilyTreeIdAndUserId(familyTreeId, userId);
    }

    @Transactional(readOnly = true)
    public List<FamilyTreeAccess> getAccessList(Long familyTreeId, Long requestingUserId) {
        if (!hasAccess(familyTreeId, requestingUserId, FamilyTreeAccess.AccessLevel.VIEWER)) {
            throw new RuntimeException("Insufficient permissions to view access list");
        }
        return accessRepository.findByFamilyTreeId(familyTreeId);
    }

    @Transactional(readOnly = true)
    public List<FamilyTreeAccess> getUserAccess(Long userId) {
        return accessRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(Long familyTreeId, Long userId, FamilyTreeAccess.AccessLevel requiredLevel) {
        return accessRepository.findByFamilyTreeIdAndUserId(familyTreeId, userId)
                .map(access -> access.getAccessLevel().ordinal() >= requiredLevel.ordinal())
                .orElse(false);
    }

    /**
     * Отправка запроса на доступ к семейному дереву
     */
    @Transactional
    public void requestAccess(Long familyTreeId, User requester, FamilyTreeAccess.AccessLevel requestedLevel, String message) {
        FamilyTree tree = familyTreeRepository.findById(familyTreeId)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

        // Проверяем, что дерево публичное
        if (!tree.isPublic()) {
            throw new RuntimeException("Cannot request access to private family tree");
        }

        // Проверяем, что пользователь не является владельцем
        if (tree.getUser().getId().equals(requester.getId())) {
            throw new RuntimeException("Cannot request access to your own family tree");
        }

        // Проверяем, что у пользователя еще нет доступа
        if (accessRepository.existsByFamilyTreeIdAndUserId(familyTreeId, requester.getId())) {
            throw new RuntimeException("You already have access to this family tree");
        }

        // Создаем уведомление для владельца дерева
        createAccessRequestNotification(tree, requester, requestedLevel, message);
        
        logger.info("Access request sent for family tree {} by user {}", familyTreeId, requester.getId());
    }

    /**
     * Ответ на запрос доступа (одобрение или отклонение)
     */
    @Transactional
    public void respondToAccessRequest(Long notificationId, boolean approve, String responseMessage, User owner) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(owner.getId())) {
            throw new RuntimeException("You can only respond to your own notifications");
        }

        if (notification.getType() != Notification.NotificationType.FAMILY_TREE_ACCESS_REQUEST) {
            throw new RuntimeException("Invalid notification type");
        }

        Long familyTreeId = notification.getRelatedEntityId();
        User requester = notification.getSender();

        if (approve) {
            // Предоставляем доступ
            FamilyTreeAccess.AccessLevel accessLevel = FamilyTreeAccess.AccessLevel.EDITOR; // По умолчанию EDITOR
            
            FamilyTreeAccess access = new FamilyTreeAccess();
            access.setFamilyTree(familyTreeRepository.findById(familyTreeId).orElseThrow());
            access.setUser(requester);
            access.setAccessLevel(accessLevel);
            access.setGrantedById(owner.getId());
            
            accessRepository.save(access);
            
            // СОЗДАЕМ ЧЕРНОВИК ДЛЯ РЕДАКТОРА
            try {
                FamilyTree originalTree = familyTreeRepository.findById(familyTreeId).orElseThrow();
                
                // Проверяем, что у редактора еще нет черновика для этого дерева
                Optional<FamilyTreeDraft> existingDraft = draftRepository.findActiveDraftByTreeAndEditor(familyTreeId, requester.getId());
                if (!existingDraft.isPresent()) {
                    // Создаем черновик с ПОЛНОЙ копией данных дерева
                    FamilyTreeDraft draft = new FamilyTreeDraft();
                    draft.setFamilyTree(originalTree);
                    draft.setEditor(requester);
                    draft.setStatus(FamilyTreeDraft.DraftStatus.DRAFT);
                    draft.setCreatedAt(LocalDateTime.now());
                    
                    // Копируем текущие данные дерева
                    draft.setDraftName(originalTree.getName());
                    draft.setDraftDescription(originalTree.getDescription());
                    draft.setDraftIsPublic(originalTree.isPublic());
                    
                    // Сохраняем исходные данные для сравнения
                    draft.setOriginalName(originalTree.getName());
                    draft.setOriginalDescription(originalTree.getDescription());
                    draft.setOriginalIsPublic(originalTree.isPublic());
                    
                    draftRepository.save(draft);
                    logger.info("Draft created for user {} for family tree {}", requester.getId(), familyTreeId);
                } else {
                    logger.info("Draft already exists for user {} for family tree {}", requester.getId(), familyTreeId);
                }
            } catch (Exception e) {
                logger.error("Error creating draft for user {} for family tree {}: {}", 
                           requester.getId(), familyTreeId, e.getMessage(), e);
                // Не прерываем процесс, если создание черновика не удалось
            }
            
            // Создаем уведомление о предоставлении доступа
            createAccessGrantedNotification(familyTreeId, requester, owner, accessLevel, responseMessage);
            
            logger.info("Access granted to user {} for family tree {} by owner {}", 
                    requester.getId(), familyTreeId, owner.getId());
        } else {
            // Создаем уведомление об отклонении
            createAccessDeniedNotification(familyTreeId, requester, owner, responseMessage);
            
            logger.info("Access denied to user {} for family tree {} by owner {}", 
                    requester.getId(), familyTreeId, owner.getId());
        }

        // Обновляем статус исходного уведомления
        notification.setStatus(approve ? Notification.NotificationStatus.ACCEPTED : Notification.NotificationStatus.REJECTED);
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private void createAccessRequestNotification(FamilyTree tree, User requester, 
                                               FamilyTreeAccess.AccessLevel requestedLevel, String message) {
        try {
            Notification notification = new Notification();
            notification.setType(Notification.NotificationType.FAMILY_TREE_ACCESS_REQUEST);
            notification.setUser(tree.getUser()); // Владелец дерева
            notification.setSender(requester);
            notification.setTitle("Запрос на доступ к генеалогическому дереву");
            
            String notificationMessage = String.format(
                "Пользователь %s запрашивает доступ к вашему генеалогическому дереву \"%s\".\n\n" +
                "Запрашиваемый уровень доступа: %s\n\n",
                requester.getFio() != null ? requester.getFio() : requester.getLogin(),
                tree.getName(),
                getAccessLevelDisplayName(requestedLevel)
            );
            
            if (message != null && !message.trim().isEmpty()) {
                notificationMessage += String.format("Сообщение от пользователя: %s\n\n", message.trim());
            }
            
            notificationMessage += "Вы можете одобрить или отклонить этот запрос в разделе уведомлений.";
            
            notification.setMessage(notificationMessage);
            notification.setStatus(Notification.NotificationStatus.PENDING);
            notification.setRelatedEntityId(tree.getId());
            notification.setRelatedEntityName(tree.getName());
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setUrgent(false);
            
            notificationRepository.save(notification);
            logger.info("Access request notification created for tree owner {}", tree.getUser().getLogin());
        } catch (Exception e) {
            logger.error("Error creating access request notification: {}", e.getMessage(), e);
        }
    }

    private void createAccessGrantedNotification(Long familyTreeId, User requester, User owner, 
                                               FamilyTreeAccess.AccessLevel accessLevel, String message) {
        try {
            FamilyTree tree = familyTreeRepository.findById(familyTreeId).orElse(null);
            if (tree == null) return;
            
            Notification notification = new Notification();
            notification.setType(Notification.NotificationType.FAMILY_TREE_ACCESS_GRANTED);
            notification.setUser(requester);
            notification.setSender(owner);
            notification.setTitle("Доступ к генеалогическому дереву предоставлен");
            
            String notificationMessage = String.format(
                "Владелец %s предоставил вам доступ к генеалогическому дереву \"%s\".\n\n" +
                "Уровень доступа: %s\n\n",
                owner.getFio() != null ? owner.getFio() : owner.getLogin(),
                tree.getName(),
                getAccessLevelDisplayName(accessLevel)
            );
            
            if (message != null && !message.trim().isEmpty()) {
                notificationMessage += String.format("Сообщение от владельца: %s\n\n", message.trim());
            }
            
            notificationMessage += "Теперь вы можете вносить изменения в это дерево. Все изменения будут отправляться владельцу на одобрение.";
            
            notification.setMessage(notificationMessage);
            notification.setStatus(Notification.NotificationStatus.INFO);
            notification.setRelatedEntityId(tree.getId());
            notification.setRelatedEntityName(tree.getName());
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            
            notificationRepository.save(notification);
            logger.info("Access granted notification created for user {}", requester.getLogin());
        } catch (Exception e) {
            logger.error("Error creating access granted notification: {}", e.getMessage(), e);
        }
    }

    private void createAccessDeniedNotification(Long familyTreeId, User requester, User owner, String message) {
        try {
            FamilyTree tree = familyTreeRepository.findById(familyTreeId).orElse(null);
            if (tree == null) return;
            
            Notification notification = new Notification();
            notification.setType(Notification.NotificationType.SYSTEM);
            notification.setUser(requester);
            notification.setSender(owner);
            notification.setTitle("Запрос на доступ к генеалогическому дереву отклонен");
            
            String notificationMessage = String.format(
                "Владелец %s отклонил ваш запрос на доступ к генеалогическому дереву \"%s\".\n\n",
                owner.getFio() != null ? owner.getFio() : owner.getLogin(),
                tree.getName()
            );
            
            if (message != null && !message.trim().isEmpty()) {
                notificationMessage += String.format("Причина отклонения: %s\n\n", message.trim());
            }
            
            notificationMessage += "Вы можете отправить новый запрос позже.";
            
            notification.setMessage(notificationMessage);
            notification.setStatus(Notification.NotificationStatus.INFO);
            notification.setRelatedEntityId(tree.getId());
            notification.setRelatedEntityName(tree.getName());
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            
            notificationRepository.save(notification);
            logger.info("Access denied notification created for user {}", requester.getLogin());
        } catch (Exception e) {
            logger.error("Error creating access denied notification: {}", e.getMessage(), e);
        }
    }

    private String getAccessLevelDisplayName(FamilyTreeAccess.AccessLevel level) {
        switch (level) {
            case VIEWER: return "Просмотр";
            case EDITOR: return "Редактирование";
            case ADMIN: return "Администрирование";
            default: return level.toString();
        }
    }
} 