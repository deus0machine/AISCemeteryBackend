-- Добавление полей для хранения предыдущего состояния мемориала и ID последнего редактора

-- Добавляем поле для хранения предыдущего состояния мемориала
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS previous_state TEXT;

-- Добавляем поле для хранения ID последнего редактора
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS last_editor_id BIGINT;

-- Обновим все существующие записи с NULL в новых полях
UPDATE memorials SET previous_state = NULL, last_editor_id = NULL;

-- Добавляем внешний ключ для last_editor_id, ссылающийся на таблицу users
ALTER TABLE memorials ADD CONSTRAINT fk_memorial_last_editor
    FOREIGN KEY (last_editor_id) REFERENCES users(id) ON DELETE SET NULL; 