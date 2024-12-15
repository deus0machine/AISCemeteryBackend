package ru.cemeterysystem.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.Models.Guest;
import ru.cemeterysystem.Repositories.GuestRepository;
import ru.cemeterysystem.Services.GuestService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class GuestController {
    @Autowired
    private GuestService guestService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @PostMapping("/register")
    public ResponseEntity<String> registerGuest(@RequestBody Guest guest) {
        guestService.registerGuest(guest);
        return ResponseEntity.ok("Guest registered successfully!");
    }
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> response = new HashMap<>();

        // Используем сервис для аутентификации
        Optional<Guest> optionalGuest = guestService.authenticate(credentials.get("login"), credentials.get("password"));

        if (optionalGuest.isPresent()) {
            Guest guest = optionalGuest.get();
            response.put("status", "SUCCESS");
            response.put("id", guest.getId());
            response.put("fio", guest.getFio());
            response.put("contacts", guest.getContacts());
            response.put("dateOfRegistration", guest.getDateOfRegistration());
            response.put("login", guest.getLogin());
            response.put("role", guest.getRole().name());
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "FAILURE");
            response.put("message", "Invalid login or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

}

