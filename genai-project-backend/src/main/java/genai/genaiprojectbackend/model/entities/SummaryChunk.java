package genai.genaiprojectbackend.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "summary_chunks")
@Getter
@Setter
@NoArgsConstructor
public class SummaryChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "text_chunk_id", nullable = false, unique = true)
    private TextChunk textChunk;

    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SummaryChunk(TextChunk textChunk, String summaryText) {
        this.textChunk = textChunk;
        this.summaryText = summaryText;
        this.createdAt = LocalDateTime.now();
    }
}