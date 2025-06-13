package ru.cemeterysystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.FamilyTreeDraft;
import ru.cemeterysystem.services.FamilyTreeDraftService;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.UserService;
import ru.cemeterysystem.dto.TreeMemorialDTO;
import ru.cemeterysystem.dto.MemorialRelationDTO;
import ru.cemeterysystem.dto.FamilyTreeDraftDTO;

@RestController
@RequestMapping("/api/family-tree-drafts")
public class FamilyTreeDraftController {
    
    @Autowired
    private FamilyTreeDraftService draftService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Получить или создать активный черновик для редактирования дерева
     */
    @GetMapping("/tree/{familyTreeId}/editor/{editorId}")
    public ResponseEntity<FamilyTreeDraft> getOrCreateActiveDraft(
            @PathVariable Long familyTreeId,
            @PathVariable Long editorId) {
        try {
            FamilyTreeDraft draft = draftService.getOrCreateActiveDraft(familyTreeId, editorId);
            return ResponseEntity.ok(draft);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Обновить данные в черновике
     */
    @PutMapping("/{draftId}")
    public ResponseEntity<FamilyTreeDraft> updateDraft(
            @PathVariable Long draftId,
            @RequestBody UpdateDraftRequest request) {
        try {
            FamilyTreeDraft draft = draftService.updateDraft(
                draftId, 
                request.getName(), 
                request.getDescription(), 
                request.getIsPublic()
            );
            return ResponseEntity.ok(draft);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Отправить черновик на рассмотрение
     */
    @PostMapping("/{draftId}/submit")
    public ResponseEntity<FamilyTreeDraft> submitDraft(
            @PathVariable Long draftId,
            @RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            FamilyTreeDraft draft = draftService.submitDraft(draftId, message);
            return ResponseEntity.ok(draft);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Получить черновики для рассмотрения владельцем
     */
    @GetMapping("/owner/{ownerId}/submitted")
    public ResponseEntity<List<FamilyTreeDraft>> getSubmittedDraftsForOwner(@PathVariable Long ownerId) {
        try {
            List<FamilyTreeDraft> drafts = draftService.getSubmittedDraftsForOwner(ownerId);
            return ResponseEntity.ok(drafts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Одобрить черновик
     */
    @PostMapping("/{draftId}/approve")
    public ResponseEntity<FamilyTreeDraft> approveDraft(
            @PathVariable Long draftId,
            @RequestBody ReviewRequest request) {
        try {
            FamilyTreeDraft draft = draftService.approveDraft(draftId, request.getMessage(), request.getReviewerId());
            return ResponseEntity.ok(draft);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Отклонить черновик
     */
    @PostMapping("/{draftId}/reject")
    public ResponseEntity<FamilyTreeDraft> rejectDraft(
            @PathVariable Long draftId,
            @RequestBody ReviewRequest request) {
        try {
            FamilyTreeDraft draft = draftService.rejectDraft(draftId, request.getMessage(), request.getReviewerId());
            return ResponseEntity.ok(draft);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Добавить мемориал в черновик дерева
     */
    @PostMapping("/tree/{familyTreeId}/memorials/{memorialId}")
    public ResponseEntity<String> addMemorialToDraft(
            @PathVariable Long familyTreeId,
            @PathVariable Long memorialId) {
        try {
            User user = getCurrentUser();
            draftService.addMemorialToDraft(familyTreeId, memorialId, user.getId());
            return ResponseEntity.ok("Memorial added to draft successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add memorial to draft: " + e.getMessage());
        }
    }
    
    /**
     * Удалить мемориал из черновика дерева
     */
    @DeleteMapping("/tree/{familyTreeId}/memorials/{memorialId}")
    public ResponseEntity<String> removeMemorialFromDraft(
            @PathVariable Long familyTreeId,
            @PathVariable Long memorialId) {
        try {
            User user = getCurrentUser();
            draftService.removeMemorialFromDraft(familyTreeId, memorialId, user.getId());
            return ResponseEntity.ok("Memorial removed from draft successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to remove memorial from draft: " + e.getMessage());
        }
    }
    
    /**
     * Добавить связь в черновик дерева
     */
    @PostMapping("/tree/{familyTreeId}/relations")
    public ResponseEntity<String> addRelationToDraft(
            @PathVariable Long familyTreeId,
            @RequestBody MemorialRelationDTO relationDTO) {
        try {
            User user = getCurrentUser();
            draftService.addRelationToDraft(familyTreeId, relationDTO, user.getId());
            return ResponseEntity.ok("Relation added to draft successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add relation to draft: " + e.getMessage());
        }
    }
    
    /**
     * Удалить связь из черновика дерева
     */
    @DeleteMapping("/tree/{familyTreeId}/relations/{relationId}")
    public ResponseEntity<String> removeRelationFromDraft(
            @PathVariable Long familyTreeId,
            @PathVariable Long relationId) {
        try {
            User user = getCurrentUser();
            draftService.removeRelationFromDraft(familyTreeId, relationId, user.getId());
            return ResponseEntity.ok("Relation removed from draft successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to remove relation from draft: " + e.getMessage());
        }
    }
    
    /**
     * Получить мемориалы черновика
     */
    @GetMapping("/tree/{familyTreeId}/memorials")
    public ResponseEntity<List<TreeMemorialDTO>> getDraftMemorials(@PathVariable Long familyTreeId) {
        try {
            User user = getCurrentUser();
            List<TreeMemorialDTO> memorials = draftService.getDraftMemorials(familyTreeId, user.getId());
            return ResponseEntity.ok(memorials);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Получить связи черновика
     */
    @GetMapping("/tree/{familyTreeId}/relations")
    public ResponseEntity<List<MemorialRelationDTO>> getDraftRelations(@PathVariable Long familyTreeId) {
        try {
            User user = getCurrentUser();
            List<MemorialRelationDTO> relations = draftService.getDraftRelations(familyTreeId, user.getId());
            return ResponseEntity.ok(relations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Получить связи черновика по ID черновика
     */
    @GetMapping("/{draftId}/relations")
    public ResponseEntity<List<MemorialRelationDTO>> getDraftRelationsByDraftId(@PathVariable Long draftId) {
        try {
            List<MemorialRelationDTO> relations = draftService.getDraftRelationsByDraftId(draftId);
            return ResponseEntity.ok(relations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Получить черновики редактора для отображения в "Мои деревья"
     */
    @GetMapping("/my")
    public ResponseEntity<List<FamilyTreeDraftDTO>> getMyDrafts() {
        try {
            User user = getCurrentUser();
            List<FamilyTreeDraftDTO> drafts = draftService.getMyDrafts(user.getId());
            return ResponseEntity.ok(drafts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        return userService.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    // DTO классы для запросов
    public static class UpdateDraftRequest {
        private String name;
        private String description;
        private Boolean isPublic;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    }
    
    public static class ReviewRequest {
        private String message;
        private Long reviewerId;
        
        // Getters and setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Long getReviewerId() { return reviewerId; }
        public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    }
} 