package ru.cemeterysystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.DraftSubmission;
import ru.cemeterysystem.services.DraftSubmissionService;
import java.util.List;

@RestController
@RequestMapping("/api/draft-submissions")
public class DraftSubmissionController {
    
    @Autowired
    private DraftSubmissionService draftSubmissionService;
    
    /**
     * Получить входящие уведомления для владельца деревьев
     */
    @GetMapping("/incoming/{ownerId}")
    public ResponseEntity<List<DraftSubmission>> getIncomingSubmissions(@PathVariable Long ownerId) {
        List<DraftSubmission> submissions = draftSubmissionService.getSubmissionsForOwner(ownerId);
        return ResponseEntity.ok(submissions);
    }
    
    /**
     * Получить исходящие уведомления для редактора
     */
    @GetMapping("/outgoing/{editorId}")
    public ResponseEntity<List<DraftSubmission>> getOutgoingSubmissions(@PathVariable Long editorId) {
        List<DraftSubmission> submissions = draftSubmissionService.getSubmissionsByEditor(editorId);
        return ResponseEntity.ok(submissions);
    }
    
    /**
     * Получить конкретное уведомление о черновике по ID
     */
    @GetMapping("/{submissionId}")
    public ResponseEntity<DraftSubmission> getDraftSubmissionById(@PathVariable Long submissionId) {
        DraftSubmission submission = draftSubmissionService.getSubmissionById(submissionId);
        return ResponseEntity.ok(submission);
    }
    
    /**
     * Ответить на отправку черновика (одобрить/отклонить)
     */
    @PostMapping("/{submissionId}/respond")
    public ResponseEntity<String> respondToSubmission(
            @PathVariable Long submissionId,
            @RequestBody RespondToSubmissionRequest request) {
        
        draftSubmissionService.respondToSubmission(submissionId, request.getApproved(), request.getReviewMessage());
        return ResponseEntity.ok("Response recorded");
    }
    
    public static class RespondToSubmissionRequest {
        private Boolean approved;
        private String reviewMessage;
        
        public Boolean getApproved() { return approved; }
        public void setApproved(Boolean approved) { this.approved = approved; }
        
        public String getReviewMessage() { return reviewMessage; }
        public void setReviewMessage(String reviewMessage) { this.reviewMessage = reviewMessage; }
    }
} 