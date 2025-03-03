package ru.cemeterysystem.models;

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
import java.util.List;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "memorials")
public class Memorial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // Внешний ключ, связывающий заказ с пользователем
    @JsonBackReference("user-memorial")
    private User user;

    @JsonManagedReference("memorial-order")
    @OneToMany(mappedBy = "memorial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    @Column(name = "fio", nullable = false)
    @NotNull(message = "ФИО не может быть пустым")
    private String fio;

    @Column(name = "death_date")
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

    //Сделать поле для хранения документа, подтверждающего существование человека и его смерть
    @Column(name = "doc_url")
    String documentUrl;
    public Memorial(User user, String fio, LocalDate deathDate, LocalDate birthDate) {
        this.user = user;
        this.fio = fio;
        this.deathDate = deathDate;
        this.birthDate = birthDate;
    }
}
