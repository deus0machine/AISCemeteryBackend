-- Увеличиваем длину поля title в таблице notifications с 50 до 255 символов
ALTER TABLE notifications ALTER COLUMN title TYPE VARCHAR(255); 