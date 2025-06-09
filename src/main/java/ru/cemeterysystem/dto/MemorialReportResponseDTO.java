package ru.cemeterysystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemorialReportResponseDTO {
    private String status;
    private String message;
} 