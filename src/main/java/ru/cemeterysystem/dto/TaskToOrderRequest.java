package ru.cemeterysystem.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TaskToOrderRequest {
    private Long guestId;
    private Long burialId;
    private Long taskId;
    private String image; // Base64 строка
}