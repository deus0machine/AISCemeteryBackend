-- Добавляем поля для хранения ожидающих изменений в таблицу memorials

-- Поля для дат
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS pending_birth_date DATE;
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS pending_death_date DATE;

-- Поле для биографии
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS pending_biography TEXT;

-- Поля для основного местоположения
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS pending_main_latitude DOUBLE PRECISION;
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS pending_main_longitude DOUBLE PRECISION;
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS pending_main_address VARCHAR(500);

-- Поля для места захоронения
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS pending_burial_latitude DOUBLE PRECISION;
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS pending_burial_longitude DOUBLE PRECISION;
ALTER TABLE memorials ADD COLUMN IF NOT EXISTS pending_burial_address VARCHAR(500);

-- Добавляем индексы для полей с координатами
CREATE INDEX IF NOT EXISTS idx_memorial_pending_main_coords ON memorials (pending_main_latitude, pending_main_longitude);
CREATE INDEX IF NOT EXISTS idx_memorial_pending_burial_coords ON memorials (pending_burial_latitude, pending_burial_longitude); 