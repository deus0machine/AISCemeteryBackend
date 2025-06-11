package ru.cemeterysystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import ru.cemeterysystem.models.SystemLog;
import ru.cemeterysystem.services.SystemLogService;

/**
 * Логирование запуска приложения
 */
@Component
public class ApplicationStartupLogger implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupLogger.class);
    
    @Autowired
    private SystemLogService systemLogService;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            // Логируем запуск системы
            systemLogService.logAction(
                SystemLog.ActionType.SYSTEM,
                SystemLog.EntityType.SYSTEM,
                null,
                "Система управления кладбищем запущена",
                "Приложение успешно инициализировано и готово к работе",
                null, // Системное действие без пользователя
                null, // Без IP
                null, // Без User-Agent
                SystemLog.Severity.INFO
            );
            
            logger.info("Система логирования инициализирована и готова к работе");
            
        } catch (Exception e) {
            logger.error("Ошибка при инициализации системы логирования", e);
        }
    }
} 