package ru.cemeterysystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.cemeterysystem.models.FamilyTreeDraft.DraftStatus;

@Data
public class FamilyTreeDraftDTO {
    private Long id;
    
    // Информация о дереве
    @JsonProperty("familyTree")
    private FamilyTreeDTO familyTree;
    
    // Информация о редакторе
    @JsonProperty("editor")
    private UserDTO editor;
    
    @JsonProperty("status")
    private DraftStatus status;
    
    // Данные черновика (редактируемые)
    @JsonProperty("draftName")
    private String draftName;
    
    @JsonProperty("draftDescription")
    private String draftDescription;
    
    @JsonProperty("draftIsPublic")
    private Boolean draftIsPublic;
    
    // Исходные данные для сравнения
    @JsonProperty("originalName")
    private String originalName;
    
    @JsonProperty("originalDescription")
    private String originalDescription;
    
    @JsonProperty("originalIsPublic")
    private Boolean originalIsPublic;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("createdAt")
    private String createdAt;
    
    @JsonProperty("submittedAt")
    private String submittedAt;
    
    @JsonProperty("reviewedAt")
    private String reviewedAt;
    
    @JsonProperty("reviewMessage")
    private String reviewMessage;
    
    // Вспомогательные поля
    @JsonProperty("hasChanges")
    private Boolean hasChanges;
    
    @JsonProperty("changesDescription")
    private String changesDescription;
    
    // JSON данные черновика
    @JsonProperty("draftMemorialsJson")
    private String draftMemorialsJson;
    
    @JsonProperty("draftRelationsJson")
    private String draftRelationsJson;
    
    @JsonProperty("originalMemorialsJson")
    private String originalMemorialsJson;
    
    @JsonProperty("originalRelationsJson")
    private String originalRelationsJson;
    
    // Вложенный класс для информации о дереве
    @Data
    public static class FamilyTreeDTO {
        private Long id;
        private String name;
        private String description;
        private Boolean isPublic;
        private String createdAt;
        private String updatedAt;
        private Integer memorialCount;
        
        // Информация о владельце дерева
        @JsonProperty("owner")
        private UserDTO owner;
    }
    
    // Вложенный класс для информации о пользователе
    @Data
    public static class UserDTO {
        private Long id;
        private String fio;
        private String contacts;
        private String login;
    }
} 