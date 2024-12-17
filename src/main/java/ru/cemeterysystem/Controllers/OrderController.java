package ru.cemeterysystem.Controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.Models.Order;
import ru.cemeterysystem.Repositories.OrderRepository;
import ru.cemeterysystem.Services.OrderService;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<Order>> getOrdersByGuest(@PathVariable Long guestId) {
        List<Order> orders = orderService.getOrdersByGuest(guestId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Order>> getOrdersBetweenDates(
            @RequestParam("startDate") Long startDateMillis,
            @RequestParam("endDate") Long endDateMillis) {
        // Преобразование миллисекунд в Date
        Date startDate = new Date(startDateMillis);
        Date endDate = new Date(endDateMillis);

        List<Order> orders = orderService.getOrdersBetweenDates(startDate, endDate);
        return ResponseEntity.ok(orders);
    }
}
