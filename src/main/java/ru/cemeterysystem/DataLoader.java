package ru.cemeterysystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.Order;
import ru.cemeterysystem.models.Task;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.repositories.OrderRepository;
import ru.cemeterysystem.repositories.TaskRepository;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private MemorialRepository memorialRepository;
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            // Создаем гостей
            User admin = new User();
            admin.setFio("ADMIN");
            admin.setLogin("admin");
            admin.setPassword(new BCryptPasswordEncoder().encode("admin"));
            admin.setRole(User.Role.ADMIN);
            admin.setDateOfRegistration(new Date());
            admin.setBalance(0L);

            User user = new User();
            user.setFio("Севостьянов Сергей Вячеславович");
            user.setContacts("7821872677");
            user.setLogin("1111");
            user.setPassword(new BCryptPasswordEncoder().encode("1111"));
            user.setRole(User.Role.USER);
            user.setDateOfRegistration(new Date());
            user.setBalance(15000L);

            userRepository.saveAll(List.of(admin, user));

            // Создаем задачи
            Task cleaningTask = new Task("Чистка надгробия", "1200", "Тщательная очистка надгробия от мха и прочего");
            Task task2 = new Task("Добавить фотографию", "500", "Добавление фотографии на захоронение");
            taskRepository.saveAll(List.of(cleaningTask, task2));

            // Создаем захоронения
            Memorial burial1 = new Memorial(user, "Иванов Иван Иванович", LocalDate.of(2023, 1, 15), LocalDate.of(1980, 5, 20));
            burial1.setPublic(true);
            burial1.setCreatedBy(user);
            Memorial burial2 = new Memorial(user, "Петрова Анна Сергеевна", LocalDate.of(2023, 2, 10), LocalDate.of(1990, 7, 30));
            burial2.setPublic(true);
            burial2.setCreatedBy(user);
            Memorial burial3 = new Memorial(admin, "Сергеев Андрей Иванович", LocalDate.of(1980, 5, 15), LocalDate.of(1950, 10, 10));
            burial3.setCreatedBy(admin);
            Memorial burial4 = new Memorial(user, "Банденков Владимир Викторович", LocalDate.of(2024, 12, 18), LocalDate.of(2003, 9, 19));
            burial4.setPublic(true);
            burial4.setCreatedBy(user);
            memorialRepository.saveAll(List.of(burial1, burial2, burial3,burial4));

            // Создаем заказы
            Order order1 = new Order(burial1,user, "Уборка территории", "Очистка территории от мусора", 500L, new GregorianCalendar(2024, Calendar.DECEMBER, 25).getTime());
            Order order2 = new Order(burial2, user, "Уход за надгробием", "Полировка до блеска", 1200L, new GregorianCalendar(2024, Calendar.DECEMBER, 28).getTime());
            orderRepository.saveAll(List.of(order1, order2));


        }
    }
}

