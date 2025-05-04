package ru.cemeterysystem.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.MemorialRelation;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import ru.cemeterysystem.repositories.MemorialRelationRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FamilyTreeStatisticsService {
    private final FamilyTreeRepository familyTreeRepository;
    private final MemorialRelationRepository memorialRelationRepository;

    @Autowired
    public FamilyTreeStatisticsService(
            FamilyTreeRepository familyTreeRepository,
            MemorialRelationRepository memorialRelationRepository) {
        this.familyTreeRepository = familyTreeRepository;
        this.memorialRelationRepository = memorialRelationRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTreeStatistics(Long familyTreeId) {
        FamilyTree tree = familyTreeRepository.findById(familyTreeId)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));
        
        List<MemorialRelation> relations = memorialRelationRepository.findByFamilyTreeId(familyTreeId);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Общее количество мемориалов в дереве
        statistics.put("totalMemorials", countUniqueMemorials(relations));
        
        // Количество связей по типам
        Map<MemorialRelation.RelationType, Integer> relationTypeCounts = new HashMap<>();
        for (MemorialRelation relation : relations) {
            relationTypeCounts.merge(relation.getRelationType(), 1, Integer::sum);
        }
        statistics.put("relationTypeCounts", relationTypeCounts);
        
        // Глубина дерева (максимальное количество поколений)
        statistics.put("treeDepth", calculateTreeDepth(relations));
        
        // Количество веток (отдельных семейных линий)
        statistics.put("numberOfBranches", calculateNumberOfBranches(relations));
        
        return statistics;
    }

    private int countUniqueMemorials(List<MemorialRelation> relations) {
        return (int) relations.stream()
                .flatMap(r -> List.of(r.getSourceMemorial().getId(), r.getTargetMemorial().getId()).stream())
                .distinct()
                .count();
    }

    private int calculateTreeDepth(List<MemorialRelation> relations) {
        // TODO: Реализовать алгоритм подсчета глубины дерева
        return 0;
    }

    private int calculateNumberOfBranches(List<MemorialRelation> relations) {
        // TODO: Реализовать алгоритм подсчета количества веток
        return 0;
    }
} 