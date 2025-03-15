package ru.cemeterysystem.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;

import java.util.List;

public interface MemorialRepository extends JpaRepository<Memorial, Long> {
    List<Memorial> findByCreatedBy(User user);
    List<Memorial> findByIsPublicTrue();
    List<Memorial> findByFio(String fio);
    List<Memorial> findByUser_Id(Long userId);
    
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
}
