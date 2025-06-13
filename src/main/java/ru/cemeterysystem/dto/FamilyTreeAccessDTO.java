package ru.cemeterysystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.cemeterysystem.models.FamilyTreeAccess.AccessLevel;

@Data
public class FamilyTreeAccessDTO {
    private Long id;
    
    @JsonProperty("familyTree")
    private Long familyTreeId;
    
    @JsonProperty("user")
    private Long userId;
    
    @JsonProperty("accessLevel")
    private AccessLevel accessLevel;
    
    @JsonProperty("grantedAt")
    private String grantedAt;
    
    @JsonProperty("grantedBy")
    private Long grantedById;
} 