package ru.cemeterysystem.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.Models.Burial;
import ru.cemeterysystem.Models.Order;
import ru.cemeterysystem.Services.BurialService;

import java.util.List;

@RestController
@RequestMapping("/api/burials")
public class BurialController {
    private final BurialService burialService;

    @Autowired
    public BurialController(BurialService burialService) {
        this.burialService = burialService;
    }
    @GetMapping("/fio/{fio}")
    public ResponseEntity<List<Burial>> getBurialsByFio(@PathVariable String fio) {
        List<Burial> burials = burialService.findBurialByFio(fio);
        return ResponseEntity.ok(burials);
    }
    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<Burial>> getBurialsByGuest(@PathVariable Long guestId) {
        List<Burial> burials = burialService.findBurialByGuestId(guestId);
        return ResponseEntity.ok(burials);
    }
    @GetMapping("/all")
    public ResponseEntity<List<Burial>> getBurials() {
        List<Burial> burials = burialService.findAll();
        return ResponseEntity.ok(burials);
    }
    @PostMapping
    public ResponseEntity<Burial> createBurial(@RequestBody @Valid Burial burial) {
        Burial savedBurial = burialService.createBurial(burial);
        return new ResponseEntity<>(savedBurial, HttpStatus.CREATED);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Burial> updateBurial(@PathVariable Long id, @RequestBody @Valid Burial burial) {
        try {
            Burial updatedBurial = burialService.updateBurial(id, burial);
            return new ResponseEntity<>(updatedBurial, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBurial(@PathVariable Long id) {
        try {
            burialService.deleteBurial(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
