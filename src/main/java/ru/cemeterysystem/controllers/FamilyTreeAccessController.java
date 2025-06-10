package ru.cemeterysystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.FamilyTreeAccess;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.FamilyTreeAccessService;
import java.util.List;

@RestController
@RequestMapping("/api/family-trees/{familyTreeId}/access")
public class FamilyTreeAccessController {
    private final FamilyTreeAccessService accessService;

    @Autowired
    public FamilyTreeAccessController(FamilyTreeAccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping("/users/{userId}")
    public ResponseEntity<FamilyTreeAccess> grantAccess(
            @PathVariable Long familyTreeId,
            @PathVariable Long userId,
            @RequestParam FamilyTreeAccess.AccessLevel accessLevel,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(accessService.grantAccess(familyTreeId, userId, accessLevel, user.getId()));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<FamilyTreeAccess> updateAccess(
            @PathVariable Long familyTreeId,
            @PathVariable Long userId,
            @RequestParam FamilyTreeAccess.AccessLevel newAccessLevel,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(accessService.updateAccess(familyTreeId, userId, newAccessLevel, user.getId()));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> revokeAccess(
            @PathVariable Long familyTreeId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {
        accessService.revokeAccess(familyTreeId, userId, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<FamilyTreeAccess>> getAccessList(
            @PathVariable Long familyTreeId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(accessService.getAccessList(familyTreeId, user.getId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<FamilyTreeAccess>> getMyAccess(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(accessService.getUserAccess(user.getId()));
    }
} 