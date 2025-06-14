# Миграция ФИО: Разделение поля fio на отдельные поля

## Обзор

Данная миграция добавляет поддержку отдельных полей для имени, фамилии и отчества в модели Memorial, сохраняя при этом полную обратную совместимость с существующим полем `fio`.

**Упрощенная логика**: Поле `fio` автоматически формируется из отдельных полей `firstName`, `lastName`, `middleName`. Прямое редактирование поля `fio` отключено.

## Изменения в базе данных

### Новые поля в таблице `memorials`:
- `first_name` VARCHAR(50) - Имя
- `last_name` VARCHAR(50) - Фамилия  
- `middle_name` VARCHAR(50) - Отчество
- `pending_first_name` VARCHAR(50) - Ожидающее имя при редактировании
- `pending_last_name` VARCHAR(50) - Ожидающая фамилия при редактировании
- `pending_middle_name` VARCHAR(50) - Ожидающее отчество при редактировании

### Индексы:
- `idx_memorials_last_name` - для быстрого поиска по фамилии
- `idx_memorials_first_name` - для быстрого поиска по имени

## Изменения в коде

### 1. Модель Memorial
- Добавлены новые поля: `firstName`, `lastName`, `middleName`
- Добавлены pending поля: `pendingFirstName`, `pendingLastName`, `pendingMiddleName`
- Добавлена односторонняя синхронизация: отдельные поля → `fio`
- Добавлены утилитные методы:
  - `getFullName()` - получение полного ФИО
  - `getShortName()` - получение краткого ФИО (Фамилия И.О.)
  - `hasSeparateNameFields()` - проверка заполненности отдельных полей

### 2. MemorialDTO
- Добавлены поля `firstName`, `lastName`, `middleName`
- Добавлены pending поля для отдельных компонентов ФИО
- Добавлены утилитные методы аналогично модели

### 3. MemorialMapper
- Обновлен для маппинга новых полей в обе стороны
- Добавлен маппинг pending полей

### 4. MemorialService
- Убрана возможность прямого редактирования поля `fio`
- Обновлены методы создания и обновления мемориалов для работы только с отдельными полями
- Добавлена поддержка pending полей для отдельных компонентов ФИО

### 5. MemorialRepository
- Добавлены методы поиска по отдельным полям ФИО
- Обновлен метод `search` для поиска по всем полям ФИО

### 6. DataLoader
- Обновлен для создания тестовых мемориалов с отдельными полями ФИО

## Логика синхронизации

### Автоматическая синхронизация (в методах @PrePersist и @PreUpdate):

**Только одностороння синхронизация**: отдельные поля → `fio`

- Если заполнены `firstName`, `lastName`, `middleName` → автоматически формируется `fio`
- Прямое редактирование поля `fio` отключено
- Поле `fio` служит только для чтения и совместимости

### Правила формирования ФИО:
Формат: "Фамилия Имя Отчество" (отчество опционально)

## Обратная совместимость

- Поле `fio` сохраняется для чтения и API совместимости
- Старые клиенты могут читать поле `fio` как раньше
- Новые клиенты должны использовать отдельные поля для ввода/редактирования
- API возвращает как объединенное поле, так и отдельные поля

## Инструкции по развертыванию

### 1. Применение миграции базы данных
Миграция `V13__add_separate_name_fields.sql` будет применена автоматически при запуске приложения.

### 2. Миграция данных НЕ ТРЕБУЕТСЯ
Поскольку логика упрощена, существующие данные в поле `fio` остаются как есть для обратной совместимости. Новые мемориалы создаются только через отдельные поля.

## Примеры использования

### Создание мемориала (только через отдельные поля):
```json
{
  "firstName": "Иван",
  "lastName": "Петров", 
  "middleName": "Сергеевич",
  "birthDate": "1950-01-01",
  ...
}
```

### Редактирование мемориала (только отдельные поля):
```json
{
  "firstName": "Николай",
  "lastName": "Иванов",
  "middleName": "Петрович"
}
```

### Чтение мемориала (доступны оба формата):
```json
{
  "id": 1,
  "fio": "Петров Иван Сергеевич",  // автоматически сформировано
  "firstName": "Иван",
  "lastName": "Петров",
  "middleName": "Сергеевич",
  ...
}
```

### Поиск по фамилии:
```java
List<Memorial> memorials = memorialRepository.findByLastNameContainingIgnoreCase("Петров");
```

## Преимущества

1. **Простота логики**: только односторонняя синхронизация
2. **Контролируемый ввод**: невозможно некорректно заполнить ФИО
3. **Улучшенный поиск**: поиск по отдельным компонентам имени
4. **Корректная сортировка**: по фамилии, имени, отчеству
5. **Обратная совместимость**: старые клиенты продолжают работать
6. **Автоматическое формирование**: поле `fio` всегда корректно

## Ограничения

- Прямое редактирование поля `fio` отключено
- Все новые мемориалы должны создаваться через отдельные поля
- Старые мемориалы с заполненным только `fio` остаются как есть

## Возможные улучшения в будущем

1. Создание утилиты для разбора существующих ФИО на отдельные поля
2. Валидация корректности заполнения отдельных полей
3. Поддержка различных культурных форматов имен 