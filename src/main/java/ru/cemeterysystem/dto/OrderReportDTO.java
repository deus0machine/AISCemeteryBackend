package ru.cemeterysystem.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderReportDTO {
    private Long id;
    private String orderName;
    private String orderDescription;
    private Long orderCost;
    private String orderDate; // Формат: yyyy-MM-dd
    private Long guestId;
    private String guestName;
    private Long burialId;
    private String burialName;
    private boolean isCompleted;
}
