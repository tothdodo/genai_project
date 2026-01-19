package genai.genaiprojectbackend.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "final_summaries")
@Getter
@Setter
@NoArgsConstructor
public class FinalSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId; // Note: In init.sql this is an Identity PK, which acts as the reference to the file.

    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_item_id")
    private CategoryItem categoryItem;

    public FinalSummary(String summaryText, CategoryItem categoryItem) {
        this.summaryText = summaryText;
        this.categoryItem = categoryItem;
        this.createdAt = LocalDateTime.now();
    }
}