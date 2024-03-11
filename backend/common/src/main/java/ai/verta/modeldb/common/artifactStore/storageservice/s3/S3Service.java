package ai.verta.modeldb.common.artifactStore.storageservice.s3;

import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.HttpCodeToGRPCCode;
import ai.verta.modeldb.common.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.google.rpc.Code;
import jakarta.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class S3Service implements ArtifactStoreService {

  private static final Logger LOGGER = LogManager.getLogger(S3Service.class);
  private final S3Client s3Client;
  private final String bucketName;
  private final ArtifactStoreConfig artifactStoreConfig;

  public S3Service(ArtifactStoreConfig artifactStoreConfig) throws ModelDBException, IOException {
    this.artifactStoreConfig = artifactStoreConfig;
    s3Client = new S3Client(artifactStoreConfig.getS3());
    this.bucketName = artifactStoreConfig.getS3().getCloudBucketName();
  }

  private Boolean doesBucketExist(String bucketName) throws ModelDBException {
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      return client.getClient().doesBucketExistV2(bucketName);
    } catch (AmazonServiceException e) {
      CommonUtils.logAmazonServiceExceptionErrorCodes(LOGGER, e);
      throw new UnavailableException(
          "AWS S3 could not be checked for bucket existence for artifact store : "
              + e.getErrorMessage());
    } catch (SdkClientException e) {
      LOGGER.warn(e.getMessage());
      throw new UnavailableException(
          "AWS S3 could not be checked for bucket existence for artifact store : "
              + e.getMessage());
    }
  }

  private Boolean doesObjectExist(String bucketName, String path) throws ModelDBException {
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      return client.getClient().doesObjectExist(bucketName, path);
    } catch (AmazonServiceException e) {
      CommonUtils.logAmazonServiceExceptionErrorCodes(LOGGER, e);
      throw new UnavailableException(
          "AWS S3 could not be checked for object existance for artifact store : "
              + e.getErrorMessage());
    } catch (SdkClientException e) {
      LOGGER.warn(e.getMessage());
      throw new UnavailableException(
          "AWS S3 could not be checked for object existance for artifact store : "
              + e.getMessage());
    } catch (Exception ex) {
      LOGGER.warn(ex.getMessage());
      throw ex;
    }
  }

  @Override
  public Optional<String> initiateMultipart(String s3Key) throws ModelDBException {
    // Validate bucket
    Boolean exist = doesBucketExist(bucketName);
    if (!exist) {
      throw new ModelDBException(CommonMessages.BUCKET_DOES_NOT_EXIST, Code.UNAVAILABLE);
    }
    var initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, s3Key);
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      InitiateMultipartUploadResult result =
          client.getClient().initiateMultipartUpload(initiateMultipartUploadRequest);
      return Optional.ofNullable(result.getUploadId());
    }
  }

  @Override
  public String generatePresignedUrl(String s3Key, String method, long partNumber, String uploadId)
      throws ModelDBException {
    if (artifactStoreConfig.getS3().getS3presignedURLEnabled()) {
      return getS3PresignedUrl(s3Key, method, partNumber, uploadId);
    } else {
      return getPresignedUrlViaMDB(s3Key, method, partNumber, uploadId);
    }
  }

  private String getS3PresignedUrl(String s3Key, String method, long partNumber, String uploadId)
      throws ModelDBException {
    // Validate bucket
    Boolean exist = doesBucketExist(bucketName);
    if (!exist) {
      throw new ModelDBException(CommonMessages.BUCKET_DOES_NOT_EXIST, Code.UNAVAILABLE);
    }

    HttpMethod reqMethod;
    if (method.equalsIgnoreCase("put")) {
      reqMethod = HttpMethod.PUT;
    } else if (method.equalsIgnoreCase("get")) {
      reqMethod = HttpMethod.GET;
    } else {
      var errorMessage = "Unsupported HTTP Method for S3 Presigned URL";
      throw new InvalidArgumentException(errorMessage);
    }

    // Set Expiration
    var expiration = new Date();
    long milliSeconds = expiration.getTime();
    milliSeconds += 1000 * 60 * 5; // Add 5 mins
    expiration.setTime(milliSeconds);

    GeneratePresignedUrlRequest request =
        new GeneratePresignedUrlRequest(bucketName, s3Key)
            .withMethod(reqMethod)
            .withExpiration(expiration);
    if (partNumber != 0) {
      request.addRequestParameter("partNumber", String.valueOf(partNumber));
      request.addRequestParameter("uploadId", uploadId);
    }

    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      return client.getClient().generatePresignedUrl(request).toString();
    }
  }

  @Override
  public void commitMultipart(String s3Key, String uploadId, List<PartETag> partETags)
      throws ModelDBException {
    // Validate bucket
    Boolean exist = doesBucketExist(bucketName);
    if (!exist) {
      throw new ModelDBException(CommonMessages.BUCKET_DOES_NOT_EXIST, Code.UNAVAILABLE);
    }
    var completeMultipartUploadRequest =
        new CompleteMultipartUploadRequest(bucketName, s3Key, uploadId, partETags);
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      CompleteMultipartUploadResult result =
          client.getClient().completeMultipartUpload(completeMultipartUploadRequest);
      LOGGER.info("upload result: {}", result);
    } catch (AmazonS3Exception e) {
      if (e.getStatusCode() == 400) {
        LOGGER.info("message: {} additional details: {}", e.getMessage(), e.getAdditionalDetails());
        throw new ModelDBException(e.getErrorMessage(), Code.FAILED_PRECONDITION);
      }
      throw e;
    }
  }

  public String uploadFile(
      String artifactPath, HttpServletRequest request, Long partNumber, String uploadId)
      throws ModelDBException, IOException {
    File tempFile = copyRequestInputToTempFile(artifactPath, request);
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      Boolean exist = doesBucketExist(bucketName);
      if (!exist) {
        throw new ModelDBException(CommonMessages.BUCKET_DOES_NOT_EXIST, Code.UNAVAILABLE);
      }

      if (partNumber != 0 && uploadId != null && !uploadId.isEmpty()) {
        UploadPartRequest uploadRequest =
            new UploadPartRequest()
                .withBucketName(bucketName)
                .withKey(artifactPath)
                .withUploadId(uploadId)
                .withPartNumber(partNumber.intValue())
                .withFile(tempFile)
                .withPartSize(request.getContentLength());
        var uploadPartResult = client.getClient().uploadPart(uploadRequest);
        return uploadPartResult.getPartETag().getETag();
      } else {
        var metadata = new ObjectMetadata();
        metadata.setContentType(request.getContentType());
        metadata.setContentLength(request.getContentLength());

        var maxUploadThreads = 5;
        var transferManager =
            TransferManagerBuilder.standard()
                .withS3Client(client.getClient())
                // TODO: Validate use and if not required then remove below two line
                .withMultipartUploadThreshold((long) (5 * 1024 * 1024)) // 5 MB
                .withExecutorFactory(() -> Executors.newFixedThreadPool(maxUploadThreads))
                .build();
        var upload =
            transferManager.upload(
                bucketName, artifactPath, new FileInputStream(tempFile), metadata);
        upload.waitForCompletion();
        var uploadResult = upload.waitForUploadResult();
        return uploadResult.getETag();
      }
    } catch (AmazonServiceException e) {
      // Amazon S3 couldn't be contacted for a response, or the client
      // couldn't parse the response from Amazon S3.
      String errorMessage = e.getMessage();
      LOGGER.warn(errorMessage);
      throw new ModelDBException(
          errorMessage, HttpCodeToGRPCCode.convertHTTPCodeToGRPCCode(e.getStatusCode()));
    } catch (InterruptedException e) {
      LOGGER.warn(e.getMessage(), e);
      // Restore interrupted state...
      Thread.currentThread().interrupt();
      throw new ModelDBException(e.getMessage(), Code.INTERNAL, e);
    } finally {
      LOGGER.info("Deleting temp file " + tempFile);
      try {
        if (tempFile.delete()) {
          LOGGER.info("temp file " + tempFile + " deleted successfully");
        } else {
          LOGGER.warn("temp file " + tempFile + " not deleted successfully");
        }
      } catch (Exception e) {
        LOGGER.warn("failed to delete temp file " + tempFile, e);
      }
    }
  }

  private static File copyRequestInputToTempFile(String artifactPath, HttpServletRequest request)
      throws IOException {
    File tempFile = File.createTempFile(artifactPath, "");
    LOGGER.info("Copying requested upload to temp file " + tempFile);
    Files.copy(request.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    return tempFile;
  }

  public ResponseEntity<Resource> loadFileAsResource(String fileName, String artifactPath)
      throws ModelDBException {
    LOGGER.trace("S3Service - loadFileAsResource called");
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      if (doesObjectExist(bucketName, artifactPath)) {
        LOGGER.trace("S3Service - loadFileAsResource - resource exists");
        LOGGER.trace("S3Service - loadFileAsResource returned");
        S3Object resource = client.getClient().getObject(bucketName, artifactPath);

        var responseHeaders = new HttpHeaders();
        for (Map.Entry<String, Object> header :
            resource.getObjectMetadata().getRawMetadata().entrySet()) {
          responseHeaders.add(header.getKey(), String.valueOf(header.getValue()));
        }
        responseHeaders.add("FileName", fileName);
        LOGGER.debug("getArtifact returned");
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .headers(responseHeaders)
            .body(new InputStreamResource(resource.getObjectContent()));
      } else {
        String errorMessage = "File not found " + artifactPath;
        LOGGER.info(errorMessage);
        throw new ModelDBException(errorMessage, Code.NOT_FOUND);
      }
    } catch (AmazonServiceException e) {
      // Amazon S3 couldn't be contacted for a response, or the client
      // couldn't parse the response from Amazon S3.
      String errorMessage = e.getMessage();
      LOGGER.warn(errorMessage);
      throw new ModelDBException(
          errorMessage, HttpCodeToGRPCCode.convertHTTPCodeToGRPCCode(e.getStatusCode()));
    } catch (SdkClientException e) {
      // Amazon S3 couldn't be contacted for a response, or the client
      // couldn't parse the response from Amazon S3.
      String errorMessage = e.getMessage();
      LOGGER.warn(errorMessage);
      throw new ModelDBException(errorMessage);
    }
  }

  public String getPresignedUrlViaMDB(
      String artifactPath, String method, long partNumber, String uploadId) {
    LOGGER.debug("S3Service - generatePresignedUrl called");
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("artifact_path", artifactPath);

    if (method.equalsIgnoreCase("put")) {
      LOGGER.debug("S3Service - generatePresignedUrl - returning " + method + " url");
      LOGGER.debug("part number: " + partNumber);
      parameters.put("part_number", partNumber);
      parameters.put("upload_id", uploadId);
      final var url =
          getUploadUrl(
              parameters,
              artifactStoreConfig.getProtocol(),
              artifactStoreConfig.getArtifactEndpoint().getStoreArtifact(),
              artifactStoreConfig.isPickArtifactStoreHostFromConfig(),
              artifactStoreConfig.getHost());
      LOGGER.debug("S3Service - generatePresignedUrl - returning URL " + url);
      return url;
    } else if (method.equalsIgnoreCase("get")) {
      LOGGER.debug("S3Service - generatePresignedUrl - returning " + method + " url");
      var filename = artifactPath.substring(artifactPath.lastIndexOf("/"));
      parameters.put("FileName", filename);
      final var url =
          getDownloadUrl(
              parameters,
              artifactStoreConfig.getProtocol(),
              artifactStoreConfig.getArtifactEndpoint().getGetArtifact(),
              artifactStoreConfig.isPickArtifactStoreHostFromConfig(),
              artifactStoreConfig.getHost());
      LOGGER.debug("S3Service - generatePresignedUrl - returning URL " + url);
      return url;
    } else {
      var errorMessage = "Unsupported HTTP Method for S3 Presigned URL";
      throw new InvalidArgumentException(errorMessage);
    }
  }

  @Override
  public InputStream downloadFileFromStorage(String key) throws ModelDBException {
    if (!doesBucketExist(bucketName)) {
      throw new ModelDBException(CommonMessages.BUCKET_DOES_NOT_EXIST, Code.UNAVAILABLE);
    }

    return downloadFileFromStorage(bucketName, key);
  }

  private InputStream downloadFileFromStorage(String bucketName, String key)
      throws ModelDBException {
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      if (client.getClient().doesObjectExist(bucketName, key)) {
        LOGGER.debug("file exist in storage");
        var s3object = client.getClient().getObject(bucketName, key);
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        LOGGER.info("file fetched successfully from s3 storage");
        return inputStream;
      }

      String errorMessage = "s3 object not found in s3 storage for given key : " + key;
      LOGGER.info(errorMessage);
      throw new ModelDBException(errorMessage, Code.NOT_FOUND);
    }
  }
}
