package ru.cemeterysystem.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cemeterysystem.Models.Order;
import ru.cemeterysystem.Repositories.OrderRepository;
import ru.cemeterysystem.Services.OrderService;

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
}
