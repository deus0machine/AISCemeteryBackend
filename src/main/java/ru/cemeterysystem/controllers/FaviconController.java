package ru.cemeterysystem.controllers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для обработки запросов favicon
 */
@Controller
public class FaviconController {

    @GetMapping("/favicon.ico")
    public ResponseEntity<Resource> favicon() {
        try {
            Resource resource = new ClassPathResource("static/favicon.svg");
            if (resource.exists()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.valueOf("image/svg+xml"));
                headers.setCacheControl("max-age=86400"); // Кэш на 24 часа
                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            // Логируем ошибку, но не выбрасываем исключение
            System.out.println("Favicon not found: " + e.getMessage());
        }
        
        // Возвращаем пустой ответ 204 No Content вместо ошибки 404
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @GetMapping("/favicon.svg")
    public ResponseEntity<Resource> faviconSvg() {
        try {
            Resource resource = new ClassPathResource("static/favicon.svg");
            if (resource.exists()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.valueOf("image/svg+xml"));
                headers.setCacheControl("max-age=86400");
                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            System.out.println("Favicon SVG not found: " + e.getMessage());
        }
        
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
} 