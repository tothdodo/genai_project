package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.FinalSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinalSummaryRepository extends JpaRepository<FinalSummary, Integer> {
}