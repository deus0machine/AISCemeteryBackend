package ru.cemeterysystem.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.cemeterysystem.dto.MemorialDTO;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.services.FileStorageService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemorialService {
    private final MemorialRepository memorialRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public List<Memorial> getAllMemorials() {
        return memorialRepository.findAll();
    }

    public List<Memorial> getMyMemorials(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return memorialRepository.findByCreatedBy(user);
    }

    public List<Memorial> getPublicMemorials() {
        return memorialRepository.findByIsPublicTrue();
    }

    public Memorial getMemorialById(Long id) {
        return memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
    }

    public Memorial createMemorial(MemorialDTO dto, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        validateMemorialDates(LocalDate.parse(dto.getBirthDate()), 
                            dto.getDeathDate() != null ? LocalDate.parse(dto.getDeathDate()) : null);

        Memorial memorial = new Memorial();
        updateMemorialFromDTO(memorial, dto);
        memorial.setCreatedBy(user);
        memorial.setUser(user);

        return memorialRepository.save(memorial);
    }

    @Transactional
    public Memorial updateMemorial(Long id, MemorialDTO dto) {
        Memorial memorial = getMemorialById(id);
        
        validateMemorialDates(LocalDate.parse(dto.getBirthDate()), 
                            dto.getDeathDate() != null ? LocalDate.parse(dto.getDeathDate()) : null);
        
        if (memorial.getPhotoUrl() != null && !memorial.getPhotoUrl().equals(dto.getPhotoUrl())) {
            fileStorageService.deleteFile(memorial.getPhotoUrl());
        }
        
        updateMemorialFromDTO(memorial, dto);
        return memorialRepository.save(memorial);
    }

    @Transactional
    public void deleteMemorial(Long id) {
        Memorial memorial = getMemorialById(id);
        
        if (memorial.getPhotoUrl() != null) {
            fileStorageService.deleteFile(memorial.getPhotoUrl());
        }
        
        memorialRepository.deleteById(id);
    }

    public void updateMemorialPrivacy(Long id, boolean isPublic) {
        Memorial memorial = getMemorialById(id);
        memorial.setPublic(isPublic);
        memorialRepository.save(memorial);
    }

    @Transactional
    public String uploadPhoto(Long id, MultipartFile file) {
        Memorial memorial = getMemorialById(id);
        
        if (memorial.getPhotoUrl() != null) {
            fileStorageService.deleteFile(memorial.getPhotoUrl());
        }
        
        String photoUrl = fileStorageService.storeFile(file);
        memorial.setPhotoUrl(photoUrl);
        memorialRepository.save(memorial);
        return photoUrl;
    }

    public List<Memorial> searchMemorials(String query, String location, String startDate,
                                        String endDate, Boolean isPublic) {
        return memorialRepository.search(query, location, startDate, endDate, isPublic);
    }

    public List<Memorial> findByFio(String fio) {
        return memorialRepository.findByFio(fio);
    }

    public List<Memorial> findByUserId(Long userId) {
        return memorialRepository.findByUser_Id(userId);
    }

    private void updateMemorialFromDTO(Memorial memorial, MemorialDTO dto) {
        memorial.setFio(dto.getFio());
        memorial.setBirthDate(LocalDate.parse(dto.getBirthDate()));
        if (dto.getDeathDate() != null) {
            memorial.setDeathDate(LocalDate.parse(dto.getDeathDate()));
        }
        memorial.setBiography(dto.getBiography());
        memorial.setMainLocation(dto.getMainLocation());
        memorial.setBurialLocation(dto.getBurialLocation());
        memorial.setPublic(dto.isPublic());
        memorial.setTreeId(dto.getTreeId());
    }

    private void validateMemorialDates(LocalDate birthDate, LocalDate deathDate) {
        if (deathDate != null) {
            if (deathDate.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Дата смерти не может быть позже сегодняшнего дня");
            }
            if (deathDate.isBefore(birthDate)) {
                throw new IllegalArgumentException("Дата смерти не может быть раньше даты рождения");
            }
        }
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