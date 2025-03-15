package ru.cemeterysystem.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.cemeterysystem.dto.MemorialDTO;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.MemorialService;
import ru.cemeterysystem.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/memorials")
@RequiredArgsConstructor
public class MemorialController {
    private final MemorialService memorialService;
    private final UserService userService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        return userService.findByLogin(login)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<Memorial> getAllMemorials() {
        return memorialService.getAllMemorials();
    }

    @GetMapping("/my")
    public List<Memorial> getMyMemorials() {
        User user = getCurrentUser();
        return memorialService.getMyMemorials(user.getId());
    }

    @GetMapping("/public")
    public List<Memorial> getPublicMemorials() {
        return memorialService.getPublicMemorials();
    }

    @GetMapping("/{id}")
    public Memorial getMemorialById(@PathVariable Long id) {
        return memorialService.getMemorialById(id);
    }

    @PostMapping
    public Memorial createMemorial(@RequestBody MemorialDTO dto) {
        User user = getCurrentUser();
        return memorialService.createMemorial(dto, user.getId());
    }

    @PutMapping("/{id}")
    public Memorial updateMemorial(@PathVariable Long id,
                                 @RequestBody MemorialDTO dto) {
        return memorialService.updateMemorial(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMemorial(@PathVariable Long id) {
        memorialService.deleteMemorial(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/privacy")
    public ResponseEntity<Void> updateMemorialPrivacy(@PathVariable Long id,
                                                    @RequestBody boolean isPublic) {
        memorialService.updateMemorialPrivacy(id, isPublic);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/photo")
    public String uploadPhoto(@PathVariable Long id,
                            @RequestParam("photo") MultipartFile file) {
        return memorialService.uploadPhoto(id, file);
    }

    @GetMapping("/search")
    public List<Memorial> searchMemorials(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(required = false) Boolean isPublic
    ) {
        return memorialService.searchMemorials(query, location, startDate, endDate, isPublic);
    }
}
