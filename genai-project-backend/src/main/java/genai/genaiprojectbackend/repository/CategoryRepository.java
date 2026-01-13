package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    @Query("""
                SELECT DISTINCT c
                FROM Category c
                LEFT JOIN FETCH c.items
                ORDER BY c.createdAt DESC
            """)
    List<Category> findAllWithItemsOrderByCreatedAtDesc();

    Boolean existsByName(String name);
}
