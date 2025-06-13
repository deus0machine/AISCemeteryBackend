package ru.cemeterysystem.services;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.dto.FamilyTreeUpdateDTO;
import ru.cemeterysystem.dto.MemorialRelationDTO;
import ru.cemeterysystem.dto.FamilyTreeDTO;
import ru.cemeterysystem.dto.TreeMemorialDTO;
import ru.cemeterysystem.mappers.FamilyTreeMapper;
import ru.cemeterysystem.models.*;
import ru.cemeterysystem.repositories.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FamilyTreeService {
    private static final Logger logger = LoggerFactory.getLogger(FamilyTreeService.class);
    private final FamilyTreeRepository familyTreeRepository;
    private final FamilyTreeAccessService accessService;
    private final FamilyTreeAccessRepository accessRepository;
    private final MemorialRelationRepository memorialRelationRepository;
    private final MemorialRepository memorialRepository;
    private final FamilyTreeMapper familyTreeMapper;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Autowired
    public FamilyTreeService(
            FamilyTreeRepository familyTreeRepository,
            FamilyTreeAccessService accessService,
            FamilyTreeAccessRepository accessRepository,
            MemorialRelationRepository memorialRelationRepository,
            MemorialRepository memorialRepository,
            FamilyTreeMapper familyTreeMapper,
            UserRepository userRepository,
            NotificationRepository notificationRepository) {
        this.familyTreeRepository = familyTreeRepository;
        this.accessService = accessService;
        this.accessRepository = accessRepository;
        this.memorialRelationRepository = memorialRelationRepository;
        this.memorialRepository = memorialRepository;
        this.familyTreeMapper = familyTreeMapper;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public FamilyTreeDTO createFamilyTree(FamilyTree familyTree, User user) {
        try {
            familyTree.setUser(user);
            // Устанавливаем начальные значения для модерации
            familyTree.setPublic(false);
            familyTree.setPublicationStatus(FamilyTree.PublicationStatus.DRAFT);
            
            FamilyTree savedTree = familyTreeRepository.save(familyTree);
            
            // Создаем доступ для владельца
            FamilyTreeAccess access = new FamilyTreeAccess();
            access.setFamilyTree(savedTree);
            access.setUser(user);
            access.setAccessLevel(FamilyTreeAccess.AccessLevel.ADMIN);
            access.setGrantedById(user.getId());
            accessRepository.save(access);
            
            return familyTreeMapper.toDTO(savedTree);
        } catch (Exception e) {
            logger.error("Error creating family tree: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create family tree", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FamilyTreeDTO> getFamilyTreesByOwner(User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User cannot be null");
            }
            logger.debug("Getting family trees for user with ID: {}", user.getId());
            return familyTreeRepository.findByUser_Id(user.getId())
                .stream().map(familyTreeMapper::toDTO).toList();
        } catch (Exception e) {
            logger.error("Error getting family trees for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to get family trees", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FamilyTreeDTO> getPublicFamilyTrees() {
        try {
            // Возвращаем только опубликованные деревья
            return familyTreeRepository.findByIsPublicTrueAndPublicationStatus(FamilyTree.PublicationStatus.PUBLISHED)
                .stream().map(familyTreeMapper::toDTO).toList();
        } catch (Exception e) {
            logger.error("Error getting public family trees: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get public family trees", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FamilyTreeDTO> getAccessibleFamilyTrees(User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User cannot be null");
            }
            return familyTreeRepository.findByUserIdOrPublicAndPublished(user.getId())
                .stream().map(familyTreeMapper::toDTO).toList();
        } catch (Exception e) {
            logger.error("Error getting accessible family trees for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to get accessible family trees", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FamilyTreeDTO> getSharedFamilyTrees(User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User cannot be null");
            }
            // Получаем только деревья с явным доступом (не публичные)
            List<FamilyTreeAccess> userAccess = accessRepository.findByUserId(user.getId());
            return userAccess.stream()
                .map(access -> familyTreeMapper.toDTO(access.getFamilyTree()))
                .toList();
        } catch (Exception e) {
            logger.error("Error getting shared family trees for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to get shared family trees", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FamilyTreeDTO> searchFamilyTrees(String query, String ownerName, String startDate, String endDate, User currentUser) {
        try {
            logger.debug("Searching family trees with parameters: query={}, ownerName={}, startDate={}, endDate={}, currentUser={}", 
                query, ownerName, startDate, endDate, currentUser != null ? currentUser.getLogin() : "null");
            
            List<FamilyTree> trees = familyTreeRepository.searchFamilyTrees(query, ownerName, startDate, endDate, 
                currentUser != null ? currentUser.getId() : null);
            
            return trees.stream().map(familyTreeMapper::toDTO).toList();
        } catch (Exception e) {
            logger.error("Error searching family trees: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search family trees", e);
        }
    }

    @Transactional
    public FamilyTreeDTO updateFamilyTree(Long id, FamilyTreeUpdateDTO updateDTO, User user) {
        try {
            logger.debug("Updating family tree {} by user {}", id, user.getId());
            
            FamilyTree existingTree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

            boolean isOwner = existingTree.getUser().getId().equals(user.getId());
            boolean hasAdminAccess = isOwner || accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.ADMIN);
            boolean hasEditorAccess = accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.EDITOR);
            
            logger.debug("Update tree access check - isOwner: {}, hasAdminAccess: {}, hasEditorAccess: {}", 
                isOwner, hasAdminAccess, hasEditorAccess);
            
            // Владельцы и админы могут редактировать напрямую
            if (hasAdminAccess) {
                // Проверяем, не находится ли дерево на модерации
                if (existingTree.getPublicationStatus() == FamilyTree.PublicationStatus.PENDING_MODERATION) {
                    throw new RuntimeException("Cannot edit family tree that is under moderation");
                }
                
                logger.debug("Direct update allowed for owner/admin - updating tree {} with name: '{}', description: '{}'", 
                    id, updateDTO.getName(), updateDTO.getDescription());
                
                existingTree.setName(updateDTO.getName());
                existingTree.setDescription(updateDTO.getDescription());
                // НЕ обновляем isPublic напрямую - это управляется через модерацию
                
                FamilyTree savedTree = familyTreeRepository.save(existingTree);
                logger.debug("Successfully updated family tree {}", id);
                return familyTreeMapper.toDTO(savedTree);
            }
            // Редакторы должны работать через систему черновиков
            else if (hasEditorAccess) {
                logger.info("Editor {} attempting direct update of tree {} - should use draft system instead", 
                    user.getId(), id);
                throw new RuntimeException("Editors must use draft system for changes. Please create a draft with your changes and submit for review.");
            }
            // Нет доступа вообще
            else {
                logger.warn("Access denied for user {} to update family tree {}", user.getId(), id);
                throw new RuntimeException("Insufficient permissions to update family tree");
            }
        } catch (Exception e) {
            logger.error("Error updating family tree {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to update family tree", e);
        }
    }

    @Transactional
    public void deleteFamilyTree(Long id, User user) {
        try {
            logger.debug("Deleting family tree {} by user {}", id, user.getId());
            
            FamilyTree tree = familyTreeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Family tree not found"));
            
            boolean isOwner = tree.getUser().getId().equals(user.getId());
            boolean hasAccess = isOwner || accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.ADMIN);
            
            logger.debug("Delete tree access check - isOwner: {}, hasExplicitAccess: {}", 
                isOwner, !isOwner ? accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.ADMIN) : "not_checked");
            
            if (!hasAccess) {
                logger.warn("Access denied for user {} to delete family tree {}", user.getId(), id);
                throw new RuntimeException("Insufficient permissions to delete family tree");
            }
            
            logger.debug("Access granted for user {} to delete family tree {}", user.getId(), id);
            familyTreeRepository.deleteById(id);
            logger.debug("Successfully deleted family tree {}", id);
        } catch (Exception e) {
            logger.error("Error deleting family tree {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete family tree", e);
        }
    }

    @Transactional(readOnly = true)
    public FamilyTreeDTO getFamilyTreeById(Long id, User user) {
        try {
            logger.debug("Getting family tree {} for user {}", id, user.getId());
            FamilyTree tree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));
            
            logger.debug("Found tree: {}, owner: {}, isPublic: {}", tree.getName(), tree.getUser().getId(), tree.isPublic());
            
            // Проверяем доступ: владелец, публичное дерево или есть доступ
            boolean isOwner = tree.getUser().getId().equals(user.getId());
            boolean hasAccess = tree.isPublic() || isOwner || accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.VIEWER);
            
            logger.debug("Access check - isOwner: {}, isPublic: {}, hasExplicitAccess: {}", 
                isOwner, tree.isPublic(), !isOwner && !tree.isPublic() ? accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.VIEWER) : "not_checked");
            
            if (!hasAccess) {
                logger.warn("Access denied for user {} to family tree {}", user.getId(), id);
                throw new RuntimeException("Insufficient permissions to view family tree");
            }
            
            logger.debug("Access granted for user {} to family tree {}", user.getId(), id);
            return familyTreeMapper.toDTO(tree);
        } catch (Exception e) {
            logger.error("Error getting family tree {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to get family tree", e);
        }
    }
    @Transactional
    public MemorialRelation addRelation(Long treeId, MemorialRelationDTO relationDTO, User user) {
        logger.info("=== FamilyTreeService.addRelation START ===");
        logger.info("TreeId: {}", treeId);
        logger.info("User: {}", user.getLogin());
        logger.info("RelationDTO: {}", relationDTO);
        
        try {
            logger.info("Finding family tree with ID: {}", treeId);
            FamilyTree tree = familyTreeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Family tree not found"));
            logger.info("Found family tree: {}", tree.getName());
            
            logger.info("Checking access permissions...");
            boolean isOwner = tree.getUser().getId().equals(user.getId());
            boolean hasAdminAccess = isOwner || accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.ADMIN);
            boolean hasEditorAccess = accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.EDITOR);
            
            logger.info("Add relation access check - isOwner: {}, hasAdminAccess: {}, hasEditorAccess: {}", 
                isOwner, hasAdminAccess, hasEditorAccess);
            
            // Владельцы и админы могут добавлять связи напрямую
            if (hasAdminAccess) {
                logger.info("Direct relation addition allowed for owner/admin");
            }
            // Редакторы должны работать через систему черновиков
            else if (hasEditorAccess) {
                logger.info("Editor {} attempting direct relation addition to tree {} - should use draft system instead", 
                    user.getId(), treeId);
                throw new RuntimeException("Editors must use draft system for changes. Please create a draft with your changes and submit for review.");
            }
            // Нет доступа вообще
            else {
                logger.error("Access denied for user {} to tree {}", user.getId(), treeId);
                throw new RuntimeException("Insufficient permissions to add relation");
            }

            logger.info("Finding source memorial...");
            Long sourceMemorialId = relationDTO.getSourceMemorial() != null ? relationDTO.getSourceMemorial().getId() : null;
            logger.info("Source memorial ID: {}", sourceMemorialId);
            Memorial sourceMemorial = memorialRepository.findById(sourceMemorialId)
                    .orElseThrow(() -> new RuntimeException("Source memorial not found"));
            logger.info("Found source memorial: {}", sourceMemorial.getFio());
            
            logger.info("Finding target memorial...");
            Long targetMemorialId = relationDTO.getTargetMemorial() != null ? relationDTO.getTargetMemorial().getId() : null;
            logger.info("Target memorial ID: {}", targetMemorialId);
            Memorial targetMemorial = memorialRepository.findById(targetMemorialId)
                    .orElseThrow(() -> new RuntimeException("Target memorial not found"));
            logger.info("Found target memorial: {}", targetMemorial.getFio());

            logger.info("Converting relation type: {}", relationDTO.getRelationType());
            MemorialRelation.RelationType relationType;
            try {
                relationType = MemorialRelation.RelationType.valueOf(relationDTO.getRelationType().toUpperCase());
                logger.info("Converted to: {}", relationType);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid relation type: {}", relationDTO.getRelationType());
                throw new RuntimeException("Invalid relation type: " + relationDTO.getRelationType(), e);
            }

            // Проверяем, существует ли уже такая связь
            logger.info("Checking for existing relation...");
            List<MemorialRelation> existingRelations = memorialRelationRepository.findByFamilyTreeId(treeId)
                    .stream()
                    .filter(relation -> 
                        (relation.getSourceMemorial().getId().equals(sourceMemorialId) && 
                         relation.getTargetMemorial().getId().equals(targetMemorialId)) ||
                        (relation.getSourceMemorial().getId().equals(targetMemorialId) && 
                         relation.getTargetMemorial().getId().equals(sourceMemorialId))
                    )
                    .collect(Collectors.toList());
            
            if (!existingRelations.isEmpty()) {
                logger.warn("Relation already exists between memorial {} and {}", sourceMemorialId, targetMemorialId);
                logger.warn("Existing relations: {}", existingRelations.size());
                existingRelations.forEach(rel -> 
                    logger.warn("Existing relation: {} -> {}, type: {}", 
                        rel.getSourceMemorial().getId(), 
                        rel.getTargetMemorial().getId(), 
                        rel.getRelationType())
                );
                throw new RuntimeException("Связь между этими мемориалами уже существует");
            }

            logger.info("Creating new MemorialRelation...");
            MemorialRelation relation = new MemorialRelation();
            relation.setFamilyTree(tree);
            relation.setSourceMemorial(sourceMemorial);
            relation.setTargetMemorial(targetMemorial);
            relation.setRelationType(relationType);

            logger.info("Saving MemorialRelation...");
            MemorialRelation savedRelation = memorialRelationRepository.save(relation);
            logger.info("Successfully saved relation with ID: {}", savedRelation.getId());
            logger.info("=== FamilyTreeService.addRelation SUCCESS ===");
            
            return savedRelation;
        } catch (Exception e) {
            logger.error("=== FamilyTreeService.addRelation ERROR ===");
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Exception message: {}", e.getMessage());
            logger.error("Full stack trace:", e);
            logger.error("=== FamilyTreeService.addRelation END ERROR ===");
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<MemorialRelationDTO> getRelations(Long treeId) {
        List<MemorialRelation> relations = memorialRelationRepository.findByFamilyTreeId(treeId);
        // Фильтруем PLACEHOLDER связи - они технические и не должны показываться пользователю
        return relations.stream()
                .filter(relation -> relation.getRelationType() != MemorialRelation.RelationType.PLACEHOLDER)
                .map(familyTreeMapper::toRelationDTO)
                .toList();
    }

    @Transactional
    public void deleteRelation(Long treeId, Long relationId, User user) {
        try {
            logger.debug("Deleting relation {} from tree {} by user {}", relationId, treeId, user.getId());
            
            MemorialRelation relation = memorialRelationRepository.findById(relationId)
                    .orElseThrow(() -> new RuntimeException("Relation not found"));

            if (!relation.getFamilyTree().getId().equals(treeId)) {
                throw new RuntimeException("Relation does not belong to the specified family tree");
            }

            FamilyTree tree = relation.getFamilyTree();
            boolean isOwner = tree.getUser().getId().equals(user.getId());
            boolean hasAdminAccess = isOwner || accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.ADMIN);
            boolean hasEditorAccess = accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.EDITOR);
            
            logger.debug("Delete relation access check - isOwner: {}, hasAdminAccess: {}, hasEditorAccess: {}", 
                isOwner, hasAdminAccess, hasEditorAccess);
            
            // Владельцы и админы могут удалять связи напрямую
            if (hasAdminAccess) {
                logger.debug("Direct relation deletion allowed for owner/admin");
                memorialRelationRepository.deleteById(relationId);
                logger.debug("Successfully deleted relation {} from tree {}", relationId, treeId);
            }
            // Редакторы должны работать через систему черновиков
            else if (hasEditorAccess) {
                logger.info("Editor {} attempting direct relation deletion from tree {} - should use draft system instead", 
                    user.getId(), treeId);
                throw new RuntimeException("Editors must use draft system for changes. Please create a draft with your changes and submit for review.");
            }
            // Нет доступа вообще
            else {
                logger.warn("Access denied for user {} to delete relation {} from tree {}", user.getId(), relationId, treeId);
                throw new RuntimeException("Insufficient permissions to delete relation");
            }
        } catch (Exception e) {
            logger.error("Error deleting relation {} from tree {}: {}", relationId, treeId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void addMemorialToTree(Long treeId, Long memorialId, User user) {
        try {
            logger.debug("Adding memorial {} to tree {} by user {}", memorialId, treeId, user.getId());
            
            FamilyTree tree = familyTreeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Family tree not found"));

            boolean isOwner = tree.getUser().getId().equals(user.getId());
            boolean hasAdminAccess = isOwner || accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.ADMIN);
            boolean hasEditorAccess = accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.EDITOR);
            
            logger.debug("Add memorial access check - isOwner: {}, hasAdminAccess: {}, hasEditorAccess: {}", 
                isOwner, hasAdminAccess, hasEditorAccess);
            
            // Владельцы и админы могут добавлять мемориалы напрямую
            if (hasAdminAccess) {
                Memorial memorial = memorialRepository.findById(memorialId)
                        .orElseThrow(() -> new RuntimeException("Memorial not found"));

                // Проверяем, есть ли уже мемориал в дереве
                boolean alreadyExists = memorialRelationRepository.findByFamilyTreeId(treeId)
                        .stream()
                        .anyMatch(relation -> 
                            relation.getSourceMemorial().getId().equals(memorialId) ||
                            relation.getTargetMemorial().getId().equals(memorialId));

                if (alreadyExists) {
                    throw new RuntimeException("Memorial already exists in the family tree");
                }

                // Для начала просто создаем placeholder связь
                // Позже пользователь может создать реальные связи между мемориалами
                MemorialRelation placeholderRelation = new MemorialRelation();
                placeholderRelation.setFamilyTree(tree);
                placeholderRelation.setSourceMemorial(memorial);
                placeholderRelation.setTargetMemorial(memorial); // Временно ссылаемся на самого себя
                placeholderRelation.setRelationType(MemorialRelation.RelationType.PLACEHOLDER);

                memorialRelationRepository.save(placeholderRelation);
                logger.debug("Successfully added memorial {} to tree {} as placeholder", memorialId, treeId);
            }
            // Редакторы должны работать через систему черновиков
            else if (hasEditorAccess) {
                logger.info("Editor {} attempting direct memorial addition to tree {} - should use draft system instead", 
                    user.getId(), treeId);
                throw new RuntimeException("Editors must use draft system for changes. Please create a draft with your changes and submit for review.");
            }
            // Нет доступа вообще
            else {
                logger.warn("Access denied for user {} to add memorial {} to tree {}", user.getId(), memorialId, treeId);
                throw new RuntimeException("Insufficient permissions to add memorial");
            }
        } catch (Exception e) {
            logger.error("Error adding memorial {} to tree {}: {}", memorialId, treeId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<TreeMemorialDTO> getTreeMemorials(Long treeId) {
        List<MemorialRelation> relations = memorialRelationRepository.findByFamilyTreeId(treeId);
        
        // Собираем уникальные мемориалы из всех связей
        Set<Memorial> memorials = new HashSet<>();
        for (MemorialRelation relation : relations) {
            if (relation.getRelationType() == MemorialRelation.RelationType.PLACEHOLDER) {
                // Для PLACEHOLDER связей добавляем только источник (он же и цель)
                // чтобы избежать дублирования одного и того же мемориала
                memorials.add(relation.getSourceMemorial());
            } else {
                // Для обычных связей добавляем оба мемориала
                memorials.add(relation.getSourceMemorial());
                memorials.add(relation.getTargetMemorial());
            }
        }
        
        return memorials.stream()
                .map(familyTreeMapper::toTreeMemorialDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeMemorialFromTree(Long treeId, Long memorialId, User user) {
        try {
            logger.debug("Removing memorial {} from tree {} by user {}", memorialId, treeId, user.getId());
            
            FamilyTree tree = familyTreeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Family tree not found"));

            boolean isOwner = tree.getUser().getId().equals(user.getId());
            boolean hasAdminAccess = isOwner || accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.ADMIN);
            boolean hasEditorAccess = accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.EDITOR);
            
            logger.debug("Remove memorial access check - isOwner: {}, hasAdminAccess: {}, hasEditorAccess: {}", 
                isOwner, hasAdminAccess, hasEditorAccess);
            
            // Владельцы и админы могут удалять мемориалы напрямую
            if (hasAdminAccess) {
                // Удаляем все связи, где участвует данный мемориал
                List<MemorialRelation> relationsToDelete = memorialRelationRepository.findByFamilyTreeId(treeId)
                        .stream()
                        .filter(relation -> 
                            relation.getSourceMemorial().getId().equals(memorialId) ||
                            relation.getTargetMemorial().getId().equals(memorialId))
                        .collect(Collectors.toList());

                logger.debug("Found {} relations to delete for memorial {} in tree {}", relationsToDelete.size(), memorialId, treeId);
                
                for (MemorialRelation relation : relationsToDelete) {
                    memorialRelationRepository.delete(relation);
                }
                
                logger.debug("Successfully removed memorial {} from tree {}", memorialId, treeId);
            }
            // Редакторы должны работать через систему черновиков
            else if (hasEditorAccess) {
                logger.info("Editor {} attempting direct memorial removal from tree {} - should use draft system instead", 
                    user.getId(), treeId);
                throw new RuntimeException("Editors must use draft system for changes. Please create a draft with your changes and submit for review.");
            }
            // Нет доступа вообще
            else {
                logger.warn("Access denied for user {} to remove memorial {} from tree {}", user.getId(), memorialId, treeId);
                throw new RuntimeException("Insufficient permissions to remove memorial");
            }
        } catch (Exception e) {
            logger.error("Error removing memorial {} from tree {}: {}", memorialId, treeId, e.getMessage(), e);
            throw e;
        }
    }

    // Методы для модерации семейных деревьев
    @Transactional
    public FamilyTreeDTO sendForModeration(Long id, User user) {
        try {
            FamilyTree tree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

            // Проверяем права доступа - только владелец может отправить на модерацию
            if (!tree.getUser().equals(user)) {
                throw new RuntimeException("Only tree owner can send for moderation");
            }

            // Проверяем текущий статус
            if (tree.getPublicationStatus() == FamilyTree.PublicationStatus.PENDING_MODERATION) {
                throw new RuntimeException("Family tree is already under moderation");
            }

            if (tree.getPublicationStatus() == FamilyTree.PublicationStatus.PUBLISHED) {
                throw new RuntimeException("Family tree is already published");
            }

            // Проверяем, что все мемориалы в дереве опубликованы
            if (!areAllMemorialsPublished(id)) {
                throw new RuntimeException("All memorials in the tree must be public before submitting for moderation");
            }

            // Отправляем на модерацию
            tree.setPublicationStatus(FamilyTree.PublicationStatus.PENDING_MODERATION);
            tree.setPublic(false); // Остается неопубликованным до одобрения
            
            FamilyTree savedTree = familyTreeRepository.save(tree);
            
            // Создаем уведомление администраторам о новом дереве на модерации
            createTreeModerationNotification(savedTree, user);
            
            logger.info("Family tree {} sent for moderation by user {}", id, user.getId());
            return familyTreeMapper.toDTO(savedTree);
        } catch (Exception e) {
            logger.error("Error sending family tree {} for moderation: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to send family tree for moderation: " + e.getMessage(), e);
        }
    }

    @Transactional
    public FamilyTreeDTO approveTree(Long id, User admin) {
        try {
            // Проверяем права администратора
            if (admin.getRole() != User.Role.ADMIN) {
                throw new RuntimeException("Unauthorized access");
            }

            FamilyTree tree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

            if (tree.getPublicationStatus() != FamilyTree.PublicationStatus.PENDING_MODERATION) {
                throw new RuntimeException("Family tree is not pending moderation");
            }

            // Одобряем и публикуем дерево
            tree.setPublicationStatus(FamilyTree.PublicationStatus.PUBLISHED);
            tree.setPublic(true);
            
            FamilyTree savedTree = familyTreeRepository.save(tree);
            
            // Создаем уведомление владельцу об одобрении
            createTreeApprovalNotification(savedTree, admin, true, null);
            
            logger.info("Family tree {} approved by admin {}", id, admin.getId());
            return familyTreeMapper.toDTO(savedTree);
        } catch (Exception e) {
            logger.error("Error approving family tree {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to approve family tree: " + e.getMessage(), e);
        }
    }

    @Transactional
    public FamilyTreeDTO rejectTree(Long id, User admin, String reason) {
        try {
            // Проверяем права администратора
            if (admin.getRole() != User.Role.ADMIN) {
                throw new RuntimeException("Unauthorized access");
            }

            FamilyTree tree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

            if (tree.getPublicationStatus() != FamilyTree.PublicationStatus.PENDING_MODERATION) {
                throw new RuntimeException("Family tree is not pending moderation");
            }

            // Отклоняем дерево
            tree.setPublicationStatus(FamilyTree.PublicationStatus.REJECTED);
            tree.setPublic(false);
            
            FamilyTree savedTree = familyTreeRepository.save(tree);
            
            // Создаем уведомление владельцу об отклонении с указанием причины
            createTreeApprovalNotification(savedTree, admin, false, reason);
            
            logger.info("Family tree {} rejected by admin {}, reason: {}", id, admin.getId(), reason);
            return familyTreeMapper.toDTO(savedTree);
        } catch (Exception e) {
            logger.error("Error rejecting family tree {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to reject family tree: " + e.getMessage(), e);
        }
    }

    // Вспомогательный метод для проверки публичности всех мемориалов в дереве
    private boolean areAllMemorialsPublished(Long treeId) {
        try {
            List<MemorialRelation> relations = memorialRelationRepository.findByFamilyTreeId(treeId);
            
            // Собираем уникальные ID мемориалов
            Set<Long> memorialIds = new HashSet<>();
            for (MemorialRelation relation : relations) {
                if (relation.getRelationType() == MemorialRelation.RelationType.PLACEHOLDER) {
                    // Для placeholder связей проверяем только исходный мемориал
                    if (relation.getSourceMemorial() != null) {
                        memorialIds.add(relation.getSourceMemorial().getId());
                    }
                } else {
                    // Для обычных связей проверяем оба мемориала
                    if (relation.getSourceMemorial() != null) {
                        memorialIds.add(relation.getSourceMemorial().getId());
                    }
                    if (relation.getTargetMemorial() != null) {
                        memorialIds.add(relation.getTargetMemorial().getId());
                    }
                }
            }

            // Проверяем публичность каждого мемориала
            for (Long memorialId : memorialIds) {
                Memorial memorial = memorialRepository.findById(memorialId).orElse(null);
                if (memorial == null || !memorial.isPublic()) {
                    logger.info("Memorial {} in tree {} is not public (isPublic: {}, status: {})", 
                        memorialId, treeId, 
                        memorial != null ? memorial.isPublic() : "N/A", 
                        memorial != null ? memorial.getPublicationStatus() : "NOT_FOUND");
                    return false;
                }
            }

            logger.info("All {} memorials in tree {} are public", memorialIds.size(), treeId);
            return true;
        } catch (Exception e) {
            logger.error("Error checking memorials publicity for tree {}: {}", treeId, e.getMessage(), e);
            return false;
        }
    }
    
    // Методы для создания уведомлений
    private void createTreeModerationNotification(FamilyTree tree, User sender) {
        try {
            // Находим всех администраторов
            List<User> admins = userRepository.findByRole(User.Role.ADMIN);
            
            for (User admin : admins) {
                Notification notification = new Notification();
                notification.setType(Notification.NotificationType.FAMILY_TREE_MODERATION);
                notification.setUser(admin);
                notification.setSender(sender);
                notification.setTitle("Запрос на публикацию генеалогического дерева");
                
                String message = String.format(
                    "Пользователь %s отправил генеалогическое дерево \"%s\" на модерацию.\n\n" +
                    "Требуется ваше решение об одобрении или отклонении публикации.",
                    sender.getFio() != null ? sender.getFio() : sender.getLogin(),
                    tree.getName()
                );
                
                notification.setMessage(message);
                notification.setStatus(Notification.NotificationStatus.PENDING);
                notification.setRelatedEntityId(tree.getId());
                notification.setRelatedEntityName(tree.getName());
                notification.setRead(false);
                notification.setCreatedAt(LocalDateTime.now());
                notification.setUrgent(true);
                
                notificationRepository.save(notification);
                logger.info("Уведомление о модерации дерева создано для администратора {}", admin.getLogin());
            }
        } catch (Exception e) {
            logger.error("Ошибка при создании уведомления о модерации дерева: {}", e.getMessage(), e);
        }
    }

    private void createTreeApprovalNotification(FamilyTree tree, User admin, boolean approved, String reason) {
        try {
            logger.info("=== СОЗДАНИЕ УВЕДОМЛЕНИЯ О РЕЗУЛЬТАТЕ МОДЕРАЦИИ ДЕРЕВА ===");
            logger.info("createTreeApprovalNotification: ID={}, approved={}, reason='{}'", tree.getId(), approved, reason);
            
            Notification notification = new Notification();
            notification.setUser(tree.getUser());
            notification.setSender(admin);
            
            if (approved) {
                notification.setType(Notification.NotificationType.SYSTEM);
                notification.setStatus(Notification.NotificationStatus.INFO);
                notification.setTitle("Генеалогическое дерево одобрено");
                notification.setMessage(String.format(
                    "Ваше генеалогическое дерево \"%s\" было одобрено администратором и опубликовано.\n\n" +
                    "Теперь оно доступно для просмотра всем пользователям.",
                    tree.getName()
                ));
            } else {
                notification.setType(Notification.NotificationType.SYSTEM);
                notification.setStatus(Notification.NotificationStatus.INFO);
                notification.setTitle("Генеалогическое дерево отклонено");
                
                String message = String.format(
                    "Ваше генеалогическое дерево \"%s\" было отклонено администратором.\n\n",
                    tree.getName()
                );
                
                // Добавляем причину отклонения, если она указана
                if (reason != null && !reason.trim().isEmpty()) {
                    logger.info("Добавляем причину отклонения дерева в сообщение: '{}'", reason.trim());
                    message += String.format("Причина отклонения: %s\n\n", reason.trim());
                } else {
                    logger.warn("Причина отклонения дерева пуста или не указана: reason='{}'", reason);
                }
                
                message += "Вы можете внести изменения и повторно отправить его на модерацию.";
                
                notification.setMessage(message);
                logger.info("Финальное сообщение уведомления о дереве: '{}'", message);
            }
            
            notification.setRelatedEntityId(tree.getId());
            notification.setRelatedEntityName(tree.getName());
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            
            notificationRepository.save(notification);
            logger.info("Уведомление о результате модерации дерева ({}) создано для пользователя {}", 
                    approved ? "одобрение" : "отклонение", tree.getUser().getLogin());
        } catch (Exception e) {
            logger.error("Ошибка при создании уведомления о результате модерации дерева: {}", e.getMessage(), e);
        }
    }

    // Административные методы для работы с деревьями
    @Transactional(readOnly = true)
    public Page<FamilyTree> findAllForAdmin(Pageable pageable) {
        return familyTreeRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<FamilyTree> findByPublicationStatus(FamilyTree.PublicationStatus status, Pageable pageable) {
        return familyTreeRepository.findByPublicationStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FamilyTree> searchByName(String name, Pageable pageable) {
        return familyTreeRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    @Transactional(readOnly = true)
    public long getTotalCount() {
        return familyTreeRepository.count();
    }

    @Transactional(readOnly = true)
    public long getCountByStatus(FamilyTree.PublicationStatus status) {
        return familyTreeRepository.countByPublicationStatus(status);
    }

    @Transactional(readOnly = true)
    public FamilyTree getFamilyTreeById(Long id) {
        return familyTreeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Family tree not found"));
    }

    @Transactional(readOnly = true)
    public List<Memorial> getAllMemorialsInTree(Long treeId) {
        List<MemorialRelation> relations = memorialRelationRepository.findByFamilyTreeId(treeId);
        
        Set<Memorial> memorials = new HashSet<>();
        for (MemorialRelation relation : relations) {
            if (relation.getRelationType() == MemorialRelation.RelationType.PLACEHOLDER) {
                memorials.add(relation.getSourceMemorial());
            } else {
                memorials.add(relation.getSourceMemorial());
                memorials.add(relation.getTargetMemorial());
            }
        }
        
        return new ArrayList<>(memorials);
    }

    @Transactional
    public FamilyTree approveTree(Long id) {
        FamilyTree tree = familyTreeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Family tree not found"));

        if (tree.getPublicationStatus() != FamilyTree.PublicationStatus.PENDING_MODERATION) {
            throw new RuntimeException("Family tree is not pending moderation");
        }

        tree.setPublicationStatus(FamilyTree.PublicationStatus.PUBLISHED);
        tree.setPublic(true);
        
        FamilyTree savedTree = familyTreeRepository.save(tree);
        
        // Создаем уведомление для владельца
        createTreeApprovalNotification(savedTree, null, true, null);
        
        return savedTree;
    }

    @Transactional
    public FamilyTree rejectTree(Long id, String reason) {
        FamilyTree tree = familyTreeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Family tree not found"));

        if (tree.getPublicationStatus() != FamilyTree.PublicationStatus.PENDING_MODERATION) {
            throw new RuntimeException("Family tree is not pending moderation");
        }

        tree.setPublicationStatus(FamilyTree.PublicationStatus.REJECTED);
        tree.setPublic(false);
        
        FamilyTree savedTree = familyTreeRepository.save(tree);
        
        // Создаем уведомление для владельца
        createTreeApprovalNotification(savedTree, null, false, reason);
        
        return savedTree;
    }

    @Transactional
    public FamilyTree unpublishTree(Long id, User owner) {
        FamilyTree tree = familyTreeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Family tree not found"));

        // Проверяем, что пользователь является владельцем дерева
        if (!tree.getUser().getId().equals(owner.getId())) {
            throw new RuntimeException("Only tree owner can unpublish the tree");
        }

        // Проверяем, что дерево действительно опубликовано
        if (tree.getPublicationStatus() != FamilyTree.PublicationStatus.PUBLISHED) {
            throw new RuntimeException("Family tree is not published");
        }

        // Снимаем дерево с публикации
        tree.setPublicationStatus(FamilyTree.PublicationStatus.DRAFT);
        tree.setPublic(false);
        
        FamilyTree savedTree = familyTreeRepository.save(tree);
        
        logger.info("Family tree {} unpublished by owner {}", id, owner.getLogin());
        
        return savedTree;
    }

    @Transactional
    public void deleteFamilyTree(Long id) {
        FamilyTree tree = familyTreeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Family tree not found"));

        // Удаляем все связи в дереве
        List<MemorialRelation> relations = memorialRelationRepository.findByFamilyTreeId(id);
        memorialRelationRepository.deleteAll(relations);

        // Удаляем все права доступа
        List<FamilyTreeAccess> accessList = accessRepository.findByFamilyTreeId(id);
        accessRepository.deleteAll(accessList);

        // Удаляем само дерево
        familyTreeRepository.delete(tree);
    }
} 