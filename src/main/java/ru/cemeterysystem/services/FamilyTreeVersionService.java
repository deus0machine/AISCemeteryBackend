package ru.cemeterysystem.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.models.FamilyTree;
import ru.cemeterysystem.models.FamilyTreeVersion;
import ru.cemeterysystem.models.MemorialRelation;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import ru.cemeterysystem.repositories.FamilyTreeVersionRepository;
import ru.cemeterysystem.repositories.MemorialRelationRepository;
import java.util.List;
import java.util.Map;

@Service
public class FamilyTreeVersionService {
    private final FamilyTreeRepository familyTreeRepository;
    private final FamilyTreeVersionRepository versionRepository;
    private final MemorialRelationRepository relationRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public FamilyTreeVersionService(
            FamilyTreeRepository familyTreeRepository,
            FamilyTreeVersionRepository versionRepository,
            MemorialRelationRepository relationRepository,
            ObjectMapper objectMapper) {
        this.familyTreeRepository = familyTreeRepository;
        this.versionRepository = versionRepository;
        this.relationRepository = relationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FamilyTreeVersion createVersion(Long familyTreeId, String description, Long createdById) {
        FamilyTree tree = familyTreeRepository.findById(familyTreeId)
                .orElseThrow(() -> new RuntimeException("Family tree not found"));

        List<MemorialRelation> relations = relationRepository.findByFamilyTreeId(familyTreeId);
        
        // Создаем снимок текущего состояния дерева
        Map<String, Object> snapshot = Map.of(
            "treeId", tree.getId(),
            "treeName", tree.getName(),
            "relations", relations
        );

        String version = generateVersionNumber(tree.getId());
        
        FamilyTreeVersion treeVersion = new FamilyTreeVersion();
        treeVersion.setFamilyTree(tree);
        treeVersion.setVersion(version);
        treeVersion.setDescription(description);
        treeVersion.setCreatedById(createdById);
        
        try {
            treeVersion.setSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create tree version snapshot", e);
        }

        return versionRepository.save(treeVersion);
    }

    @Transactional(readOnly = true)
    public List<FamilyTreeVersion> getVersions(Long familyTreeId) {
        return versionRepository.findByFamilyTreeIdOrderByCreatedAtDesc(familyTreeId);
    }

    @Transactional(readOnly = true)
    public FamilyTreeVersion getLatestVersion(Long familyTreeId) {
        FamilyTreeVersion version = versionRepository.findFirstByFamilyTreeIdOrderByCreatedAtDesc(familyTreeId);
        if (version == null) {
            throw new RuntimeException("No versions found for tree");
        }
        return version;
    }

    private String generateVersionNumber(Long treeId) {
        FamilyTreeVersion latestVersion = versionRepository.findFirstByFamilyTreeIdOrderByCreatedAtDesc(treeId);
        
        if (latestVersion == null) {
            return "1.0";
        }

        String[] parts = latestVersion.getVersion().split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        
        return major + "." + (minor + 1);
    }
} 