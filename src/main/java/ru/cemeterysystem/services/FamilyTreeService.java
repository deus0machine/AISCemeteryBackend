package ru.cemeterysystem.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.dto.FamilyTreeUpdateDTO;
import ru.cemeterysystem.dto.MemorialRelationDTO;
import ru.cemeterysystem.models.*;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import ru.cemeterysystem.repositories.FamilyTreeAccessRepository;
import ru.cemeterysystem.repositories.MemorialRelationRepository;
import ru.cemeterysystem.repositories.MemorialRepository;

import java.util.List;

@Service
public class FamilyTreeService {
    private static final Logger logger = LoggerFactory.getLogger(FamilyTreeService.class);
    private final FamilyTreeRepository familyTreeRepository;
    private final FamilyTreeAccessService accessService;
    private final FamilyTreeAccessRepository accessRepository;
    private final MemorialRelationRepository memorialRelationRepository;
    private final MemorialRepository memorialRepository;

    @Autowired
    public FamilyTreeService(
            FamilyTreeRepository familyTreeRepository,
            FamilyTreeAccessService accessService,
            FamilyTreeAccessRepository accessRepository,
            MemorialRelationRepository memorialRelationRepository,
            MemorialRepository memorialRepository) {
        this.familyTreeRepository = familyTreeRepository;
        this.accessService = accessService;
        this.accessRepository = accessRepository;
        this.memorialRelationRepository = memorialRelationRepository;
        this.memorialRepository = memorialRepository;
    }

    @Transactional
    public FamilyTree createFamilyTree(FamilyTree familyTree, User user) {
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
            
            return savedTree;
        } catch (Exception e) {
            logger.error("Error creating family tree: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create family tree", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FamilyTree> getFamilyTreesByOwner(User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User cannot be null");
            }
            logger.debug("Getting family trees for user with ID: {}", user.getId());
            return familyTreeRepository.findByUser_Id(user.getId());
        } catch (Exception e) {
            logger.error("Error getting family trees for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to get family trees", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FamilyTree> getPublicFamilyTrees() {
        try {
            return familyTreeRepository.findByIsPublicTrue();
        } catch (Exception e) {
            logger.error("Error getting public family trees: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get public family trees", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FamilyTree> getAccessibleFamilyTrees(User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User cannot be null");
            }
            return familyTreeRepository.findByUserIdOrPublic(user.getId());
        } catch (Exception e) {
            logger.error("Error getting accessible family trees for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to get accessible family trees", e);
        }
    }

    @Transactional
    public FamilyTree updateFamilyTree(Long id, FamilyTreeUpdateDTO updateDTO, User user) {
        try {
            FamilyTree existingTree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

            if (!accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.ADMIN)) {
                throw new RuntimeException("Insufficient permissions to update family tree");
            }
            
            existingTree.setName(updateDTO.getName());
            existingTree.setDescription(updateDTO.getDescription());
            existingTree.setPublic(updateDTO.isPublic());
            
            return familyTreeRepository.save(existingTree);
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
    public FamilyTree getFamilyTreeById(Long id, User user) {
        try {
            FamilyTree tree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));
                
            if (!tree.isPublic() && !accessService.hasAccess(id, user.getId(), FamilyTreeAccess.AccessLevel.VIEWER)) {
                throw new RuntimeException("Insufficient permissions to view family tree");
            }
            
            return tree;
        } catch (Exception e) {
            logger.error("Error getting family tree {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to get family tree", e);
        }
    }
    @Transactional
    public MemorialRelation addRelation(Long treeId, MemorialRelationDTO relationDTO, User user) {
        if (!accessService.hasAccess(treeId, user.getId(), FamilyTreeAccess.AccessLevel.EDITOR)) {
            throw new RuntimeException("Insufficient permissions to add relation");
        }

        FamilyTree tree = familyTreeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

        Memorial sourceMemorial = memorialRepository.findById(relationDTO.getSourceMemorialId())
                .orElseThrow(() -> new RuntimeException("Source memorial not found"));
        Memorial targetMemorial = memorialRepository.findById(relationDTO.getTargetMemorialId())
                .orElseThrow(() -> new RuntimeException("Target memorial not found"));

        MemorialRelation relation = new MemorialRelation();
        relation.setFamilyTree(tree);
        relation.setSourceMemorial(sourceMemorial);
        relation.setTargetMemorial(targetMemorial);
        relation.setRelationType(MemorialRelation.RelationType.valueOf(relationDTO.getRelationType().toUpperCase()));

        return memorialRelationRepository.save(relation);
    }

    @Transactional(readOnly = true)
    public List<MemorialRelation> getRelations(Long treeId) {
        return memorialRelationRepository.findByFamilyTreeId(treeId);
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
} 