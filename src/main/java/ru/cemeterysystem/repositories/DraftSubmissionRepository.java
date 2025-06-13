package ru.cemeterysystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.DraftSubmission;
import java.util.List;

@Repository
public interface DraftSubmissionRepository extends JpaRepository<DraftSubmission, Long> {
    
    /**
     * Получить все отправки черновиков для владельца деревьев
     */
    @Query("SELECT ds FROM DraftSubmission ds " +
           "JOIN ds.draft d " +
           "JOIN d.familyTree ft " +
           "WHERE ft.user.id = :ownerId " +
           "ORDER BY ds.submittedAt DESC")
    List<DraftSubmission> findSubmissionsForOwner(@Param("ownerId") Long ownerId);
    
    /**
     * Получить все отправки черновиков от редактора
     */
    @Query("SELECT ds FROM DraftSubmission ds " +
           "JOIN ds.draft d " +
           "WHERE d.editor.id = :editorId " +
           "ORDER BY ds.submittedAt DESC")
    List<DraftSubmission> findSubmissionsByEditor(@Param("editorId") Long editorId);
    
    /**
     * Получить неотвеченные отправки для владельца
     */
    @Query("SELECT ds FROM DraftSubmission ds " +
           "JOIN ds.draft d " +
           "JOIN d.familyTree ft " +
           "WHERE ft.user.id = :ownerId AND ds.isReviewed = false " +
           "ORDER BY ds.submittedAt DESC")
    List<DraftSubmission> findPendingSubmissionsForOwner(@Param("ownerId") Long ownerId);
} 