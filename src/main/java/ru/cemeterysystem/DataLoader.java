package ru.cemeterysystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {
    private final UserRepository userRepository;
    private final MemorialRepository memorialRepository;
    private final FamilyTreeRepository familyTreeRepository;
    private final FamilyTreeAccessRepository familyTreeAccessRepository;
    private final MemorialRelationRepository memorialRelationRepository;
    private final FamilyTreeVersionRepository familyTreeVersionRepository;

    @Autowired
    public DataLoader(UserRepository userRepository,
                     MemorialRepository memorialRepository,
                     FamilyTreeRepository familyTreeRepository,
                     FamilyTreeAccessRepository familyTreeAccessRepository,
                     MemorialRelationRepository memorialRelationRepository,
                     FamilyTreeVersionRepository familyTreeVersionRepository) {
        this.userRepository = userRepository;
        this.memorialRepository = memorialRepository;
        this.familyTreeRepository = familyTreeRepository;
        this.familyTreeAccessRepository = familyTreeAccessRepository;
        this.memorialRelationRepository = memorialRelationRepository;
        this.familyTreeVersionRepository = familyTreeVersionRepository;
    }

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

            // Создаем тестовое семейное дерево
            FamilyTree familyTree = new FamilyTree();
            familyTree.setName("Семья Ивановых");
            familyTree.setDescription("Генеалогическое древо семьи Ивановых");
            familyTree.setUser(user);
            familyTree.setPublic(true);
            familyTreeRepository.save(familyTree);
        }
    }
}

