package ru.cemeterysystem.controllers;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.dto.FamilyTreeDTO;
import ru.cemeterysystem.dto.FamilyTreeUpdateDTO;
import ru.cemeterysystem.dto.MemorialRelationDTO;
import ru.cemeterysystem.dto.TreeMemorialDTO;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.MemorialRelation;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.FamilyTreeService;
import ru.cemeterysystem.services.UserService;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/family-trees")
@RequiredArgsConstructor
public class FamilyTreeController {
    private static final Logger logger = LoggerFactory.getLogger(FamilyTreeController.class);
    private final FamilyTreeService familyTreeService;
    private final UserService userService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        return userService.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<?> createFamilyTree(@RequestBody FamilyTree familyTree) {
        try {
            User user = getCurrentUser();
            logger.debug("Creating family tree for user: {}", user.getId());
            return ResponseEntity.ok(familyTreeService.createFamilyTree(familyTree, user));
        } catch (Exception e) {
            logger.error("Error creating family tree: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to create family tree: " + e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyFamilyTrees() {
        try {
            User user = getCurrentUser();
            logger.debug("Getting family trees for user: {}", user.getLogin());
            return ResponseEntity.ok(familyTreeService.getFamilyTreesByOwner(user));
        } catch (Exception e) {
            logger.error("Error getting family trees: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get family trees: " + e.getMessage());
        }
    }

    @GetMapping("/public")
    public ResponseEntity<?> getPublicFamilyTrees() {
        try {
            return ResponseEntity.ok(familyTreeService.getPublicFamilyTrees());
        } catch (Exception e) {
            logger.error("Error getting public family trees: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get public family trees: " + e.getMessage());
        }
    }

    @GetMapping("/accessible")
    public ResponseEntity<?> getAccessibleFamilyTrees() {
        try {
            User user = getCurrentUser();
            return ResponseEntity.ok(familyTreeService.getAccessibleFamilyTrees(user));
        } catch (Exception e) {
            logger.error("Error getting accessible family trees: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get accessible family trees: " + e.getMessage());
        }
    }

    @GetMapping("/shared")
    public ResponseEntity<?> getSharedFamilyTrees() {
        try {
            User user = getCurrentUser();
            return ResponseEntity.ok(familyTreeService.getSharedFamilyTrees(user));
        } catch (Exception e) {
            logger.error("Error getting shared family trees: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get shared family trees: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchFamilyTrees(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "false") boolean myOnly) {
        try {
            User user = getCurrentUser();
            logger.debug("Searching family trees with query: {}, ownerName: {}, startDate: {}, endDate: {}, myOnly: {}", 
                query, ownerName, startDate, endDate, myOnly);
            return ResponseEntity.ok(familyTreeService.searchFamilyTrees(query, ownerName, startDate, endDate, myOnly ? user : null));
        } catch (Exception e) {
            logger.error("Error searching family trees: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to search family trees: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<FamilyTreeDTO> updateFamilyTree(
            @PathVariable Long id,
            @RequestBody FamilyTreeUpdateDTO updateDTO) {
        try {
            User user = getCurrentUser();
            FamilyTreeDTO updatedTree = familyTreeService.updateFamilyTree(id, updateDTO, user);
            return ResponseEntity.ok(updatedTree);
        } catch (RuntimeException e) {
            logger.error("Error updating family tree {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFamilyTree(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            familyTreeService.deleteFamilyTree(id, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting family tree {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to delete family tree: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFamilyTree(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            return ResponseEntity.ok(familyTreeService.getFamilyTreeById(id, user));
        } catch (Exception e) {
            logger.error("Error getting family tree {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get family tree: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/full-data")
    public ResponseEntity<?> getFamilyTreeFullData(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            logger.debug("Getting full data for family tree {} for user {}", id, user.getId());
            
            FamilyTreeDTO familyTree = familyTreeService.getFamilyTreeById(id, user);
            List<MemorialRelationDTO> relations = familyTreeService.getRelations(id);
            List<TreeMemorialDTO> memorials = familyTreeService.getTreeMemorials(id);
            
            // Создаем объект с полными данными
            Map<String, Object> fullData = new HashMap<>();
            fullData.put("familyTree", familyTree);
            fullData.put("relations", relations);
            fullData.put("memorials", memorials);
            
            return ResponseEntity.ok(fullData);
        } catch (Exception e) {
            logger.error("Error getting full data for family tree {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Произошла непредвиденная ошибка"));
        }
    }

    // API эндпоинты для модерации семейных деревьев
    @PostMapping("/{id}/send-for-moderation")
    public ResponseEntity<?> sendFamilyTreeForModeration(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            logger.debug("Sending family tree {} for moderation by user: {}", id, user.getLogin());
            FamilyTreeDTO result = familyTreeService.sendForModeration(id, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error sending family tree {} for moderation: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to send family tree for moderation: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<?> unpublishFamilyTree(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            logger.debug("Unpublishing family tree {} by user: {}", id, user.getLogin());
            FamilyTree result = familyTreeService.unpublishTree(id, user);
            return ResponseEntity.ok(familyTreeService.getFamilyTreeById(id, user));
        } catch (Exception e) {
            logger.error("Error unpublishing family tree {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to unpublish family tree: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/submit-changes-for-moderation")
    public ResponseEntity<?> submitTreeChangesForModeration(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String message) {
        try {
            User user = getCurrentUser();
            logger.debug("Submitting changes for moderation for family tree {} by user: {}", id, user.getLogin());
            
            familyTreeService.submitTreeChangesForModeration(id, user, name, description, message);
            
            return ResponseEntity.ok().body("Changes submitted for moderation successfully");
        } catch (Exception e) {
            logger.error("Error submitting tree changes for moderation {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to submit changes for moderation: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/approve-changes")
    public ResponseEntity<?> approveTreeChanges(
            @PathVariable Long id,
            @RequestParam(required = false) Long notificationId) {
        try {
            User admin = getCurrentUser();
            logger.debug("Approving changes for family tree {} by admin: {}", id, admin.getLogin());
            
            familyTreeService.approveTreeChanges(id, admin, notificationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Изменения дерева одобрены успешно");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error approving tree changes {}: {}", id, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при одобрении изменений: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/{id}/reject-changes")
    public ResponseEntity<?> rejectTreeChanges(
            @PathVariable Long id,
            @RequestParam(required = false) Long notificationId,
            @RequestParam String reason) {
        try {
            User admin = getCurrentUser();
            logger.debug("Rejecting changes for family tree {} by admin: {}", id, admin.getLogin());
            
            familyTreeService.rejectTreeChanges(id, admin, notificationId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Изменения дерева отклонены");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error rejecting tree changes {}: {}", id, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при отклонении изменений: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveFamilyTree(@PathVariable Long id) {
        try {
            User admin = getCurrentUser();
            logger.debug("Approving family tree {} by admin: {}", id, admin.getLogin());
            FamilyTreeDTO result = familyTreeService.approveTree(id, admin);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error approving family tree {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to approve family tree: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectFamilyTree(@PathVariable Long id, @RequestBody String reason) {
        try {
            User admin = getCurrentUser();
            logger.debug("Rejecting family tree {} by admin: {} with reason: {}", id, admin.getLogin(), reason);
            FamilyTreeDTO result = familyTreeService.rejectTree(id, admin, reason);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error rejecting family tree {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to reject family tree: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/relations")
    public ResponseEntity<?> addRelation(
            @PathVariable Long id,
            @RequestBody MemorialRelationDTO relationDTO) {
        logger.info("=== START addRelation ===");
        logger.info("Family Tree ID: {}", id);
        logger.info("Incoming MemorialRelationDTO: {}", relationDTO);
        logger.info("RelationDTO.sourceMemorial: {}", relationDTO.getSourceMemorial());
        logger.info("RelationDTO.targetMemorial: {}", relationDTO.getTargetMemorial());
        logger.info("RelationDTO.relationType: {}", relationDTO.getRelationType());
        
        try {
            User user = getCurrentUser();
            logger.info("Current user: {}", user.getLogin());
            
            logger.info("Calling familyTreeService.addRelation...");
            MemorialRelation relation = familyTreeService.addRelation(id, relationDTO, user);
            logger.info("Successfully created relation with ID: {}", relation.getId());
            
            logger.info("Returning response with relation: {}", relation);
            logger.info("=== END addRelation SUCCESS ===");
            return ResponseEntity.ok(relation);
        } catch (Exception e) {
            logger.error("=== ERROR in addRelation ===");
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Exception message: {}", e.getMessage());
            logger.error("Full stack trace:", e);
            logger.error("=== END addRelation ERROR ===");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add relation: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/relations/{relationId}")
    public ResponseEntity<?> deleteRelation(@PathVariable Long id, @PathVariable Long relationId) {
        try {
            User user = getCurrentUser();
            familyTreeService.deleteRelation(id, relationId, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting relation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete relation: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/relations/{relationId}")
    public ResponseEntity<?> updateRelation(
            @PathVariable Long id,
            @PathVariable Long relationId,
            @RequestBody MemorialRelationDTO relationDTO) {
        logger.info("=== START updateRelation ===");
        logger.info("Family Tree ID: {}", id);
        logger.info("Relation ID: {}", relationId);
        logger.info("Incoming MemorialRelationDTO: {}", relationDTO);
        
        try {
            User user = getCurrentUser();
            logger.info("Current user: {}", user.getLogin());
            
            // Устанавливаем ID связи из URL
            relationDTO.setId(relationId);
            relationDTO.setFamilyTreeId(id);
            
            logger.info("Calling familyTreeService.updateRelation...");
            MemorialRelation updatedRelation = familyTreeService.updateRelation(id, relationDTO, user);
            logger.info("Successfully updated relation with ID: {}", updatedRelation.getId());
            
            logger.info("Returning response with updated relation: {}", updatedRelation);
            logger.info("=== END updateRelation SUCCESS ===");
            return ResponseEntity.ok(updatedRelation);
        } catch (Exception e) {
            logger.error("=== ERROR in updateRelation ===");
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Exception message: {}", e.getMessage());
            logger.error("Full stack trace:", e);
            logger.error("=== END updateRelation ERROR ===");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update relation: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/relations")
    public ResponseEntity<?> getRelations(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(familyTreeService.getRelations(id));
        } catch (Exception e) {
            logger.error("Error getting relations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get relations: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/memorials/{memorialId}")
    public ResponseEntity<?> addMemorialToTree(
            @PathVariable Long id, 
            @PathVariable Long memorialId) {
        try {
            User user = getCurrentUser();
            familyTreeService.addMemorialToTree(id, memorialId, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error adding memorial {} to tree {}: {}", memorialId, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add memorial to tree: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/memorials")
    public ResponseEntity<?> getTreeMemorials(@PathVariable Long id) {
        try {
            List<TreeMemorialDTO> memorials = familyTreeService.getTreeMemorials(id);
            return ResponseEntity.ok(memorials);
        } catch (Exception e) {
            logger.error("Error getting memorials for tree {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get tree memorials: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/memorials/{memorialId}")
    public ResponseEntity<?> removeMemorialFromTree(
            @PathVariable Long id, 
            @PathVariable Long memorialId) {
        try {
            User user = getCurrentUser();
            familyTreeService.removeMemorialFromTree(id, memorialId, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error removing memorial {} from tree {}: {}", memorialId, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to remove memorial from tree: " + e.getMessage());
        }
    }
} 