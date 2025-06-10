package ru.cemeterysystem.dto;

import lombok.Data;

@Data
public class MemorialRelationDTO {
    private Long id;
    private Long familyTreeId;
    private TreeMemorialDTO sourceMemorial;
    private TreeMemorialDTO targetMemorial;
    private String relationType; // PARENT, CHILD, SPOUSE, etc.
}
