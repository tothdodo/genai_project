package genai.genaiprojectbackend.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "final_summaries")
@Getter
@Setter
@NoArgsConstructor
public class FinalSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_item_id", nullable = false)
    private CategoryItem categoryItem;

    public FinalSummary(String summaryText, CategoryItem categoryItem) {
        this.summaryText = summaryText;
        this.categoryItem = categoryItem;
        this.createdAt = Instant.now();
    }
}
