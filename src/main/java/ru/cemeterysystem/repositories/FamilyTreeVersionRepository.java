package ru.cemeterysystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.FamilyTreeVersion;
import java.util.List;

@Repository
public interface FamilyTreeVersionRepository extends JpaRepository<FamilyTreeVersion, Long> {
    List<FamilyTreeVersion> findByFamilyTreeIdOrderByCreatedAtDesc(Long familyTreeId);
    
    FamilyTreeVersion findFirstByFamilyTreeIdOrderByCreatedAtDesc(Long familyTreeId);
    
    boolean existsByFamilyTreeIdAndVersion(Long familyTreeId, String version);
} 