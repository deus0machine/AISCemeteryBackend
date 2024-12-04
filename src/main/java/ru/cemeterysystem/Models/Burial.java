package ru.cemeterysystem.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String fio;
    @Column(name = "death_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date deathDate;
    @Column(name = "birth_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date birthDate;

    @Column(name = "biography")
    private String biography;

    @Column(name = "photo")
    private byte[] photo;
}
