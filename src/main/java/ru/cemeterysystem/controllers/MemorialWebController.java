package ru.cemeterysystem.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/memorials")
@RequiredArgsConstructor
public class MemorialWebController {

    private final MemorialRepository memorialRepository;
    private final UserRepository userRepository;

    /**
     * Обрабатывает базовый путь /memorials
     */
    @GetMapping
    public String listMemorials(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "successMessage", required = false) String successMessage,
            @RequestParam(value = "errorMessage", required = false) String errorMessage,
            Model model) {
        
        // Получаем публичные опубликованные незаблокированные мемориалы
        List<Memorial> publicMemorials = memorialRepository.findByIsPublicTrueAndPublicationStatusAndIsBlockedFalse(
                Memorial.PublicationStatus.PUBLISHED);
        model.addAttribute("memorials", publicMemorials);
        
        // Если есть сообщение об ошибке, добавляем его в модель
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        
        // Добавляем сообщения об успехе/ошибке если они переданы
        if (successMessage != null) {
            model.addAttribute("successMessage", successMessage);
        }
        
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }
        
        return "memorials/list";
    }

    @GetMapping("/{id}")
    public String viewMemorial(
            @PathVariable Long id, 
            Model model,
            @RequestParam(value = "successMessage", required = false) String successMessage,
            @RequestParam(value = "errorMessage", required = false) String errorMessage) {
        Optional<Memorial> optionalMemorial = memorialRepository.findById(id);
        
        // Если мемориал не найден, перенаправляем на список с сообщением об ошибке
        if (optionalMemorial.isEmpty()) {
            return "redirect:/memorials?error=Memorial+not+found";
        }
        
        Memorial memorial = optionalMemorial.get();
        
        // Проверяем доступность мемориала для просмотра
        if (!memorial.isPublic()) {
            // Проверяем, авторизован ли пользователь для просмотра непубличного мемориала
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return "redirect:/memorials?error=Memorial+not+available";
            }
            
            User currentUser = userRepository.findByLogin(auth.getName()).orElse(null);
            if (currentUser == null) {
                return "redirect:/memorials?error=Memorial+not+available";
            }
            
            // Проверяем, является ли пользователь администратором, владельцем или редактором мемориала
            boolean isAdmin = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isOwner = memorial.getCreatedBy() != null && 
                             memorial.getCreatedBy().getId().equals(currentUser.getId());
            boolean isEditor = memorial.getEditors() != null && 
                              memorial.getEditors().contains(currentUser);
            
            if (!isAdmin && !isOwner && !isEditor) {
                return "redirect:/memorials?error=Memorial+not+available";
            }
            
            // Добавляем флаг модерации для администраторов
            if (isAdmin) {
                model.addAttribute("isModeration", true);
            }
        }
        
        // Увеличиваем счетчик просмотров
        if (memorial.getViewCount() == null) {
            memorial.setViewCount(1);
        } else {
            memorial.setViewCount(memorial.getViewCount() + 1);
        }
        memorialRepository.save(memorial);
        
        model.addAttribute("memorial", memorial);
        
        // Добавляем сообщения об успехе/ошибке если они переданы
        if (successMessage != null) {
            model.addAttribute("successMessage", successMessage);
        }
        
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }
        
        // Проверяем, авторизован ли текущий пользователь для добавления дополнительных прав доступа
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            User currentUser = userRepository.findByLogin(auth.getName()).orElse(null);
            if (currentUser != null) {
                boolean isAdmin = auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                boolean isOwner = memorial.getCreatedBy() != null && 
                                 memorial.getCreatedBy().getId().equals(currentUser.getId());
                boolean isEditor = memorial.getEditors() != null && 
                                  memorial.getEditors().contains(currentUser);
                
                model.addAttribute("isAdmin", isAdmin);
                model.addAttribute("isOwner", isOwner);
                model.addAttribute("isEditor", isEditor);
            }
        }
        
        return "memorials/view";
    }
} 