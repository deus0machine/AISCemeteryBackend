package ru.cemeterysystem.mappers;

import org.springframework.jdbc.core.RowMapper;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.models.Memorial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.cemeterysystem.dto.MemorialDTO;
import java.time.format.DateTimeFormatter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MemorialMapper implements RowMapper<Memorial> {
    private final UserMapper userMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    @Override
    public Memorial mapRow(ResultSet rs, int rowNum) throws SQLException {
        Memorial burial = new Memorial();
        burial.setId(rs.getLong("id"));
        burial.setFio(rs.getString("fio"));
        burial.setDeathDate(rs.getObject("death_date", LocalDate.class));
        burial.setBirthDate(rs.getObject("birth_date", LocalDate.class));
        burial.setBiography(rs.getString("biography"));
        //burial.setPhoto(rs.getBytes("photo"));
        burial.setXCoord(rs.getLong("xCoord"));
        burial.setYCoord(rs.getLong("yCoord"));

        User user = new User();
        user.setId(rs.getLong("guest_id"));
        burial.setUser(user);

        return burial;
    }

    public MemorialDTO toDTO(Memorial memorial) {
        if (memorial == null) {
            return null;
        }

        MemorialDTO dto = new MemorialDTO();
        dto.setId(memorial.getId());
        dto.setFio(memorial.getFio());
        
        // Добавляем маппинг новых полей ФИО
        dto.setFirstName(memorial.getFirstName());
        dto.setLastName(memorial.getLastName());
        dto.setMiddleName(memorial.getMiddleName());
        
        dto.setBirthDate(memorial.getBirthDate() != null ? memorial.getBirthDate().format(DATE_FORMATTER) : null);
        dto.setDeathDate(memorial.getDeathDate() != null ? memorial.getDeathDate().format(DATE_FORMATTER) : null);
        dto.setBiography(memorial.getBiography());
        dto.setMainLocation(memorial.getMainLocation());
        dto.setBurialLocation(memorial.getBurialLocation());
        dto.setPhotoUrl(memorial.getPhotoUrl());
        dto.setDocumentUrl(memorial.getDocumentUrl());
        dto.setPublic(memorial.isPublic());
        dto.setTreeId(memorial.getTreeId());
        dto.setCreatedBy(userMapper.toDTO(memorial.getCreatedBy()));
        dto.setCreatedAt(memorial.getCreatedAt());
        dto.setUpdatedAt(memorial.getUpdatedAt());
        
        // Добавляем копирование статуса публикации
        dto.setPublicationStatus(memorial.getPublicationStatus());
        
        if (memorial.getEditors() != null) {
            List<Long> editorIds = memorial.getEditors().stream()
                .map(User::getId)
                .collect(Collectors.toList());
            dto.setEditorIds(editorIds);
        }
        
        dto.setPendingChanges(memorial.isPendingChanges());
        
        dto.setChangesUnderModeration(memorial.isChangesUnderModeration());
        
        // Маппинг полей ожидающих изменений
        dto.setPendingPhotoUrl(memorial.getPendingPhotoUrl());
        dto.setPendingDocumentUrl(memorial.getPendingDocumentUrl());
        dto.setPendingFio(memorial.getPendingFio());
        dto.setPendingBiography(memorial.getPendingBiography());
        dto.setPendingBirthDate(memorial.getPendingBirthDate() != null ? 
                memorial.getPendingBirthDate().format(DATE_FORMATTER) : null);
        dto.setPendingDeathDate(memorial.getPendingDeathDate() != null ? 
                memorial.getPendingDeathDate().format(DATE_FORMATTER) : null);
        dto.setPendingIsPublic(memorial.getPendingIsPublic());
        dto.setPendingMainLocation(memorial.getPendingMainLocation());
        dto.setPendingBurialLocation(memorial.getPendingBurialLocation());
        
        // Маппинг pending полей для отдельных компонентов ФИО
        dto.setPendingFirstName(memorial.getPendingFirstName());
        dto.setPendingLastName(memorial.getPendingLastName());
        dto.setPendingMiddleName(memorial.getPendingMiddleName());
        
        // Маппинг количества просмотров
        dto.setViewCount(memorial.getViewCount() != null ? memorial.getViewCount() : 0);
        
        // Маппинг полей блокировки
        dto.setBlocked(memorial.isBlocked());
        dto.setBlockReason(memorial.getBlockReason());
        dto.setBlockedAt(memorial.getBlockedAt());
        dto.setBlockedBy(memorial.getBlockedBy() != null ? userMapper.toDTO(memorial.getBlockedBy()) : null);
        
        return dto;
    }

    public Memorial toEntity(MemorialDTO dto) {
        if (dto == null) {
            return null;
        }

        Memorial memorial = new Memorial();
        memorial.setId(dto.getId());
        memorial.setFio(dto.getFio());
        
        // Добавляем маппинг новых полей ФИО
        memorial.setFirstName(dto.getFirstName());
        memorial.setLastName(dto.getLastName());
        memorial.setMiddleName(dto.getMiddleName());
        
        memorial.setBirthDate(dto.getBirthDate() != null ? LocalDate.parse(dto.getBirthDate(), DATE_FORMATTER) : null);
        memorial.setDeathDate(dto.getDeathDate() != null ? LocalDate.parse(dto.getDeathDate(), DATE_FORMATTER) : null);
        memorial.setBiography(dto.getBiography());
        memorial.setMainLocation(dto.getMainLocation());
        memorial.setBurialLocation(dto.getBurialLocation());
        memorial.setPhotoUrl(dto.getPhotoUrl());
        memorial.setDocumentUrl(dto.getDocumentUrl());
        memorial.setPublic(dto.isPublic());
        memorial.setTreeId(dto.getTreeId());
        
        // Добавляем копирование статуса публикации
        memorial.setPublicationStatus(dto.getPublicationStatus());
        
        return memorial;
    }
}