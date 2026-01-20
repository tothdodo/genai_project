package genai.genaiprojectbackend.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "final_flashcards")
@Getter
@Setter
@NoArgsConstructor
public class FinalFlashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_item_id", nullable = false)
    private CategoryItem categoryItem;

    public FinalFlashcard(String question, String answer, File file, CategoryItem categoryItem) {
        this.question = question;
        this.answer = answer;
        this.file = file;
        this.categoryItem = categoryItem;
        this.createdAt = Instant.now();
    }
}
