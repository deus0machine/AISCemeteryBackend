# Система логирования действий пользователей

## Обзор

Система логирования предоставляет комплексное отслеживание всех действий пользователей в приложении управления кладбищем. Она автоматически собирает данные о действиях пользователей, системных событиях и ошибках.

## Компоненты системы

### 1. Аннотация `@LogActivity`

Используется для автоматического логирования методов сервисов:

```java
@LogActivity(
    action = SystemLog.ActionType.CREATE,
    entityType = SystemLog.EntityType.MEMORIAL,
    description = "Создание мемориала: #{#dto.firstName} #{#dto.lastName}",
    entityIdExpression = "#result.id",
    includeDetails = true,
    severity = SystemLog.Severity.INFO
)
public MemorialDTO createMemorial(MemorialDTO dto, Long userId) {
    // логика создания
}
```

**Параметры аннотации:**
- `action` - тип действия (CREATE, UPDATE, DELETE, LOGIN и др.)
- `entityType` - тип сущности (USER, MEMORIAL, FAMILY_TREE и др.)
- `description` - описание с поддержкой SpEL выражений
- `entityIdExpression` - SpEL выражение для получения ID сущности
- `includeDetails` - включать ли детальную информацию
- `severity` - уровень важности (INFO, WARNING, ERROR, CRITICAL)

### 2. AOP Аспект `LoggingAspect`

Автоматически перехватывает методы с аннотацией `@LogActivity` и записывает логи:

- Поддержка SpEL выражений в описаниях
- Автоматическое определение пользователя
- Извлечение IP адреса и User-Agent
- Безопасная обработка чувствительных данных
- Асинхронное логирование

### 3. Слушатели событий

#### `AuthenticationEventListener`
Отслеживает события аутентификации:
- Успешный вход
- Неудачные попытки входа
- Выход из системы

#### `ActivityInterceptor`
HTTP интерцептор для отслеживания просмотров страниц:
- Просмотр мемориалов
- Доступ к админ панели
- Поиск по системе
- И другие активности

### 4. Глобальный обработчик исключений

`GlobalExceptionHandler` автоматически логирует все исключения:
- Runtime exceptions
- Security exceptions
- Критические ошибки
- С полной информацией о контексте

## Типы действий (ActionType)

- `CREATE` - создание записей
- `UPDATE` - обновление данных
- `DELETE` - удаление записей
- `LOGIN` - вход в систему
- `LOGOUT` - выход из системы
- `VIEW` - просмотр данных
- `APPROVE` - одобрение
- `REJECT` - отклонение
- `BLOCK` / `UNBLOCK` - блокировка/разблокировка
- `EXPORT` / `IMPORT` - экспорт/импорт данных
- `BACKUP` / `RESTORE` - резервное копирование
- `SYSTEM` - системные события

## Типы сущностей (EntityType)

- `USER` - пользователи
- `MEMORIAL` - мемориалы
- `FAMILY_TREE` - семейные древа
- `NOTIFICATION` - уведомления
- `REPORT` - отчеты
- `SYSTEM` - системные операции
- `SETTINGS` - настройки
- `FILE` - файлы
- `BACKUP` - резервные копии

## Уровни важности (Severity)

- `INFO` - информационные сообщения
- `WARNING` - предупреждения
- `ERROR` - ошибки
- `CRITICAL` - критические события

## Использование в сервисах

### Простое логирование

```java
@LogActivity(
    action = SystemLog.ActionType.UPDATE,
    entityType = SystemLog.EntityType.USER,
    description = "Обновление профиля пользователя"
)
public User updateProfile(User user) {
    return userRepository.save(user);
}
```

### С SpEL выражениями

```java
@LogActivity(
    action = SystemLog.ActionType.DELETE,
    entityType = SystemLog.EntityType.MEMORIAL,
    description = "Удаление мемориала #{#memorial.firstName} #{#memorial.lastName}",
    entityIdExpression = "#memorial.id",
    severity = SystemLog.Severity.WARNING
)
public void deleteMemorial(Memorial memorial) {
    memorialRepository.delete(memorial);
}
```

### Ручное логирование

```java
@Autowired
private SystemLogService systemLogService;

public void someBusinessMethod() {
    // выполнение бизнес логики
    
    systemLogService.logAction(
        SystemLog.ActionType.EXPORT,
        SystemLog.EntityType.REPORT,
        reportId,
        "Экспорт отчета в PDF",
        "Файл размером: " + fileSize + " байт",
        getCurrentUser(),
        getClientIP(),
        getUserAgent(),
        SystemLog.Severity.INFO
    );
}
```

## Администрирование

### Просмотр логов

Администраторы могут просматривать логи через веб-интерфейс:
- URL: `/admin/logs`
- Фильтрация по типу действия, сущности, пользователю, дате
- Пагинация и поиск
- Детальный просмотр записей

### Статистика

Система предоставляет статистику:
- Количество записей за день
- Количество ошибок
- Активность пользователей
- Общая статистика

### Очистка логов

Администраторы могут очищать старые логи:
- Настраиваемый период хранения
- Массовое удаление по критериям
- Подтверждение действий

## Производительность

### Асинхронное логирование

Все логирование выполняется асинхронно:

```java
@Async("logExecutor")
public void logAction(...) {
    // логирование в отдельном потоке
}
```

### Оптимизация запросов

- Индексы на часто используемые поля
- Оптимизированные запросы для фильтрации
- Пагинация больших результатов

### Настройка производительности

В `application.properties`:

```properties
# Размер пула потоков для логирования
logging.async.core-pool-size=2
logging.async.max-pool-size=5
logging.async.queue-capacity=1000

# Настройки очистки
logging.cleanup.retention-days=90
logging.cleanup.batch-size=1000
```

## Безопасность

### Защита чувствительных данных

- Автоматическое скрытие паролей и токенов
- Ограничение размера логируемых данных
- Санитизация входных данных

### Контроль доступа

- Только администраторы могут просматривать логи
- Аудит действий администраторов
- Защищенные API endpoints

## Мониторинг и алерты

Система может интегрироваться с внешними системами мониторинга:

```java
// Пример алерта для критических ошибок
@EventListener
public void handleCriticalError(SystemLogEvent event) {
    if (event.getSeverity() == SystemLog.Severity.CRITICAL) {
        alertService.sendAlert("Критическая ошибка: " + event.getDescription());
    }
}
```

## Лучшие практики

1. **Используйте аннотации** для автоматического логирования
2. **Осмысленные описания** с использованием SpEL
3. **Правильные уровни важности** для разных типов событий
4. **Регулярная очистка** старых логов
5. **Мониторинг ошибок** и быстрое реагирование

## Примеры интеграции

### В контроллере

```java
@PostMapping("/memorials")
public ResponseEntity<MemorialDTO> createMemorial(@RequestBody MemorialDTO dto) {
    // Логирование происходит автоматически через аннотацию в сервисе
    MemorialDTO result = memorialService.createMemorial(dto, getCurrentUserId());
    return ResponseEntity.ok(result);
}
```

### В сервисе

```java
@Service
public class MemorialService {
    
    @LogActivity(
        action = SystemLog.ActionType.CREATE,
        entityType = SystemLog.EntityType.MEMORIAL,
        description = "Создание мемориала: #{#dto.firstName} #{#dto.lastName}",
        entityIdExpression = "#result.id"
    )
    public MemorialDTO createMemorial(MemorialDTO dto, Long userId) {
        // бизнес логика
    }
}
```

Эта система обеспечивает полную прозрачность и контролируемость всех действий в приложении, что критически важно для систем управления данными в сфере ритуальных услуг. 