package ru.cemeterysystem.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_tree_drafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyTreeDraft {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_tree_id", nullable = false)
    private FamilyTree familyTree;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editor_id", nullable = false)
    private User editor;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status = DraftStatus.DRAFT;
    
    // Данные черновика - копия данных дерева
    @Column(name = "draft_name", nullable = false)
    private String draftName;
    
    @Column(name = "draft_description", columnDefinition = "TEXT")
    private String draftDescription;
    
    @Column(name = "draft_is_public")
    private Boolean draftIsPublic = false;
    
    // Исходные данные для сравнения
    @Column(name = "original_name", nullable = false)
    private String originalName;
    
    @Column(name = "original_description", columnDefinition = "TEXT")
    private String originalDescription;
    
    @Column(name = "original_is_public")
    private Boolean originalIsPublic = false;
    
    // JSON поля для хранения копий мемориалов и связей
    @Column(name = "draft_memorials_json", columnDefinition = "TEXT")
    private String draftMemorialsJson; // JSON массив мемориалов черновика
    
    @Column(name = "draft_relations_json", columnDefinition = "TEXT")
    private String draftRelationsJson; // JSON массив связей черновика
    
    @Column(name = "original_memorials_json", columnDefinition = "TEXT")
    private String originalMemorialsJson; // JSON массив исходных мемориалов
    
    @Column(name = "original_relations_json", columnDefinition = "TEXT")
    private String originalRelationsJson; // JSON массив исходных связей
    
    @Column(columnDefinition = "TEXT")
    private String message; // Сообщение от редактора
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "last_submitted_at")
    private LocalDateTime lastSubmittedAt; // Время последней отправки на рассмотрение
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "review_message", columnDefinition = "TEXT")
    private String reviewMessage; // Комментарий владельца при рассмотрении
    
    public enum DraftStatus {
        DRAFT,          // Черновик в работе
        SUBMITTED,      // Отправлен на рассмотрение
        APPROVED,       // Одобрен владельцем
        REJECTED,       // Отклонен владельцем
        APPLIED         // Изменения применены
    }
    
    // Методы для проверки изменений
    public boolean hasNameChanged() {
        return !draftName.equals(originalName);
    }
    
    public boolean hasDescriptionChanged() {
        return !java.util.Objects.equals(draftDescription, originalDescription);
    }
    
    public boolean hasPublicStatusChanged() {
        return !java.util.Objects.equals(draftIsPublic, originalIsPublic);
    }
    
    public boolean hasAnyChanges() {
        return hasNameChanged() || hasDescriptionChanged() || hasPublicStatusChanged() || 
               hasMemorialsChanged() || hasRelationsChanged();
    }
    
    public boolean hasMemorialsChanged() {
        return !java.util.Objects.equals(draftMemorialsJson, originalMemorialsJson);
    }
    
    public boolean hasRelationsChanged() {
        return !java.util.Objects.equals(draftRelationsJson, originalRelationsJson);
    }
    
    // Метод для сброса к исходным данным
    public void resetToOriginal() {
        this.draftName = this.originalName;
        this.draftDescription = this.originalDescription;
        this.draftIsPublic = this.originalIsPublic;
        this.status = DraftStatus.DRAFT;
        this.submittedAt = null;
        this.reviewedAt = null;
        this.reviewMessage = null;
        this.message = null;
    }
} 