package ru.cemeterysystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.cemeterysystem.services.UserService;

@Controller
public class AuthController {
    
    private final UserService userService;
    
    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Если пользователь уже аутентифицирован и не анонимный, перенаправляем на админ-панель
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin";
            }
            return "redirect:/";
        }
        
        // Обработка ошибок и сообщений
        if (error != null) {
            model.addAttribute("error", "Неверный логин или пароль");
        }
        
        if (logout != null) {
            model.addAttribute("message", "Вы успешно вышли из системы");
        }
        
        return "login";
    }
    
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
    
    @GetMapping("/")
    public String root() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Если пользователь администратор, перенаправляем на админ-панель
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin";
        }
        
        // Иначе на страницу входа
        return "redirect:/login";
    }
} 