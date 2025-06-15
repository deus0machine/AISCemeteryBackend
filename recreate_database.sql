-- =====================================================
-- Скрипт для пересоздания базы данных cemetery
-- Выполнить как пользователь postgres
-- =====================================================

-- Отключаем все соединения к базе данных
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'cemetery';

-- Удаляем базу данных если она существует
DROP DATABASE IF EXISTS cemetery;

-- Удаляем пользователя если он существует
DROP USER IF EXISTS cemetery_user;

-- Создаем пользователя заново
CREATE USER cemetery_user WITH PASSWORD 'cemetery_password';

-- Создаем базу данных
CREATE DATABASE cemetery OWNER cemetery_user;

-- Предоставляем права на базу данных
GRANT ALL PRIVILEGES ON DATABASE cemetery TO cemetery_user;

-- Подключаемся к базе данных cemetery
\c cemetery

-- Предоставляем права на схему public
GRANT ALL PRIVILEGES ON SCHEMA public TO cemetery_user;
GRANT CREATE ON SCHEMA public TO cemetery_user;

-- Устанавливаем права по умолчанию
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO cemetery_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO cemetery_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO cemetery_user; 