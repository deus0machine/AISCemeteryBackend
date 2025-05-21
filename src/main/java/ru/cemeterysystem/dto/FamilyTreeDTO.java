package ru.cemeterysystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FamilyTreeDTO {
    private Long id;
    private String name;
    private String description;
    @JsonProperty("user")
    private Long userId;
    private boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MemorialRelationDTO> memorialRelations;
    private Integer memorialCount;
} 