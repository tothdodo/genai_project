package genai.genaiprojectbackend.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "files")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String filename;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "file_creation_date")
    private LocalDate fileCreationDate;

    @Column(name = "uploaded")
    private Boolean uploaded;

    @Column(name = "uploaded_at", updatable = false)
    @CreationTimestamp //TODO change this to the actual upload time when implementing uploadFinished
    private Instant uploadedAt;

//    @Column(name = "status")
//    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_item_id")
    private CategoryItem categoryItem;

    @OneToMany(mappedBy = "file")
    private Set<Url> urls = new LinkedHashSet<>();

    public File() {
    }
}

