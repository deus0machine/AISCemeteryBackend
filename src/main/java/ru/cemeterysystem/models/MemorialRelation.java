package ru.cemeterysystem.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "memorial_relations")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class MemorialRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_tree_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    private FamilyTree familyTree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_memorial_id", nullable = false)
    //@JsonIdentityReference(alwaysAsId = true)
    private Memorial sourceMemorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_memorial_id", nullable = false)
    //@JsonIdentityReference(alwaysAsId = true)
    private Memorial targetMemorial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationType relationType;

    public enum RelationType {
        PARENT,    // Родитель
        CHILD,     // Ребенок
        SPOUSE,    // Супруг/супруга
        SIBLING,   // Брат/сестра
        GRANDPARENT, // Дедушка/бабушка
        GRANDCHILD,  // Внук/внучка
        UNCLE_AUNT,  // Дядя/тетя
        NEPHEW_NIECE // Племянник/племянница
    }
} 