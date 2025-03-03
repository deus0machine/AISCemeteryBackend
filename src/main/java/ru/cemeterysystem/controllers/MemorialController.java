package ru.cemeterysystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.services.MemorialService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/burials")
public class MemorialController {
    private final MemorialService memorialService;

    @Autowired
    public MemorialController(MemorialService memorialService) {
        this.memorialService = memorialService;
    }
    @GetMapping("/fio/{fio}")
    public ResponseEntity<List<Memorial>> getBurialsByFio(@PathVariable String fio) {
        List<Memorial> burials = memorialService.findBurialByFio(fio);
        return ResponseEntity.ok(burials);
    }
    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<Memorial>> getBurialsByGuest(@PathVariable Long guestId) {
        List<Memorial> burials = memorialService.findBurialByGuestId(guestId);
        return ResponseEntity.ok(burials);
    }
    @GetMapping("/all")
    public ResponseEntity<List<Memorial>> getBurials() {
        List<Memorial> burials = memorialService.findAll();
        return ResponseEntity.ok(burials);
    }
    @GetMapping("/burial/{burialId}")
    public ResponseEntity<Optional<Memorial>> getBurialById(@PathVariable Long burialId) {
        Optional<Memorial> burial = memorialService.findBurialById(burialId);
        return ResponseEntity.ok(burial);
    }
    @PostMapping
    public ResponseEntity<Memorial> createBurial(@RequestBody @Valid Memorial burial) {
        Memorial savedBurial = memorialService.createBurial(burial);
        return new ResponseEntity<>(savedBurial, HttpStatus.CREATED);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Memorial> updateBurial(@PathVariable Long id, @RequestBody @Valid Memorial burial) {
        try {
            Memorial updatedBurial = memorialService.updateBurial(id, burial);
            return new ResponseEntity<>(updatedBurial, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @PutMapping("/part/{id}")
    public ResponseEntity<Memorial> updatePartBurial(@PathVariable Long id, @RequestBody @Valid Memorial burial) {
        try {
            Memorial updatedBurial = memorialService.updatePartBurial(id, burial);
            return new ResponseEntity<>(updatedBurial, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBurial(@PathVariable Long id) {
        try {
            memorialService.deleteBurial(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
