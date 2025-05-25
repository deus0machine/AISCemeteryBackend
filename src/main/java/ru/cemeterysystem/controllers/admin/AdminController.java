package ru.cemeterysystem.controllers.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.services.UserService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Logger logger = Logger.getLogger(AdminController.class.getName());
    
    private final UserService userService;
    private final UserRepository userRepository;
    private final MemorialRepository memorialRepository;
    private final FamilyTreeRepository familyTreeRepository;

    @GetMapping
    public String adminHome(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Admin home accessed by: " + auth.getName() + " with authorities: " + auth.getAuthorities());
        
        // Получение статистики
        Map<String, Long> stats = new HashMap<>();
        stats.put("userCount", userRepository.count());
        stats.put("memorialCount", memorialRepository.count());
        stats.put("familyTreeCount", familyTreeRepository.count());
        stats.put("subscriberCount", userRepository.countByHasSubscriptionTrue());
        
        model.addAttribute("stats", stats);
        
        // Получение недавно зарегистрированных пользователей
        List<User> recentUsers = userRepository.findAll(
                PageRequest.of(0, 5, Sort.by("dateOfRegistration").descending()))
                .getContent();
        model.addAttribute("recentUsers", recentUsers);
        
        // Получение недавно добавленных мемориалов
        List<Memorial> recentMemorials = memorialRepository.findAll(
                PageRequest.of(0, 5, Sort.by("createdAt").descending()))
                .getContent();
        model.addAttribute("recentMemorials", recentMemorials);
        
        // Данные для графика регистраций (последние 30 дней)
        List<Map<String, Object>> chartData = getRegistrationChartData();
        model.addAttribute("chartData", chartData);
        
        // Недавние действия в системе
        List<Map<String, Object>> recentActions = getRecentActions();
        model.addAttribute("recentActions", recentActions);
        
        return "admin/dashboard";
    }

    // Все методы управления пользователями перенесены в AdminUserController
    // Для управления пользователями используйте соответствующие endpoints в AdminUserController
    
    private List<Map<String, Object>> getRegistrationChartData() {
        // Получаем данные о регистрациях за последние 30 дней
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);
        
        // Создаем карту для хранения количества регистраций по дням
        Map<String, Integer> registrationsPerDay = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
        
        // Инициализируем все дни нулевыми значениями
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            registrationsPerDay.put(date.format(formatter), 0);
        }
        
        // Конвертируем LocalDate в Date для запроса в репозитории
        Date startDateAsDate = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        // Получаем пользователей, зарегистрированных после начальной даты
        List<User> recentUsers = userRepository.findByDateOfRegistrationAfter(startDateAsDate);
        
        // Подсчитываем количество регистраций для каждого дня
        for (User user : recentUsers) {
            LocalDate registrationDate = user.getDateOfRegistration().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            String formattedDate = registrationDate.format(formatter);
            
            if (registrationsPerDay.containsKey(formattedDate)) {
                registrationsPerDay.put(formattedDate, registrationsPerDay.get(formattedDate) + 1);
            }
        }
        
        // Преобразуем данные в формат для Chart.js
        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : registrationsPerDay.entrySet()) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("label", entry.getKey());
            dataPoint.put("value", entry.getValue());
            chartData.add(dataPoint);
        }
        
        return chartData;
    }
    
    private List<Map<String, Object>> getRecentActions() {
        // Создаем имитацию списка недавних действий
        // В реальном приложении здесь должен быть запрос к журналу аудита или подобной системе
        List<Map<String, Object>> actions = new ArrayList<>();
        
        // Получаем последних добавленных пользователей
        List<User> recentUsers = userRepository.findAll(
                PageRequest.of(0, 3, Sort.by("dateOfRegistration").descending()))
                .getContent();
        
        for (User user : recentUsers) {
            Map<String, Object> action = new HashMap<>();
            action.put("type", "USER");
            action.put("date", user.getDateOfRegistration());
            action.put("description", "Регистрация пользователя: " + user.getFio());
            actions.add(action);
        }
        
        // Получаем последние добавленные мемориалы
        List<Memorial> recentMemorials = memorialRepository.findAll(
                PageRequest.of(0, 3, Sort.by("createdAt").descending()))
                .getContent();
        
        for (Memorial memorial : recentMemorials) {
            Map<String, Object> action = new HashMap<>();
            action.put("type", "MEMORIAL");
            action.put("date", Date.from(memorial.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
            action.put("description", "Добавление захоронения: " + memorial.getFio());
            actions.add(action);
        }
        
        // Сортируем все действия по дате (от новых к старым)
        actions.sort((a, b) -> ((Date) b.get("date")).compareTo((Date) a.get("date")));
        
        // Возвращаем только первые 5 действий
        return actions.stream().limit(5).collect(Collectors.toList());
    }
} 