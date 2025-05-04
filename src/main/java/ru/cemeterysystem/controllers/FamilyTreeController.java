package ru.cemeterysystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.FamilyTreeService;
import java.util.List;

@RestController
@RequestMapping("/api/family-trees")
public class FamilyTreeController {
    private final FamilyTreeService familyTreeService;

    @Autowired
    public FamilyTreeController(FamilyTreeService familyTreeService) {
        this.familyTreeService = familyTreeService;
    }

    @PostMapping
    public ResponseEntity<FamilyTree> createFamilyTree(
            @RequestBody FamilyTree familyTree,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(familyTreeService.createFamilyTree(familyTree, user));
    }

    @GetMapping("/my")
    public ResponseEntity<List<FamilyTree>> getMyFamilyTrees(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(familyTreeService.getFamilyTreesByOwner(user));
    }

    @GetMapping("/public")
    public ResponseEntity<List<FamilyTree>> getPublicFamilyTrees() {
        return ResponseEntity.ok(familyTreeService.getPublicFamilyTrees());
    }

    @GetMapping("/accessible")
    public ResponseEntity<List<FamilyTree>> getAccessibleFamilyTrees(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(familyTreeService.getAccessibleFamilyTrees(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FamilyTree> updateFamilyTree(
            @PathVariable Long id,
            @RequestBody FamilyTree familyTree,
            @AuthenticationPrincipal User user) {
        familyTree.setId(id);
        return ResponseEntity.ok(familyTreeService.updateFamilyTree(familyTree, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFamilyTree(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        familyTreeService.deleteFamilyTree(id, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FamilyTree> getFamilyTree(@PathVariable Long id) {
        return ResponseEntity.ok(familyTreeService.getFamilyTreeById(id));
    }
} 