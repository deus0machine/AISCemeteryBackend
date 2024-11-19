package ru.cemeterysystem.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cemeterysystem.Models.Order;
import ru.cemeterysystem.Repositories.OrderRepository;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<Order>> getOrdersByGuest(@PathVariable Long guestId) {
        List<Order> orders = orderRepository.findByGuest_Id(guestId);
        return ResponseEntity.ok(orders);
    }
}
