package genai.genaiprojectbackend.configuration;

import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEndpoint(), props.getPort(), false)
                .credentials(props.getUsername(), props.getPassword())
                .build();
    }

    @Bean
    public MinioAsyncClient minioAsyncClient(MinioProperties props) {
        return MinioAsyncClient.builder()
                .endpoint(props.getEndpoint(), props.getPort(), false)
                .credentials(props.getUsername(), props.getPassword())
                .build();
    }
}

