package ru.cemeterysystem.mappers;

import org.springframework.jdbc.core.RowMapper;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.models.Memorial;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class MemorialMapper implements RowMapper<Memorial> {
    @Override
    public Memorial mapRow(ResultSet rs, int rowNum) throws SQLException {
        Memorial burial = new Memorial();
        burial.setId(rs.getLong("id"));
        burial.setFio(rs.getString("fio"));
        burial.setDeathDate(rs.getObject("death_date", LocalDate.class));
        burial.setBirthDate(rs.getObject("birth_date", LocalDate.class));
        burial.setBiography(rs.getString("biography"));
        burial.setPhoto(rs.getBytes("photo"));
        burial.setXCoord(rs.getLong("xCoord"));
        burial.setYCoord(rs.getLong("yCoord"));

        User user = new User();
        user.setId(rs.getLong("guest_id"));
        burial.setUser(user);

        return burial;
    }
}