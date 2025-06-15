-- =====================================================
-- Предоставление прав доступа пользователю cemetery_user
-- =====================================================

-- Подключиться к базе данных cemetery как пользователь postgres
-- psql -U postgres -d cemetery

-- Предоставляем права на все таблицы
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO cemetery_user;

-- Предоставляем права на все последовательности (для SERIAL полей)
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO cemetery_user;

-- Предоставляем права на схему public
GRANT USAGE ON SCHEMA public TO cemetery_user;

-- Предоставляем права на создание объектов в схеме public
GRANT CREATE ON SCHEMA public TO cemetery_user;

-- Предоставляем права по умолчанию для будущих таблиц
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO cemetery_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO cemetery_user;

-- Проверяем права доступа
\dp users
\dp system_logs
\dp memorials

-- Выводим информацию о пользователе
\du cemetery_user 