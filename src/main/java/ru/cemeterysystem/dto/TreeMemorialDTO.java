package ru.cemeterysystem.dto;

import lombok.Data;

@Data
public class TreeMemorialDTO {
    private Long id;
    private String fio;
    private String birthDate;
    private String deathDate;
    private String biography;
    private String photoUrl;
    private boolean isPublic;
} 