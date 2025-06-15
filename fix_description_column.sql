-- Исправление типа столбца description с bytea на text
-- Сначала создаем временный столбец
ALTER TABLE system_logs ADD COLUMN description_temp TEXT;

-- Копируем данные, конвертируя bytea в text
UPDATE system_logs SET description_temp = CASE 
    WHEN description IS NOT NULL THEN convert_from(description, 'UTF8')
    ELSE NULL 
END;

-- Удаляем старый столбец
ALTER TABLE system_logs DROP COLUMN description;

-- Переименовываем временный столбец
ALTER TABLE system_logs RENAME COLUMN description_temp TO description;

-- Добавляем ограничение длины как в модели
ALTER TABLE system_logs ALTER COLUMN description TYPE VARCHAR(1000); 