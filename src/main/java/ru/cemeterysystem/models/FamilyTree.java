package ru.cemeterysystem.models;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "family_trees")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class FamilyTree {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty("user")
    private User user;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @JsonProperty("is_public")
    private boolean isPublic;

    // Статус публикации дерева
    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, columnDefinition = "VARCHAR(255) default 'DRAFT'")
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;
    
    // Enum для статуса публикации (используем тот же что и у Memorial)
    public enum PublicationStatus {
        DRAFT,               // Черновик (не опубликован)
        PENDING_MODERATION,  // На модерации
        PUBLISHED,           // Опубликован
        REJECTED             // Отклонен
    }

    @Column(name = "created_at", nullable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "familyTree", cascade = CascadeType.ALL, orphanRemoval = true)
    //@JsonIdentityReference(alwaysAsId = true)
    @JsonProperty("memorialRelations")
    private List<MemorialRelation> memorialRelations;

    @OneToMany(mappedBy = "familyTree", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty("accessList")
    @JsonManagedReference
    private List<FamilyTreeAccess> accessList;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 