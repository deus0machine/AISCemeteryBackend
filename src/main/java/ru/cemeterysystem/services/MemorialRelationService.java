package ru.cemeterysystem.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.models.MemorialRelation;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.repositories.MemorialRelationRepository;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import java.util.List;

@Service
public class MemorialRelationService {
    private final MemorialRelationRepository memorialRelationRepository;
    private final FamilyTreeRepository familyTreeRepository;

    @Autowired
    public MemorialRelationService(
            MemorialRelationRepository memorialRelationRepository,
            FamilyTreeRepository familyTreeRepository) {
        this.memorialRelationRepository = memorialRelationRepository;
        this.familyTreeRepository = familyTreeRepository;
    }

    @Transactional
    public MemorialRelation createRelation(MemorialRelation relation) {
        // Проверяем, не существует ли уже такая связь
        if (memorialRelationRepository.existsByFamilyTreeIdAndSourceMemorialIdAndTargetMemorialId(
                relation.getFamilyTree().getId(),
                relation.getSourceMemorial().getId(),
                relation.getTargetMemorial().getId())) {
            throw new RuntimeException("Relation already exists");
        }
        return memorialRelationRepository.save(relation);
    }

    @Transactional(readOnly = true)
    public List<MemorialRelation> getRelationsByFamilyTree(Long familyTreeId) {
        return memorialRelationRepository.findByFamilyTreeId(familyTreeId);
    }

    @Transactional(readOnly = true)
    public List<MemorialRelation> getRelationsByMemorial(Long memorialId) {
        return memorialRelationRepository.findBySourceMemorialId(memorialId);
    }

    @Transactional(readOnly = true)
    public List<MemorialRelation> getRelationsByFamilyTreeAndMemorial(Long familyTreeId, Long memorialId) {
        return memorialRelationRepository.findByFamilyTreeIdAndMemorialId(familyTreeId, memorialId);
    }

    @Transactional
    public void deleteRelation(Long id) {
        memorialRelationRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllRelationsByFamilyTree(Long familyTreeId) {
        memorialRelationRepository.deleteByFamilyTreeId(familyTreeId);
    }

    @Transactional
    public MemorialRelation updateRelation(MemorialRelation relation) {
        if (!memorialRelationRepository.existsById(relation.getId())) {
            throw new RuntimeException("Relation not found");
        }
        return memorialRelationRepository.save(relation);
    }
} 