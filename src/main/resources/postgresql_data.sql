-- Тестовые данные для PostgreSQL
-- Эквивалент DataLoader.java в SQL формате

-- Пользователи
INSERT INTO users (fio, login, password, role, dateofregistration, has_subscription, contacts) VALUES 
('ADMIN', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye8Y.CbsJFZCWqEHkT4H7M3MLXC/q7bD6', 'ADMIN', NOW(), true, 'admin@example.com'),
('Севостьянов Сергей Вячеславович', '1111', '$2a$10$N9qo8uLOickgx2ZMRZoMye8Y.CbsJFZCWqEHkT4H7M3MLXC/q7bD6', 'USER', NOW(), false, '7821872677'),
('Тестова Подписка Подписковна', '2222', '$2a$10$N9qo8uLOickgx2ZMRZoMye8Y.CbsJFZCWqEHkT4H7M3MLXC/q7bD6', 'USER', NOW(), true, '7937287734');

-- Мемориалы
INSERT INTO memorials (user_id, created_by, first_name, last_name, middle_name, death_date, birth_date, is_public, publication_status, fio, created_at) VALUES 
-- Пользователь 2 (1111)
(2, 2, 'Иван', 'Иванов', 'Иванович', '2023-01-15', '1980-05-20', false, 'DRAFT', 'Иванов Иван Иванович', NOW()),
(2, 2, 'Анна', 'Иванова', 'Сергеевна', '2023-02-10', '1990-07-30', false, 'DRAFT', 'Иванова Анна Сергеевна', NOW()),
(2, 2, 'Владимир', 'Иванов', 'Викторович', '2024-12-18', '2003-09-19', false, 'DRAFT', 'Иванов Владимир Викторович', NOW()),
(2, 2, 'Мария', 'Сергеева', 'Петровна', '2022-03-05', '1955-08-12', false, 'DRAFT', 'Сергеева Мария Петровна', NOW()),
(2, 2, 'Елена', 'Иванова', 'Ивановна', '2025-04-22', '2005-11-03', false, 'DRAFT', 'Иванова Елена Ивановна', NOW()),
-- Админ (пользователь 1)
(1, 1, 'Андрей', 'Сергеев', 'Иванович', '1980-05-15', '1950-10-10', true, 'PUBLISHED', 'Сергеев Андрей Иванович', NOW()),
(1, 1, 'Иван', 'Сергеев', 'Андреевич', '1995-09-01', '1925-04-07', true, 'PUBLISHED', 'Сергеев Иван Андреевич', NOW()),
(1, 1, 'Ольга', 'Сергеева', 'Николаевна', '1998-06-14', '1928-12-15', true, 'PUBLISHED', 'Сергеева Ольга Николаевна', NOW()),
-- Пользователь 3 (2222)
(3, 3, 'выыав', 'Сергапвеева', 'вапавпав', '1998-06-14', '1928-12-15', false, 'DRAFT', 'Сергапвеева выыав вапавпав', NOW());

-- Семейное дерево
INSERT INTO family_trees (name, description, user_id, is_public, publication_status, created_at) VALUES 
('Семья Ивановых', 'Генеалогическое древо семьи Ивановых', 2, true, 'PUBLISHED', NOW());

-- Связи между мемориалами (предполагаем, что ID мемориалов будут 1-9)
INSERT INTO memorial_relations (family_tree_id, source_memorial_id, target_memorial_id, relation_type) VALUES 
-- Супружеские связи
(1, 1, 2, 'SPOUSE'), -- Иван + Анна
(1, 7, 8, 'SPOUSE'), -- Иван_Андр + Ольга (прародители)
-- Родительские связи (родитель → ребенок)
(1, 7, 6, 'PARENT'), -- Иван_Андр → Андрей
(1, 8, 6, 'PARENT'), -- Ольга → Андрей
(1, 7, 5, 'PARENT'), -- Иван_Андр → Мария
(1, 8, 5, 'PARENT'), -- Ольга → Мария
(1, 7, 1, 'PARENT'), -- Иван_Андр → Иван (основной)
(1, 8, 1, 'PARENT'), -- Ольга → Иван (основной)
-- Дети основной пары
(1, 1, 3, 'PARENT'), -- Иван → Владимир
(1, 2, 3, 'PARENT'), -- Анна → Владимир
(1, 1, 4, 'PARENT'), -- Иван → Елена
(1, 2, 4, 'PARENT'); -- Анна → Елена

-- Уведомления
INSERT INTO notifications (user_id, type, title, message, is_read, created_at) VALUES 
(2, 'FAMILY_TREE_CREATED', 'Семейное дерево создано', 'Ваше семейное дерево "Семья Ивановых" было успешно создано', false, NOW()),
(2, 'MEMORIAL_ADDED', 'Мемориал добавлен', 'Новый мемориал добавлен в ваше семейное дерево', false, NOW()),
(1, 'SYSTEM_NOTIFICATION', 'Добро пожаловать', 'Добро пожаловать в систему Cemetery System', false, NOW()); 