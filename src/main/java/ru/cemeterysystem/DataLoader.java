package ru.cemeterysystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.cemeterysystem.models.*;
import ru.cemeterysystem.repositories.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

@Component
public class DataLoader implements CommandLineRunner {
    private static final Logger logger = Logger.getLogger(DataLoader.class.getName());
    
    private final UserRepository userRepository;
    private final MemorialRepository memorialRepository;
    private final FamilyTreeRepository familyTreeRepository;
    private final FamilyTreeAccessRepository familyTreeAccessRepository;
    private final MemorialRelationRepository memorialRelationRepository;
    private final FamilyTreeVersionRepository familyTreeVersionRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataLoader(UserRepository userRepository,
                     MemorialRepository memorialRepository,
                     FamilyTreeRepository familyTreeRepository,
                     FamilyTreeAccessRepository familyTreeAccessRepository,
                     MemorialRelationRepository memorialRelationRepository,
                     FamilyTreeVersionRepository familyTreeVersionRepository,
                     PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.memorialRepository = memorialRepository;
        this.familyTreeRepository = familyTreeRepository;
        this.familyTreeAccessRepository = familyTreeAccessRepository;
        this.memorialRelationRepository = memorialRelationRepository;
        this.familyTreeVersionRepository = familyTreeVersionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        logger.info("Running DataLoader...");
        if (userRepository.count() == 0) {
            logger.info("Initializing test data...");
            
            // Создаем администратора
            User admin = new User();
            admin.setFio("ADMIN");
            admin.setLogin("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setRole(User.Role.ADMIN);
            admin.setDateOfRegistration(new Date());
            admin.setHasSubscription(true);
            admin.setContacts("admin@example.com");
            
            // Создаем пользователя
            User user = new User();
            user.setFio("Севостьянов Сергей Вячеславович");
            user.setContacts("7821872677");
            user.setLogin("1111");
            user.setPassword(passwordEncoder.encode("1111"));
            user.setRole(User.Role.USER);
            user.setDateOfRegistration(new Date());
            user.setHasSubscription(false);

            User user2 = new User();
            user2.setFio("Тестова Подписка Подписковна");
            user2.setContacts("7937287734");
            user2.setLogin("2222");
            user2.setPassword(new BCryptPasswordEncoder().encode("2222"));
            user2.setRole(User.Role.USER);
            user2.setDateOfRegistration(new Date());
            user2.setHasSubscription(true);
            logger.info("Saving users to database...");
            userRepository.saveAll(List.of(admin, user, user2));

            // Создаем захоронения
            Memorial burial1 = new Memorial(user, "Иванов Иван Иванович", LocalDate.of(2023, 1, 15), LocalDate.of(1980, 5, 20));
            burial1.setPublic(false);
            burial1.setCreatedBy(user);

            Memorial burial2 = new Memorial(user, "Иванова Анна Сергеевна", LocalDate.of(2023, 2, 10), LocalDate.of(1990, 7, 30));
            burial2.setPublic(false);
            burial2.setCreatedBy(user);

            Memorial burial3 = new Memorial(admin, "Сергеев Андрей Иванович", LocalDate.of(1980, 5, 15), LocalDate.of(1950, 10, 10));
            burial3.setCreatedBy(admin);

            Memorial burial4 = new Memorial(user, "Иванов Владимир Викторович", LocalDate.of(2024, 12, 18), LocalDate.of(2003, 9, 19));
            burial4.setPublic(false);
            burial4.setCreatedBy(user);

// Новые члены семьи
            Memorial burial5 = new Memorial(user, "Сергеева Мария Петровна", LocalDate.of(2022, 3, 5), LocalDate.of(1955, 8, 12));
            burial5.setPublic(false);
            burial5.setCreatedBy(user);

            Memorial burial6 = new Memorial(user, "Иванова Елена Ивановна", LocalDate.of(2025, 4, 22), LocalDate.of(2005, 11, 3));
            burial6.setPublic(false);
            burial6.setCreatedBy(user);

            Memorial burial7 = new Memorial(admin, "Сергеев Иван Андреевич", LocalDate.of(1995, 9, 1), LocalDate.of(1925, 4, 7));
            burial7.setCreatedBy(admin);
            burial7.setPublic(true);
            Memorial burial8 = new Memorial(admin, "Сергеева Ольга Николаевна", LocalDate.of(1998, 6, 14), LocalDate.of(1928, 12, 15));
            burial8.setCreatedBy(admin);
            logger.info("Saving memorials to database...");
            memorialRepository.saveAll(List.of(burial1, burial2, burial3, burial4, burial5, burial6, burial7, burial8));


            // Создаем тестовое семейное дерево
            FamilyTree familyTree = new FamilyTree();
            familyTree.setName("Семья Ивановых");
            familyTree.setDescription("Генеалогическое древо семьи Ивановых");
            familyTree.setUser(user);
            familyTree.setPublic(true);
            
            logger.info("Saving family tree to database...");
            familyTreeRepository.save(familyTree);
            // Создаем связи между захоронениями
            List<MemorialRelation> relations = new ArrayList<>();

// Базовые связи из оригинала
            relations.add(createRelation(familyTree, burial1, burial2, MemorialRelation.RelationType.SPOUSE));
            relations.add(createRelation(familyTree, burial1, burial3, MemorialRelation.RelationType.PARENT));
            relations.add(createRelation(familyTree, burial4, burial1, MemorialRelation.RelationType.CHILD));

// Новые связи
// Родители Ивана (burial1)
            relations.add(createRelation(familyTree, burial1, burial5, MemorialRelation.RelationType.PARENT));  // Мать
            relations.add(createRelation(familyTree, burial3, burial5, MemorialRelation.RelationType.SPOUSE)); // Супруги

// Дети Ивана и Анны
            relations.add(createRelation(familyTree, burial6, burial1, MemorialRelation.RelationType.CHILD));
            relations.add(createRelation(familyTree, burial6, burial2, MemorialRelation.RelationType.PARENT));

// Родители Андрея (burial3) - прадеды
            relations.add(createRelation(familyTree, burial3, burial7, MemorialRelation.RelationType.PARENT)); // Отец
            relations.add(createRelation(familyTree, burial3, burial8, MemorialRelation.RelationType.PARENT)); // Мать
            relations.add(createRelation(familyTree, burial7, burial8, MemorialRelation.RelationType.SPOUSE)); // Супруги

            memorialRelationRepository.saveAll(relations);

            logger.info("Test data initialization completed successfully");
        } else {
            logger.info("Database already contains data, skipping initialization");
        }
    }
    private MemorialRelation createRelation(FamilyTree tree, Memorial source, Memorial target, MemorialRelation.RelationType type) {
        MemorialRelation relation = new MemorialRelation();
        relation.setFamilyTree(tree);
        relation.setSourceMemorial(source);
        relation.setTargetMemorial(target);
        relation.setRelationType(type);
        return relation;
    }
}

