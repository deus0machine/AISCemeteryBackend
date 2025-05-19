package ru.cemeterysystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemorialRelationDTO {
    private Long id;
    private Long familyTreeId;
    private Long sourceMemorialId;
    private Long targetMemorialId;
    private String relationType; // PARENT, CHILD, SPOUSE, etc.
}
