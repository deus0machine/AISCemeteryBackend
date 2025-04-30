package ru.cemeterysystem.dto;

import lombok.Data;
import ru.cemeterysystem.models.Location;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class MemorialDTO {
    private Long id;
    private String fio;
    private String birthDate;
    private String deathDate;
    private String biography;
    private Location mainLocation;
    private Location burialLocation;
    private String photoUrl;
    @JsonProperty("is_public")
    private boolean isPublic;
    private Long treeId;
    private UserDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 