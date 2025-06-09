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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @OneToMany(mappedBy = "sourceMemorial", cascade = CascadeType.ALL)
    private List<MemorialRelation> relations = new ArrayList<>();

    // Редакторы мемориала (совместные владельцы)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "memorial_editors",
        joinColumns = @JoinColumn(name = "memorial_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIdentityReference(alwaysAsId = true)
    private Set<User> editors = new HashSet<>();

    @Column(name = "fio", nullable = false)
    @NotNull(message = "ФИО не может быть пустым")
    private String fio;

    // Новые поля для разделенного ФИО
    @Column(name = "first_name", length = 50)
    private String firstName;
    
    @Column(name = "last_name", length = 50)
    private String lastName;
    
    @Column(name = "middle_name", length = 50)
    private String middleName;

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
    
    // Статус публикации мемориала
    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, columnDefinition = "VARCHAR(255) default 'DRAFT'")
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;
    
    // Enum для статуса публикации
    public enum PublicationStatus {
        DRAFT,               // Черновик (не опубликован)
        PENDING_MODERATION,  // На модерации
        PUBLISHED,           // Опубликован
        REJECTED             // Отклонен
    }

    private Long treeId;

    @ManyToOne
    @JoinColumn(name = "created_by")
    @JsonIdentityReference(alwaysAsId = true)
    private User createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Флаг, указывающий, требуются ли изменения подтверждения основным владельцем
    @Column(name = "pending_changes", nullable = false, columnDefinition = "boolean default false")
    private boolean pendingChanges = false;
    
    // Флаг, указывающий, что изменения опубликованного мемориала находятся на модерации у администраторов
    @Column(name = "changes_under_moderation", nullable = false, columnDefinition = "boolean default false")
    private boolean changesUnderModeration = false;
    
    // Временное хранение предыдущего состояния мемориала
    @Column(name = "previous_state", columnDefinition = "TEXT")
    private String previousState;
    
    // Временное хранение предложенных изменений
    @Column(name = "proposed_changes", columnDefinition = "TEXT")
    private String proposedChanges;
    
    // Временный URL изображения при редактировании
    @Column(name = "pending_photo_url")
    private String pendingPhotoUrl;
    
    // Временные данные для ФИО при редактировании
    @Column(name = "pending_fio")
    private String pendingFio;
    
    // Временные данные для отдельных полей ФИО при редактировании
    @Column(name = "pending_first_name", length = 50)
    private String pendingFirstName;
    
    @Column(name = "pending_last_name", length = 50)
    private String pendingLastName;
    
    @Column(name = "pending_middle_name", length = 50)
    private String pendingMiddleName;
    
    // Временные данные для биографии при редактировании
    @Column(name = "pending_biography", columnDefinition = "TEXT")
    private String pendingBiography;
    
    // Временные данные для даты рождения при редактировании
    @Column(name = "pending_birth_date")
    private LocalDate pendingBirthDate;
    
    // Временные данные для даты смерти при редактировании
    @Column(name = "pending_death_date")
    private LocalDate pendingDeathDate;
    
    // Временные данные для публичности при редактировании
    @Column(name = "pending_is_public")
    private Boolean pendingIsPublic;
    
    // Временные данные для основного местоположения при редактировании
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "pending_main_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "pending_main_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "pending_main_address"))
    })
    private Location pendingMainLocation;
    
    // Временные данные для места захоронения при редактировании
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "pending_burial_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "pending_burial_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "pending_burial_address"))
    })
    private Location pendingBurialLocation;
    
    // ID пользователя, который внес последние изменения
    @Column(name = "last_editor_id")
    private Long lastEditorId;
    
    // Флаг блокировки мемориала администратором
    @Column(name = "is_blocked", nullable = false, columnDefinition = "boolean default false")
    private boolean isBlocked = false;
    
    // Причина блокировки мемориала
    @Column(name = "block_reason", columnDefinition = "TEXT")
    private String blockReason;
    
    // Дата блокировки
    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;
    
    // Администратор, который заблокировал мемориал
    @ManyToOne
    @JoinColumn(name = "blocked_by")
    @JsonIdentityReference(alwaysAsId = true)
    private User blockedBy;

    // Счетчик просмотров
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        syncFioFields();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        syncFioFields();
    }

    public Memorial(User user, String fio, LocalDate deathDate, LocalDate birthDate) {
        this.user = user;
        this.fio = fio;
        this.deathDate = deathDate;
        this.birthDate = birthDate;
    }
    
    // Метод для проверки, является ли пользователь редактором
    public boolean isEditor(User user) {
        return editors.contains(user);
    }
    
    // Метод для проверки, является ли пользователь владельцем
    public boolean isOwner(User user) {
        return this.user.equals(user);
    }
    
    // Метод для добавления редактора
    public void addEditor(User editor) {
        editors.add(editor);
    }
    
    // Метод для удаления редактора
    public void removeEditor(User editor) {
        editors.remove(editor);
    }

    // Геттер для editors - убедимся, что он есть
    public Set<User> getEditors() {
        return editors;
    }

    /**
     * Синхронизация полей ФИО - автоматически вызывается при сохранении/обновлении
     * Собирает поле fio из отдельных полей firstName, lastName, middleName
     */
    private void syncFioFields() {
        // Всегда собираем fio из отдельных полей, если они заполнены
        if (firstName != null && lastName != null && 
            !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
            fio = buildFullName(firstName.trim(), lastName.trim(), 
                    middleName != null ? middleName.trim() : null);
        }
        // Если отдельные поля не заполнены, но есть fio - оставляем как есть
        // (для обратной совместимости со старыми данными)
    }
    
    /**
     * Собирает полное ФИО из отдельных частей
     */
    private String buildFullName(String firstName, String lastName, String middleName) {
        StringBuilder sb = new StringBuilder();
        if (lastName != null && !lastName.isEmpty()) {
            sb.append(lastName);
        }
        if (firstName != null && !firstName.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(firstName);
        }
        if (middleName != null && !middleName.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName);
        }
        return sb.toString();
    }
    
    /**
     * Получает полное ФИО, приоритезируя отдельные поля над объединенным
     */
    public String getFullName() {
        if (firstName != null && lastName != null && 
            !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
            return buildFullName(firstName.trim(), lastName.trim(), 
                    middleName != null ? middleName.trim() : null);
        } else {
            return fio != null ? fio : "";
        }
    }
    
    /**
     * Получает краткое ФИО (Фамилия И.О.)
     */
    public String getShortName() {
        if (firstName != null && lastName != null && 
            !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(lastName.trim());
            sb.append(" ");
            sb.append(firstName.trim().charAt(0)).append(".");
            if (middleName != null && !middleName.trim().isEmpty()) {
                sb.append(middleName.trim().charAt(0)).append(".");
            }
            return sb.toString();
        } else if (fio != null && !fio.trim().isEmpty()) {
            // Пытаемся сократить объединенное ФИО
            String[] parts = fio.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            if (parts.length >= 1) {
                sb.append(parts[0]); // Фамилия
            }
            if (parts.length >= 2) {
                sb.append(" ").append(parts[1].charAt(0)).append(".");
            }
            if (parts.length >= 3) {
                sb.append(parts[2].charAt(0)).append(".");
            }
            return sb.toString();
        } else {
            return "";
        }
    }
    
    /**
     * Проверяет, заполнены ли отдельные поля ФИО
     */
    public boolean hasSeparateNameFields() {
        return firstName != null && lastName != null && 
               !firstName.trim().isEmpty() && !lastName.trim().isEmpty();
    }
}
