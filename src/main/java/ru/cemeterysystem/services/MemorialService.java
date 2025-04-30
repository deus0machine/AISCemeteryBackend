package ru.cemeterysystem.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.cemeterysystem.dto.MemorialDTO;
import ru.cemeterysystem.mappers.MemorialMapper;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.UserRepository;
import ru.cemeterysystem.services.FileStorageService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemorialService {
    private final MemorialRepository memorialRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final MemorialMapper memorialMapper;

    public List<MemorialDTO> getAllMemorials() {
        return memorialRepository.findAll().stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
    }

    public List<MemorialDTO> getMyMemorials(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return memorialRepository.findByCreatedBy(user).stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
    }

    public List<MemorialDTO> getPublicMemorials() {
        return memorialRepository.findByIsPublicTrue().stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
    }

    public MemorialDTO getMemorialById(Long id) {
        return memorialMapper.toDTO(memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found")));
    }

    public MemorialDTO createMemorial(MemorialDTO dto, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        validateMemorialDates(LocalDate.parse(dto.getBirthDate()), 
                            dto.getDeathDate() != null ? LocalDate.parse(dto.getDeathDate()) : null);

        Memorial memorial = new Memorial();
        updateMemorialFromDTO(memorial, dto);
        memorial.setCreatedBy(user);
        memorial.setUser(user);

        return memorialMapper.toDTO(memorialRepository.save(memorial));
    }

    @Transactional
    public MemorialDTO updateMemorial(Long id, MemorialDTO dto) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        validateMemorialDates(LocalDate.parse(dto.getBirthDate()), 
                            dto.getDeathDate() != null ? LocalDate.parse(dto.getDeathDate()) : null);
        
        if (memorial.getPhotoUrl() != null && !memorial.getPhotoUrl().equals(dto.getPhotoUrl())) {
            fileStorageService.deleteFile(memorial.getPhotoUrl());
        }
        
        updateMemorialFromDTO(memorial, dto);
        return memorialMapper.toDTO(memorialRepository.save(memorial));
    }

    @Transactional
    public void deleteMemorial(Long id) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        if (memorial.getPhotoUrl() != null) {
            fileStorageService.deleteFile(memorial.getPhotoUrl());
        }
        
        memorialRepository.deleteById(id);
    }

    public void updateMemorialPrivacy(Long id, boolean isPublic) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        memorial.setPublic(isPublic);
        memorialRepository.save(memorial);
    }

    @Transactional
    public String uploadPhoto(Long id, MultipartFile file) {
        Memorial memorial = memorialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        if (memorial.getPhotoUrl() != null) {
            fileStorageService.deleteFile(memorial.getPhotoUrl());
        }
        
        String photoUrl = fileStorageService.storeFile(file);
        memorial.setPhotoUrl(photoUrl);
        memorialRepository.save(memorial);
        return photoUrl;
    }

    public List<MemorialDTO> searchMemorials(String query, String location, String startDate,
                                        String endDate, Boolean isPublic) {
        return memorialRepository.search(query, location, startDate, endDate, isPublic).stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
    }

    public List<MemorialDTO> findByFio(String fio) {
        return memorialRepository.findByFio(fio).stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
    }

    public List<MemorialDTO> findByUserId(Long userId) {
        return memorialRepository.findByUser_Id(userId).stream()
            .map(memorialMapper::toDTO)
            .collect(Collectors.toList());
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
}