package ru.cemeterysystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.cemeterysystem.models.Notification;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long userId;
    private Long senderId;
    private String senderName;
    private String title;
    private String message;
    private String type;
    private String status;
    private Long relatedEntityId;
    private String relatedEntityName;
    private boolean read;
    private String createdAt;
} 