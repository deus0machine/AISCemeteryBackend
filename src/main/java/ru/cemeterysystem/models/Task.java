package ru.cemeterysystem.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "cost", nullable = false)
    private String cost;
    @Column(name = "description")
    private String description;

    public Task(String name, String cost, String description) {
        this.name = name;
        this.cost = cost;
        this.description=description;
    }
}
