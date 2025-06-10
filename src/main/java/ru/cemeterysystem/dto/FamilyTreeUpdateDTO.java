package ru.cemeterysystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FamilyTreeUpdateDTO {
    private Long id;
    private String name;
    private String description;
    
    @JsonProperty("is_public")
    private boolean isPublic;
} 