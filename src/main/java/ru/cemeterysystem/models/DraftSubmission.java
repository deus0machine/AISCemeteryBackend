package ru.cemeterysystem.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "draft_submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftSubmission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id", nullable = false)
    private FamilyTreeDraft draft;
    
    @Column(columnDefinition = "TEXT")
    private String message; // Сообщение от редактора
    
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();
    
    @Column(name = "is_reviewed")
    private Boolean isReviewed = false;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "review_message", columnDefinition = "TEXT")
    private String reviewMessage; // Ответ владельца
    
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status")
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;
    
    public enum ReviewStatus {
        PENDING,    // Ожидает рассмотрения
        APPROVED,   // Одобрено
        REJECTED    // Отклонено
    }
} 