package ru.cemeterysystem.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.cemeterysystem.models.Memorial;

import java.util.List;

public interface MemorialRepository extends CrudRepository<Memorial, Long> {

    List<Memorial> findByFio(String fio);
    List<Memorial> findByUser_Id(Long guestId);
}
