package genai.genaiprojectbackend.model.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "text_chunks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_chunks_file_index", columnNames = {"file_id", "chunk_index"})
        }
)
public class TextChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "page_start")
    private Integer pageStart;

    @Column(name = "page_end")
    private Integer pageEnd;

    @Column(name = "text_content", nullable = false, columnDefinition = "TEXT")
    private String textContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_item_id")
    private CategoryItem categoryItem;

    public TextChunk() {}

    public TextChunk(
            File file,
            CategoryItem item,
            Integer chunkIndex,
            String textContent,
            Integer pageStart,
            Integer pageEnd
            ) {
        this.file = file;
        this.categoryItem = item;
        this.chunkIndex = chunkIndex;
        this.textContent = textContent;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.createdAt = LocalDateTime.now();
    }
}
