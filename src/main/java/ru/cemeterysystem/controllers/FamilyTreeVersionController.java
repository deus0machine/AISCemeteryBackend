package ru.cemeterysystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.FamilyTreeVersion;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.FamilyTreeVersionService;
import java.util.List;

@RestController
@RequestMapping("/api/family-trees/{familyTreeId}/versions")
public class FamilyTreeVersionController {
    private final FamilyTreeVersionService versionService;

    @Autowired
    public FamilyTreeVersionController(FamilyTreeVersionService versionService) {
        this.versionService = versionService;
    }

    @PostMapping
    public ResponseEntity<FamilyTreeVersion> createVersion(
            @PathVariable Long familyTreeId,
            @RequestParam String description,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(versionService.createVersion(familyTreeId, description, user.getId()));
    }

    @GetMapping
    public ResponseEntity<List<FamilyTreeVersion>> getVersions(@PathVariable Long familyTreeId) {
        return ResponseEntity.ok(versionService.getVersions(familyTreeId));
    }

    @GetMapping("/latest")
    public ResponseEntity<FamilyTreeVersion> getLatestVersion(@PathVariable Long familyTreeId) {
        return ResponseEntity.ok(versionService.getLatestVersion(familyTreeId));
    }
} 