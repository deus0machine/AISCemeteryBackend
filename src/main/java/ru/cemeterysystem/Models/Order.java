package ru.cemeterysystem.Models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    @JsonBackReference("burial-guest")
    @ManyToOne
    @JoinColumn(name = "guest_id", nullable = false) // Внешний ключ, связывающий заказ с пользователем
    private Guest guest;

    @JsonBackReference("burial-order")
    @ManyToOne
    @JoinColumn(name = "burial_id", nullable = false) // Внешний ключ, связывающий заказ с пользователем
    private Burial burial;

    @Column(name = "order_name", nullable = false)
    private String orderName;

    @Column(name = "order_description", nullable = false)
    private String orderDescription;

    @Column(name = "order_cost", nullable = false)
    private Long orderCost;

    @Column(name = "order_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date orderDate;

    public Order(Burial burial , Guest guest, String orderName, String orderDescription, Long orderCost, Date orderDate) {
        this.burial = burial;
        this.guest = guest;
        this.orderName = orderName;
        this.orderDescription = orderDescription;
        this.orderCost = orderCost;
        this.orderDate = orderDate;
    }
}