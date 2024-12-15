package ru.cemeterysystem.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.Models.Burial;
import ru.cemeterysystem.Models.Guest;
import ru.cemeterysystem.Repositories.GuestRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class GuestService implements UserDetailsService {
    private GuestRepository guestRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    public void setGuestRepository(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    public Optional<Guest> authenticate(String login, String password) {
        Optional<Guest> guestOpt = guestRepository.findByLogin(login);
        if (guestOpt.isPresent() && passwordEncoder.matches(password, guestOpt.get().getPassword())) {
            return guestOpt;
        }
        return Optional.empty();
    }
    public Optional<Guest> saveGuestBalance(Guest guest){
        return Optional.of(guestRepository.save(guest));
    }
    public Optional<Guest> findById(long id){
        return guestRepository.findById(id);
    }
    public List<Guest> getAllGuests() {
        return (List<Guest>) guestRepository.findAll();
    }
    public void deleteGuestById(Long id) {
        guestRepository.deleteById(id);
    }
    public void registerGuest(Guest guest) {
        guest.setPassword(passwordEncoder.encode(guest.getPassword())); // Хэшируем пароль
        guestRepository.save(guest); // Сохраняем гостя в базу
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Guest guest = guestRepository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
                guest.getLogin(),
                guest.getPassword(),
                getAuthorities(guest.getRole())
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Guest.Role role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
