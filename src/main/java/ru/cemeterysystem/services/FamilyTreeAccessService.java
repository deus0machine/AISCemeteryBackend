package ru.cemeterysystem.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.FamilyTreeAccess;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.FamilyTreeAccessRepository;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import ru.cemeterysystem.repositories.UserRepository;
import java.util.List;

@Service
public class FamilyTreeAccessService {
    private final FamilyTreeAccessRepository accessRepository;
    private final FamilyTreeRepository familyTreeRepository;
    private final UserRepository userRepository;

    @Autowired
    public FamilyTreeAccessService(
            FamilyTreeAccessRepository accessRepository,
            FamilyTreeRepository familyTreeRepository,
            UserRepository userRepository) {
        this.accessRepository = accessRepository;
        this.familyTreeRepository = familyTreeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FamilyTreeAccess grantAccess(Long familyTreeId, Long userId, FamilyTreeAccess.AccessLevel accessLevel, Long grantedById) {
        FamilyTree tree = familyTreeRepository.findById(familyTreeId)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!hasAccess(familyTreeId, grantedById, FamilyTreeAccess.AccessLevel.ADMIN)) {
            throw new RuntimeException("Insufficient permissions to grant access");
        }

        if (accessRepository.existsByFamilyTreeIdAndUserId(familyTreeId, userId)) {
            throw new RuntimeException("Access already granted for this user");
        }

        FamilyTreeAccess access = new FamilyTreeAccess();
        access.setFamilyTree(tree);
        access.setUser(user);
        access.setAccessLevel(accessLevel);
        access.setGrantedById(grantedById);

        return accessRepository.save(access);
    }

    @Transactional
    public FamilyTreeAccess updateAccess(Long familyTreeId, Long userId, FamilyTreeAccess.AccessLevel newAccessLevel, Long updatedById) {
        if (!hasAccess(familyTreeId, updatedById, FamilyTreeAccess.AccessLevel.ADMIN)) {
            throw new RuntimeException("Insufficient permissions to update access");
        }

        FamilyTreeAccess access = accessRepository.findByFamilyTreeIdAndUserId(familyTreeId, userId)
                .orElseThrow(() -> new RuntimeException("Access not found"));

        access.setAccessLevel(newAccessLevel);
        return accessRepository.save(access);
    }

    @Transactional
    public void revokeAccess(Long familyTreeId, Long userId, Long revokedById) {
        if (!hasAccess(familyTreeId, revokedById, FamilyTreeAccess.AccessLevel.ADMIN)) {
            throw new RuntimeException("Insufficient permissions to revoke access");
        }

        if (!accessRepository.existsByFamilyTreeIdAndUserId(familyTreeId, userId)) {
            throw new RuntimeException("Access not found");
        }
        accessRepository.deleteByFamilyTreeIdAndUserId(familyTreeId, userId);
    }

    @Transactional(readOnly = true)
    public List<FamilyTreeAccess> getAccessList(Long familyTreeId, Long requestingUserId) {
        if (!hasAccess(familyTreeId, requestingUserId, FamilyTreeAccess.AccessLevel.VIEWER)) {
            throw new RuntimeException("Insufficient permissions to view access list");
        }
        return accessRepository.findByFamilyTreeId(familyTreeId);
    }

    @Transactional(readOnly = true)
    public List<FamilyTreeAccess> getUserAccess(Long userId) {
        return accessRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(Long familyTreeId, Long userId, FamilyTreeAccess.AccessLevel requiredLevel) {
        return accessRepository.findByFamilyTreeIdAndUserId(familyTreeId, userId)
                .map(access -> access.getAccessLevel().ordinal() >= requiredLevel.ordinal())
                .orElse(false);
    }
} 