package ru.cemeterysystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.MemorialRelation;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.*;

import java.time.LocalDate;
import java.util.ArrayList;
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
            admin.setHasSubscription(true);

            User user = new User();
            user.setFio("Севостьянов Сергей Вячеславович");
            user.setContacts("7821872677");
            user.setLogin("1111");
            user.setPassword(new BCryptPasswordEncoder().encode("1111"));
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
            userRepository.saveAll(List.of(admin, user, user2));


            // Создаем захоронения
            // Создаем захоронения (добавлены новые члены семьи)
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

            memorialRepository.saveAll(List.of(burial1, burial2, burial3, burial4, burial5, burial6, burial7, burial8));

// Создаем тестовое семейное дерево
            FamilyTree familyTree = new FamilyTree();
            familyTree.setName("Семья Ивановых");
            familyTree.setDescription("Генеалогическое древо семьи Ивановых с тремя поколениями");
            familyTree.setUser(user);
            familyTree.setPublic(true);
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

