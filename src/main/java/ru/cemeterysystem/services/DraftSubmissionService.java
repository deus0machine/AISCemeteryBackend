package ru.cemeterysystem.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.models.DraftSubmission;
import ru.cemeterysystem.repositories.DraftSubmissionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DraftSubmissionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DraftSubmissionService.class);
    
    @Autowired
    private DraftSubmissionRepository draftSubmissionRepository;
    
    @Autowired
    private FamilyTreeDraftService familyTreeDraftService;
    
    /**
     * Получить все отправки черновиков для владельца деревьев
     */
    @Transactional(readOnly = true)
    public List<DraftSubmission> getSubmissionsForOwner(Long ownerId) {
        return draftSubmissionRepository.findSubmissionsForOwner(ownerId);
    }
    
    /**
     * Получить все отправки черновиков от редактора
     */
    @Transactional(readOnly = true)
    public List<DraftSubmission> getSubmissionsByEditor(Long editorId) {
        return draftSubmissionRepository.findSubmissionsByEditor(editorId);
    }
    
    /**
     * Получить неотвеченные отправки для владельца
     */
    @Transactional(readOnly = true)
    public List<DraftSubmission> getPendingSubmissionsForOwner(Long ownerId) {
        return draftSubmissionRepository.findPendingSubmissionsForOwner(ownerId);
    }
    
    /**
     * Получить конкретную отправку черновика по ID
     */
    @Transactional(readOnly = true)
    public DraftSubmission getSubmissionById(Long submissionId) {
        return draftSubmissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("Submission not found with ID: " + submissionId));
    }
    
    /**
     * Ответить на отправку черновика
     */
    @Transactional
    public void respondToSubmission(Long submissionId, Boolean approved, String reviewMessage) {
        DraftSubmission submission = draftSubmissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("Submission not found"));
        
        if (submission.getIsReviewed()) {
            throw new RuntimeException("Submission already reviewed");
        }
        
        submission.setIsReviewed(true);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setReviewMessage(reviewMessage);
        submission.setReviewStatus(approved ? 
            DraftSubmission.ReviewStatus.APPROVED : 
            DraftSubmission.ReviewStatus.REJECTED);
        
        draftSubmissionRepository.save(submission);
        
        logger.info("Submission {} {} by owner with message: {}", 
                   submissionId, approved ? "approved" : "rejected", reviewMessage);
        
        // Если одобрено, применяем изменения к оригинальному дереву
        if (approved) {
            try {
                familyTreeDraftService.applyDraftChanges(
                    submission.getDraft().getId(), 
                    reviewMessage
                );
                logger.info("Draft {} changes applied to original tree", submission.getDraft().getId());
            } catch (Exception e) {
                logger.error("Error applying draft changes to original tree: {}", e.getMessage(), e);
                throw new RuntimeException("Error applying changes to original tree: " + e.getMessage());
            }
        } else {
            // Если отклонено, отклоняем черновик
            try {
                familyTreeDraftService.rejectDraftChanges(
                    submission.getDraft().getId(), 
                    reviewMessage
                );
                logger.info("Draft {} rejected", submission.getDraft().getId());
            } catch (Exception e) {
                logger.error("Error rejecting draft: {}", e.getMessage(), e);
                throw new RuntimeException("Error rejecting draft: " + e.getMessage());
            }
        }
    }
} 