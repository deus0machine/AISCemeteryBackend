package ru.cemeterysystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.cemeterysystem.models.FamilyTreeAccess;

/**
 * DTO для запросов на доступ к семейному дереву
 */
@Data
public class FamilyTreeAccessRequestDTO {
    @JsonProperty("familyTreeId")
    private Long familyTreeId;
    
    @JsonProperty("requestedAccessLevel")
    private FamilyTreeAccess.AccessLevel requestedAccessLevel;
    
    @JsonProperty("message")
    private String message; // Сообщение от пользователя с обоснованием запроса
    
    @JsonProperty("action")
    private String action; // "request", "approve", "reject"
} 