package ru.cemeterysystem.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.services.UserService;
import ru.cemeterysystem.utils.JwtUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @GetMapping("/guest/get/{guestId}")
    public ResponseEntity<Optional<User>> getGuestById(@PathVariable Long guestId) {
        Optional<User> guest = userService.findById(guestId);
        return ResponseEntity.ok(guest);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerGuest(@RequestBody User user) {
        userService.registerGuest(user);
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Guest registered successfully!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> optionalGuest = userService.authenticate(
            credentials.get("login"), 
            credentials.get("password")
        );

        if (optionalGuest.isPresent()) {
            User user = optionalGuest.get();
            UserDetails userDetails = userService.loadUserByUsername(user.getLogin());
            String token = jwtUtils.generateToken(userDetails);

            response.put("status", "SUCCESS");
            response.put("id", user.getId());
            response.put("fio", user.getFio());
            response.put("contacts", user.getContacts());
            response.put("dateOfRegistration", user.getDateOfRegistration());
            response.put("login", user.getLogin());
            response.put("balance", user.getBalance());
            response.put("role", user.getRole().name());
            response.put("token", token);
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "FAILURE");
            response.put("message", "Invalid login or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/guest/all")
    public ResponseEntity<List<User>> getGuests() {
        List<User> users = userService.getAllGuests();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/guest/{id}")
    public ResponseEntity<Void> deleteGuest(@PathVariable Long id) {
        try {
            userService.deleteGuestById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}

