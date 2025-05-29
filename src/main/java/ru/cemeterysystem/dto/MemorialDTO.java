package ru.cemeterysystem.dto;

import lombok.Data;
import ru.cemeterysystem.models.Location;
import ru.cemeterysystem.models.Memorial.PublicationStatus;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

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
    
    @JsonProperty("editors")
    private List<Long> editorIds;
    
    @JsonProperty("is_editor")
    private boolean isEditor;
    
    @JsonProperty("pendingChanges")
    private boolean pendingChanges;
    
    // Добавляем поле статуса публикации
    private PublicationStatus publicationStatus;
    
    // Поля для ожидающих изменений
    private String pendingPhotoUrl;
    private String pendingBiography;
    private String pendingBirthDate;
    private String pendingDeathDate;
    private Location pendingMainLocation;
    private Location pendingBurialLocation;
} 