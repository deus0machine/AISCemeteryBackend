package ru.cemeterysystem.services;

import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class UserService implements UserDetailsService {
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    public void setGuestRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> authenticate(String login, String password) {
        Optional<User> guestOpt = userRepository.findByLogin(login);
        if (guestOpt.isPresent() && passwordEncoder.matches(password, guestOpt.get().getPassword())) {
            return guestOpt;
        }
        return Optional.empty();
    }
    public Optional<User> saveGuestBalance(User user){
        return Optional.of(userRepository.save(user));
    }
    public Optional<User> findById(long id){
        return userRepository.findById(id);
    }

    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    public List<User> getAllGuests() {
        return (List<User>) userRepository.findAll();
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
        user.setBalance(10000L);
        user.setDateOfRegistration(new Date());
        user.setRole(User.Role.USER);
        userRepository.save(user); // Сохраняем гостя в базу
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPassword(),
                getAuthorities(user.getRole())
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User.Role role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
