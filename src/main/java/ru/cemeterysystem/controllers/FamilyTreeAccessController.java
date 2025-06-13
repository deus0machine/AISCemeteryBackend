package ru.cemeterysystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.dto.FamilyTreeAccessRequestDTO;
import ru.cemeterysystem.models.FamilyTreeAccess;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.services.FamilyTreeAccessService;
import java.util.List;

@RestController
@RequestMapping("/api/family-trees")
public class FamilyTreeAccessController {
    private final FamilyTreeAccessService accessService;
    private final UserRepository userRepository;

    @Autowired
    public FamilyTreeAccessController(FamilyTreeAccessService accessService, UserRepository userRepository) {
        this.accessService = accessService;
        this.userRepository = userRepository;
    }

    /**
     * Получает текущего аутентифицированного пользователя.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/{familyTreeId}/access/users/{userId}")
    public ResponseEntity<FamilyTreeAccess> grantAccess(
            @PathVariable Long familyTreeId,
            @PathVariable Long userId,
            @RequestParam FamilyTreeAccess.AccessLevel accessLevel) {
        User user = getCurrentUser();
        return ResponseEntity.ok(accessService.grantAccess(familyTreeId, userId, accessLevel, user.getId()));
    }

    @PutMapping("/{familyTreeId}/access/users/{userId}")
    public ResponseEntity<FamilyTreeAccess> updateAccess(
            @PathVariable Long familyTreeId,
            @PathVariable Long userId,
            @RequestParam FamilyTreeAccess.AccessLevel newAccessLevel) {
        User user = getCurrentUser();
        return ResponseEntity.ok(accessService.updateAccess(familyTreeId, userId, newAccessLevel, user.getId()));
    }

    @DeleteMapping("/{familyTreeId}/access/users/{userId}")
    public ResponseEntity<Void> revokeAccess(
            @PathVariable Long familyTreeId,
            @PathVariable Long userId) {
        User user = getCurrentUser();
        accessService.revokeAccess(familyTreeId, userId, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{familyTreeId}/access")
    public ResponseEntity<List<FamilyTreeAccess>> getAccessList(
            @PathVariable Long familyTreeId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(accessService.getAccessList(familyTreeId, user.getId()));
    }

    @GetMapping("/access/my")
    public ResponseEntity<List<FamilyTreeAccess>> getMyAccess() {
        User user = getCurrentUser();
        return ResponseEntity.ok(accessService.getUserAccess(user.getId()));
    }

    /**
     * Отправка запроса на доступ к семейному дереву
     */
    @PostMapping("/{familyTreeId}/access/request")
    public ResponseEntity<String> requestAccess(
            @PathVariable Long familyTreeId,
            @RequestBody FamilyTreeAccessRequestDTO request) {
        User user = getCurrentUser();
        accessService.requestAccess(familyTreeId, user, request.getRequestedAccessLevel(), request.getMessage());
        return ResponseEntity.ok("Access request sent successfully");
    }

    /**
     * Одобрение или отклонение запроса на доступ
     */
    @PostMapping("/access/requests/{notificationId}/respond")
    public ResponseEntity<String> respondToAccessRequest(
            @PathVariable Long notificationId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String message) {
        User user = getCurrentUser();
        accessService.respondToAccessRequest(notificationId, approve, message, user);
        return ResponseEntity.ok(approve ? "Access granted" : "Access denied");
    }
} 