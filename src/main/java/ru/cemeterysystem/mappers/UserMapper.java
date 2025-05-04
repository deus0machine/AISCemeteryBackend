package ru.cemeterysystem.mappers;

import org.springframework.stereotype.Component;
import ru.cemeterysystem.dto.UserDTO;
import ru.cemeterysystem.models.User;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class UserMapper {
    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFio(user.getFio());
        dto.setContacts(user.getContacts());
        dto.setLogin(user.getLogin());
        dto.setDateOfRegistration(user.getDateOfRegistration().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime());
        return dto;
    }
} 