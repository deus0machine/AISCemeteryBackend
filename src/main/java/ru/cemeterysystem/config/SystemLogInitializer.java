package ru.cemeterysystem.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.cemeterysystem.models.SystemLog;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.SystemLogRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.services.SystemLogService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SystemLogInitializer implements CommandLineRunner {

    private final SystemLogRepository systemLogRepository;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;

    @Override
    public void run(String... args) throws Exception {
        // Проверяем, есть ли уже логи в системе
        if (systemLogRepository.count() > 0) {
            return; // Логи уже есть, не добавляем тестовые данные
        }

        // Получаем админа и несколько пользователей для логов
        List<User> users = userRepository.findAll();
        User admin = users.stream()
                .filter(u -> u.getLogin().equals("admin"))
                .findFirst()
                .orElse(users.isEmpty() ? null : users.get(0));

        // Добавляем тестовые логи
        if (admin != null) {
            // Логи входа в систему
            systemLogService.logUserLogin(admin, null);
            
            // Логи системных действий
            systemLogService.logAction(
                SystemLog.ActionType.SYSTEM, 
                SystemLog.EntityType.SYSTEM,
                "Система журналов инициализирована",
                admin,
                SystemLog.Severity.INFO
            );

            // Логи управления пользователями
            if (users.size() > 1) {
                User user = users.get(1);
                systemLogService.logAction(
                    SystemLog.ActionType.VIEW, 
                    SystemLog.EntityType.USER,
                    user.getId(),
                    "Просмотр профиля пользователя: " + user.getFio(),
                    admin
                );
            }

            // Лог генерации отчета
            systemLogService.logReportGeneration("Отчет по пользователям", admin);

            // Добавляем несколько логов разного уровня важности
            systemLogService.logAction(
                SystemLog.ActionType.SYSTEM, 
                SystemLog.EntityType.SYSTEM,
                "Проверка целостности базы данных",
                null,
                SystemLog.Severity.INFO
            );

            systemLogService.logAction(
                SystemLog.ActionType.SYSTEM, 
                SystemLog.EntityType.BACKUP,
                "Создание резервной копии базы данных",
                admin,
                SystemLog.Severity.INFO
            );

            // Добавляем предупреждение
            systemLogService.logAction(
                SystemLog.ActionType.SYSTEM, 
                SystemLog.EntityType.SYSTEM,
                "Высокая нагрузка на сервер",
                null,
                SystemLog.Severity.WARNING
            );
        }

        // Добавляем системные логи без пользователя
        SystemLog systemStartLog = new SystemLog(
            SystemLog.ActionType.SYSTEM,
            SystemLog.EntityType.SYSTEM,
            "Запуск системы управления кладбищем",
            null,
            SystemLog.Severity.INFO
        );
        systemLogRepository.save(systemStartLog);

        SystemLog dbConnectionLog = new SystemLog(
            SystemLog.ActionType.SYSTEM,
            SystemLog.EntityType.SYSTEM,
            "Подключение к базе данных успешно",
            null,
            SystemLog.Severity.INFO
        );
        systemLogRepository.save(dbConnectionLog);

        System.out.println("✅ Система журналов инициализирована с тестовыми данными");
    }
} 