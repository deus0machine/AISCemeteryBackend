# Инструкция по исправлению проблем с правами доступа PostgreSQL

## Проблема
Приложение не может получить доступ к таблицам PostgreSQL из-за недостаточных прав пользователя `cemetery_user`.

**Ошибки:**
- `ОШИБКА: нет доступа к таблице system_logs`
- `ОШИБКА: нет доступа к таблице users`

## Решение

### Вариант 1: Быстрое исправление (рекомендуется)

1. **Подключитесь к PostgreSQL как администратор:**
   ```bash
   psql -U postgres -d cemetery
   ```

2. **Выполните команды предоставления прав:**
   ```sql
   -- Предоставляем права на все таблицы
   GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO cemetery_user;
   
   -- Предоставляем права на все последовательности (для SERIAL полей)
   GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO cemetery_user;
   
   -- Предоставляем права на схему public
   GRANT USAGE ON SCHEMA public TO cemetery_user;
   GRANT CREATE ON SCHEMA public TO cemetery_user;
   
   -- Предоставляем права по умолчанию для будущих таблиц
   ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO cemetery_user;
   ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO cemetery_user;
   ```

3. **Выйдите из psql:**
   ```sql
   \q
   ```

### Вариант 2: Полное пересоздание базы данных

1. **Выполните скрипт пересоздания базы:**
   ```bash
   psql -U postgres -f recreate_database.sql
   ```

2. **Выполните миграцию заново:**
   ```bash
   psql -U postgres -d cemetery -f postgresql_migration_fixed.sql
   ```

### Вариант 3: Использование готового скрипта

Выполните готовый скрипт предоставления прав:
```bash
psql -U postgres -d cemetery -f grant_permissions.sql
```

## Проверка результата

После выполнения любого из вариантов запустите приложение:

```bash
cd CemeterySystem
mvn spring-boot:run
```

Приложение должно успешно запуститься без ошибок доступа к таблицам.

## Тестовые пользователи

После успешного запуска будут доступны следующие пользователи:

- **Администратор:**
  - Логин: `admin`
  - Пароль: `password`

- **Тестовый пользователь:**
  - Логин: `testuser`
  - Пароль: `password`

## Дополнительная информация

- База данных: `cemetery`
- Пользователь БД: `cemetery_user`
- Пароль БД: `cemetery_password`
- Порт: `5432`
- Хост: `localhost`

## Устранение неполадок

Если проблемы продолжаются:

1. **Проверьте права пользователя:**
   ```sql
   \du cemetery_user
   ```

2. **Проверьте права на таблицы:**
   ```sql
   \dp users
   \dp system_logs
   ```

3. **Убедитесь, что пользователь может подключиться:**
   ```bash
   psql -U cemetery_user -d cemetery -h localhost
   ``` 