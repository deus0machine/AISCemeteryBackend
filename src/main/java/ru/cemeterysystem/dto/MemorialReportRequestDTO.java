package ru.cemeterysystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemorialReportRequestDTO {
    private Long memorialId;
    private String reason;
} 