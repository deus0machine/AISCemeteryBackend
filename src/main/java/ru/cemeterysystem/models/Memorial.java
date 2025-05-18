package ru.cemeterysystem.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "memorials")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Memorial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // Внешний ключ, связывающий заказ с пользователем
    @JsonIdentityReference(alwaysAsId = true)
    private User user;

    @OneToMany(mappedBy = "memorial", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIdentityReference(alwaysAsId = true)
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

    @Column(name = "xCoord")
    private Long xCoord;
    @Column(name = "yCoord")
    private Long yCoord;

    //Сделать поле для хранения документа, подтверждающего существование человека и его смерть
    @Column(name = "doc_url")
    String documentUrl;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "main_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "main_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "main_address"))
    })
    private Location mainLocation;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "burial_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "burial_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "burial_address"))
    })
    private Location burialLocation;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "is_public", nullable = false, columnDefinition = "boolean default false")
    @JsonProperty("is_public")
    private boolean isPublic;

    private Long treeId;

    @ManyToOne
    @JoinColumn(name = "created_by")
    @JsonIdentityReference(alwaysAsId = true)
    private User createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Memorial(User user, String fio, LocalDate deathDate, LocalDate birthDate) {
        this.user = user;
        this.fio = fio;
        this.deathDate = deathDate;
        this.birthDate = birthDate;
    }
}
