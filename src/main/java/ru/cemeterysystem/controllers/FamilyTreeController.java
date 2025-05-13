package ru.cemeterysystem.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.FamilyTreeService;
import ru.cemeterysystem.services.UserService;
import java.util.List;

@RestController
@RequestMapping("/api/family-trees")
public class FamilyTreeController {
    private static final Logger logger = LoggerFactory.getLogger(FamilyTreeController.class);
    private final FamilyTreeService familyTreeService;
    private final UserService userService;

    @Autowired
    public FamilyTreeController(FamilyTreeService familyTreeService, UserService userService) {
        this.familyTreeService = familyTreeService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createFamilyTree(
            @RequestBody FamilyTree familyTree,
            @AuthenticationPrincipal User user) {
        try {
            logger.debug("Creating family tree for user: {}", user.getId());
            return ResponseEntity.ok(familyTreeService.createFamilyTree(familyTree, user));
        } catch (Exception e) {
            logger.error("Error creating family tree: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to create family tree: " + e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyFamilyTrees(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                logger.error("User is not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
            }
            User user = userService.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
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
    public ResponseEntity<?> getAccessibleFamilyTrees(@AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                logger.error("User is not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
            }
            return ResponseEntity.ok(familyTreeService.getAccessibleFamilyTrees(user));
        } catch (Exception e) {
            logger.error("Error getting accessible family trees: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get accessible family trees: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFamilyTree(
            @PathVariable Long id,
            @RequestBody FamilyTree familyTree,
            @AuthenticationPrincipal User user) {
        try {
            familyTree.setId(id);
            return ResponseEntity.ok(familyTreeService.updateFamilyTree(familyTree, user));
        } catch (Exception e) {
            logger.error("Error updating family tree {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to update family tree: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFamilyTree(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        try {
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
            return ResponseEntity.ok(familyTreeService.getFamilyTreeById(id));
        } catch (Exception e) {
            logger.error("Error getting family tree {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get family tree: " + e.getMessage());
        }
    }
} 