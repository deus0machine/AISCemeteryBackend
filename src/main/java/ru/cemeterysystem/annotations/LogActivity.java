package ru.cemeterysystem.annotations;

import ru.cemeterysystem.models.SystemLog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для автоматического логирования действий пользователей
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {
    
    /**
     * Тип действия
     */
    SystemLog.ActionType action();
    
    /**
     * Тип сущности
     */
    SystemLog.EntityType entityType();
    
    /**
     * Описание действия (может содержать SpEL выражения)
     */
    String description();
    
    /**
     * Уровень важности
     */
    SystemLog.Severity severity() default SystemLog.Severity.INFO;
    
    /**
     * Логировать только при успешном выполнении
     */
    boolean onSuccessOnly() default true;
    
    /**
     * SpEL выражение для получения ID сущности из параметров метода
     */
    String entityIdExpression() default "";
    
    /**
     * Включить детали запроса (параметры, результат)
     */
    boolean includeDetails() default false;
} 