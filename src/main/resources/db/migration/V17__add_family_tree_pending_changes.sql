-- Добавляем поля для модерации изменений семейных деревьев
ALTER TABLE family_trees 
ADD COLUMN pending_changes BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE family_trees 
ADD COLUMN pending_name VARCHAR(255);

ALTER TABLE family_trees 
ADD COLUMN pending_description TEXT;

-- Добавляем комментарии для ясности
COMMENT ON COLUMN family_trees.pending_changes IS 'Флаг наличия изменений ожидающих модерации';
COMMENT ON COLUMN family_trees.pending_name IS 'Предлагаемое новое название дерева';
COMMENT ON COLUMN family_trees.pending_description IS 'Предлагаемое новое описание дерева'; 