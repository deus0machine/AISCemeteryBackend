package ru.cemeterysystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.FamilyTreeAccess;
import ru.cemeterysystem.models.FamilyTreeAccess.AccessLevel;
import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyTreeAccessRepository extends JpaRepository<FamilyTreeAccess, Long> {
    List<FamilyTreeAccess> findByFamilyTreeId(Long familyTreeId);
    
    List<FamilyTreeAccess> findByUserId(Long userId);
    
    Optional<FamilyTreeAccess> findByFamilyTreeIdAndUserId(Long familyTreeId, Long userId);
    
    @Query("SELECT fa FROM FamilyTreeAccess fa WHERE fa.familyTree.id = ?1 AND fa.accessLevel = ?2")
    List<FamilyTreeAccess> findByFamilyTreeIdAndAccessLevel(Long familyTreeId, AccessLevel accessLevel);
    
    void deleteByFamilyTreeIdAndUserId(Long familyTreeId, Long userId);
    
    boolean existsByFamilyTreeIdAndUserId(Long familyTreeId, Long userId);
} 