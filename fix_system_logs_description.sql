-- =====================================================
-- Исправление типа поля description в таблице system_logs
-- Проблема: поле description имеет тип bytea вместо varchar/text
-- =====================================================

-- Проверим текущий тип поля
SELECT column_name, data_type, is_nullable, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'system_logs' AND column_name = 'description';

-- Если description имеет тип bytea, исправляем его на varchar(255)
DO $$
BEGIN
    -- Проверяем, существует ли таблица system_logs
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'system_logs') THEN
        
        -- Проверяем, имеет ли поле description неправильный тип
        IF EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'system_logs' 
            AND column_name = 'description' 
            AND data_type = 'bytea'
        ) THEN
            RAISE NOTICE 'Исправляем тип поля description с bytea на varchar(255)';
            
            -- Создаем резервную копию данных (если есть)
            CREATE TEMP TABLE system_logs_backup AS 
            SELECT id, action_type, entity_type, entity_id, 
                   CASE 
                       WHEN description IS NOT NULL THEN 
                           convert_from(description, 'UTF8')
                       ELSE NULL 
                   END as description_text,
                   details, user_id, ip_address, user_agent, created_at, severity
            FROM system_logs;
            
            -- Удаляем старое поле
            ALTER TABLE system_logs DROP COLUMN description;
            
            -- Добавляем новое поле с правильным типом
            ALTER TABLE system_logs ADD COLUMN description VARCHAR(255) NOT NULL DEFAULT '';
            
            -- Восстанавливаем данные
            UPDATE system_logs 
            SET description = backup.description_text
            FROM system_logs_backup backup
            WHERE system_logs.id = backup.id
            AND backup.description_text IS NOT NULL;
            
            RAISE NOTICE 'Поле description успешно исправлено';
            
        ELSIF EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'system_logs' 
            AND column_name = 'description' 
            AND data_type IN ('character varying', 'varchar', 'text')
        ) THEN
            RAISE NOTICE 'Поле description уже имеет правильный тип';
        ELSE
            RAISE NOTICE 'Поле description не найдено или имеет неожиданный тип';
        END IF;
        
    ELSE
        RAISE NOTICE 'Таблица system_logs не найдена';
    END IF;
END $$;

-- Проверяем результат
SELECT column_name, data_type, is_nullable, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'system_logs' AND column_name = 'description';

-- Добавляем индекс для быстрого поиска по описанию (если его нет)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'system_logs' 
        AND indexname = 'idx_system_logs_description'
    ) THEN
        CREATE INDEX idx_system_logs_description ON system_logs(description);
        RAISE NOTICE 'Создан индекс idx_system_logs_description';
    ELSE
        RAISE NOTICE 'Индекс idx_system_logs_description уже существует';
    END IF;
END $$; 