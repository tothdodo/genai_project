package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.TextChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TextChunkRepository extends JpaRepository<TextChunk, Integer> {
    Optional<TextChunk> findByFile_IdAndChunkIndex(Long fileId, Integer chunkIndex);
    List<TextChunk> findAllByCategoryItem_Id(Integer categoryItemId);
}