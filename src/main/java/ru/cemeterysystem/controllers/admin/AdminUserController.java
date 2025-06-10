package ru.cemeterysystem.controllers.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.UserService;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    
    @GetMapping
    public String getUsersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> usersPage;
        
        if (search != null && !search.trim().isEmpty()) {
            usersPage = userService.findUsersByFioContaining(search.trim(), pageable);
        } else {
            usersPage = userService.getAllUsers(pageable);
        }
        
        model.addAttribute("users", usersPage);
        
        // Получаем недавно зарегистрированных пользователей (за последние 30 дней)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        Date thirtyDaysAgo = calendar.getTime();
        
        List<User> recentUsers = userService.getAllUsers(PageRequest.of(0, 100, Sort.by("dateOfRegistration").descending()))
                .stream()
                .filter(user -> user.getDateOfRegistration().after(thirtyDaysAgo))
                .collect(Collectors.toList());
        
        model.addAttribute("recentUsers", recentUsers);
        
        return "admin/users";
    }
    
    @GetMapping("/{id}/edit")
    public String getEditUserPage(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "admin/user-edit";
    }
    
    @PostMapping("/{id}/edit")
    public String updateUser(
            @PathVariable Long id,
            @ModelAttribute User updatedUser,
            @RequestParam(required = false) String newPassword,
            RedirectAttributes redirectAttributes) {
        
        User existingUser = userService.getUserById(id);
        
        existingUser.setFio(updatedUser.getFio());
        existingUser.setLogin(updatedUser.getLogin());
        existingUser.setContacts(updatedUser.getContacts());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setHasSubscription(updatedUser.getHasSubscription());
        
        if (newPassword != null && !newPassword.isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(newPassword));
        }
        
        userService.updateUser(existingUser);
        redirectAttributes.addFlashAttribute("successMessage", "Пользователь успешно обновлен");
        
        return "redirect:/admin/users";
    }
    
    @PostMapping("/add")
    public String addUser(
            @ModelAttribute User user,
            @RequestParam(required = false, defaultValue = "false") boolean hasSubscription,
            RedirectAttributes redirectAttributes) {
        
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setDateOfRegistration(new Date());
            user.setHasSubscription(hasSubscription);
            
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "Пользователь успешно добавлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при добавлении пользователя: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    @PostMapping("/delete")
    public String deleteUser(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteGuestById(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Пользователь успешно удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении пользователя: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    @PostMapping("/{id}/toggle-subscription")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> toggleSubscription(@PathVariable Long id) {
        try {
            userService.toggleUserActiveStatus(id);
            User user = userService.getUserById(id);
            Map<String, Boolean> response = new HashMap<>();
            response.put("hasSubscription", user.getHasSubscription());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable Long id, 
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userService.getUserById(id);
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "Пароль успешно сброшен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при сбросе пароля: " + e.getMessage());
        }
        
        return "redirect:/admin/users/" + id + "/edit";
    }
    
    @GetMapping("/export")
    public String exportUsers(Model model) {
        List<User> allUsers = userService.getAllGuests();
        model.addAttribute("users", allUsers);
        return "admin/users-export";
    }
    
    /**
     * API endpoint для получения всех пользователей в JSON формате
     */
    @GetMapping("/api/all")
    @ResponseBody
    public List<Map<String, Object>> getAllUsersApi() {
        List<User> users = userService.getAllGuests();
        
        return users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("login", user.getLogin());
                    userMap.put("contacts", user.getContacts());
                    userMap.put("fio", user.getFio());
                    userMap.put("role", user.getRole().name());
                    return userMap;
                })
                .collect(Collectors.toList());
    }
} 