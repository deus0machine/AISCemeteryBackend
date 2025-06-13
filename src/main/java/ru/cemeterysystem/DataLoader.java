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
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataLoader(UserRepository userRepository,
                     MemorialRepository memorialRepository,
                     FamilyTreeRepository familyTreeRepository,
                     FamilyTreeAccessRepository familyTreeAccessRepository,
                     MemorialRelationRepository memorialRelationRepository,
                     FamilyTreeVersionRepository familyTreeVersionRepository,
                     NotificationRepository notificationRepository,
                     PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.memorialRepository = memorialRepository;
        this.familyTreeRepository = familyTreeRepository;
        this.familyTreeAccessRepository = familyTreeAccessRepository;
        this.memorialRelationRepository = memorialRelationRepository;
        this.familyTreeVersionRepository = familyTreeVersionRepository;
        this.notificationRepository = notificationRepository;
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

            // Создаем захоронения с отдельными полями ФИО
            Memorial burial1 = new Memorial();
            burial1.setUser(user);
            burial1.setCreatedBy(user);
            burial1.setFirstName("Иван");
            burial1.setLastName("Иванов");
            burial1.setMiddleName("Иванович");
            burial1.setDeathDate(LocalDate.of(2023, 1, 15));
            burial1.setBirthDate(LocalDate.of(1980, 5, 20));
            burial1.setPublic(false);

            Memorial burial2 = new Memorial();
            burial2.setUser(user);
            burial2.setCreatedBy(user);
            burial2.setFirstName("Анна");
            burial2.setLastName("Иванова");
            burial2.setMiddleName("Сергеевна");
            burial2.setDeathDate(LocalDate.of(2023, 2, 10));
            burial2.setBirthDate(LocalDate.of(1990, 7, 30));
            burial2.setPublic(false);

            Memorial burial3 = new Memorial();
            burial3.setUser(admin);
            burial3.setCreatedBy(admin);
            burial3.setFirstName("Андрей");
            burial3.setLastName("Сергеев");
            burial3.setMiddleName("Иванович");
            burial3.setDeathDate(LocalDate.of(1980, 5, 15));
            burial3.setBirthDate(LocalDate.of(1950, 10, 10));

            Memorial burial4 = new Memorial();
            burial4.setUser(user);
            burial4.setCreatedBy(user);
            burial4.setFirstName("Владимир");
            burial4.setLastName("Иванов");
            burial4.setMiddleName("Викторович");
            burial4.setDeathDate(LocalDate.of(2024, 12, 18));
            burial4.setBirthDate(LocalDate.of(2003, 9, 19));
            burial4.setPublic(false);

// Новые члены семьи
            Memorial burial5 = new Memorial();
            burial5.setUser(user);
            burial5.setCreatedBy(user);
            burial5.setFirstName("Мария");
            burial5.setLastName("Сергеева");
            burial5.setMiddleName("Петровна");
            burial5.setDeathDate(LocalDate.of(2022, 3, 5));
            burial5.setBirthDate(LocalDate.of(1955, 8, 12));
            burial5.setPublic(false);

            Memorial burial6 = new Memorial();
            burial6.setUser(user);
            burial6.setCreatedBy(user);
            burial6.setFirstName("Елена");
            burial6.setLastName("Иванова");
            burial6.setMiddleName("Ивановна");
            burial6.setDeathDate(LocalDate.of(2025, 4, 22));
            burial6.setBirthDate(LocalDate.of(2005, 11, 3));
            burial6.setPublic(false);

            Memorial burial7 = new Memorial();
            burial7.setUser(admin);
            burial7.setCreatedBy(admin);
            burial7.setFirstName("Иван");
            burial7.setLastName("Сергеев");
            burial7.setMiddleName("Андреевич");
            burial7.setDeathDate(LocalDate.of(1995, 9, 1));
            burial7.setBirthDate(LocalDate.of(1925, 4, 7));
            burial7.setPublic(true);
            burial7.setPublicationStatus(Memorial.PublicationStatus.PUBLISHED);
            
            Memorial burial8 = new Memorial();
            burial8.setUser(admin);
            burial8.setCreatedBy(admin);
            burial8.setFirstName("Ольга");
            burial8.setLastName("Сергеева");
            burial8.setMiddleName("Николаевна");
            burial8.setDeathDate(LocalDate.of(1998, 6, 14));
            burial8.setBirthDate(LocalDate.of(1928, 12, 15));

            Memorial burial9 = new Memorial();
            burial9.setUser(user2);
            burial9.setCreatedBy(user2);
            burial9.setFirstName("выыав");
            burial9.setLastName("Сергапвеева");
            burial9.setMiddleName("вапавпав");
            burial9.setDeathDate(LocalDate.of(1998, 6, 14));
            burial9.setBirthDate(LocalDate.of(1928, 12, 15));
            
            logger.info("Saving memorials to database...");
            memorialRepository.saveAll(List.of(burial1, burial2, burial3, burial4, burial5, burial6, burial7, burial8, burial9));


            // Создаем тестовое семейное дерево
            FamilyTree familyTree = new FamilyTree();
            familyTree.setName("Семья Ивановых");
            familyTree.setDescription("Генеалогическое древо семьи Ивановых");
            familyTree.setUser(user);
            familyTree.setPublic(true);
            familyTree.setPublicationStatus(FamilyTree.PublicationStatus.PUBLISHED);
            
            logger.info("Saving family tree to database...");
            familyTreeRepository.save(familyTree);
            // Создаем связи между захоронениями
            List<MemorialRelation> relations = new ArrayList<>();

            // СУПРУЖЕСКИЕ СВЯЗИ
            relations.add(createRelation(familyTree, burial1, burial2, MemorialRelation.RelationType.SPOUSE)); // Иван + Анна
            relations.add(createRelation(familyTree, burial7, burial8, MemorialRelation.RelationType.SPOUSE)); // Иван_Андр + Ольга (прародители)

            // РОДИТЕЛЬСКИЕ СВЯЗИ (ПРАВИЛЬНОЕ НАПРАВЛЕНИЕ: РОДИТЕЛЬ → РЕБЕНОК)
            
            // Прародители → их дети (Андрей, Мария, Иван - НЕ Анна!)
            relations.add(createRelation(familyTree, burial7, burial3, MemorialRelation.RelationType.PARENT)); // Иван_Андр → Андрей
            relations.add(createRelation(familyTree, burial8, burial3, MemorialRelation.RelationType.PARENT)); // Ольга → Андрей
            relations.add(createRelation(familyTree, burial7, burial5, MemorialRelation.RelationType.PARENT)); // Иван_Андр → Мария
            relations.add(createRelation(familyTree, burial8, burial5, MemorialRelation.RelationType.PARENT)); // Ольга → Мария
            relations.add(createRelation(familyTree, burial7, burial1, MemorialRelation.RelationType.PARENT)); // Иван_Андр → Иван
            relations.add(createRelation(familyTree, burial8, burial1, MemorialRelation.RelationType.PARENT)); // Ольга → Иван
            
            // Иван и Анна → их дети (Владимир и Елена) - Анна теперь внешняя невестка!
            relations.add(createRelation(familyTree, burial1, burial4, MemorialRelation.RelationType.PARENT)); // Иван → Владимир
            relations.add(createRelation(familyTree, burial2, burial4, MemorialRelation.RelationType.PARENT)); // Анна → Владимир
            relations.add(createRelation(familyTree, burial1, burial6, MemorialRelation.RelationType.PARENT)); // Иван → Елена
            relations.add(createRelation(familyTree, burial2, burial6, MemorialRelation.RelationType.PARENT)); // Анна → Елена

            memorialRelationRepository.saveAll(relations);
            
            // Создаем тестовое уведомление для администратора о публикации мемориала
            Notification notification = new Notification();
            notification.setUser(admin);
            notification.setSender(null); // Системное уведомление
            notification.setTitle("Мемориал опубликован");
            notification.setMessage("Мемориал '" + burial7.getFio() + "' был успешно опубликован на сайте.");
            notification.setType(Notification.NotificationType.SYSTEM);
            notification.setStatus(Notification.NotificationStatus.INFO);
            notification.setRelatedEntityId(burial7.getId());
            notification.setRelatedEntityName(burial7.getFio());
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            notification.setUrgent(false);

            notificationRepository.save(notification);
            logger.info("Test notification created for admin");
            
            // Создаем тестовое уведомление о запросе на модерацию мемориала
            Notification moderationNotification = new Notification();
            moderationNotification.setUser(admin); // Для администратора
            moderationNotification.setSender(user); // От обычного пользователя
            moderationNotification.setTitle("Запрос на публикацию мемориала");
            moderationNotification.setMessage("Пользователь '" + user.getFio() + "' запрашивает публикацию мемориала '" + burial1.getFio() + "'");
            moderationNotification.setType(Notification.NotificationType.MODERATION);
            moderationNotification.setStatus(Notification.NotificationStatus.PENDING);
            moderationNotification.setRelatedEntityId(burial1.getId());
            moderationNotification.setRelatedEntityName(burial1.getFio());
            moderationNotification.setCreatedAt(LocalDateTime.now().minusHours(2)); // Создано 2 часа назад
            moderationNotification.setRead(false);
            moderationNotification.setUrgent(true); // Пометим как важное

            // Изменяем статус мемориала на "На модерации"
            burial1.setPublicationStatus(Memorial.PublicationStatus.PENDING_MODERATION);
            memorialRepository.save(burial1);
            
            notificationRepository.save(moderationNotification);
            logger.info("Test moderation notification created for admin");
            
            // Создаем тестовое уведомление о ранее отклоненном мемориале
            Notification rejectedNotification = new Notification();
            rejectedNotification.setUser(user); // Для пользователя
            rejectedNotification.setSender(admin); // От администратора
            rejectedNotification.setTitle("Мемориал не опубликован");
            rejectedNotification.setMessage("Ваш мемориал '" + burial4.getFio() + "' не был одобрен администратором и не будет опубликован на сайте.");
            rejectedNotification.setType(Notification.NotificationType.SYSTEM);
            rejectedNotification.setStatus(Notification.NotificationStatus.INFO);
            rejectedNotification.setRelatedEntityId(burial4.getId());
            rejectedNotification.setRelatedEntityName(burial4.getFio());
            rejectedNotification.setCreatedAt(LocalDateTime.now().minusDays(1)); // Создано день назад
            rejectedNotification.setRead(true); // Уже прочитано
            rejectedNotification.setUrgent(false);
            
            // Изменяем статус мемориала на "Отклонен"
            burial4.setPublicationStatus(Memorial.PublicationStatus.REJECTED);
            memorialRepository.save(burial4);
            
            notificationRepository.save(rejectedNotification);
            logger.info("Test rejected notification created for user");

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

