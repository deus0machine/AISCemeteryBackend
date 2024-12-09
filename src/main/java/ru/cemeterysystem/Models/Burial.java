package ru.cemeterysystem.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "burials")
public class Burial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fio", nullable = false)
    @NotNull(message = "ФИО не может быть пустым")
    private String fio;

    @Column(name = "death_date", nullable = false)
    @Temporal(TemporalType.DATE)
    @NotNull(message = "Дата смерти не может быть пустой")
    private LocalDate deathDate;

    @Column(name = "birth_date", nullable = false)
    @Temporal(TemporalType.DATE)
    @NotNull(message = "Дата рождения не может быть пустой")
    private LocalDate birthDate;

    @Column(name = "biography")
    private String biography;

    @Column(name = "photo")
    private byte[] photo;

    @Column(name = "xCoord")
    private Long xCoord;
    @Column(name = "yCoord")
    private Long yCoord;
}
