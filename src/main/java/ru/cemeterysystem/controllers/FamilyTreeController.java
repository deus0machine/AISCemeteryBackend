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
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.MemorialRelation;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.FamilyTreeService;
import ru.cemeterysystem.services.UserService;
import java.util.List;

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
    @PostMapping("/{id}/relations")
    public ResponseEntity<?> addRelation(
            @PathVariable Long id,
            @RequestBody MemorialRelationDTO relationDTO) {
        try {
            User user = getCurrentUser();
            MemorialRelation relation = familyTreeService.addRelation(id, relationDTO, user);
            return ResponseEntity.ok(relation);
        } catch (Exception e) {
            logger.error("Error adding relation: {}", e.getMessage(), e);
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
} 