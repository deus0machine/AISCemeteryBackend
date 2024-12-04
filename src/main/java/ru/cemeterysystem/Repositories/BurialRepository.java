package ru.cemeterysystem.Repositories;

import org.springframework.data.repository.CrudRepository;
import ru.cemeterysystem.Models.Burial;

public interface BurialRepository extends CrudRepository<Burial, Long> {
}
