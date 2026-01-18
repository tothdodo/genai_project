package genai.genaiprojectbackend.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "temporary_flashcards")
@Getter
@Setter
@NoArgsConstructor
public class TemporaryFlashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summary_chunk_id", nullable = false)
    private SummaryChunk summaryChunk;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TemporaryFlashcard(SummaryChunk summaryChunk, String question, String answer) {
        this.summaryChunk = summaryChunk;
        this.question = question;
        this.answer = answer;
    }
}