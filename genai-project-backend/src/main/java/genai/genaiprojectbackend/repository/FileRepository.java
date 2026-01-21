package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.dtos.WorkerFile;
import genai.genaiprojectbackend.model.entities.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findFileByFilenameAndUploaded(String filename, Boolean uploaded);
    List<WorkerFile> findByCategoryItemId(Integer categoryItemId);
}

