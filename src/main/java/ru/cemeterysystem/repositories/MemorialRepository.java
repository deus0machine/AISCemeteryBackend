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
    List<Memorial> findByFio(String fio);
    List<Memorial> findByUser_Id(Long userId);
    
    List<Memorial> findByEditorsContaining(User user);
    
    @Query("SELECT m FROM Memorial m JOIN m.editors e WHERE e.id = :userId")
    List<Memorial> findMemorialsWhereUserIsEditor(@Param("userId") Long userId);
    
    @Query("SELECT m FROM Memorial m WHERE " +
           "(:query IS NULL OR LOWER(m.fio) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:location IS NULL OR LOWER(m.mainLocation.address) LIKE LOWER(CONCAT('%', :location, '%')) OR " +
           "LOWER(m.burialLocation.address) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:startDate IS NULL OR m.birthDate >= :startDate) AND " +
           "(:endDate IS NULL OR m.deathDate <= :endDate) AND " +
           "(:isPublic IS NULL OR m.isPublic = :isPublic)")
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
    
    Page<Memorial> findByFioContainingIgnoreCaseAndIsPublic(String fio, boolean isPublic, Pageable pageable);
    
    long countByIsPublic(boolean isPublic);
    
    long countByCreatedAtAfter(LocalDateTime date);
    
    // Метод для подсчета мемориалов, созданных в заданном интервале времени
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Методы для работы с PublicationStatus
    Page<Memorial> findByPublicationStatus(Memorial.PublicationStatus status, Pageable pageable);
    
    Page<Memorial> findByFioContainingIgnoreCaseAndPublicationStatus(String fio, Memorial.PublicationStatus status, Pageable pageable);
    
    long countByPublicationStatus(Memorial.PublicationStatus status);
}
