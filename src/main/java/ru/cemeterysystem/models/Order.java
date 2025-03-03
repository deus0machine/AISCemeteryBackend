package ru.cemeterysystem.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

    @JsonBackReference("memorial-user")
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonBackReference("memorial-order")
    @ManyToOne
    @JoinColumn(name = "memorial_id", nullable = false)
    private Memorial memorial;

    @Column(name = "order_name", nullable = false)
    private String orderName;

    @Column(name = "order_description", nullable = false)
    private String orderDescription;

    @Column(name = "order_cost", nullable = false)
    private Long orderCost;

    @Column(name = "order_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date orderDate;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;
    public Order(Memorial memorial , User user, String orderName, String orderDescription, Long orderCost, Date orderDate) {
        this.memorial = memorial;
        this.user = user;
        this.orderName = orderName;
        this.orderDescription = orderDescription;
        this.orderCost = orderCost;
        this.orderDate = orderDate;
    }
}