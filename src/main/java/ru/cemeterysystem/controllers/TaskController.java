package ru.cemeterysystem.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.Order;
import ru.cemeterysystem.models.Task;
import ru.cemeterysystem.services.MemorialService;
import ru.cemeterysystem.services.UserService;
import ru.cemeterysystem.services.OrderService;
import ru.cemeterysystem.services.TaskService;
import ru.cemeterysystem.dto.TaskToOrderRequest;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    @Autowired
    TaskService taskService = new TaskService();
    @Autowired
    OrderService orderService = new OrderService();
    //@Autowired
    //MemorialService memorialService = new MemorialService();
    @Autowired
    UserService userService = new UserService();
    @GetMapping("/all")
    public ResponseEntity<List<Task>> getTasks() {
        List<Task> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }
    @PostMapping("/perform")
    public ResponseEntity<String> performTask(@RequestBody TaskToOrderRequest request) {
        // Логика обработки данных
        Long burialId = request.getBurialId();
        Long taskId = request.getTaskId();
        Long guestId = request.getGuestId();
        String imageBase64 = request.getImage();

        if (imageBase64 != null) {
            // Если изображение отправлено, можно его декодировать
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
        //    Optional<Memorial> burialOpt = memorialService.findBurialById(burialId);
        //    Memorial burial = burialOpt.get();
        //    burial.setPhoto(imageBytes);
        //    memorialService.updateBurial(burialId, burial);
            // Сохранить изображение или обработать
        }
        Optional<Task> taskOpt = taskService.getTaskById(taskId);
        Task task = taskOpt.get();
        Order order = new Order();
        order.setOrderDescription(task.getDescription());
        order.setOrderCost(Long.valueOf(task.getCost()));
        order.setOrderName(task.getName());
        order.setOrderDate(new Date());
        User user = new User();
        user.setId(guestId);
        order.setUser(user);
        Memorial burial = new Memorial();
        burial.setId(burialId);
        order.setMemorial(burial);
        orderService.addOrder(order);
        Optional<User> guestBalance = userService.findById(guestId);
        User userBalance2 = guestBalance.get();
        Long newBalance = userBalance2.getBalance()-Long.valueOf(task.getCost());
        userBalance2.setBalance(newBalance);
        userService.saveGuestBalance(userBalance2);
        return ResponseEntity.ok("Задача выполнена");
    }
}
