package ru.cemeterysystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.FamilyTreeDraft;
import ru.cemeterysystem.models.FamilyTreeDraft.DraftStatus;
import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyTreeDraftRepository extends JpaRepository<FamilyTreeDraft, Long> {
    
    /**
     * Найти активный черновик для редактора и дерева
     * Активный = DRAFT или REJECTED (можно продолжить редактирование)
     */
    @Query("SELECT d FROM FamilyTreeDraft d WHERE d.familyTree.id = :familyTreeId " +
           "AND d.editor.id = :editorId AND d.status IN ('DRAFT', 'REJECTED')")
    Optional<FamilyTreeDraft> findActiveDraftByTreeAndEditor(
        @Param("familyTreeId") Long familyTreeId, 
        @Param("editorId") Long editorId
    );
    
    /**
     * Найти все черновики для дерева
     */
    List<FamilyTreeDraft> findByFamilyTreeIdOrderByCreatedAtDesc(Long familyTreeId);
    
    /**
     * Найти черновики по статусу
     */
    List<FamilyTreeDraft> findByStatusOrderBySubmittedAtDesc(DraftStatus status);
    
    /**
     * Найти отправленные черновики для владельца деревьев
     */
    @Query("SELECT d FROM FamilyTreeDraft d WHERE d.familyTree.user.id = :ownerId " +
           "AND d.status = 'SUBMITTED' ORDER BY d.submittedAt DESC")
    List<FamilyTreeDraft> findSubmittedDraftsForOwner(@Param("ownerId") Long ownerId);
    
    /**
     * Найти черновики редактора
     */
    List<FamilyTreeDraft> findByEditorIdOrderByCreatedAtDesc(Long editorId);
    
    /**
     * Найти черновики редактора по статусу
     */
    List<FamilyTreeDraft> findByEditorIdAndStatusOrderByCreatedAtDesc(Long editorId, DraftStatus status);
    
    /**
     * Подсчитать количество отправленных черновиков для владельца
     */
    @Query("SELECT COUNT(d) FROM FamilyTreeDraft d WHERE d.familyTree.user.id = :ownerId " +
           "AND d.status = 'SUBMITTED'")
    long countSubmittedDraftsForOwner(@Param("ownerId") Long ownerId);
    
    /**
     * Найти черновики редактора по списку статусов
     */
    List<FamilyTreeDraft> findByEditorIdAndStatusInOrderByCreatedAtDesc(Long editorId, List<DraftStatus> statuses);
} 