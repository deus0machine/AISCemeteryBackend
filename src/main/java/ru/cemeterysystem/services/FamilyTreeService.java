package ru.cemeterysystem.services;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.dto.FamilyTreeUpdateDTO;
import ru.cemeterysystem.dto.MemorialRelationDTO;
import ru.cemeterysystem.dto.FamilyTreeDTO;
import ru.cemeterysystem.dto.TreeMemorialDTO;
import ru.cemeterysystem.mappers.FamilyTreeMapper;
import ru.cemeterysystem.models.*;
import ru.cemeterysystem.repositories.*;

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

    @Autowired
    public FamilyTreeService(
            FamilyTreeRepository familyTreeRepository,
            FamilyTreeAccessService accessService,
            FamilyTreeAccessRepository accessRepository,
            MemorialRelationRepository memorialRelationRepository,
            MemorialRepository memorialRepository,
            FamilyTreeMapper familyTreeMapper) {
        this.familyTreeRepository = familyTreeRepository;
        this.accessService = accessService;
        this.accessRepository = accessRepository;
        this.memorialRelationRepository = memorialRelationRepository;
        this.memorialRepository = memorialRepository;
        this.familyTreeMapper = familyTreeMapper;
    }

    @Transactional
    public FamilyTreeDTO createFamilyTree(FamilyTree familyTree, User user) {
        try {
            familyTree.setUser(user);
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
            return familyTreeRepository.findByIsPublicTrue()
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
            return familyTreeRepository.findByUserIdOrPublic(user.getId())
                .stream().map(familyTreeMapper::toDTO).toList();
        } catch (Exception e) {
            logger.error("Error getting accessible family trees for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to get accessible family trees", e);
        }
    }

    @Transactional
    public FamilyTreeDTO updateFamilyTree(Long id, FamilyTreeUpdateDTO updateDTO, User user) {
        try {
            FamilyTree existingTree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

            if (!accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.ADMIN)) {
                throw new RuntimeException("Insufficient permissions to update family tree");
            }
            
            existingTree.setName(updateDTO.getName());
            existingTree.setDescription(updateDTO.getDescription());
            existingTree.setPublic(updateDTO.isPublic());
            
            return familyTreeMapper.toDTO(familyTreeRepository.save(existingTree));
        } catch (Exception e) {
            logger.error("Error updating family tree {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to update family tree", e);
        }
    }

    @Transactional
    public void deleteFamilyTree(Long id, User user) {
        try {
            if (!accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.ADMIN)) {
                throw new RuntimeException("Insufficient permissions to delete family tree");
            }
            
            familyTreeRepository.deleteById(id);
        } catch (Exception e) {
            logger.error("Error deleting family tree {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete family tree", e);
        }
    }

    @Transactional(readOnly = true)
    public FamilyTreeDTO getFamilyTreeById(Long id, User user) {
        try {
            FamilyTree tree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));
                
            if (!tree.isPublic() && !accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.VIEWER)) {
                throw new RuntimeException("Insufficient permissions to view family tree");
            }
            
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
            logger.info("Checking access permissions...");
            if (!accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.EDITOR)) {
                logger.error("Access denied for user {} to tree {}", user.getId(), treeId);
                throw new RuntimeException("Insufficient permissions to add relation");
            }
            logger.info("Access granted");

            logger.info("Finding family tree with ID: {}", treeId);
            FamilyTree tree = familyTreeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Family tree not found"));
            logger.info("Found family tree: {}", tree.getName());

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
        MemorialRelation relation = memorialRelationRepository.findById(relationId)
                .orElseThrow(() -> new RuntimeException("Relation not found"));

        if (!relation.getFamilyTree().getId().equals(treeId)) {
            throw new RuntimeException("Relation does not belong to the specified family tree");
        }

        if (!accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.EDITOR)) {
            throw new RuntimeException("Insufficient permissions to delete relation");
        }

        memorialRelationRepository.deleteById(relationId);
    }

    @Transactional
    public void addMemorialToTree(Long treeId, Long memorialId, User user) {
        if (!accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.EDITOR)) {
            throw new RuntimeException("Insufficient permissions to add memorial");
        }

        FamilyTree tree = familyTreeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

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
        if (!accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.EDITOR)) {
            throw new RuntimeException("Insufficient permissions to remove memorial");
        }

        // Удаляем все связи, где участвует данный мемориал
        List<MemorialRelation> relationsToDelete = memorialRelationRepository.findByFamilyTreeId(treeId)
                .stream()
                .filter(relation -> 
                    relation.getSourceMemorial().getId().equals(memorialId) ||
                    relation.getTargetMemorial().getId().equals(memorialId))
                .collect(Collectors.toList());

        for (MemorialRelation relation : relationsToDelete) {
            memorialRelationRepository.delete(relation);
        }
    }
} 