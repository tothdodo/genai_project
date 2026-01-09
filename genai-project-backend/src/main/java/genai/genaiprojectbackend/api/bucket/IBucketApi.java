package genai.genaiprojectbackend.api.bucket;

import genai.genaiprojectbackend.api.bucket.dtos.FileInfoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;


@Validated
@RequestMapping(IBucketApi.BASE_URL)
public interface IBucketApi {
    String BASE_URL = "/api/v1/bucket";

    @Operation(summary = "Fetch presigned URL from bucket", description = "Fetch a presigned url for a specific file name from the bucket.", tags = {"bucket"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.io.File.class))),

            @ApiResponse(responseCode = "400", description = "Invalid input"),

            @ApiResponse(responseCode = "404", description = "File not Found"),


    })
    @RequestMapping(
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.POST)
    ResponseEntity<FileInfoDto> fetchFile(
            @Parameter(in = ParameterIn.DEFAULT, description = "Fetch a presigned url for a specific file name from the bucket.", required = true, schema = @Schema())
            @Valid
            @RequestBody FileInfoDto body
    );

    @Operation(summary = "Fetch presigned URL from bucket to upload a file", description = "Fetch presigned URL from bucket to upload a file to the bucket.", tags = {
            "bucket"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.io.File.class))),

            @ApiResponse(responseCode = "400", description = "Invalid input"),

            @ApiResponse(responseCode = "409", description = "File with that name already exists"),


    })
    @RequestMapping(value = "/upload",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.POST)
    ResponseEntity<FileInfoDto> fetchURLForUpload(
            @Parameter(in = ParameterIn.DEFAULT, description = "Fetch a presigned upload url for a specific file name from the bucket.", required = true, schema = @Schema())
            @Valid
            @RequestBody FileInfoDto body
    );


    @Operation(summary = "Signalizes that the upload of a file has finished", description = "Signalizes that the upload of a file has finished, changes relevant metadata for that file.", tags = {
            "bucket"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.io.File.class))),

            @ApiResponse(responseCode = "400", description = "Invalid input"),

            @ApiResponse(responseCode = "404", description = "File not Found"),
    })
    @RequestMapping(value = "/upload",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.PUT)
    ResponseEntity<FileInfoDto> uploadFinished(
            @Parameter(in = ParameterIn.DEFAULT, description = "Signalizes that the upload of a file has finished, changes relevant metadata for that file.", required = true, schema = @Schema())
            @RequestBody FileInfoDto body
    ); // returns


}

