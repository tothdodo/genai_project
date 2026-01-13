package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.CategoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryItemRepository extends JpaRepository<CategoryItem, Integer> {
    List<CategoryItem> findAllByCategoryIdOrderByCreatedAtDesc(Integer categoryId);

    boolean existsByCategoryIdAndName(Integer categoryId, String name);

    @Query("SELECT ci FROM CategoryItem ci JOIN FETCH ci.category WHERE ci.id = :id")
    Optional<CategoryItem> findByIdWithCategory(@Param("id") Integer id);
}
