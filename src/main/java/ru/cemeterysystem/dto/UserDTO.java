package ru.cemeterysystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String fio;
    private String contacts;
    private String login;
    private LocalDateTime dateOfRegistration;
} 