-- Добавляем поле для отслеживания изменений опубликованных мемориалов на модерации
ALTER TABLE memorials 
ADD COLUMN changes_under_moderation BOOLEAN NOT NULL DEFAULT FALSE;

-- Добавляем комментарий к полю
COMMENT ON COLUMN memorials.changes_under_moderation IS 'Флаг, указывающий что изменения опубликованного мемориала находятся на модерации у администраторов'; 