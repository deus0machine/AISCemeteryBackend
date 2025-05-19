package ru.cemeterysystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.FamilyTree;
import java.util.List;

@Repository
public interface FamilyTreeRepository extends JpaRepository<FamilyTree, Long> {
    List<FamilyTree> findByUser_Id(Long userId);
    
    List<FamilyTree> findByIsPublicTrue();
    
    @Query("SELECT ft FROM FamilyTree ft WHERE ft.user.id = ?1 OR ft.isPublic = true")
    List<FamilyTree> findByUserIdOrPublic(Long userId);
    
    boolean existsByIdAndUser_Id(Long id, Long userId);
} 