package ru.cemeterysystem.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.Models.Guest;
import ru.cemeterysystem.Repositories.GuestRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class GuestController {
    @Autowired
    private GuestRepository guestRepository;
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Guest credentials) {
        Map<String, Object> response = new HashMap<>();

        Optional<Guest> optionalGuest = guestRepository.findByLoginAndPassword(
                credentials.getLogin(), credentials.getPassword());

        if (optionalGuest.isPresent()) {
            Guest guest = optionalGuest.get();
            response.put("status", "SUCCESS");
            response.put("id", guest.getId());
            response.put("fio", guest.getFio());
            response.put("contacts", guest.getContacts());
            response.put("dateOfRegistration", guest.getDateOfRegistration());
            response.put("login", guest.getLogin());
        } else {
            response.put("status", "FAILURE");
        }

        return response;
    }
}

