package ru.cemeterysystem.Models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
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

    @ManyToOne
    @JoinColumn(name = "guest_id", nullable = false) // Внешний ключ, связывающий заказ с пользователем
    @JsonBackReference("guest-burial")
    private Guest guest;

    @JsonManagedReference("burial-order")
    @OneToMany(mappedBy = "burial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    @Column(name = "fio", nullable = false)
    @NotNull(message = "ФИО не может быть пустым")
    private String fio;

    @Column(name = "death_date", nullable = false)
    @NotNull(message = "Дата смерти не может быть пустой")
    private LocalDate deathDate;

    @Column(name = "birth_date", nullable = false)
    @NotNull(message = "Дата рождения не может быть пустой")
    private LocalDate birthDate;

    @Column(name = "biography")
    private String biography;

    @Lob
    @Column(name = "photo")
    private byte[] photo;

    @Column(name = "xCoord")
    private Long xCoord;
    @Column(name = "yCoord")
    private Long yCoord;

    public Burial(Guest guest, String fio, LocalDate deathDate, LocalDate birthDate) {
        this.guest = guest;
        this.fio = fio;
        this.deathDate = deathDate;
        this.birthDate = birthDate;
    }
}
