package ru.cemeterysystem.controllers.admin;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.FamilyTreeService;
import ru.cemeterysystem.services.NotificationService;
import ru.cemeterysystem.services.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/family-trees")
@RequiredArgsConstructor
public class AdminFamilyTreeController {

    private static final Logger logger = LoggerFactory.getLogger(AdminFamilyTreeController.class);

    private final FamilyTreeService familyTreeService;
    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String familyTrees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Model model) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<FamilyTree> familyTreePage;
        
        if (search != null && !search.trim().isEmpty()) {
            // Поиск по названию дерева
            familyTreePage = familyTreeService.searchByName(search.trim(), pageable);
        } else if (status != null && !status.isEmpty()) {
            // Фильтрация по статусу публикации
            switch (status) {
                case "pending":
                    familyTreePage = familyTreeService.findByPublicationStatus(
                        FamilyTree.PublicationStatus.PENDING_MODERATION, pageable);
                    break;
                case "published":
                    familyTreePage = familyTreeService.findByPublicationStatus(
                        FamilyTree.PublicationStatus.PUBLISHED, pageable);
                    break;
                case "rejected":
                    familyTreePage = familyTreeService.findByPublicationStatus(
                        FamilyTree.PublicationStatus.REJECTED, pageable);
                    break;
                case "draft":
                    familyTreePage = familyTreeService.findByPublicationStatus(
                        FamilyTree.PublicationStatus.DRAFT, pageable);
                    break;
                default:
                    familyTreePage = familyTreeService.findAllForAdmin(pageable);
                    break;
            }
        } else {
            familyTreePage = familyTreeService.findAllForAdmin(pageable);
        }

        // Добавляем статистику
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", familyTreeService.getTotalCount());
        stats.put("pending", familyTreeService.getCountByStatus(FamilyTree.PublicationStatus.PENDING_MODERATION));
        stats.put("published", familyTreeService.getCountByStatus(FamilyTree.PublicationStatus.PUBLISHED));
        stats.put("rejected", familyTreeService.getCountByStatus(FamilyTree.PublicationStatus.REJECTED));
        stats.put("draft", familyTreeService.getCountByStatus(FamilyTree.PublicationStatus.DRAFT));

        model.addAttribute("familyTrees", familyTreePage);
        model.addAttribute("stats", stats);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", familyTreePage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDirection", sortDirection);

        return "admin/family-trees";
    }

    @GetMapping("/{id}")
    public String viewFamilyTree(@PathVariable Long id, Model model) {
        try {
            FamilyTree familyTree = familyTreeService.getFamilyTreeById(id);
            
            // Получаем все мемориалы в дереве
            List<Memorial> memorials = familyTreeService.getAllMemorialsInTree(id);
            
            // Получаем владельца дерева
            User owner = familyTree.getUser();
            
            model.addAttribute("familyTree", familyTree);
            model.addAttribute("memorials", memorials);
            model.addAttribute("owner", owner);
            model.addAttribute("memorialCount", memorials.size());
            
            return "admin/family-tree-view";
        } catch (Exception e) {
            logger.error("Error viewing family tree {}: {}", id, e.getMessage());
            return "redirect:/admin/family-trees?error=Дерево не найдено";
        }
    }

    @PostMapping("/{id}/approve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveFamilyTree(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User admin = userService.findByLogin(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            FamilyTree approvedTree = familyTreeService.approveTree(id);
            
            logger.info("Family tree {} approved by admin {}", id, admin.getLogin());
            
            response.put("success", true);
            response.put("message", "Семейное дерево успешно одобрено");
            response.put("newStatus", "PUBLISHED");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error approving family tree {}: {}", id, e.getMessage());
            response.put("success", false);
            response.put("message", "Ошибка при одобрении дерева: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/reject")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectFamilyTree(
            @PathVariable Long id,
            @RequestParam String reason) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (reason == null || reason.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Необходимо указать причину отклонения");
                return ResponseEntity.badRequest().body(response);
            }
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User admin = userService.findByLogin(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            FamilyTree rejectedTree = familyTreeService.rejectTree(id, reason.trim());
            
            logger.info("Family tree {} rejected by admin {} with reason: {}", 
                id, admin.getLogin(), reason);
            
            response.put("success", true);
            response.put("message", "Семейное дерево отклонено");
            response.put("newStatus", "REJECTED");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error rejecting family tree {}: {}", id, e.getMessage());
            response.put("success", false);
            response.put("message", "Ошибка при отклонении дерева: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFamilyTree(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User admin = userService.findByLogin(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            familyTreeService.deleteFamilyTree(id);
            
            logger.info("Family tree {} deleted by admin {}", id, admin.getLogin());
            
            response.put("success", true);
            response.put("message", "Семейное дерево удалено");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting family tree {}: {}", id, e.getMessage());
            response.put("success", false);
            response.put("message", "Ошибка при удалении дерева: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}/memorials")
    @ResponseBody
    public ResponseEntity<List<Memorial>> getFamilyTreeMemorials(@PathVariable Long id) {
        try {
            List<Memorial> memorials = familyTreeService.getAllMemorialsInTree(id);
            return ResponseEntity.ok(memorials);
        } catch (Exception e) {
            logger.error("Error getting memorials for family tree {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 