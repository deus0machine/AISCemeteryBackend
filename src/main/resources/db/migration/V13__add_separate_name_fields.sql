-- Добавление отдельных полей для ФИО
-- Миграция V13: разделение поля fio на firstName, lastName, middleName

-- Добавляем новые поля для разделенного ФИО
ALTER TABLE memorials ADD COLUMN first_name VARCHAR(50);
ALTER TABLE memorials ADD COLUMN last_name VARCHAR(50);
ALTER TABLE memorials ADD COLUMN middle_name VARCHAR(50);

-- Добавляем pending поля для отдельных компонентов ФИО
ALTER TABLE memorials ADD COLUMN pending_first_name VARCHAR(50);
ALTER TABLE memorials ADD COLUMN pending_last_name VARCHAR(50);
ALTER TABLE memorials ADD COLUMN pending_middle_name VARCHAR(50);

-- Создаем индексы для быстрого поиска по фамилии и имени
CREATE INDEX idx_memorials_last_name ON memorials(last_name);
CREATE INDEX idx_memorials_first_name ON memorials(first_name);

-- Комментарии для полей
COMMENT ON COLUMN memorials.first_name IS 'Имя персоны';
COMMENT ON COLUMN memorials.last_name IS 'Фамилия персоны';  
COMMENT ON COLUMN memorials.middle_name IS 'Отчество персоны';
COMMENT ON COLUMN memorials.pending_first_name IS 'Ожидающее имя при редактировании';
COMMENT ON COLUMN memorials.pending_last_name IS 'Ожидающая фамилия при редактировании';
COMMENT ON COLUMN memorials.pending_middle_name IS 'Ожидающее отчество при редактировании';

-- Поле fio остается для обратной совместимости
COMMENT ON COLUMN memorials.fio IS 'Полное ФИО (формируется автоматически из отдельных полей)'; 