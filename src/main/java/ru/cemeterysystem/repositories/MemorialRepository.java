package ru.cemeterysystem.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;

import java.time.LocalDateTime;
import java.util.List;

public interface MemorialRepository extends JpaRepository<Memorial, Long> {
    List<Memorial> findByCreatedBy(User user);
    List<Memorial> findByIsPublicTrue();
    
    // Метод для пагинации публичных мемориалов
    Page<Memorial> findByIsPublicTrue(Pageable pageable);
    
    // Метод для пагинации публичных незаблокированных мемориалов
    Page<Memorial> findByIsPublicTrueAndIsBlockedFalse(Pageable pageable);
    
    // Метод для пагинации публичных опубликованных незаблокированных мемориалов
    Page<Memorial> findByIsPublicTrueAndPublicationStatusAndIsBlockedFalse(Memorial.PublicationStatus status, Pageable pageable);
    
    // Метод для получения списка публичных опубликованных незаблокированных мемориалов
    List<Memorial> findByIsPublicTrueAndPublicationStatusAndIsBlockedFalse(Memorial.PublicationStatus status);
    
    List<Memorial> findByFio(String fio);
    List<Memorial> findByUser_Id(Long userId);
    
    // Новые методы поиска по отдельным полям ФИО
    List<Memorial> findByFirstNameContainingIgnoreCase(String firstName);
    List<Memorial> findByLastNameContainingIgnoreCase(String lastName);
    List<Memorial> findByMiddleNameContainingIgnoreCase(String middleName);
    
    // Комбинированные поиски
    List<Memorial> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(String firstName, String lastName);
    List<Memorial> findByLastNameContainingIgnoreCaseAndFirstNameContainingIgnoreCaseAndMiddleNameContainingIgnoreCase(
        String lastName, String firstName, String middleName);
    
    List<Memorial> findByEditorsContaining(User user);
    
    @Query("SELECT m FROM Memorial m JOIN m.editors e WHERE e.id = :userId")
    List<Memorial> findMemorialsWhereUserIsEditor(@Param("userId") Long userId);
    
    @Query("SELECT m FROM Memorial m WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(m.fio) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.middleName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(m.lastName, ' ', m.firstName)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(m.firstName, ' ', m.lastName)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(m.lastName, ' ', m.firstName, ' ', m.middleName)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(m.firstName, ' ', m.middleName, ' ', m.lastName)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(m.middleName, ' ', m.lastName, ' ', m.firstName)) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:location IS NULL OR LOWER(m.mainLocation.address) LIKE LOWER(CONCAT('%', :location, '%')) OR " +
           "LOWER(m.burialLocation.address) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:startDate IS NULL OR m.birthDate >= :startDate) AND " +
           "(:endDate IS NULL OR m.deathDate <= :endDate) AND " +
           "(:isPublic IS NULL OR m.isPublic = :isPublic) AND " +
           "m.isBlocked = false AND " +
           "m.publicationStatus = 'PUBLISHED'")
    List<Memorial> search(
        @Param("query") String query,
        @Param("location") String location,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("isPublic") Boolean isPublic
    );
    
    // Добавленные методы для AdminMemorialController
    Page<Memorial> findByIsPublic(boolean isPublic, Pageable pageable);
    
    Page<Memorial> findByFioContainingIgnoreCase(String fio, Pageable pageable);
    
    // Новые методы поиска по отдельным полям ФИО с пагинацией
    Page<Memorial> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);
    Page<Memorial> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);
    
    Page<Memorial> findByFioContainingIgnoreCaseAndIsPublic(String fio, boolean isPublic, Pageable pageable);
    
    long countByIsPublic(boolean isPublic);
    
    long countByCreatedAtAfter(LocalDateTime date);
    
    // Метод для подсчета мемориалов, созданных в заданном интервале времени
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Методы для работы с PublicationStatus
    Page<Memorial> findByPublicationStatus(Memorial.PublicationStatus status, Pageable pageable);
    
    Page<Memorial> findByFioContainingIgnoreCaseAndPublicationStatus(String fio, Memorial.PublicationStatus status, Pageable pageable);
    
    long countByPublicationStatus(Memorial.PublicationStatus status);

    /**
     * Расширенный поиск мемориалов с дополнительными критериями
     */
    @Query("SELECT m FROM Memorial m WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(m.fio) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(m.lastName, ' ', m.firstName, ' ', COALESCE(m.middleName, ''))) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:firstName IS NULL OR :firstName = '' OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
           "(:lastName IS NULL OR :lastName = '' OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
           "(:middleName IS NULL OR :middleName = '' OR LOWER(m.middleName) LIKE LOWER(CONCAT('%', :middleName, '%'))) AND " +
           "(:birthDateFrom IS NULL OR m.birthDate >= CAST(:birthDateFrom AS date)) AND " +
           "(:birthDateTo IS NULL OR m.birthDate <= CAST(:birthDateTo AS date)) AND " +
           "(:deathDateFrom IS NULL OR m.deathDate >= CAST(:deathDateFrom AS date)) AND " +
           "(:deathDateTo IS NULL OR m.deathDate <= CAST(:deathDateTo AS date)) AND " +
           "(:location IS NULL OR :location = '' OR " +
           "LOWER(m.mainLocation.address) LIKE LOWER(CONCAT('%', :location, '%')) OR " +
           "LOWER(m.burialLocation.address) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:isPublic IS NULL OR m.isPublic = :isPublic) AND " +
           "m.isBlocked = false AND " +
           "m.publicationStatus = 'PUBLISHED' " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'lastName' AND :sortDirection = 'asc' THEN m.lastName END ASC, " +
           "CASE WHEN :sortBy = 'lastName' AND :sortDirection = 'desc' THEN m.lastName END DESC, " +
           "CASE WHEN :sortBy = 'firstName' AND :sortDirection = 'asc' THEN m.firstName END ASC, " +
           "CASE WHEN :sortBy = 'firstName' AND :sortDirection = 'desc' THEN m.firstName END DESC, " +
           "CASE WHEN :sortBy = 'birthDate' AND :sortDirection = 'asc' THEN m.birthDate END ASC, " +
           "CASE WHEN :sortBy = 'birthDate' AND :sortDirection = 'desc' THEN m.birthDate END DESC, " +
           "CASE WHEN :sortBy = 'deathDate' AND :sortDirection = 'asc' THEN m.deathDate END ASC, " +
           "CASE WHEN :sortBy = 'deathDate' AND :sortDirection = 'desc' THEN m.deathDate END DESC, " +
           "CASE WHEN :sortBy = 'createdAt' AND :sortDirection = 'asc' THEN m.createdAt END ASC, " +
           "CASE WHEN :sortBy = 'createdAt' AND :sortDirection = 'desc' THEN m.createdAt END DESC, " +
           "m.lastName ASC")
    List<Memorial> advancedSearch(
        @Param("query") String query,
        @Param("firstName") String firstName,
        @Param("lastName") String lastName,
        @Param("middleName") String middleName,
        @Param("birthDateFrom") String birthDateFrom,
        @Param("birthDateTo") String birthDateTo,
        @Param("deathDateFrom") String deathDateFrom,
        @Param("deathDateTo") String deathDateTo,
        @Param("location") String location,
        @Param("isPublic") Boolean isPublic,
        @Param("sortBy") String sortBy,
        @Param("sortDirection") String sortDirection
    );

    /**
     * Быстрый поиск для автодополнения
     */
    @Query(value = "SELECT * FROM memorials m WHERE " +
           "(LOWER(m.fio) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.first_name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.last_name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(m.last_name, ' ', m.first_name)) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "m.is_blocked = false AND " +
           "m.publication_status = 'PUBLISHED' AND " +
           "m.is_public = true " +
           "ORDER BY " +
           "CASE WHEN LOWER(m.last_name) LIKE LOWER(CONCAT(:query, '%')) THEN 1 " +
           "     WHEN LOWER(m.first_name) LIKE LOWER(CONCAT(:query, '%')) THEN 2 " +
           "     WHEN LOWER(m.fio) LIKE LOWER(CONCAT(:query, '%')) THEN 3 " +
           "     ELSE 4 END, " +
           "m.last_name ASC " +
           "LIMIT :limit", nativeQuery = true)
    List<Memorial> quickSearch(@Param("query") String query, @Param("limit") int limit);

    /**
     * Поиск мемориалов по годовщинам
     */
    @Query("SELECT m FROM Memorial m WHERE " +
           "(:type = 'birth' AND MONTH(m.birthDate) = :month AND (:day IS NULL OR DAY(m.birthDate) = :day)) OR " +
           "(:type = 'death' AND m.deathDate IS NOT NULL AND MONTH(m.deathDate) = :month AND (:day IS NULL OR DAY(m.deathDate) = :day)) AND " +
           "m.isBlocked = false AND " +
           "m.publicationStatus = 'PUBLISHED' AND " +
           "m.isPublic = true " +
           "ORDER BY " +
           "CASE WHEN :type = 'birth' THEN DAY(m.birthDate) ELSE DAY(m.deathDate) END ASC, " +
           "m.lastName ASC")
    List<Memorial> searchAnniversaries(
        @Param("type") String type,
        @Param("month") Integer month,
        @Param("day") Integer day
    );
}
