package ru.cemeterysystem.Repositories;

import org.springframework.data.repository.CrudRepository;
import ru.cemeterysystem.Models.Burial;
import ru.cemeterysystem.Models.Order;

import java.util.List;

public interface BurialRepository extends CrudRepository<Burial, Long> {
    List<Burial> findByFio(String fio);
    List<Burial> findByGuest_Id(Long guestId);
}
