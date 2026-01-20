package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.FinalFlashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinalFlashcardRepository extends JpaRepository<FinalFlashcard, Integer> {
    List<FinalFlashcard> findAllByCategoryItemId(Integer categoryItemId);
}