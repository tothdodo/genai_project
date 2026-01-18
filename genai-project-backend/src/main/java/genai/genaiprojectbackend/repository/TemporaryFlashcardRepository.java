package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.TemporaryFlashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemporaryFlashcardRepository extends JpaRepository<TemporaryFlashcard, Integer> {
}