package ru.cemeterysystem.Repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.Models.Guest;

import java.util.Optional;

@Repository
public interface GuestRepository extends CrudRepository<Guest, Long> {
    Optional<Guest> findByLoginAndPassword(String login, String password);
    Optional<Guest> findByLogin(String login);
}
