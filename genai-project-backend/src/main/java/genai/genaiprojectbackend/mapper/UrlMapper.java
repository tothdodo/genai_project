package genai.genaiprojectbackend.mapper;

import genai.genaiprojectbackend.api.bucket.dtos.FileInfoDto;
import genai.genaiprojectbackend.model.entities.Url;
import org.springframework.stereotype.Component;

@Component
public class UrlMapper {
    public FileInfoDto urlToFileInfoDto(Url url) {
        FileInfoDto fileInfoDto = new FileInfoDto(url.getFile().getFilename());
        fileInfoDto.setPresignedURL(url.getPresignedURL());
        fileInfoDto.setUrlExpiresAt(url.getExpiresAt());
        return fileInfoDto;
    }
}

