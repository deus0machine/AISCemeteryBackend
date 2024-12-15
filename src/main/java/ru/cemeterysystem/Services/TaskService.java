package ru.cemeterysystem.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.Models.Task;
import ru.cemeterysystem.Repositories.TaskRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {
    private TaskRepository taskRepository;
    @Autowired
    public void setServiceRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }
    public List<Task> getAllTasks() {
        return (List<Task>) taskRepository.findAll();
    }
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }
    public void deleteTaskById(Long id) {
        taskRepository.deleteById(id);
    }
    public Task addTask(Task task){
        return taskRepository.save(task);
    }
}
