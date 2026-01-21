package genai.genaiprojectbackend.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
public class MinioProperties {
    private String endpoint;
    private int port;
    private String username;
    private String password;
    private String bucket;
    private String baseObject;
    private String baseObjectPath;
    private int defaultExpiryTime;
    private int defaultRefresh;
}
