package ru.cemeterysystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import ru.cemeterysystem.Models.Burial;
import ru.cemeterysystem.Models.Guest;
import ru.cemeterysystem.Models.Order;
import ru.cemeterysystem.Models.Task;
import ru.cemeterysystem.Repositories.BurialRepository;
import ru.cemeterysystem.Repositories.GuestRepository;
import ru.cemeterysystem.Repositories.OrderRepository;
import ru.cemeterysystem.Repositories.TaskRepository;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {
    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private BurialRepository burialRepository;
    @Override
    public void run(String... args) {
        if (guestRepository.count() == 0) {
            // Создаем гостей
            Guest admin = new Guest();
            admin.setFio("ADMIN");
            admin.setLogin("admin");
            admin.setPassword(new BCryptPasswordEncoder().encode("admin"));
            admin.setRole(Guest.Role.ADMIN);
            admin.setDateOfRegistration(new Date());
            admin.setBalance(0L);

            Guest user = new Guest();
            user.setFio("Севостьянов Сергей Вячеславович");
            user.setContacts("7821872677");
            user.setLogin("1111");
            user.setPassword(new BCryptPasswordEncoder().encode("1111"));
            user.setRole(Guest.Role.USER);
            user.setDateOfRegistration(new Date());
            user.setBalance(15000L);

            guestRepository.saveAll(List.of(admin, user));

            // Создаем задачи
            Task cleaningTask = new Task("Чистка надгробия", "1200", "Тщательная очистка надгробия от мха и прочего");
            Task task2 = new Task("Добавить фотографию", "500", "Добавление фотографии на захоронение");
            taskRepository.saveAll(List.of(cleaningTask, task2));

            // Создаем захоронения
            Burial burial1 = new Burial(user, "Иванов Иван Иванович", LocalDate.of(2023, 1, 15), LocalDate.of(1980, 5, 20));
            Burial burial2 = new Burial(user, "Петрова Анна Сергеевна", LocalDate.of(2023, 2, 10), LocalDate.of(1990, 7, 30));
            Burial burial3 = new Burial(admin, "Сергеев Андрей Иванович", LocalDate.of(1980, 5, 15), LocalDate.of(1950, 10, 10));
            Burial burial4 = new Burial(user, "Банденков Владимир Викторович", LocalDate.of(2024, 12, 18), LocalDate.of(2003, 9, 19));
            burialRepository.saveAll(List.of(burial1, burial2, burial3,burial4));

            // Создаем заказы
            Order order1 = new Order(burial1,user, "Уборка территории", "Очистка территории от мусора", 500L, new GregorianCalendar(2024, Calendar.DECEMBER, 25).getTime());
            Order order2 = new Order(burial2, user, "Уход за надгробием", "Полировка до блеска", 1200L, new GregorianCalendar(2024, Calendar.DECEMBER, 28).getTime());
            orderRepository.saveAll(List.of(order1, order2));


        }
    }
}

