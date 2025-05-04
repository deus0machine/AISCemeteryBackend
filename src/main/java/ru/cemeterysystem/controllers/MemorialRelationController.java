package ru.cemeterysystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.MemorialRelation;
import ru.cemeterysystem.services.MemorialRelationService;
import java.util.List;

@RestController
@RequestMapping("/api/memorial-relations")
public class MemorialRelationController {
    private final MemorialRelationService memorialRelationService;

    @Autowired
    public MemorialRelationController(MemorialRelationService memorialRelationService) {
        this.memorialRelationService = memorialRelationService;
    }

    @PostMapping
    public ResponseEntity<MemorialRelation> createRelation(@RequestBody MemorialRelation relation) {
        return ResponseEntity.ok(memorialRelationService.createRelation(relation));
    }

    @GetMapping("/tree/{familyTreeId}")
    public ResponseEntity<List<MemorialRelation>> getRelationsByFamilyTree(@PathVariable Long familyTreeId) {
        return ResponseEntity.ok(memorialRelationService.getRelationsByFamilyTree(familyTreeId));
    }

    @GetMapping("/memorial/{memorialId}")
    public ResponseEntity<List<MemorialRelation>> getRelationsByMemorial(@PathVariable Long memorialId) {
        return ResponseEntity.ok(memorialRelationService.getRelationsByMemorial(memorialId));
    }

    @GetMapping("/tree/{familyTreeId}/memorial/{memorialId}")
    public ResponseEntity<List<MemorialRelation>> getRelationsByFamilyTreeAndMemorial(
            @PathVariable Long familyTreeId,
            @PathVariable Long memorialId) {
        return ResponseEntity.ok(memorialRelationService.getRelationsByFamilyTreeAndMemorial(familyTreeId, memorialId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRelation(@PathVariable Long id) {
        memorialRelationService.deleteRelation(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tree/{familyTreeId}")
    public ResponseEntity<Void> deleteAllRelationsByFamilyTree(@PathVariable Long familyTreeId) {
        memorialRelationService.deleteAllRelationsByFamilyTree(familyTreeId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemorialRelation> updateRelation(
            @PathVariable Long id,
            @RequestBody MemorialRelation relation) {
        relation.setId(id);
        return ResponseEntity.ok(memorialRelationService.updateRelation(relation));
    }
} 