package ru.cemeterysystem.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.UserRepository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public Optional<User> authenticate(String login, String password) {
        Optional<User> guestOpt = userRepository.findByLogin(login);
        if (guestOpt.isPresent() && passwordEncoder.matches(password, guestOpt.get().getPassword())) {
            return guestOpt;
        }
        return Optional.empty();
    }

    public Optional<User> saveGuestBalance(User user) {
        return Optional.of(userRepository.save(user));
    }

    public Optional<User> findById(long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    public List<User> getAllGuests() {
        return userRepository.findAll();
    }

    public void deleteGuestById(Long id) {
        userRepository.deleteById(id);
    }

    public void registerGuest(User user) {
        Optional<User> existingGuest = userRepository.findByLogin(user.getLogin());
        if (existingGuest.isPresent()) {
            throw new IllegalArgumentException("Guest with this login already exists.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Хэшируем пароль
        user.setHasSubscription(false);
        user.setDateOfRegistration(new Date());
        user.setRole(User.Role.USER);
        userRepository.save(user); // Сохраняем гостя в базу
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        logger.info("Loading user by username: " + username + ", role: " + user.getRole());
        
        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPassword(),
                getAuthorities(user.getRole())
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User.Role role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    // Методы для административной панели
    
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    public Page<User> findUsersByFioContaining(String fio, Pageable pageable) {
        return userRepository.findByFioContainingIgnoreCase(fio, pageable);
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
    
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    public void toggleUserActiveStatus(Long id) {
        User user = getUserById(id);
        // В текущей модели нет поля active, поэтому используем hasSubscription
        user.setHasSubscription(!user.getHasSubscription());
        userRepository.save(user);
    }
}
