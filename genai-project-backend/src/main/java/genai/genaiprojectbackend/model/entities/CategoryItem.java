package genai.genaiprojectbackend.model.entities;

import genai.genaiprojectbackend.model.enums.CategoryItemStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "category_items")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class CategoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 32, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CategoryItemStatus status;

    @OneToMany(mappedBy = "categoryItem")
    private List<File> files;

    public CategoryItem() {

    }

    public CategoryItem(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.createdAt = Instant.now();
        this.status = CategoryItemStatus.PENDING;
    }
}
