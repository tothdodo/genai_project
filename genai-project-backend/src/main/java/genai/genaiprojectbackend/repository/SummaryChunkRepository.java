package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.SummaryChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryChunkRepository extends JpaRepository<SummaryChunk, Integer> {
}