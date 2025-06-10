package ru.cemeterysystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemorialOwnershipRequestDTO {
    private String receiverId;
    private String memorialId;
    private String message;
} 