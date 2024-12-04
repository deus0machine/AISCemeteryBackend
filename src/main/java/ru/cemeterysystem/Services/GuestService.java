package ru.cemeterysystem.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.Models.Guest;
import ru.cemeterysystem.Repositories.GuestRepository;

import java.util.List;
import java.util.Optional;

@Service
public class GuestService {
    private GuestRepository guestRepository;
    @Autowired
    public void setGuestRepository(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }
    public Optional<Guest> findByLoginAndPassword(String login, String password){ return guestRepository.findByLoginAndPassword(login, password);}
    public List<Guest> getAllGuests() {
        return (List<Guest>) guestRepository.findAll();
    }
    public Optional<Guest> getGuestById(Long id) {
        return guestRepository.findById(id);
    }
    public void deleteGuestById(Long id) {
        guestRepository.deleteById(id);
    }
    public void addGuest(Guest guest){
        guestRepository.save(guest);
    }
}
