package ru.cemeterysystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO для запросов на добавление или удаление редакторов мемориала
 */
@Data
public class EditorRequestDTO {
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("action")
    private String action; // "add" или "remove"
} 