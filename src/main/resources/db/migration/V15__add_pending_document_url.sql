-- Добавляем поле для временного хранения URL документа при редактировании
ALTER TABLE memorials ADD COLUMN pending_document_url VARCHAR(500); 