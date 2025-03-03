package ru.cemeterysystem.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.Task;
@Repository
public interface TaskRepository extends CrudRepository<Task, Long> {
}
