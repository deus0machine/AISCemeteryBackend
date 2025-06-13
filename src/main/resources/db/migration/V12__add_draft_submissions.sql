-- Добавляем поле last_submitted_at в таблицу family_tree_drafts
ALTER TABLE family_tree_drafts 
ADD COLUMN last_submitted_at TIMESTAMP;

-- Создаем таблицу для отслеживания отправок черновиков
CREATE TABLE draft_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    draft_id BIGINT NOT NULL,
    message TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_reviewed BOOLEAN DEFAULT FALSE,
    reviewed_at TIMESTAMP,
    review_message TEXT,
    review_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    
    FOREIGN KEY (draft_id) REFERENCES family_tree_drafts(id) ON DELETE CASCADE,
    INDEX idx_draft_submissions_draft_id (draft_id),
    INDEX idx_draft_submissions_submitted_at (submitted_at),
    INDEX idx_draft_submissions_review_status (review_status)
); 