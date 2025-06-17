package ru.cemeterysystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.MemorialRelation;
import java.util.List;

@Repository
public interface MemorialRelationRepository extends JpaRepository<MemorialRelation, Long> {
    List<MemorialRelation> findByFamilyTreeId(Long familyTreeId);
    
    List<MemorialRelation> findBySourceMemorialId(Long memorialId);
    
    List<MemorialRelation> findByTargetMemorialId(Long memorialId);
    
    @Query("SELECT mr FROM MemorialRelation mr WHERE mr.familyTree.id = ?1 AND (mr.sourceMemorial.id = ?2 OR mr.targetMemorial.id = ?2)")
    List<MemorialRelation> findByFamilyTreeIdAndMemorialId(Long familyTreeId, Long memorialId);
    
    void deleteByFamilyTreeId(Long familyTreeId);
    
    boolean existsByFamilyTreeIdAndSourceMemorialIdAndTargetMemorialId(
        Long familyTreeId, Long sourceMemorialId, Long targetMemorialId);
    
    // Проверяем, есть ли мемориал в каком-либо дереве
    @Query("SELECT DISTINCT mr.familyTree.id FROM MemorialRelation mr WHERE mr.sourceMemorial.id = ?1 OR mr.targetMemorial.id = ?1")
    List<Long> findFamilyTreeIdsByMemorialId(Long memorialId);
    
    // Проверяем, есть ли мемориал в конкретном дереве
    @Query("SELECT COUNT(mr) > 0 FROM MemorialRelation mr WHERE (mr.sourceMemorial.id = ?1 OR mr.targetMemorial.id = ?1) AND mr.familyTree.id = ?2")
    boolean existsByMemorialIdAndFamilyTreeId(Long memorialId, Long familyTreeId);
} 