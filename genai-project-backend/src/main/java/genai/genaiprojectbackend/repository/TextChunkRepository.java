package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.TextChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TextChunkRepository extends JpaRepository<TextChunk, Integer> {
}
