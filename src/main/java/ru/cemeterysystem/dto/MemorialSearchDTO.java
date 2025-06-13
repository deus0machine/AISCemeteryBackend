package ru.cemeterysystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO для поисковых фильтров мемориалов
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemorialSearchDTO {
    
    // Основной поисковый запрос
    private String query;
    
    // Поиск по отдельным полям ФИО
    private String firstName;
    private String lastName;
    private String middleName;
    
    // Диапазон дат рождения
    private String birthDateFrom;
    private String birthDateTo;
    
    // Диапазон дат смерти
    private String deathDateFrom;
    private String deathDateTo;
    
    // Местоположение
    private String location;
    
    // Публичность
    private Boolean isPublic;
    
    // Параметры сортировки
    private String sortBy = "lastName";
    private String sortDirection = "asc";
    
    // Параметры пагинации
    private int page = 0;
    private int size = 10;
    
    // Дополнительные фильтры
    private Integer ageFrom;
    private Integer ageTo;
    private String yearOfBirth;
    private String yearOfDeath;
    
    /**
     * Проверяет, есть ли активные фильтры поиска
     */
    public boolean hasFilters() {
        return (query != null && !query.trim().isEmpty()) ||
               (firstName != null && !firstName.trim().isEmpty()) ||
               (lastName != null && !lastName.trim().isEmpty()) ||
               (middleName != null && !middleName.trim().isEmpty()) ||
               (birthDateFrom != null && !birthDateFrom.trim().isEmpty()) ||
               (birthDateTo != null && !birthDateTo.trim().isEmpty()) ||
               (deathDateFrom != null && !deathDateFrom.trim().isEmpty()) ||
               (deathDateTo != null && !deathDateTo.trim().isEmpty()) ||
               (location != null && !location.trim().isEmpty()) ||
               isPublic != null ||
               ageFrom != null ||
               ageTo != null ||
               (yearOfBirth != null && !yearOfBirth.trim().isEmpty()) ||
               (yearOfDeath != null && !yearOfDeath.trim().isEmpty());
    }
    
    /**
     * Очищает все фильтры
     */
    public void clearFilters() {
        query = null;
        firstName = null;
        lastName = null;
        middleName = null;
        birthDateFrom = null;
        birthDateTo = null;
        deathDateFrom = null;
        deathDateTo = null;
        location = null;
        isPublic = null;
        ageFrom = null;
        ageTo = null;
        yearOfBirth = null;
        yearOfDeath = null;
    }
} 