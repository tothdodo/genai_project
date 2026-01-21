package genai.genaiprojectbackend.model.dtos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WorkerFile {
    private Long id;
    private String url;
}
