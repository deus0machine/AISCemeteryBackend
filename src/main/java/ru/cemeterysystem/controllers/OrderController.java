package ru.cemeterysystem.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.Order;
import ru.cemeterysystem.services.OrderService;
import ru.cemeterysystem.dto.OrderReportDTO;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @GetMapping("/orders/all")
    public List<OrderReportDTO> getAllOrders() {
        List<Order> orders = orderService.getOrders();
        return orders.stream()
                .map(orderService::convertToOrderReportDTO)
                .collect(Collectors.toList());
    }
    @PutMapping("/update/{id}")
    public ResponseEntity<Void> updateOrderStatus(@PathVariable Long id, @RequestBody boolean isCompleted) {
        Optional<Order> orderOptional = orderService.findById(id);
        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            order.setCompleted(isCompleted);
            orderService.addOrder(order);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBurial(@PathVariable Long id) {
        try {
            orderService.deleteOrder(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
