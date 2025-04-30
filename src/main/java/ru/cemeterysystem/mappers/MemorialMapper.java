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
        dto.setBirthDate(memorial.getBirthDate() != null ? memorial.getBirthDate().format(DATE_FORMATTER) : null);
        dto.setDeathDate(memorial.getDeathDate() != null ? memorial.getDeathDate().format(DATE_FORMATTER) : null);
        dto.setBiography(memorial.getBiography());
        dto.setMainLocation(memorial.getMainLocation());
        dto.setBurialLocation(memorial.getBurialLocation());
        dto.setPhotoUrl(memorial.getPhotoUrl());
        dto.setPublic(memorial.isPublic());
        dto.setTreeId(memorial.getTreeId());
        dto.setCreatedBy(userMapper.toDTO(memorial.getCreatedBy()));
        dto.setCreatedAt(memorial.getCreatedAt());
        dto.setUpdatedAt(memorial.getUpdatedAt());
        return dto;
    }

    public Memorial toEntity(MemorialDTO dto) {
        if (dto == null) {
            return null;
        }

        Memorial memorial = new Memorial();
        memorial.setId(dto.getId());
        memorial.setFio(dto.getFio());
        memorial.setBirthDate(dto.getBirthDate() != null ? LocalDate.parse(dto.getBirthDate(), DATE_FORMATTER) : null);
        memorial.setDeathDate(dto.getDeathDate() != null ? LocalDate.parse(dto.getDeathDate(), DATE_FORMATTER) : null);
        memorial.setBiography(dto.getBiography());
        memorial.setMainLocation(dto.getMainLocation());
        memorial.setBurialLocation(dto.getBurialLocation());
        memorial.setPhotoUrl(dto.getPhotoUrl());
        memorial.setPublic(dto.isPublic());
        memorial.setTreeId(dto.getTreeId());
        return memorial;
    }
}