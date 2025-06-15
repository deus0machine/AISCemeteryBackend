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
    
    // Новые поля для разделенного ФИО
    private String firstName;
    private String lastName;
    private String middleName;
    
    private String birthDate;
    private String deathDate;
    private String biography;
    private Location mainLocation;
    private Location burialLocation;
    private String photoUrl;
    private String documentUrl;
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
    
    @JsonProperty("changesUnderModeration")
    private boolean changesUnderModeration;
    
    // Добавляем поле статуса публикации
    private PublicationStatus publicationStatus;
    
    // Добавляем поле для количества просмотров
    private Integer viewCount = 0;
    
    // Поля для ожидающих изменений
    private String pendingPhotoUrl;
    private String pendingDocumentUrl;
    private String pendingFio;
    private String pendingBiography;
    private String pendingBirthDate;
    private String pendingDeathDate;
    private Boolean pendingIsPublic;
    private Location pendingMainLocation;
    private Location pendingBurialLocation;
    
    // Pending поля для отдельных компонентов ФИО
    private String pendingFirstName;
    private String pendingLastName;
    private String pendingMiddleName;
    
    // Поля блокировки
    private boolean isBlocked = false;
    private String blockReason;
    private LocalDateTime blockedAt;
    private UserDTO blockedBy;
    
    // Поле для прав редактирования
    private boolean canEdit = false;
    
    // Утилитные методы для работы с ФИО
    public String getFullName() {
        if (firstName != null && lastName != null && 
            !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(lastName.trim());
            if (firstName != null && !firstName.trim().isEmpty()) {
                sb.append(" ").append(firstName.trim());
            }
            if (middleName != null && !middleName.trim().isEmpty()) {
                sb.append(" ").append(middleName.trim());
            }
            return sb.toString();
        } else {
            return fio != null ? fio : "";
        }
    }
    
    public boolean hasSeparateNameFields() {
        return firstName != null && lastName != null && 
               !firstName.trim().isEmpty() && !lastName.trim().isEmpty();
    }
} 