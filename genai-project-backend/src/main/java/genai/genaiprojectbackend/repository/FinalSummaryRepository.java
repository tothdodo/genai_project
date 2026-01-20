package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.FinalSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinalSummaryRepository extends JpaRepository<FinalSummary, Integer> {

    @Query("SELECT fs.summaryText FROM FinalSummary fs WHERE fs.categoryItem.id = :categoryItemId")
    Optional<String> findSummaryTextByCategoryItemId(@Param("categoryItemId") Integer categoryItemId);
}
