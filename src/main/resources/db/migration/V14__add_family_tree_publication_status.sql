-- V14: Добавление поля publication_status в таблицу family_trees для системы модерации

-- Добавляем колонку publication_status
ALTER TABLE family_trees 
ADD COLUMN publication_status VARCHAR(255) NOT NULL DEFAULT 'DRAFT';

-- Обновляем существующие записи: если дерево публичное, ставим PUBLISHED, иначе DRAFT
UPDATE family_trees 
SET publication_status = CASE 
    WHEN is_public = true THEN 'PUBLISHED'
    ELSE 'DRAFT'
END;

-- Добавляем индекс для оптимизации запросов по статусу публикации
CREATE INDEX idx_family_trees_publication_status ON family_trees(publication_status);

-- Добавляем составной индекс для оптимизации запросов публичных опубликованных деревьев
CREATE INDEX idx_family_trees_public_published ON family_trees(is_public, publication_status); 