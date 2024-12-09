package ru.cemeterysystem.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.Models.Burial;
import ru.cemeterysystem.Repositories.BurialRepository;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@Service
public class BurialService {

    private final BurialRepository burialRepository;

    @Autowired
    public BurialService(BurialRepository burialRepository) {
        this.burialRepository = burialRepository;
    }

    public Burial createBurial(Burial burial) {
        // Простейшая валидация даты смерти
        if (burial.getDeathDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Дата смерти не может быть позже сегодняшнего дня");
        }

        // Проверка, что дата смерти не раньше даты рождения
        if (burial.getDeathDate().isBefore(burial.getBirthDate())) {
            throw new IllegalArgumentException("Дата смерти не может быть раньше даты рождения");
        }

        // Прочая логика валидации может быть добавлена здесь

        return burialRepository.save(burial);
    }
    public Burial updateBurial(Long id, Burial burial) {
        Optional<Burial> existingBurial = burialRepository.findById(id);
        if (!existingBurial.isPresent()) {
            throw new IllegalArgumentException("Захоронение с таким id не найдено");
        }

        if (burial.getDeathDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Дата смерти не может быть позже сегодняшнего дня");
        }

        if (burial.getDeathDate().isBefore(burial.getBirthDate())) {
            throw new IllegalArgumentException("Дата смерти не может быть раньше даты рождения");
        }

        Burial updatedBurial = existingBurial.get();
        updatedBurial.setFio(burial.getFio());
        updatedBurial.setDeathDate(burial.getDeathDate());
        updatedBurial.setBirthDate(burial.getBirthDate());
        updatedBurial.setBiography(burial.getBiography());
        updatedBurial.setPhoto(burial.getPhoto());
        updatedBurial.setXCoord(burial.getXCoord());
        updatedBurial.setYCoord(burial.getYCoord());

        return burialRepository.save(updatedBurial);
    }

    public void deleteBurial(Long id) {
        // Проверка, существует ли запись с таким id
        if (!burialRepository.existsById(id)) {
            throw new IllegalArgumentException("Захоронение с таким id не найдено");
        }

        burialRepository.deleteById(id);
    }
}