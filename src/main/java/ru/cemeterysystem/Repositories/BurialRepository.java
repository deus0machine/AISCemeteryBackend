package ru.cemeterysystem.Repositories;

import org.springframework.data.repository.CrudRepository;
import ru.cemeterysystem.Models.Burial;

import java.util.List;

public interface BurialRepository extends CrudRepository<Burial, Long> {
    List<Burial> findByFio(String fio);
}
