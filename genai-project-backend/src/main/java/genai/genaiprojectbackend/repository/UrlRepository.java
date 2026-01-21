package genai.genaiprojectbackend.repository;

import genai.genaiprojectbackend.model.entities.File;
import genai.genaiprojectbackend.model.entities.Url;
import io.minio.http.Method;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    /**
     * Find all Url records for a specific File entity.
     */
    List<Url> findByFile(File file);

    /**
     * Find the most recently created Url for a File (useful to get the active/last presigned URL).
     */
    Optional<Url> findFirstByFileOrderByCreatedAtDesc(File file);

    /**
     * Find by file id directly.
     */
    Optional<Url> findByFileId(Long fileId);

    /**
     * Find all Urls for a specific bucket and filename.
     */
    List<Url> findByBucketAndFile_Filename(String bucket, String filename);

    /**
     * Check whether a URL record exists for given bucket and filename.
     */
    boolean existsByBucketAndFile_Filename(String bucket, String filename);

    /**
     * Delete all Url records belonging to a file id.
     */
    void deleteByFileId(Long fileId);

    List<Url> findByFileId_AndMethod(Long fileId, Method method);

    List<Url> findByFileId_AndMethod_AndExpiresAtAfter(Long fileId, Method method, Instant expiresAtAfter);

    List<Url> findAllByExpiresAt(Instant expiresAt);

    List<Url> findByFile_Filename_AndMethod_AndExpiresAtAfter(String fileFilename, Method method, Instant expiresAtAfter);

    String file(File file);

    List<Url> findUrlByExpiresAtBefore(Instant expiresAtBefore);

    void deleteByFileIdAndMethod(Long fileId, Method method);

    List<Url> getUrlsByExpiresAtBefore(Instant expiresAtBefore);
}


