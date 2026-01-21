package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.CategoryItem;
import genai.genaiprojectbackend.repository.projection.StatusOnly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryItemRepository extends JpaRepository<CategoryItem, Integer> {
    List<CategoryItem> findAllByCategoryIdOrderByCreatedAtDesc(Integer categoryId);

    boolean existsByCategoryIdAndName(Integer categoryId, String name);

    @Query("SELECT ci FROM CategoryItem ci " +
            "JOIN FETCH ci.category " +
            "LEFT JOIN FETCH ci.files f " +
            "WHERE ci.id = :id")
    Optional<CategoryItem> findByIdWithCategoryAndFiles(@Param("id") Integer id);

    Optional<StatusOnly> findProjectedById(Integer id);
}
