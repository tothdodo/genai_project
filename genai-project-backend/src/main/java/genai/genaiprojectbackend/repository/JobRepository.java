package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.Job;
import genai.genaiprojectbackend.model.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {
    long countByFileIdAndStatusIn(Long fileId, Collection<JobStatus> statuses);
    long countByCategoryItemIdAndStatusIn(Integer categoryItemId, Collection<JobStatus> statuses);

    @Modifying
    @Query("UPDATE Job j SET j.status = 'CANCELLED' WHERE j.categoryItemId = :categoryItemId AND j.status NOT IN ('FINISHED', 'FAILED')")
    void cancelRemainingJobs(@Param("categoryItemId") Integer categoryItemId);
}