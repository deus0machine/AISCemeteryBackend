package ru.cemeterysystem.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.Models.Order;
import ru.cemeterysystem.Repositories.OrderRepository;
import ru.cemeterysystem.Repositories.TaskRepository;

import java.util.List;

@Service
public class OrderService {
    private OrderRepository orderRepository;
    @Autowired
    public void setServiceRepository(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    public Order addOrder(Order order){
        return orderRepository.save(order);
    }
    public List<Order> getOrdersByGuest(Long guestId){
        return  orderRepository.findByGuest_Id(guestId);
    }
}
