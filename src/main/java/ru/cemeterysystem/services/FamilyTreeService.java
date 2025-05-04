package ru.cemeterysystem.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import java.util.List;

@Service
public class FamilyTreeService {
    private final FamilyTreeRepository familyTreeRepository;

    @Autowired
    public FamilyTreeService(FamilyTreeRepository familyTreeRepository) {
        this.familyTreeRepository = familyTreeRepository;
    }

    @Transactional
    public FamilyTree createFamilyTree(FamilyTree familyTree, User owner) {
        familyTree.setOwner(owner);
        return familyTreeRepository.save(familyTree);
    }

    @Transactional(readOnly = true)
    public List<FamilyTree> getFamilyTreesByOwner(User owner) {
        return familyTreeRepository.findByOwnerId(owner.getId());
    }

    @Transactional(readOnly = true)
    public List<FamilyTree> getPublicFamilyTrees() {
        return familyTreeRepository.findByIsPublicTrue();
    }

    @Transactional(readOnly = true)
    public List<FamilyTree> getAccessibleFamilyTrees(User user) {
        return familyTreeRepository.findByOwnerIdOrPublic(user.getId());
    }

    @Transactional
    public FamilyTree updateFamilyTree(FamilyTree familyTree, User owner) {
        if (!familyTreeRepository.existsByIdAndOwnerId(familyTree.getId(), owner.getId())) {
            throw new RuntimeException("Family tree not found or access denied");
        }
        return familyTreeRepository.save(familyTree);
    }

    @Transactional
    public void deleteFamilyTree(Long id, User owner) {
        if (!familyTreeRepository.existsByIdAndOwnerId(id, owner.getId())) {
            throw new RuntimeException("Family tree not found or access denied");
        }
        familyTreeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public FamilyTree getFamilyTreeById(Long id) {
        return familyTreeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Family tree not found"));
    }
} 