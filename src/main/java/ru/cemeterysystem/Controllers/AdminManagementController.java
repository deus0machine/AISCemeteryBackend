package ru.cemeterysystem.Controllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cemeterysystem.Models.Burial;
import ru.cemeterysystem.Services.AdminService;
import ru.cemeterysystem.Services.BurialService;

@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {
    private final AdminService adminService;

    @Autowired
    public AdminManagementController(AdminService adminService) {
        this.adminService = adminService;
    }
    @PostMapping("/request/email")
    public ResponseEntity<Void> sendRequest(@RequestBody String email) {
        try {
            adminService.sendPdfToEmail(email);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
