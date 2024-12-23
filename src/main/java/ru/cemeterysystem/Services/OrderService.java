package ru.cemeterysystem.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.Models.Order;
import ru.cemeterysystem.Repositories.OrderRepository;
import ru.cemeterysystem.dto.OrderReportDTO;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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
    public List<Order> getOrdersBetweenDates(Date startDate, Date endDate) {
        return orderRepository.findByOrderDateBetween(startDate, endDate);
    }
    public List<Order> getOrders(){
        return (List<Order>) orderRepository.findAll();
    }
    public Optional<Order> findById(Long id){
        return orderRepository.findById(id);
    }
    public OrderReportDTO convertToOrderReportDTO(Order order) {
        OrderReportDTO dto = new OrderReportDTO();
        dto.setId(order.getId());
        dto.setOrderName(order.getOrderName());
        dto.setOrderDescription(order.getOrderDescription());
        dto.setOrderCost(order.getOrderCost());
        dto.setOrderDate(order.getOrderDate().toString());
        dto.setGuestId(order.getGuest().getId());
        dto.setGuestName(order.getGuest().getFio());
        dto.setBurialId(order.getBurial().getId());
        dto.setBurialName(order.getBurial().getFio());
        dto.setCompleted(order.isCompleted());
        return dto;
    }
    public void deleteOrder(Long id) {
        // Проверка, существует ли запись с таким id
        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Заказ с таким id не найден");
        }

        orderRepository.deleteById(id);
    }
}
