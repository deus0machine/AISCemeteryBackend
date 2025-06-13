package ru.cemeterysystem.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cemeterysystem.models.*;
import ru.cemeterysystem.models.FamilyTreeDraft.DraftStatus;
import ru.cemeterysystem.repositories.*;
import ru.cemeterysystem.dto.TreeMemorialDTO;
import ru.cemeterysystem.dto.MemorialRelationDTO;
import ru.cemeterysystem.dto.FamilyTreeDraftDTO;
import ru.cemeterysystem.services.FamilyTreeService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class FamilyTreeDraftService {
    private static final Logger logger = LoggerFactory.getLogger(FamilyTreeDraftService.class);
    
    @Autowired
    private FamilyTreeDraftRepository draftRepository;
    
    @Autowired
    private FamilyTreeRepository familyTreeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FamilyTreeAccessRepository accessRepository;
    
    @Autowired
    private MemorialRepository memorialRepository;
    
    @Autowired
    private MemorialRelationRepository memorialRelationRepository;
    
    @Autowired
    private DraftSubmissionRepository draftSubmissionRepository;
    
    @Autowired
    private FamilyTreeService familyTreeService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Получить или создать активный черновик для редактора
     * Автоматически копирует текущие данные дерева в черновик
     */
    @Transactional
    public FamilyTreeDraft getOrCreateActiveDraft(Long familyTreeId, Long editorId) {
        // Проверяем права доступа
        if (!accessRepository.hasAccess(familyTreeId, editorId)) {
            throw new RuntimeException("Insufficient permissions to edit family tree");
        }
        
        // Ищем существующий активный черновик
        Optional<FamilyTreeDraft> existingDraft = draftRepository.findActiveDraftByTreeAndEditor(familyTreeId, editorId);
        if (existingDraft.isPresent()) {
            FamilyTreeDraft draft = existingDraft.get();
            // Если черновик отклонен, сбрасываем его к исходным данным
            if (draft.getStatus() == DraftStatus.REJECTED) {
                draft.resetToOriginal();
                return draftRepository.save(draft);
            }
            return draft;
        }
        
        // Создаем новый черновик с копией данных дерева
        FamilyTree familyTree = familyTreeRepository.findById(familyTreeId)
            .orElseThrow(() -> new RuntimeException("Family tree not found"));
        User editor = userRepository.findById(editorId)
            .orElseThrow(() -> new RuntimeException("Editor not found"));
        
        FamilyTreeDraft draft = new FamilyTreeDraft();
        draft.setFamilyTree(familyTree);
        draft.setEditor(editor);
        draft.setStatus(DraftStatus.DRAFT);
        draft.setCreatedAt(LocalDateTime.now());
        
        // Копируем текущие данные дерева
        copyTreeDataToDraft(familyTree, draft);
        
        logger.info("Created new draft for tree {} by editor {}", familyTreeId, editorId);
        return draftRepository.save(draft);
    }
    
    /**
     * Обновить данные в черновике
     */
    @Transactional
    public FamilyTreeDraft updateDraft(Long draftId, String name, String description, Boolean isPublic) {
        FamilyTreeDraft draft = draftRepository.findById(draftId)
            .orElseThrow(() -> new RuntimeException("Draft not found"));
        
        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new RuntimeException("Cannot modify submitted draft");
        }
        
        // Обновляем данные черновика
        draft.setDraftName(name);
        draft.setDraftDescription(description);
        draft.setDraftIsPublic(isPublic);
        
        FamilyTreeDraft savedDraft = draftRepository.save(draft);
        logger.info("Updated draft {} with new data", draftId);
        
        return savedDraft;
    }
    
    /**
     * Отправить черновик на рассмотрение
     */
    @Transactional
    public FamilyTreeDraft submitDraft(Long draftId, String message) {
        logger.info("Attempting to submit draft with ID: {}", draftId);
        
        FamilyTreeDraft draft = draftRepository.findById(draftId)
            .orElseThrow(() -> new RuntimeException("Draft not found"));
        
        logger.info("Found draft: ID={}, status={}, editorId={}, treeId={}", 
                   draft.getId(), draft.getStatus(), draft.getEditor().getId(), draft.getFamilyTree().getId());
        
        if (draft.getStatus() != DraftStatus.DRAFT) {
            logger.error("Draft {} is not in DRAFT status, current status: {}", draftId, draft.getStatus());
            throw new RuntimeException("Draft is not in DRAFT status");
        }
        
        // Проверяем, есть ли изменения
        boolean hasChanges = draft.hasAnyChanges();
        logger.info("Draft {} has changes: {}", draftId, hasChanges);
        logger.info("  - Name changed: {}", draft.hasNameChanged());
        logger.info("  - Description changed: {}", draft.hasDescriptionChanged());
        logger.info("  - Public status changed: {}", draft.hasPublicStatusChanged());
        logger.info("  - Memorials changed: {}", draft.hasMemorialsChanged());
        logger.info("  - Relations changed: {}", draft.hasRelationsChanged());
        
        if (!hasChanges) {
            logger.error("No changes to submit in draft {}", draftId);
            throw new RuntimeException("No changes to submit");
        }
        
        // НЕ меняем статус на SUBMITTED - черновик остается в статусе DRAFT
        // Только обновляем время последней отправки и сообщение
        draft.setMessage(message);
        draft.setLastSubmittedAt(LocalDateTime.now()); // Используем новое поле
        
        FamilyTreeDraft savedDraft = draftRepository.save(draft);
        
        // Создаем запись об отправке черновика для уведомлений
        DraftSubmission submission = new DraftSubmission();
        submission.setDraft(savedDraft);
        submission.setMessage(message);
        submission.setSubmittedAt(LocalDateTime.now());
        draftSubmissionRepository.save(submission);
        
        logger.info("Draft {} submitted for review (status remains DRAFT) with {} changes", draftId, getChangesDescription(draft));
        logger.info("Created submission record for draft {} to notify owner", draftId);
        
        return savedDraft;
    }
    
    /**
     * Получить черновики для рассмотрения владельцем
     */
    @Transactional(readOnly = true)
    public List<FamilyTreeDraft> getSubmittedDraftsForOwner(Long ownerId) {
        return draftRepository.findSubmittedDraftsForOwner(ownerId);
    }
    
    /**
     * Одобрить черновик и применить изменения
     */
    @Transactional
    public FamilyTreeDraft approveDraft(Long draftId, String reviewMessage, Long reviewerId) {
        FamilyTreeDraft draft = draftRepository.findById(draftId)
            .orElseThrow(() -> new RuntimeException("Draft not found"));
        
        // Проверяем, что рецензент - владелец дерева
        if (!draft.getFamilyTree().getUser().getId().equals(reviewerId)) {
            throw new RuntimeException("Only tree owner can approve drafts");
        }
        
        if (draft.getStatus() != DraftStatus.SUBMITTED) {
            throw new RuntimeException("Draft is not submitted for review");
        }
        
        // Применяем изменения к основному дереву
        applyDraftToTree(draft);
        
        // Обновляем статус черновика
        draft.setStatus(DraftStatus.APPLIED);
        draft.setReviewMessage(reviewMessage);
        draft.setReviewedAt(LocalDateTime.now());
        
        FamilyTreeDraft savedDraft = draftRepository.save(draft);
        
        // TODO: Создать уведомление для редактора
        logger.info("Draft {} approved and applied to tree {}", draftId, draft.getFamilyTree().getId());
        
        return savedDraft;
    }
    
    /**
     * Отклонить черновик
     */
    @Transactional
    public FamilyTreeDraft rejectDraft(Long draftId, String reviewMessage, Long reviewerId) {
        FamilyTreeDraft draft = draftRepository.findById(draftId)
            .orElseThrow(() -> new RuntimeException("Draft not found"));
        
        // Проверяем, что рецензент - владелец дерева
        if (!draft.getFamilyTree().getUser().getId().equals(reviewerId)) {
            throw new RuntimeException("Only tree owner can reject drafts");
        }
        
        if (draft.getStatus() != DraftStatus.SUBMITTED) {
            throw new RuntimeException("Draft is not submitted for review");
        }
        
        // Отклоняем черновик и сбрасываем к исходным данным
        draft.setStatus(DraftStatus.REJECTED);
        draft.setReviewMessage(reviewMessage);
        draft.setReviewedAt(LocalDateTime.now());
        
        FamilyTreeDraft savedDraft = draftRepository.save(draft);
        
        // TODO: Создать уведомление для редактора
        logger.info("Draft {} rejected by owner {}", draftId, reviewerId);
        
        return savedDraft;
    }
    
    /**
     * Копирует данные дерева в черновик
     */
    private void copyTreeDataToDraft(FamilyTree tree, FamilyTreeDraft draft) {
        // Копируем в draft (редактируемые данные)
        draft.setDraftName(tree.getName());
        draft.setDraftDescription(tree.getDescription());
        draft.setDraftIsPublic(tree.isPublic());
        
        // Копируем в original (для сравнения)
        draft.setOriginalName(tree.getName());
        draft.setOriginalDescription(tree.getDescription());
        draft.setOriginalIsPublic(tree.isPublic());
        
        try {
            // Получаем мемориалы и связи дерева
            List<TreeMemorialDTO> memorials = familyTreeService.getTreeMemorials(tree.getId());
            List<MemorialRelationDTO> relations = familyTreeService.getRelations(tree.getId());
            
            // Сериализуем в JSON и сохраняем в черновик
            draft.setDraftMemorialsJson(objectMapper.writeValueAsString(memorials));
            draft.setDraftRelationsJson(objectMapper.writeValueAsString(relations));
            draft.setOriginalMemorialsJson(objectMapper.writeValueAsString(memorials));
            draft.setOriginalRelationsJson(objectMapper.writeValueAsString(relations));
            
            logger.info("Copied {} memorials and {} relations to draft for tree {}", 
                       memorials.size(), relations.size(), tree.getId());
        } catch (Exception e) {
            logger.error("Error copying tree data to draft: {}", e.getMessage(), e);
            // Устанавливаем пустые массивы в случае ошибки
            draft.setDraftMemorialsJson("[]");
            draft.setDraftRelationsJson("[]");
            draft.setOriginalMemorialsJson("[]");
            draft.setOriginalRelationsJson("[]");
        }
    }
    
    /**
     * Применяет изменения из черновика к основному дереву
     */
    private void applyDraftToTree(FamilyTreeDraft draft) {
        FamilyTree tree = draft.getFamilyTree();
        
        // Применяем изменения метаданных дерева
        tree.setName(draft.getDraftName());
        tree.setDescription(draft.getDraftDescription());
        tree.setPublic(draft.getDraftIsPublic());
        familyTreeRepository.save(tree);
        
        logger.info("Applied draft metadata to tree {}: name='{}', description='{}', isPublic={}", 
            tree.getId(), draft.getDraftName(), draft.getDraftDescription(), draft.getDraftIsPublic());
        
        try {
            // Применяем изменения мемориалов
            applyMemorialChanges(draft, tree);
            
            // Применяем изменения связей
            applyRelationChanges(draft, tree);
            
        } catch (Exception e) {
            logger.error("Error applying draft data changes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply draft changes: " + e.getMessage());
        }
    }
    
    /**
     * Применяет изменения мемориалов из черновика к дереву
     */
    private void applyMemorialChanges(FamilyTreeDraft draft, FamilyTree tree) {
        try {
            String draftMemorialsJson = draft.getDraftMemorialsJson();
            if (draftMemorialsJson == null || draftMemorialsJson.isEmpty()) {
                logger.info("No memorial changes to apply for tree {}", tree.getId());
                return;
            }
            
            List<TreeMemorialDTO> draftMemorials = objectMapper.readValue(draftMemorialsJson, new TypeReference<List<TreeMemorialDTO>>() {});
            logger.info("Applying {} memorials from draft to tree {}", draftMemorials.size(), tree.getId());
            
            // Получаем текущие мемориалы дерева
            List<TreeMemorialDTO> currentMemorials = familyTreeService.getTreeMemorials(tree.getId());
            Set<Long> currentMemorialIds = currentMemorials.stream().map(TreeMemorialDTO::getId).collect(Collectors.toSet());
            
            // Получаем владельца дерева для операций
            User treeOwner = tree.getUser();
            
            // Добавляем новые мемориалы (те, которых нет в оригинальном дереве)
            for (TreeMemorialDTO draftMemorial : draftMemorials) {
                if (!currentMemorialIds.contains(draftMemorial.getId())) {
                    // Это новый мемориал, добавляем его в дерево
                    familyTreeService.addMemorialToTree(tree.getId(), draftMemorial.getId(), treeOwner);
                    logger.info("Added memorial {} to tree {}", draftMemorial.getId(), tree.getId());
                }
            }
            
            // Удаляем мемориалы, которых нет в черновике
            Set<Long> draftMemorialIds = draftMemorials.stream().map(TreeMemorialDTO::getId).collect(Collectors.toSet());
            for (TreeMemorialDTO currentMemorial : currentMemorials) {
                if (!draftMemorialIds.contains(currentMemorial.getId())) {
                    // Этого мемориала нет в черновике, удаляем из дерева
                    familyTreeService.removeMemorialFromTree(tree.getId(), currentMemorial.getId(), treeOwner);
                    logger.info("Removed memorial {} from tree {}", currentMemorial.getId(), tree.getId());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error applying memorial changes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply memorial changes: " + e.getMessage());
        }
    }
    
    /**
     * Применяет изменения связей из черновика к дереву
     */
    private void applyRelationChanges(FamilyTreeDraft draft, FamilyTree tree) {
        try {
            String draftRelationsJson = draft.getDraftRelationsJson();
            String originalRelationsJson = draft.getOriginalRelationsJson();
            
            if (draftRelationsJson == null || draftRelationsJson.isEmpty()) {
                logger.info("No relation changes to apply for tree {}", tree.getId());
                return;
            }
            
            List<MemorialRelationDTO> draftRelations = objectMapper.readValue(draftRelationsJson, new TypeReference<List<MemorialRelationDTO>>() {});
            List<MemorialRelationDTO> originalRelations = new ArrayList<>();
            
            if (originalRelationsJson != null && !originalRelationsJson.isEmpty()) {
                originalRelations = objectMapper.readValue(originalRelationsJson, new TypeReference<List<MemorialRelationDTO>>() {});
            }
            
            logger.info("Applying relation changes: {} draft relations, {} original relations", 
                draftRelations.size(), originalRelations.size());
            
            // Создаем множества ID для быстрого поиска
            Set<Long> originalRelationIds = originalRelations.stream().map(MemorialRelationDTO::getId).collect(Collectors.toSet());
            Set<Long> draftRelationIds = draftRelations.stream().map(MemorialRelationDTO::getId).collect(Collectors.toSet());
            
            // 1. Удаляем связи, которые были в оригинале, но нет в черновике
            for (MemorialRelationDTO originalRelation : originalRelations) {
                if (!draftRelationIds.contains(originalRelation.getId())) {
                    // Эта связь была удалена в черновике
                    memorialRelationRepository.deleteById(originalRelation.getId());
                    logger.info("Removed relation {} from tree {} (deleted in draft)", originalRelation.getId(), tree.getId());
                }
            }
            
            // 2. Добавляем новые связи (с отрицательными ID)
            for (MemorialRelationDTO draftRelation : draftRelations) {
                if (draftRelation.getId() < 0) {
                    // Это новая связь, создаем её
                    MemorialRelation newRelation = new MemorialRelation();
                    newRelation.setFamilyTree(tree);
                    
                    Memorial sourceMemorial = memorialRepository.findById(draftRelation.getSourceMemorial().getId())
                        .orElseThrow(() -> new RuntimeException("Source memorial not found: " + draftRelation.getSourceMemorial().getId()));
                    Memorial targetMemorial = memorialRepository.findById(draftRelation.getTargetMemorial().getId())
                        .orElseThrow(() -> new RuntimeException("Target memorial not found: " + draftRelation.getTargetMemorial().getId()));
                    
                    newRelation.setSourceMemorial(sourceMemorial);
                    newRelation.setTargetMemorial(targetMemorial);
                    newRelation.setRelationType(MemorialRelation.RelationType.valueOf(draftRelation.getRelationType().toUpperCase()));
                    
                    memorialRelationRepository.save(newRelation);
                    logger.info("Created new relation between {} and {} in tree {}", 
                        sourceMemorial.getId(), targetMemorial.getId(), tree.getId());
                }
                // Связи с положительными ID, которые есть в оригинале, остаются без изменений
            }
            
            logger.info("Applied relation changes to tree {}", tree.getId());
            
        } catch (Exception e) {
            logger.error("Error applying relation changes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply relation changes: " + e.getMessage());
        }
    }
    
    /**
     * Получает описание изменений для логирования
     */
    private String getChangesDescription(FamilyTreeDraft draft) {
        StringBuilder changes = new StringBuilder();
        if (draft.hasNameChanged()) {
            changes.append("name: '").append(draft.getOriginalName())
                   .append("' -> '").append(draft.getDraftName()).append("'; ");
        }
        if (draft.hasDescriptionChanged()) {
            changes.append("description changed; ");
        }
        if (draft.hasPublicStatusChanged()) {
            changes.append("public: ").append(draft.getOriginalIsPublic())
                   .append(" -> ").append(draft.getDraftIsPublic()).append("; ");
        }
        return changes.toString();
    }
    
    /**
     * Добавить мемориал в черновик дерева
     */
    @Transactional
    public void addMemorialToDraft(Long familyTreeId, Long memorialId, Long editorId) {
        // Получаем или создаем активный черновик
        FamilyTreeDraft draft = getOrCreateActiveDraft(familyTreeId, editorId);
        
        // Проверяем, что черновик в статусе DRAFT
        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new RuntimeException("Cannot modify submitted draft");
        }
        
        // Проверяем, что мемориал существует
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("Memorial not found"));
        
        try {
            // Получаем текущие мемориалы из JSON черновика
            List<TreeMemorialDTO> currentMemorials;
            String memorialsJson = draft.getDraftMemorialsJson();
            if (memorialsJson != null && !memorialsJson.isEmpty() && !memorialsJson.equals("[]")) {
                currentMemorials = objectMapper.readValue(memorialsJson, new TypeReference<List<TreeMemorialDTO>>() {});
            } else {
                // Если JSON пустой, загружаем все мемориалы из оригинального дерева
                currentMemorials = new ArrayList<>(familyTreeService.getTreeMemorials(familyTreeId));
            }
            
            // Проверяем, есть ли уже мемориал в черновике
            boolean alreadyInDraft = currentMemorials.stream()
                .anyMatch(m -> m.getId().equals(memorialId));
            
            if (alreadyInDraft) {
                throw new RuntimeException("Memorial already exists in the draft");
            }
            
            // Создаем DTO для нового мемориала
            TreeMemorialDTO newMemorialDTO = new TreeMemorialDTO();
            newMemorialDTO.setId(memorial.getId());
            newMemorialDTO.setFio(memorial.getFio());
            newMemorialDTO.setBirthDate(memorial.getBirthDate() != null ? memorial.getBirthDate().toString() : null);
            newMemorialDTO.setDeathDate(memorial.getDeathDate() != null ? memorial.getDeathDate().toString() : null);
            newMemorialDTO.setBiography(memorial.getBiography());
            newMemorialDTO.setPhotoUrl(memorial.getPhotoUrl());
            newMemorialDTO.setPublic(memorial.isPublic());
            
            // Добавляем мемориал в список
            currentMemorials.add(newMemorialDTO);
            
            // Сохраняем обновленный список в JSON
            draft.setDraftMemorialsJson(objectMapper.writeValueAsString(currentMemorials));
            draftRepository.save(draft);
            
            logger.info("Added memorial {} to draft JSON for tree {} by editor {}", memorialId, familyTreeId, editorId);
            
        } catch (Exception e) {
            logger.error("Error adding memorial to draft: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add memorial to draft: " + e.getMessage());
        }
    }
    
    /**
     * Удалить мемориал из черновика дерева
     */
    @Transactional
    public void removeMemorialFromDraft(Long familyTreeId, Long memorialId, Long editorId) {
        // Получаем активный черновик
        FamilyTreeDraft draft = getOrCreateActiveDraft(familyTreeId, editorId);
        
        // Проверяем, что черновик в статусе DRAFT
        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new RuntimeException("Cannot modify submitted draft");
        }
        
        try {
            // Получаем текущие мемориалы из JSON черновика
            List<TreeMemorialDTO> currentMemorials;
            String memorialsJson = draft.getDraftMemorialsJson();
            if (memorialsJson != null && !memorialsJson.isEmpty() && !memorialsJson.equals("[]")) {
                currentMemorials = objectMapper.readValue(memorialsJson, new TypeReference<List<TreeMemorialDTO>>() {});
            } else {
                // Если JSON пустой, загружаем все мемориалы из оригинального дерева
                currentMemorials = new ArrayList<>(familyTreeService.getTreeMemorials(familyTreeId));
            }
            
            // Удаляем мемориал из списка
            boolean removed = currentMemorials.removeIf(m -> m.getId().equals(memorialId));
            
            if (!removed) {
                throw new RuntimeException("Memorial not found in draft");
            }
            
            // Получаем текущие связи из JSON черновика
            List<MemorialRelationDTO> currentRelations;
            String relationsJson = draft.getDraftRelationsJson();
            if (relationsJson != null && !relationsJson.isEmpty()) {
                currentRelations = objectMapper.readValue(relationsJson, new TypeReference<List<MemorialRelationDTO>>() {});
            } else {
                currentRelations = new ArrayList<>();
            }
            
            // Удаляем все связи с данным мемориалом
            currentRelations.removeIf(relation -> 
                relation.getSourceMemorial().getId().equals(memorialId) ||
                relation.getTargetMemorial().getId().equals(memorialId));
            
            // Сохраняем обновленные списки в JSON
            draft.setDraftMemorialsJson(objectMapper.writeValueAsString(currentMemorials));
            draft.setDraftRelationsJson(objectMapper.writeValueAsString(currentRelations));
            draftRepository.save(draft);
            
            logger.info("Removed memorial {} from draft JSON for tree {} by editor {}", memorialId, familyTreeId, editorId);
            
        } catch (Exception e) {
            logger.error("Error removing memorial from draft: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to remove memorial from draft: " + e.getMessage());
        }
    }
    
    /**
     * Добавить связь в черновик дерева
     */
    @Transactional
    public void addRelationToDraft(Long familyTreeId, MemorialRelationDTO relationDTO, Long editorId) {
        // Получаем или создаем активный черновик
        FamilyTreeDraft draft = getOrCreateActiveDraft(familyTreeId, editorId);
        
        // Проверяем, что черновик в статусе DRAFT
        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new RuntimeException("Cannot modify submitted draft");
        }
        
        try {
            // Получаем текущие связи из JSON черновика
            List<MemorialRelationDTO> currentRelations;
            String relationsJson = draft.getDraftRelationsJson();
            if (relationsJson != null && !relationsJson.isEmpty() && !relationsJson.equals("[]")) {
                currentRelations = objectMapper.readValue(relationsJson, new TypeReference<List<MemorialRelationDTO>>() {});
            } else {
                // Если JSON пустой, загружаем все связи из оригинального дерева
                currentRelations = new ArrayList<>(familyTreeService.getRelations(familyTreeId));
            }
            
            // Проверяем, что мемориалы существуют
            Memorial sourceMemorial = memorialRepository.findById(relationDTO.getSourceMemorial().getId())
                .orElseThrow(() -> new RuntimeException("Source memorial not found"));
            Memorial targetMemorial = memorialRepository.findById(relationDTO.getTargetMemorial().getId())
                .orElseThrow(() -> new RuntimeException("Target memorial not found"));
            
            // Создаем новую связь с уникальным ID (используем отрицательные ID для черновых связей)
            MemorialRelationDTO newRelation = new MemorialRelationDTO();
            // Генерируем временный отрицательный ID для черновой связи
            Long tempId = -(System.currentTimeMillis() % 1000000);
            newRelation.setId(tempId);
            newRelation.setSourceMemorial(relationDTO.getSourceMemorial());
            newRelation.setTargetMemorial(relationDTO.getTargetMemorial());
            newRelation.setRelationType(relationDTO.getRelationType());
            
            // Добавляем связь в список
            currentRelations.add(newRelation);
            
            // Сохраняем обновленный список в JSON
            draft.setDraftRelationsJson(objectMapper.writeValueAsString(currentRelations));
            draftRepository.save(draft);
            
            logger.info("Added relation to draft JSON for tree {} by editor {}", familyTreeId, editorId);
            
        } catch (Exception e) {
            logger.error("Error adding relation to draft: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add relation to draft: " + e.getMessage());
        }
    }
    
    /**
     * Удалить связь из черновика дерева
     */
    @Transactional
    public void removeRelationFromDraft(Long familyTreeId, Long relationId, Long editorId) {
        // Получаем активный черновик
        FamilyTreeDraft draft = getOrCreateActiveDraft(familyTreeId, editorId);
        
        // Проверяем, что черновик в статусе DRAFT
        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new RuntimeException("Cannot modify submitted draft");
        }
        
        try {
            // Получаем текущие связи из JSON черновика
            List<MemorialRelationDTO> currentRelations;
            String relationsJson = draft.getDraftRelationsJson();
            if (relationsJson != null && !relationsJson.isEmpty() && !relationsJson.equals("[]")) {
                currentRelations = objectMapper.readValue(relationsJson, new TypeReference<List<MemorialRelationDTO>>() {});
            } else {
                // Если JSON пустой, загружаем все связи из оригинального дерева
                currentRelations = new ArrayList<>(familyTreeService.getRelations(familyTreeId));
            }
            
            // Удаляем связь из списка
            boolean removed = currentRelations.removeIf(relation -> relation.getId().equals(relationId));
            
            if (!removed) {
                throw new RuntimeException("Relation not found in draft");
            }
            
            // Сохраняем обновленный список в JSON
            draft.setDraftRelationsJson(objectMapper.writeValueAsString(currentRelations));
            draftRepository.save(draft);
            
            logger.info("Removed relation {} from draft JSON for tree {} by editor {}", relationId, familyTreeId, editorId);
            
        } catch (Exception e) {
            logger.error("Error removing relation from draft: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to remove relation from draft: " + e.getMessage());
        }
    }
    
    /**
     * Получить мемориалы черновика
     */
    @Transactional(readOnly = true)
    public List<TreeMemorialDTO> getDraftMemorials(Long familyTreeId, Long editorId) {
        logger.info("Getting draft memorials for tree {} and editor {}", familyTreeId, editorId);
        try {
            // Получаем активный черновик
            Optional<FamilyTreeDraft> draftOpt = draftRepository.findActiveDraftByTreeAndEditor(familyTreeId, editorId);
            if (draftOpt.isPresent()) {
                FamilyTreeDraft draft = draftOpt.get();
                logger.info("Found draft {} with status {}", draft.getId(), draft.getStatus());
                String memorialsJson = draft.getDraftMemorialsJson();
                logger.info("Draft memorials JSON: {}", memorialsJson != null ? memorialsJson.substring(0, Math.min(100, memorialsJson.length())) + "..." : "null");
                if (memorialsJson != null && !memorialsJson.isEmpty()) {
                    // Десериализуем мемориалы из JSON
                    List<TreeMemorialDTO> draftMemorials = objectMapper.readValue(memorialsJson, new TypeReference<List<TreeMemorialDTO>>() {});
                    logger.info("Returning {} draft memorials", draftMemorials.size());
                    return draftMemorials;
                } else {
                    logger.info("Draft memorials JSON is empty, falling back to original tree");
                }
            } else {
                logger.info("No active draft found for tree {} and editor {}, falling back to original tree", familyTreeId, editorId);
            }
            
            // Если черновика нет или JSON пустой, возвращаем данные оригинального дерева
            List<TreeMemorialDTO> originalMemorials = familyTreeService.getTreeMemorials(familyTreeId);
            logger.info("Returning {} original tree memorials", originalMemorials.size());
            return originalMemorials;
        } catch (Exception e) {
            logger.error("Error getting draft memorials: {}", e.getMessage(), e);
            // В случае ошибки возвращаем данные оригинального дерева
            List<TreeMemorialDTO> originalMemorials = familyTreeService.getTreeMemorials(familyTreeId);
            logger.info("Error fallback: returning {} original tree memorials", originalMemorials.size());
            return originalMemorials;
        }
    }
    
    /**
     * Получить связи черновика
     */
    @Transactional(readOnly = true)
    public List<MemorialRelationDTO> getDraftRelations(Long familyTreeId, Long editorId) {
        logger.info("Getting draft relations for tree {} and editor {}", familyTreeId, editorId);
        try {
            // Получаем активный черновик
            Optional<FamilyTreeDraft> draftOpt = draftRepository.findActiveDraftByTreeAndEditor(familyTreeId, editorId);
            if (draftOpt.isPresent()) {
                FamilyTreeDraft draft = draftOpt.get();
                logger.info("Found draft {} with status {}", draft.getId(), draft.getStatus());
                String relationsJson = draft.getDraftRelationsJson();
                logger.info("Draft relations JSON: {}", relationsJson != null ? relationsJson.substring(0, Math.min(100, relationsJson.length())) + "..." : "null");
                if (relationsJson != null && !relationsJson.isEmpty()) {
                    // Десериализуем связи из JSON
                    List<MemorialRelationDTO> draftRelations = objectMapper.readValue(relationsJson, new TypeReference<List<MemorialRelationDTO>>() {});
                    logger.info("Returning {} draft relations", draftRelations.size());
                    return draftRelations;
                } else {
                    logger.info("Draft relations JSON is empty, falling back to original tree");
                }
            } else {
                logger.info("No active draft found for tree {} and editor {}, falling back to original tree", familyTreeId, editorId);
            }
            
            // Если черновика нет или JSON пустой, возвращаем данные оригинального дерева
            List<MemorialRelationDTO> originalRelations = familyTreeService.getRelations(familyTreeId);
            logger.info("Returning {} original tree relations", originalRelations.size());
            return originalRelations;
        } catch (Exception e) {
            logger.error("Error getting draft relations: {}", e.getMessage(), e);
            // В случае ошибки возвращаем данные оригинального дерева
            List<MemorialRelationDTO> originalRelations = familyTreeService.getRelations(familyTreeId);
            logger.info("Error fallback: returning {} original tree relations", originalRelations.size());
            return originalRelations;
        }
    }
    
    /**
     * Получить связи черновика по ID черновика
     */
    @Transactional(readOnly = true)
    public List<MemorialRelationDTO> getDraftRelationsByDraftId(Long draftId) {
        logger.info("Getting draft relations by draft ID: {}", draftId);
        try {
            Optional<FamilyTreeDraft> draftOpt = draftRepository.findById(draftId);
            if (draftOpt.isPresent()) {
                FamilyTreeDraft draft = draftOpt.get();
                logger.info("Found draft {} with status {}", draft.getId(), draft.getStatus());
                String relationsJson = draft.getDraftRelationsJson();
                logger.info("Draft relations JSON: {}", relationsJson != null ? relationsJson.substring(0, Math.min(100, relationsJson.length())) + "..." : "null");
                if (relationsJson != null && !relationsJson.isEmpty()) {
                    // Десериализуем связи из JSON
                    List<MemorialRelationDTO> draftRelations = objectMapper.readValue(relationsJson, new TypeReference<List<MemorialRelationDTO>>() {});
                    logger.info("Returning {} draft relations", draftRelations.size());
                    return draftRelations;
                } else {
                    logger.info("Draft relations JSON is empty, falling back to original tree");
                    // Если JSON пустой, возвращаем данные оригинального дерева
                    List<MemorialRelationDTO> originalRelations = familyTreeService.getRelations(draft.getFamilyTree().getId());
                    logger.info("Returning {} original tree relations", originalRelations.size());
                    return originalRelations;
                }
            } else {
                logger.error("Draft not found with ID: {}", draftId);
                throw new RuntimeException("Draft not found with ID: " + draftId);
            }
        } catch (Exception e) {
            logger.error("Error getting draft relations by draft ID: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting draft relations: " + e.getMessage());
        }
    }
    
    /**
     * Получить черновики редактора
     */
    @Transactional(readOnly = true)
    public List<FamilyTreeDraftDTO> getMyDrafts(Long editorId) {
        List<FamilyTreeDraft> drafts = draftRepository.findByEditorIdAndStatusInOrderByCreatedAtDesc(editorId, 
            List.of(DraftStatus.DRAFT, DraftStatus.REJECTED));
        
        return drafts.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Конвертирует FamilyTreeDraft в DTO с полной информацией
     */
    private FamilyTreeDraftDTO convertToDTO(FamilyTreeDraft draft) {
        FamilyTreeDraftDTO dto = new FamilyTreeDraftDTO();
        dto.setId(draft.getId());
        dto.setStatus(draft.getStatus());
        dto.setDraftName(draft.getDraftName());
        dto.setDraftDescription(draft.getDraftDescription());
        dto.setDraftIsPublic(draft.getDraftIsPublic());
        dto.setOriginalName(draft.getOriginalName());
        dto.setOriginalDescription(draft.getOriginalDescription());
        dto.setOriginalIsPublic(draft.getOriginalIsPublic());
        dto.setMessage(draft.getMessage());
        dto.setCreatedAt(draft.getCreatedAt() != null ? draft.getCreatedAt().toString() : null);
        dto.setSubmittedAt(draft.getSubmittedAt() != null ? draft.getSubmittedAt().toString() : null);
        dto.setReviewedAt(draft.getReviewedAt() != null ? draft.getReviewedAt().toString() : null);
        dto.setReviewMessage(draft.getReviewMessage());
        dto.setHasChanges(draft.hasAnyChanges());
        
        // Формируем описание изменений
        StringBuilder changes = new StringBuilder();
        if (draft.hasNameChanged()) {
            changes.append("название изменено; ");
        }
        if (draft.hasDescriptionChanged()) {
            changes.append("описание изменено; ");
        }
        if (draft.hasPublicStatusChanged()) {
            changes.append("видимость изменена; ");
        }
        dto.setChangesDescription(changes.length() > 0 ? changes.toString() : "Нет изменений");
        
        // Добавляем JSON данные черновика для статистики в уведомлениях
        dto.setDraftMemorialsJson(draft.getDraftMemorialsJson());
        dto.setDraftRelationsJson(draft.getDraftRelationsJson());
        dto.setOriginalMemorialsJson(draft.getOriginalMemorialsJson());
        dto.setOriginalRelationsJson(draft.getOriginalRelationsJson());
        
        // Заполняем информацию о дереве
        FamilyTree tree = draft.getFamilyTree();
        FamilyTreeDraftDTO.FamilyTreeDTO treeDTO = new FamilyTreeDraftDTO.FamilyTreeDTO();
        treeDTO.setId(tree.getId());
        treeDTO.setName(tree.getName());
        treeDTO.setDescription(tree.getDescription());
        treeDTO.setIsPublic(tree.isPublic());
        treeDTO.setCreatedAt(tree.getCreatedAt() != null ? tree.getCreatedAt().toString() : null);
        treeDTO.setUpdatedAt(tree.getUpdatedAt() != null ? tree.getUpdatedAt().toString() : null);
        
        // Получаем количество мемориалов из оригинального дерева
        try {
            List<TreeMemorialDTO> memorials = familyTreeService.getTreeMemorials(tree.getId());
            treeDTO.setMemorialCount(memorials.size());
        } catch (Exception e) {
            treeDTO.setMemorialCount(0);
        }
        
        // Заполняем информацию о владельце дерева
        User owner = tree.getUser();
        FamilyTreeDraftDTO.UserDTO ownerDTO = new FamilyTreeDraftDTO.UserDTO();
        ownerDTO.setId(owner.getId());
        ownerDTO.setFio(owner.getFio());
        ownerDTO.setContacts(owner.getContacts());
        ownerDTO.setLogin(owner.getLogin());
        treeDTO.setOwner(ownerDTO);
        
        dto.setFamilyTree(treeDTO);
        
        // Заполняем информацию о редакторе
        User editor = draft.getEditor();
        FamilyTreeDraftDTO.UserDTO editorDTO = new FamilyTreeDraftDTO.UserDTO();
        editorDTO.setId(editor.getId());
        editorDTO.setFio(editor.getFio());
        editorDTO.setContacts(editor.getContacts());
        editorDTO.setLogin(editor.getLogin());
        dto.setEditor(editorDTO);
        
        return dto;
    }
    
    /**
     * Создать черновик из существующего дерева (при предоставлении доступа)
     */
    @Transactional
    public FamilyTreeDraft createDraftFromTree(FamilyTree familyTree, Long editorId) {
        User editor = userRepository.findById(editorId)
            .orElseThrow(() -> new RuntimeException("Editor not found"));
        
        // Проверяем, что у редактора еще нет активного черновика для этого дерева
        Optional<FamilyTreeDraft> existingDraft = draftRepository.findActiveDraftByTreeAndEditor(familyTree.getId(), editorId);
        if (existingDraft.isPresent()) {
            logger.info("Draft already exists for tree {} and editor {}", familyTree.getId(), editorId);
            return existingDraft.get();
        }
        
        FamilyTreeDraft draft = new FamilyTreeDraft();
        draft.setFamilyTree(familyTree);
        draft.setEditor(editor);
        draft.setStatus(DraftStatus.DRAFT);
        draft.setCreatedAt(LocalDateTime.now());
        
        // Копируем текущие данные дерева
        copyTreeDataToDraft(familyTree, draft);
        
        FamilyTreeDraft savedDraft = draftRepository.save(draft);
        logger.info("Created draft {} for tree {} and editor {}", savedDraft.getId(), familyTree.getId(), editorId);
        
        return savedDraft;
    }
    
    /**
     * Применить изменения черновика к оригинальному дереву (без проверки статуса)
     * Используется при ответе на уведомления о черновиках
     */
    @Transactional
    public void applyDraftChanges(Long draftId, String reviewMessage) {
        FamilyTreeDraft draft = draftRepository.findById(draftId)
            .orElseThrow(() -> new RuntimeException("Draft not found"));
        
        logger.info("Applying draft {} changes to tree {}", draftId, draft.getFamilyTree().getId());
        
        // Применяем изменения к основному дереву
        applyDraftToTree(draft);
        
        // Обновляем статус черновика
        draft.setStatus(DraftStatus.APPLIED);
        draft.setReviewMessage(reviewMessage);
        draft.setReviewedAt(LocalDateTime.now());
        
        draftRepository.save(draft);
        
        logger.info("Draft {} changes applied successfully to tree {}", draftId, draft.getFamilyTree().getId());
    }
    
    /**
     * Отклонить черновик (без проверки статуса)
     * Используется при ответе на уведомления о черновиках
     */
    @Transactional
    public void rejectDraftChanges(Long draftId, String reviewMessage) {
        FamilyTreeDraft draft = draftRepository.findById(draftId)
            .orElseThrow(() -> new RuntimeException("Draft not found"));
        
        logger.info("Rejecting draft {} for tree {}", draftId, draft.getFamilyTree().getId());
        
        // Отклоняем черновик
        draft.setStatus(DraftStatus.REJECTED);
        draft.setReviewMessage(reviewMessage);
        draft.setReviewedAt(LocalDateTime.now());
        
        draftRepository.save(draft);
        
        logger.info("Draft {} rejected successfully", draftId);
    }
} 