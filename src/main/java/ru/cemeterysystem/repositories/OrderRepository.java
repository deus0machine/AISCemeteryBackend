package ru.cemeterysystem.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.Order;

import java.util.Date;
import java.util.List;

@Repository
public interface OrderRepository extends CrudRepository<Order, Long> {
    List<Order> findByUser_Id(Long guestId);
    List<Order> findByOrderDateBetween(Date startDate, Date endDate);
}
