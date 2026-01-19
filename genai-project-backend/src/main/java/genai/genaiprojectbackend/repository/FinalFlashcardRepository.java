package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.FinalFlashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinalFlashcardRepository extends JpaRepository<FinalFlashcard, Integer> {
}