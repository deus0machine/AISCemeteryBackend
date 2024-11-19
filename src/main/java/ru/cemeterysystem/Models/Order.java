package ru.cemeterysystem.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "guest_id", nullable = false) // Внешний ключ, связывающий заказ с пользователем
    private Guest guest;

    @Column(name = "order_name", nullable = false)
    private String orderName;

    @Column(name = "order_description", nullable = false)
    private String orderDescription;

    @Column(name = "order_cost", nullable = false)
    private Long orderCost;

    @Column(name = "order_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date orderDate;
}