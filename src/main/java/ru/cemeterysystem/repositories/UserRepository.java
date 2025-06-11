package ru.cemeterysystem.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.cemeterysystem.models.User;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    Page<User> findByFioContainingIgnoreCase(String fio, Pageable pageable);
    List<User> findByDateOfRegistrationAfter(Date date);
    long countByHasSubscriptionTrue();
    
    // Поиск пользователей по роли
    List<User> findByRole(User.Role role);
    
    // Подсчёт пользователей по роли
    long countByRole(User.Role role);
    
    // Подсчёт пользователей с подпиской
    long countByHasSubscription(boolean hasSubscription);
    
    // Подсчёт пользователей зарегистрированных после указанной даты
    long countByDateOfRegistrationAfter(Date date);
}
