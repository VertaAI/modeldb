package ai.verta.modeldb.artifactStore.storageservice.s3;

import ai.verta.modeldb.App;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.HttpCodeToGRPCCode;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.TrialUtils;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.google.api.client.http.HttpStatusCodes;
import com.google.rpc.Code;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Executors;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class S3Service implements ArtifactStoreService {

  private static final Logger LOGGER = LogManager.getLogger(S3Service.class);
  private S3Client s3Client;
  private String bucketName;
  private final App app = App.getInstance();
  private final Config config = app.config;

  public S3Service(String cloudBucketName) throws ModelDBException, IOException {
    s3Client = new S3Client(cloudBucketName);
    this.bucketName = cloudBucketName;
  }

  private Boolean doesBucketExist(String bucketName) throws ModelDBException {
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      return client.getClient().doesBucketExistV2(bucketName);
    } catch (AmazonServiceException e) {
      ModelDBUtils.logAmazonServiceExceptionErrorCodes(LOGGER, e);
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
      ModelDBUtils.logAmazonServiceExceptionErrorCodes(LOGGER, e);
      throw new UnavailableException(
          "AWS S3 could not be checked for bucket existance for artifact store : "
              + e.getErrorMessage());
    } catch (SdkClientException e) {
      LOGGER.warn(e.getMessage());
      throw new UnavailableException(
          "AWS S3 could not be checked for bucket existance for artifact store : "
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
      throw new ModelDBException("Bucket does not exists", Code.UNAVAILABLE);
    }
    InitiateMultipartUploadRequest initiateMultipartUploadRequest =
        new InitiateMultipartUploadRequest(bucketName, s3Key);
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      InitiateMultipartUploadResult result =
          client.getClient().initiateMultipartUpload(initiateMultipartUploadRequest);
      return Optional.ofNullable(result.getUploadId());
    }
  }

  @Override
  public GetUrlForArtifact.Response generatePresignedUrlForTrial(
      String s3Key, String method, long partNumber, String uploadId) throws ModelDBException {
    if (config.artifactStoreConfig.S3.s3presignedURLEnabled) {
      if (method.equalsIgnoreCase(ModelDBConstants.GET)) {
        return GetUrlForArtifact.Response.newBuilder()
            .setMultipartUploadOk(false)
            .setUrl(getS3PresignedUrl(s3Key, method, partNumber, uploadId))
            .build();
      } else if (method.equalsIgnoreCase(ModelDBConstants.POST)
          || method.equalsIgnoreCase(ModelDBConstants.PUT)) {
        int maxArtifactSize = config.trial.restrictions.max_artifact_size_MB;
        LOGGER.debug("bucketName " + bucketName);
        try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
          return GetUrlForArtifact.Response.newBuilder()
              .setMultipartUploadOk(false)
              .setUrl(String.format("http://%s.s3.amazonaws.com", bucketName))
              .putAllFields(
                  TrialUtils.getBodyParameterMapForTrialPresignedURL(
                      client.getCredentials(),
                      bucketName,
                      config.artifactStoreConfig.S3.awsRegion,
                      s3Key,
                      maxArtifactSize * 1024 * 1024))
              .build();
        }
      } else {
        throw new ModelDBException(
            ModelDBConstants.LIMIT_RUN_ARTIFACT_SIZE
                + "Method type "
                + method
                + " is not supported during the trial",
            Code.RESOURCE_EXHAUSTED);
      }
    } else {
      return GetUrlForArtifact.Response.newBuilder()
          .setMultipartUploadOk(false)
          .setUrl(getPresignedUrlViaMDB(s3Key, method, partNumber, uploadId))
          .build();
    }
  }

  @Override
  public String generatePresignedUrl(String s3Key, String method, long partNumber, String uploadId)
      throws ModelDBException {
    if (config.artifactStoreConfig.S3.s3presignedURLEnabled) {
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
      throw new ModelDBException("Bucket does not exists", Code.UNAVAILABLE);
    }

    HttpMethod reqMethod;
    if (method.equalsIgnoreCase(ModelDBConstants.PUT)) {
      reqMethod = HttpMethod.PUT;
    } else if (method.equalsIgnoreCase(ModelDBConstants.GET)) {
      reqMethod = HttpMethod.GET;
    } else {
      String errorMessage = "Unsupported HTTP Method for S3 Presigned URL";
      throw new InvalidArgumentException(errorMessage);
    }

    // Set Expiration
    Date expiration = new Date();
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
      throw new ModelDBException("Bucket does not exists", Code.UNAVAILABLE);
    }
    CompleteMultipartUploadRequest completeMultipartUploadRequest =
        new CompleteMultipartUploadRequest(bucketName, s3Key, uploadId, partETags);
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      CompleteMultipartUploadResult result =
          client.getClient().completeMultipartUpload(completeMultipartUploadRequest);
      LOGGER.info("upload result: {}", result);
    } catch (AmazonS3Exception e) {
      if (e.getStatusCode() == HttpStatusCodes.STATUS_CODE_BAD_REQUEST) {
        LOGGER.info("message: {} additional details: {}", e.getMessage(), e.getAdditionalDetails());
        throw new ModelDBException(e.getErrorMessage(), Code.FAILED_PRECONDITION);
      }
      throw e;
    }
  }

  public String uploadFile(
      String artifactPath, HttpServletRequest request, Long partNumber, String uploadId)
      throws ModelDBException, IOException {
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      Boolean exist = doesBucketExist(bucketName);
      if (!exist) {
        throw new ModelDBException("Bucket does not exists", Code.UNAVAILABLE);
      }

      // Validate Artifact size for trial case
      TrialUtils.validateArtifactSizeForTrial(
          config.trial, artifactPath, request.getContentLength());

      if (partNumber != 0 && uploadId != null && !uploadId.isEmpty()) {
        UploadPartRequest uploadRequest =
            new UploadPartRequest()
                .withBucketName(bucketName)
                .withKey(artifactPath)
                .withUploadId(uploadId)
                .withPartNumber(partNumber.intValue())
                .withInputStream(request.getInputStream())
                .withPartSize(request.getContentLength());
        UploadPartResult uploadPartResult = client.getClient().uploadPart(uploadRequest);
        return uploadPartResult.getPartETag().getETag();
      } else {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(request.getContentType());
        metadata.setContentLength(request.getContentLength());

        int maxUploadThreads = 5;
        TransferManager transferManager =
            TransferManagerBuilder.standard()
                .withS3Client(client.getClient())
                // TODO: Validate use and if not required then remove below two line
                .withMultipartUploadThreshold((long) (5 * 1024 * 1024)) // 5 MB
                .withExecutorFactory(() -> Executors.newFixedThreadPool(maxUploadThreads))
                .build();
        Upload upload =
            transferManager.upload(bucketName, artifactPath, request.getInputStream(), metadata);
        upload.waitForCompletion();
        UploadResult uploadResult = upload.waitForUploadResult();
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
      throw new ModelDBException(e.getMessage(), Code.INTERNAL);
    }
  }

  public ResponseEntity<Resource> loadFileAsResource(String fileName, String artifactPath)
      throws ModelDBException {
    LOGGER.trace("S3Service - loadFileAsResource called");
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      if (doesObjectExist(bucketName, artifactPath)) {
        LOGGER.trace("S3Service - loadFileAsResource - resource exists");
        LOGGER.trace("S3Service - loadFileAsResource returned");
        S3Object resource = client.getClient().getObject(bucketName, artifactPath);

        HttpHeaders responseHeaders = new HttpHeaders();
        for (Map.Entry<String, Object> header :
            resource.getObjectMetadata().getRawMetadata().entrySet()) {
          responseHeaders.add(header.getKey(), String.valueOf(header.getValue()));
        }
        responseHeaders.add(ModelDBConstants.FILENAME, fileName);
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
    LOGGER.trace("S3Service - generatePresignedUrl called");
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("artifact_path", artifactPath);

    if (method.equalsIgnoreCase(ModelDBConstants.PUT)) {
      LOGGER.trace("S3Service - generatePresignedUrl - put url returned");
      parameters.put("part_number", partNumber);
      parameters.put("upload_id", uploadId);
      return getUploadUrl(
          parameters,
          config.artifactStoreConfig.protocol,
          config.artifactStoreConfig.artifactEndpoint.getArtifact,
          config.artifactStoreConfig.pickArtifactStoreHostFromConfig,
          config.artifactStoreConfig.host);
    } else if (method.equalsIgnoreCase(ModelDBConstants.GET)) {
      LOGGER.trace("S3Service - generatePresignedUrl - get url returned");
      String filename = artifactPath.substring(artifactPath.lastIndexOf("/"));
      parameters.put(ModelDBConstants.FILENAME, filename);
      return getDownloadUrl(
          parameters,
          config.artifactStoreConfig.protocol,
          config.artifactStoreConfig.artifactEndpoint.getArtifact,
          config.artifactStoreConfig.pickArtifactStoreHostFromConfig,
          config.artifactStoreConfig.host);
    } else {
      String errorMessage = "Unsupported HTTP Method for S3 Presigned URL";
      throw new InvalidArgumentException(errorMessage);
    }
  }

  @Override
  public InputStream downloadFileFromStorage(String key) throws ModelDBException {
    if (!doesBucketExist(bucketName)) {
      throw new ModelDBException("Bucket does not exists", Code.UNAVAILABLE);
    }

    return downloadFileFromStorage(bucketName, key);
  }

  private InputStream downloadFileFromStorage(String bucketName, String key)
      throws ModelDBException {
    try (RefCountedS3Client client = s3Client.getRefCountedClient()) {
      if (client.getClient().doesObjectExist(bucketName, key)) {
        LOGGER.debug("file exist in storage");
        S3Object s3object = client.getClient().getObject(bucketName, key);
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
