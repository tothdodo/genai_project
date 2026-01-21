package genai.genaiprojectbackend.repository.projection;

public interface StatusOnly {
    String getStatus();
    String getFailedJobType();
}
