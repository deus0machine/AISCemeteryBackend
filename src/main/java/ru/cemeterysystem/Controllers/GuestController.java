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
import ru.cemeterysystem.Models.Burial;
import ru.cemeterysystem.Models.Guest;
import ru.cemeterysystem.Repositories.GuestRepository;
import ru.cemeterysystem.Services.GuestService;
import ru.cemeterysystem.utils.JwtUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class GuestController {
    @Autowired
    private GuestService guestService;
    @GetMapping("/guest/get/{guestId}")
    public ResponseEntity<Optional<Guest>> getGuestById(@PathVariable Long guestId) {
        Optional<Guest> guest = guestService.findById(guestId);
        return ResponseEntity.ok(guest);
    }
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerGuest(@RequestBody Guest guest) {
        guestService.registerGuest(guest);
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Guest registered successfully!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> response = new HashMap<>();

        // Используем сервис для аутентификации
        Optional<Guest> optionalGuest = guestService.authenticate(credentials.get("login"), credentials.get("password"));

        if (optionalGuest.isPresent()) {
            Guest guest = optionalGuest.get();
            String token = JwtUtils.generateToken(guest);
            response.put("status", "SUCCESS");
            response.put("id", guest.getId());
            response.put("fio", guest.getFio());
            response.put("contacts", guest.getContacts());
            response.put("dateOfRegistration", guest.getDateOfRegistration());
            response.put("login", guest.getLogin());
            response.put("balance", guest.getBalance());
            response.put("role", guest.getRole().name());
            response.put("token", token);
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "FAILURE");
            response.put("message", "Invalid login or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    @GetMapping("/guest/all")
    public ResponseEntity<List<Guest>> getGuests() {
        List<Guest> guests = guestService.getAllGuests();
        return ResponseEntity.ok(guests);
    }

}

