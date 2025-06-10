package ru.cemeterysystem.models;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Embeddable
public class Location {
    private Double latitude;
    private Double longitude;
    private String address;
} 