-- Добавляем JSON поля для хранения копий мемориалов и связей в черновиках
ALTER TABLE family_tree_drafts 
ADD COLUMN draft_memorials TEXT,
ADD COLUMN draft_relations TEXT,
ADD COLUMN original_memorials TEXT,
ADD COLUMN original_relations TEXT; 