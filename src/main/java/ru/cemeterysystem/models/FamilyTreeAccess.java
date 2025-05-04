package ru.cemeterysystem.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "family_tree_access")
public class FamilyTreeAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_tree_id", nullable = false)
    private FamilyTree familyTree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessLevel accessLevel;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "granted_by", nullable = false)
    private Long grantedById;

    public enum AccessLevel {
        VIEWER,     // Только просмотр
        EDITOR,     // Может редактировать связи
        ADMIN       // Полный доступ к управлению деревом
    }

    @PrePersist
    protected void onCreate() {
        grantedAt = LocalDateTime.now();
    }
} 