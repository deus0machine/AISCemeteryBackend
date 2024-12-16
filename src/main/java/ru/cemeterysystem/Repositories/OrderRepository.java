package ru.cemeterysystem.Repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.Models.Order;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Repository
public interface OrderRepository extends CrudRepository<Order, Long> {
    List<Order> findByGuest_Id(Long guestId);
    List<Order> findByOrderDateBetween(Date startDate, Date endDate);
}
