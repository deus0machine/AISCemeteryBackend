-- Добавляем поля для временного хранения ФИО и публичности при редактировании

ALTER TABLE memorials ADD COLUMN pending_fio VARCHAR(255);
ALTER TABLE memorials ADD COLUMN pending_is_public BOOLEAN; 