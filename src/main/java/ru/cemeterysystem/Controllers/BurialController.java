package ru.cemeterysystem.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.Models.Burial;
import ru.cemeterysystem.Services.BurialService;

@RestController
@RequestMapping("/api/burials")
public class BurialController {

    private final BurialService burialService;

    @Autowired
    public BurialController(BurialService burialService) {
        this.burialService = burialService;
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
