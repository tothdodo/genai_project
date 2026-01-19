package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.Job;
import genai.genaiprojectbackend.model.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {
    long countByFileIdAndStatusIn(Long fileId, Collection<JobStatus> statuses);
    long countByCategoryItemIdAndStatusIn(Integer categoryItemId, Collection<JobStatus> statuses);
}
