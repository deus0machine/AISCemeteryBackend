package ru.cemeterysystem.Repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.Models.Guest;
@Repository
public interface GuestRepository extends CrudRepository<Guest, Long> {
}
