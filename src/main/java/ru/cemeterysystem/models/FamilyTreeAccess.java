package ru.cemeterysystem.models;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "family_tree_access")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class FamilyTreeAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_tree_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty("familyTree")
    private FamilyTree familyTree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty("user")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty("accessLevel")
    private AccessLevel accessLevel;

    @Column(name = "granted_at", nullable = false)
    @JsonProperty("grantedAt")
    private LocalDateTime grantedAt;

    @Column(name = "granted_by", nullable = false)
    @JsonProperty("grantedById")
    private Long grantedById;

    @JsonBackReference
    public FamilyTree getFamilyTree() {
        return familyTree;
    }

    public enum AccessLevel {
        VIEWER,     // Только просмотр
        EDITOR,     // Может редактировать связи
        ADMIN       // Полный доступ к управлению деревом
    }

    @PrePersist
    protected void onCreate() {
        grantedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FamilyTreeAccess)) return false;
        FamilyTreeAccess that = (FamilyTreeAccess) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
} 