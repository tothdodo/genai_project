package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.FinalFlashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinalFlashcardRepository extends JpaRepository<FinalFlashcard, Integer> {

    @Query("SELECT ff FROM FinalFlashcard ff " +
            "JOIN FETCH ff.file " +
            "WHERE ff.categoryItem.id = :categoryItemId")
    List<FinalFlashcard> findAllByCategoryItemId(@Param("categoryItemId") Integer categoryItemId);
}