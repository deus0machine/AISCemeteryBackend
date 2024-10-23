package ru.cemeterysystem.Repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.Models.Task;
@Repository
public interface TaskRepository extends CrudRepository<Task, Long> {
}
