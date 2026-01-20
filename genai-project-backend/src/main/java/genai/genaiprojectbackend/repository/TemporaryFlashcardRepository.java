package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.TemporaryFlashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemporaryFlashcardRepository extends JpaRepository<TemporaryFlashcard, Integer> {
    List<TemporaryFlashcard> findAllBySummaryChunk_TextChunk_File_Id(Long fileId);
    List<TemporaryFlashcard> findAllBySummaryChunk_TextChunk_File_CategoryItem_Id(Integer categoryItemId);
}