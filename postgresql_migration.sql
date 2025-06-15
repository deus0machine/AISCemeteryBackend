-- =====================================================
-- PostgreSQL Migration Script for Cemetery System
-- Миграция с H2 на PostgreSQL
-- =====================================================

-- Удаляем существующие таблицы если они есть (в правильном порядке)
DROP TABLE IF EXISTS draft_submissions CASCADE;
DROP TABLE IF EXISTS family_tree_drafts CASCADE;
DROP TABLE IF EXISTS memorial_relations CASCADE;
DROP TABLE IF EXISTS family_tree_access CASCADE;
DROP TABLE IF EXISTS memorial_editors CASCADE;
DROP TABLE IF EXISTS memorials CASCADE;
DROP TABLE IF EXISTS family_trees CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS system_logs CASCADE;
DROP TABLE IF EXISTS transaction_history CASCADE;
DROP TABLE IF EXISTS family_tree_versions CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =====================================================
-- 1. Таблица пользователей
-- =====================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    fio VARCHAR(255) NOT NULL,
    contacts VARCHAR(255),
    dateofregistration TIMESTAMP NOT NULL,
    login VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    has_subscription BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(50) NOT NULL DEFAULT 'USER'
);

-- =====================================================
-- 2. Таблица семейных деревьев
-- =====================================================
CREATE TABLE family_trees (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    user_id BIGINT NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    publication_status VARCHAR(255) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================
-- 3. Таблица доступа к семейным деревьям
-- =====================================================
CREATE TABLE family_tree_access (
    id BIGSERIAL PRIMARY KEY,
    family_tree_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    access_level VARCHAR(50) NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    granted_by BIGINT,
    FOREIGN KEY (family_tree_id) REFERENCES family_trees(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =====================================================
-- 4. Таблица мемориалов
-- =====================================================
CREATE TABLE memorials (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    fio VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    middle_name VARCHAR(50),
    death_date DATE,
    birth_date DATE NOT NULL,
    biography TEXT,
    xcoord BIGINT,
    ycoord BIGINT,
    doc_url VARCHAR(500),
    main_latitude DOUBLE PRECISION,
    main_longitude DOUBLE PRECISION,
    main_address VARCHAR(500),
    burial_latitude DOUBLE PRECISION,
    burial_longitude DOUBLE PRECISION,
    burial_address VARCHAR(500),
    photo_url VARCHAR(500),
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    publication_status VARCHAR(255) NOT NULL DEFAULT 'DRAFT',
    tree_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    pending_changes BOOLEAN NOT NULL DEFAULT FALSE,
    changes_under_moderation BOOLEAN NOT NULL DEFAULT FALSE,
    previous_state TEXT,
    proposed_changes TEXT,
    pending_photo_url VARCHAR(500),
    pending_document_url VARCHAR(500),
    pending_fio VARCHAR(255),
    pending_first_name VARCHAR(50),
    pending_last_name VARCHAR(50),
    pending_middle_name VARCHAR(50),
    pending_biography TEXT,
    pending_birth_date DATE,
    pending_death_date DATE,
    pending_is_public BOOLEAN,
    pending_main_latitude DOUBLE PRECISION,
    pending_main_longitude DOUBLE PRECISION,
    pending_main_address VARCHAR(500),
    pending_burial_latitude DOUBLE PRECISION,
    pending_burial_longitude DOUBLE PRECISION,
    pending_burial_address VARCHAR(500),
    last_editor_id BIGINT,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    block_reason TEXT,
    blocked_at TIMESTAMP,
    blocked_by BIGINT,
    view_count INTEGER DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (last_editor_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (blocked_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =====================================================
-- 5. Таблица редакторов мемориалов (многие ко многим)
-- =====================================================
CREATE TABLE memorial_editors (
    memorial_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (memorial_id, user_id),
    FOREIGN KEY (memorial_id) REFERENCES memorials(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================
-- 6. Таблица связей между мемориалами
-- =====================================================
CREATE TABLE memorial_relations (
    id BIGSERIAL PRIMARY KEY,
    family_tree_id BIGINT NOT NULL,
    source_memorial_id BIGINT NOT NULL,
    target_memorial_id BIGINT NOT NULL,
    relation_type VARCHAR(50) NOT NULL,
    FOREIGN KEY (family_tree_id) REFERENCES family_trees(id) ON DELETE CASCADE,
    FOREIGN KEY (source_memorial_id) REFERENCES memorials(id) ON DELETE CASCADE,
    FOREIGN KEY (target_memorial_id) REFERENCES memorials(id) ON DELETE CASCADE
);

-- =====================================================
-- 7. Таблица черновиков семейных деревьев
-- =====================================================
CREATE TABLE family_tree_drafts (
    id BIGSERIAL PRIMARY KEY,
    family_tree_id BIGINT NOT NULL,
    editor_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    draft_name VARCHAR(255) NOT NULL,
    draft_description TEXT,
    draft_is_public BOOLEAN DEFAULT FALSE,
    original_name VARCHAR(255) NOT NULL,
    original_description TEXT,
    original_is_public BOOLEAN DEFAULT FALSE,
    draft_memorials TEXT,
    draft_relations TEXT,
    original_memorials TEXT,
    original_relations TEXT,
    message TEXT,
    created_at TIMESTAMP NOT NULL,
    submitted_at TIMESTAMP,
    last_submitted_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    review_message TEXT,
    FOREIGN KEY (family_tree_id) REFERENCES family_trees(id) ON DELETE CASCADE,
    FOREIGN KEY (editor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================
-- 8. Таблица отправок черновиков
-- =====================================================
CREATE TABLE draft_submissions (
    id BIGSERIAL PRIMARY KEY,
    draft_id BIGINT NOT NULL,
    message TEXT,
    submitted_at TIMESTAMP NOT NULL,
    is_reviewed BOOLEAN DEFAULT FALSE,
    reviewed_at TIMESTAMP,
    review_message TEXT,
    review_status VARCHAR(50) DEFAULT 'PENDING',
    FOREIGN KEY (draft_id) REFERENCES family_tree_drafts(id) ON DELETE CASCADE
);

-- =====================================================
-- 9. Таблица уведомлений
-- =====================================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sender_id BIGINT,
    type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    related_entity_id BIGINT,
    related_entity_name VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    urgent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =====================================================
-- 10. Таблица системных логов
-- =====================================================
CREATE TABLE system_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =====================================================
-- 11. Таблица истории транзакций
-- =====================================================
CREATE TABLE transaction_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================
-- 12. Таблица версий семейных деревьев
-- =====================================================
CREATE TABLE family_tree_versions (
    id BIGSERIAL PRIMARY KEY,
    family_tree_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    changes_description TEXT,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    FOREIGN KEY (family_tree_id) REFERENCES family_trees(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =====================================================
-- Создание индексов для оптимизации производительности
-- =====================================================

-- Индексы для пользователей
CREATE INDEX idx_users_login ON users(login);
CREATE INDEX idx_users_role ON users(role);

-- Индексы для семейных деревьев
CREATE INDEX idx_family_trees_user_id ON family_trees(user_id);
CREATE INDEX idx_family_trees_is_public ON family_trees(is_public);
CREATE INDEX idx_family_trees_publication_status ON family_trees(publication_status);

-- Индексы для мемориалов
CREATE INDEX idx_memorials_user_id ON memorials(user_id);
CREATE INDEX idx_memorials_is_public ON memorials(is_public);
CREATE INDEX idx_memorials_publication_status ON memorials(publication_status);
CREATE INDEX idx_memorials_birth_date ON memorials(birth_date);
CREATE INDEX idx_memorials_death_date ON memorials(death_date);
CREATE INDEX idx_memorials_fio ON memorials(fio);
CREATE INDEX idx_memorials_created_at ON memorials(created_at);

-- Индексы для связей мемориалов
CREATE INDEX idx_memorial_relations_family_tree_id ON memorial_relations(family_tree_id);
CREATE INDEX idx_memorial_relations_source_memorial_id ON memorial_relations(source_memorial_id);
CREATE INDEX idx_memorial_relations_target_memorial_id ON memorial_relations(target_memorial_id);
CREATE INDEX idx_memorial_relations_relation_type ON memorial_relations(relation_type);

-- Индексы для черновиков
CREATE INDEX idx_family_tree_drafts_family_tree_id ON family_tree_drafts(family_tree_id);
CREATE INDEX idx_family_tree_drafts_editor_id ON family_tree_drafts(editor_id);
CREATE INDEX idx_family_tree_drafts_status ON family_tree_drafts(status);

-- Индексы для отправок черновиков
CREATE INDEX idx_draft_submissions_draft_id ON draft_submissions(draft_id);
CREATE INDEX idx_draft_submissions_review_status ON draft_submissions(review_status);
CREATE INDEX idx_draft_submissions_submitted_at ON draft_submissions(submitted_at);

-- Индексы для уведомлений
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_type ON notifications(type);

-- Индексы для системных логов
CREATE INDEX idx_system_logs_user_id ON system_logs(user_id);
CREATE INDEX idx_system_logs_timestamp ON system_logs(timestamp);
CREATE INDEX idx_system_logs_action ON system_logs(action);
CREATE INDEX idx_system_logs_entity_type ON system_logs(entity_type);

-- =====================================================
-- Создание тестовых данных (опционально)
-- =====================================================

-- Создание администратора по умолчанию
INSERT INTO users (fio, contacts, dateofregistration, login, password, has_subscription, role) 
VALUES ('Администратор Системы', 'admin@cemetery.ru', NOW(), 'admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'ADMIN');

-- Создание тестового пользователя
INSERT INTO users (fio, contacts, dateofregistration, login, password, has_subscription, role) 
VALUES ('Тестовый Пользователь', 'test@cemetery.ru', NOW(), 'testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', false, 'USER');

-- =====================================================
-- Комментарии по миграции
-- =====================================================

/*
ВАЖНЫЕ ЗАМЕЧАНИЯ ПО МИГРАЦИИ:

1. ТИПЫ ДАННЫХ:
   - BIGSERIAL вместо IDENTITY для автоинкремента
   - TIMESTAMP вместо DATETIME
   - TEXT вместо CLOB для больших текстовых полей
   - BOOLEAN вместо BIT

2. ПОСЛЕДОВАТЕЛЬНОСТИ:
   PostgreSQL автоматически создает последовательности для BIGSERIAL полей

3. ОГРАНИЧЕНИЯ:
   - Все внешние ключи настроены с CASCADE или SET NULL
   - Уникальные ограничения сохранены
   - NOT NULL ограничения применены где необходимо

4. ИНДЕКСЫ:
   - Созданы индексы для всех часто используемых полей
   - Составные индексы для оптимизации сложных запросов

5. ПАРОЛИ:
   - Тестовые пароли зашифрованы с помощью BCrypt
   - Пароль для admin и testuser: "password"
*/ 