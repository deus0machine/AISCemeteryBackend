package ru.cemeterysystem.Models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.Collection;
import java.util.Date;
import java.util.List;
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "guests")
public class Guest  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "fio", nullable = false)
    private String fio;
    @Column(name = "contacts")
    private String contacts;
    @Column(name = "DATEOFREGISTRATION", nullable = false)
    private Date dateOfRegistration;
    @Column(name = "login", unique = true, nullable = false)
    private String login;
    @Column(name = "password", nullable = false)
    private String password;

    @OneToMany(mappedBy = "guest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Burial> burials;

    @Column(name = "balance", nullable = false)
    private Long balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;
    public enum Role {
        ADMIN, USER
    }
}
