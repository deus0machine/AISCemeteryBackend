package ru.cemeterysystem.mappers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.cemeterysystem.dto.FamilyTreeDTO;
import ru.cemeterysystem.dto.FamilyTreeAccessDTO;
import ru.cemeterysystem.dto.MemorialRelationDTO;
import ru.cemeterysystem.dto.MemorialDTO;
import ru.cemeterysystem.dto.TreeMemorialDTO;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.FamilyTreeAccess;
import ru.cemeterysystem.models.MemorialRelation;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.repositories.FamilyTreeAccessRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FamilyTreeMapper {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private FamilyTreeAccessRepository accessRepository;
    public FamilyTreeDTO toDTO(FamilyTree tree) {
        FamilyTreeDTO dto = new FamilyTreeDTO();
        dto.setId(tree.getId());
        dto.setName(tree.getName());
        dto.setDescription(tree.getDescription());
        dto.setUserId(tree.getUser() != null ? tree.getUser().getId() : null);
        
        // Добавляем полную информацию о владельце
        dto.setOwner(userMapper.toDTO(tree.getUser()));
        
        dto.setPublic(tree.isPublic());
        dto.setPublicationStatus(tree.getPublicationStatus());
        
        // Добавляем поля для модерации изменений
        dto.setPendingChanges(tree.isPendingChanges());
        dto.setPendingName(tree.getPendingName());
        dto.setPendingDescription(tree.getPendingDescription());
        
        // Форматируем даты как строки
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        dto.setCreatedAt(tree.getCreatedAt() != null ? tree.getCreatedAt().format(formatter) : null);
        dto.setUpdatedAt(tree.getUpdatedAt() != null ? tree.getUpdatedAt().format(formatter) : null);
        
        // Добавляем информацию о доступе
        List<FamilyTreeAccess> accessList = accessRepository.findByFamilyTreeId(tree.getId());
        List<FamilyTreeAccessDTO> accessDTOs = accessList.stream()
            .map(this::toAccessDTO)
            .collect(Collectors.toList());
        dto.setAccessList(accessDTOs);
        
        if (tree.getMemorialRelations() != null) {
            List<MemorialRelationDTO> relations = tree.getMemorialRelations().stream().map(this::toRelationDTO).collect(Collectors.toList());
            dto.setMemorialRelations(relations);
            long count = relations.stream()
                .flatMap(rel -> java.util.stream.Stream.of(rel.getSourceMemorial(), rel.getTargetMemorial()))
                .filter(java.util.Objects::nonNull)
                .map(TreeMemorialDTO::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
            dto.setMemorialCount((int) count);
        } else {
            dto.setMemorialCount(0);
        }
        return dto;
    }

    public MemorialRelationDTO toRelationDTO(MemorialRelation rel) {
        MemorialRelationDTO dto = new MemorialRelationDTO();
        dto.setId(rel.getId());
        dto.setFamilyTreeId(rel.getFamilyTree() != null ? rel.getFamilyTree().getId() : null);
        dto.setRelationType(rel.getRelationType().name());
        dto.setSourceMemorial(toTreeMemorialDTO(rel.getSourceMemorial()));
        dto.setTargetMemorial(toTreeMemorialDTO(rel.getTargetMemorial()));
        return dto;
    }

    public TreeMemorialDTO toTreeMemorialDTO(Memorial memorial) {
        if (memorial == null) return null;
        TreeMemorialDTO dto = new TreeMemorialDTO();
        dto.setId(memorial.getId());
        dto.setFio(memorial.getFio());
        dto.setBirthDate(memorial.getBirthDate() != null ? memorial.getBirthDate().toString() : null);
        dto.setDeathDate(memorial.getDeathDate() != null ? memorial.getDeathDate().toString() : null);
        dto.setBiography(memorial.getBiography());
        dto.setPhotoUrl(memorial.getPhotoUrl());
        dto.setPublic(memorial.isPublic());
        return dto;
    }
    
    public FamilyTreeAccessDTO toAccessDTO(FamilyTreeAccess access) {
        if (access == null) return null;
        FamilyTreeAccessDTO dto = new FamilyTreeAccessDTO();
        dto.setId(access.getId());
        dto.setFamilyTreeId(access.getFamilyTree() != null ? access.getFamilyTree().getId() : null);
        dto.setUserId(access.getUser() != null ? access.getUser().getId() : null);
        dto.setAccessLevel(access.getAccessLevel());
        dto.setGrantedById(access.getGrantedById());
        
        // Форматируем дату
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        dto.setGrantedAt(access.getGrantedAt() != null ? access.getGrantedAt().format(formatter) : null);
        
        return dto;
    }
} 