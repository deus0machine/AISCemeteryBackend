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
            burial7.setPublicationStatus(Memorial.PublicationStatus.PUBLISHED);
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

