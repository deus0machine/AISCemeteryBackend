-- Создание таблицы черновиков изменений семейных деревьев
CREATE TABLE family_tree_drafts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_tree_id BIGINT NOT NULL,
    editor_id BIGINT NOT NULL,
    status ENUM('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'APPLIED') NOT NULL DEFAULT 'DRAFT',
    
    -- Данные черновика (редактируемые)
    draft_name VARCHAR(255) NOT NULL,
    draft_description TEXT,
    draft_is_public BOOLEAN DEFAULT FALSE,
    
    -- Исходные данные для сравнения
    original_name VARCHAR(255) NOT NULL,
    original_description TEXT,
    original_is_public BOOLEAN DEFAULT FALSE,
    
    message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL,
    reviewed_at TIMESTAMP NULL,
    review_message TEXT,
    
    FOREIGN KEY (family_tree_id) REFERENCES family_trees(id) ON DELETE CASCADE,
    FOREIGN KEY (editor_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_family_tree_drafts_tree_editor (family_tree_id, editor_id),
    INDEX idx_family_tree_drafts_status (status),
    INDEX idx_family_tree_drafts_created (created_at),
    INDEX idx_family_tree_drafts_submitted (submitted_at),
    
    -- Ограничение: только один активный черновик на редактора для дерева
    UNIQUE KEY unique_active_draft (family_tree_id, editor_id, status)
);

-- Удаляем старую таблицу изменений, так как теперь не нужна
-- DROP TABLE IF EXISTS family_tree_draft_changes; 