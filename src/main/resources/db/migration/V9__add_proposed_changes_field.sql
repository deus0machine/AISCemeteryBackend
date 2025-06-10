-- Добавление поля для хранения предложенных изменений мемориала

-- Добавляем поле для хранения предложенных изменений
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS proposed_changes TEXT;

-- Обновляем все существующие записи с NULL в новом поле
UPDATE memorials SET proposed_changes = NULL; 