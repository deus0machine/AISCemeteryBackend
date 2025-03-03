package ru.cemeterysystem.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.repositories.MemorialRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MemorialService {
    private MemorialRepository memorialRepository;
    @Autowired
    public void setServiceRepository(MemorialRepository memorialRepository) {
        this.memorialRepository = memorialRepository;
    }
    public List<Memorial> findBurialByFio(String fio){
        return memorialRepository.findByFio(fio);
    }
    public List<Memorial> findBurialByGuestId(Long guestId){
        return memorialRepository.findByUser_Id(guestId);
    }
    public List<Memorial> findAll(){
        return (List<Memorial>) memorialRepository.findAll();
    }
    public Optional<Memorial> findBurialById(Long id){
        return memorialRepository.findById(id);
    }
    public Memorial createBurial(Memorial burial) {
        if (burial.getDeathDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Дата смерти не может быть позже сегодняшнего дня");
        }

        if (burial.getDeathDate().isBefore(burial.getBirthDate())) {
            throw new IllegalArgumentException("Дата смерти не может быть раньше даты рождения");
        }
        return memorialRepository.save(burial);
    }
    public Memorial updateBurial(Long id, Memorial burial) {
        Optional<Memorial> existingBurial = memorialRepository.findById(id);
        if (!existingBurial.isPresent()) {
            throw new IllegalArgumentException("Захоронение с таким id не найдено");
        }

        if (burial.getDeathDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Дата смерти не может быть позже сегодняшнего дня");
        }

        if (burial.getDeathDate().isBefore(burial.getBirthDate())) {
            throw new IllegalArgumentException("Дата смерти не может быть раньше даты рождения");
        }

        Memorial updatedBurial = existingBurial.get();
        updatedBurial.setFio(burial.getFio());
        updatedBurial.setDeathDate(burial.getDeathDate());
        updatedBurial.setBirthDate(burial.getBirthDate());
        updatedBurial.setBiography(burial.getBiography());
        updatedBurial.setPhoto(burial.getPhoto());
        updatedBurial.setXCoord(burial.getXCoord());
        updatedBurial.setYCoord(burial.getYCoord());

        return memorialRepository.save(updatedBurial);
    }
    public Memorial updatePartBurial(Long id, Memorial burial){
        Optional<Memorial> existingBurial = memorialRepository.findById(id);
        Memorial updatedBurial = existingBurial.get();
        updatedBurial.setFio(burial.getFio());
        updatedBurial.setDeathDate(burial.getDeathDate());
        updatedBurial.setBirthDate(burial.getBirthDate());
        updatedBurial.setBiography(burial.getBiography());
        return memorialRepository.save(updatedBurial);
    }

    public void deleteBurial(Long id) {
        if (!memorialRepository.existsById(id)) {
            throw new IllegalArgumentException("Захоронение с таким id не найдено");
        }

        memorialRepository.deleteById(id);
    }
}