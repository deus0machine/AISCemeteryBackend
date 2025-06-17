package ru.cemeterysystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.cemeterysystem.models.FamilyTree.PublicationStatus;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FamilyTreeDTO {
    private Long id;
    private String name;
    private String description;
    @JsonProperty("user")
    private Long userId;
    private UserDTO owner; // Добавляем полную информацию о владельце
    @JsonProperty("is_public")
    private boolean isPublic;
    private PublicationStatus publicationStatus;
    private String createdAt; // Изменяем на String для передачи на клиент
    private String updatedAt; // Изменяем на String для передачи на клиент
    private List<MemorialRelationDTO> memorialRelations;
    private Integer memorialCount;
    private List<FamilyTreeAccessDTO> accessList; // Добавляем информацию о доступе
    
    // Поля для модерации изменений
    private boolean pendingChanges;
    private String pendingName;
    private String pendingDescription;
} 