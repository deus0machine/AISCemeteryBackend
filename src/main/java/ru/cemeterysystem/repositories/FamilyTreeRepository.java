package ru.cemeterysystem.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.FamilyTree;
import java.util.List;

@Repository
public interface FamilyTreeRepository extends JpaRepository<FamilyTree, Long> {
    List<FamilyTree> findByUser_Id(Long userId);
    
    List<FamilyTree> findByIsPublicTrue();
    
    List<FamilyTree> findByPublicationStatus(FamilyTree.PublicationStatus status);
    
    List<FamilyTree> findByIsPublicTrueAndPublicationStatus(FamilyTree.PublicationStatus status);
    
    @Query("SELECT ft FROM FamilyTree ft WHERE ft.user.id = ?1 OR (ft.isPublic = true AND ft.publicationStatus = 'PUBLISHED')")
    List<FamilyTree> findByUserIdOrPublicAndPublished(Long userId);
    
    @Query("SELECT ft FROM FamilyTree ft WHERE ft.user.id = ?1 OR ft.isPublic = true")
    List<FamilyTree> findByUserIdOrPublic(Long userId);
    
    boolean existsByIdAndUser_Id(Long id, Long userId);
    
    // Административные методы с пагинацией
    Page<FamilyTree> findByPublicationStatus(FamilyTree.PublicationStatus status, Pageable pageable);
    
    Page<FamilyTree> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    long countByPublicationStatus(FamilyTree.PublicationStatus status);
    
    @Query("""
        SELECT DISTINCT ft FROM FamilyTree ft 
        LEFT JOIN ft.user u 
        WHERE 
            ((:currentUserId IS NULL AND ft.isPublic = true AND ft.publicationStatus = 'PUBLISHED') 
             OR (:currentUserId IS NOT NULL AND (ft.user.id = :currentUserId OR (ft.isPublic = true AND ft.publicationStatus = 'PUBLISHED'))))
            AND (:query IS NULL OR :query = '' OR LOWER(ft.name) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:ownerName IS NULL OR :ownerName = '' OR LOWER(u.fio) LIKE LOWER(CONCAT('%', :ownerName, '%')))
            AND (:startDate IS NULL OR :startDate = '' OR ft.createdAt >= CAST(:startDate AS timestamp))
            AND (:endDate IS NULL OR :endDate = '' OR ft.createdAt <= CAST(:endDate AS timestamp))
        ORDER BY ft.createdAt DESC
        """)
    List<FamilyTree> searchFamilyTrees(
        @Param("query") String query,
        @Param("ownerName") String ownerName, 
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("currentUserId") Long currentUserId
    );
} 