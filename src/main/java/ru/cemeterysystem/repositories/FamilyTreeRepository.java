package ru.cemeterysystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.FamilyTree;
import java.util.List;

@Repository
public interface FamilyTreeRepository extends JpaRepository<FamilyTree, Long> {
    List<FamilyTree> findByOwnerId(Long ownerId);
    
    List<FamilyTree> findByIsPublicTrue();
    
    @Query("SELECT ft FROM FamilyTree ft WHERE ft.owner.id = ?1 OR ft.isPublic = true")
    List<FamilyTree> findByOwnerIdOrPublic(Long ownerId);
    
    boolean existsByIdAndOwnerId(Long id, Long ownerId);
} 