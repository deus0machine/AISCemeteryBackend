package ru.cemeterysystem.Controllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.Models.Burial;
import ru.cemeterysystem.Models.Guest;
import ru.cemeterysystem.Models.Order;
import ru.cemeterysystem.Models.Task;
import ru.cemeterysystem.Services.BurialService;
import ru.cemeterysystem.Services.GuestService;
import ru.cemeterysystem.Services.OrderService;
import ru.cemeterysystem.Services.TaskService;
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
    @Autowired
    BurialService burialService = new BurialService();
    @Autowired
    GuestService guestService = new GuestService();
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
            Optional<Burial> burialOpt = burialService.findBurialById(burialId);
            Burial burial = burialOpt.get();
            burial.setPhoto(imageBytes);
            burialService.updateBurial(burialId, burial);
            // Сохранить изображение или обработать
        }
        Optional<Task> taskOpt = taskService.getTaskById(taskId);
        Task task = taskOpt.get();
        Order order = new Order();
        order.setOrderDescription(task.getDescription());
        order.setOrderCost(Long.valueOf(task.getCost()));
        order.setOrderName(task.getName());
        order.setOrderDate(new Date());
        Guest guest = new Guest();
        guest.setId(guestId);
        order.setGuest(guest);
        Burial burial = new Burial();
        burial.setId(burialId);
        order.setBurial(burial);
        orderService.addOrder(order);
        Optional<Guest> guestBalance = guestService.findById(guestId);
        Guest guestBalance2 = guestBalance.get();
        Long newBalance = guestBalance2.getBalance()-Long.valueOf(task.getCost());
        guestBalance2.setBalance(newBalance);
        guestService.saveGuestBalance(guestBalance2);
        return ResponseEntity.ok("Задача выполнена");
    }
}
