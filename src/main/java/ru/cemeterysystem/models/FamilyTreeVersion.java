package ru.cemeterysystem.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "family_tree_versions")
public class FamilyTreeVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_tree_id", nullable = false)
    private FamilyTree familyTree;

    @Column(nullable = false)
    private String version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private Long createdById;

    @Column(length = 1000)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String snapshot; // JSON-представление состояния дерева на момент создания версии

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 