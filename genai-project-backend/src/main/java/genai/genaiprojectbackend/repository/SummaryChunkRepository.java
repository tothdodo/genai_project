package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.SummaryChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummaryChunkRepository extends JpaRepository<SummaryChunk, Integer> {
    List<SummaryChunk> findAllByTextChunk_File_Id(Long fileId);
    List<SummaryChunk> findAllByTextChunk_File_CategoryItem_Id(Integer categoryItemId);
}