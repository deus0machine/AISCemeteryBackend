package ru.cemeterysystem.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import java.util.List;

@Service
public class FamilyTreeService {
    private static final Logger logger = LoggerFactory.getLogger(FamilyTreeService.class);
    private final FamilyTreeRepository familyTreeRepository;

    @Autowired
    public FamilyTreeService(FamilyTreeRepository familyTreeRepository) {
        this.familyTreeRepository = familyTreeRepository;
    }

    @Transactional
    public FamilyTree createFamilyTree(FamilyTree familyTree, User owner) {
        try {
            familyTree.setOwner(owner);
            return familyTreeRepository.save(familyTree);
        } catch (Exception e) {
            logger.error("Error creating family tree: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create family tree", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FamilyTree> getFamilyTreesByOwner(User owner) {
        try {
            if (owner == null) {
                throw new IllegalArgumentException("Owner cannot be null");
            }
            logger.debug("Getting family trees for owner with ID: {}", owner.getId());
            return familyTreeRepository.findByOwnerId(owner.getId());
        } catch (Exception e) {
            logger.error("Error getting family trees for owner {}: {}", owner.getId(), e.getMessage(), e);
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
            return familyTreeRepository.findByOwnerIdOrPublic(user.getId());
        } catch (Exception e) {
            logger.error("Error getting accessible family trees for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to get accessible family trees", e);
        }
    }

    @Transactional
    public FamilyTree updateFamilyTree(FamilyTree familyTree, User owner) {
        try {
            if (!familyTreeRepository.existsByIdAndOwnerId(familyTree.getId(), owner.getId())) {
                throw new RuntimeException("Family tree not found or access denied");
            }
            return familyTreeRepository.save(familyTree);
        } catch (Exception e) {
            logger.error("Error updating family tree {}: {}", familyTree.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to update family tree", e);
        }
    }

    @Transactional
    public void deleteFamilyTree(Long id, User owner) {
        try {
            if (!familyTreeRepository.existsByIdAndOwnerId(id, owner.getId())) {
                throw new RuntimeException("Family tree not found or access denied");
            }
            familyTreeRepository.deleteById(id);
        } catch (Exception e) {
            logger.error("Error deleting family tree {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete family tree", e);
        }
    }

    @Transactional(readOnly = true)
    public FamilyTree getFamilyTreeById(Long id) {
        try {
            return familyTreeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));
        } catch (Exception e) {
            logger.error("Error getting family tree {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to get family tree", e);
        }
    }
} 