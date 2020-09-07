package ai.verta.modeldb.artifactStore.storageservice.s3;

import ai.verta.modeldb.App;
import ai.verta.modeldb.HttpCodeToGRPCCode;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.cron_jobs.FetchTemporaryS3Token;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.google.api.client.http.HttpStatusCodes;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

public class S3Service implements ArtifactStoreService {

  private static final Logger LOGGER = LogManager.getLogger(S3Service.class);
  private AmazonS3 s3Client;
  private String bucketName;
  private Regions awsRegion;
  private static Credentials temporarySessionCredentials;
  private App app = App.getInstance();

  public S3Service(String cloudBucketName) throws ModelDBException {
    App app = App.getInstance();
    String cloudAccessKey = app.getCloudAccessKey();
    String cloudSecretKey = app.getCloudSecretKey();
    String minioEndpoint = app.getMinioEndpoint();
    awsRegion = Regions.fromName(app.getAwsRegion());
    this.bucketName = cloudBucketName;

    if (cloudAccessKey != null && cloudSecretKey != null) {
      if (minioEndpoint == null) {
        LOGGER.debug("config based credentials based s3 client");
        initializeS3ClientWithAccessKey(cloudAccessKey, cloudSecretKey, awsRegion);
      } else {
        LOGGER.debug("minio client");
        initializeMinioClient(cloudAccessKey, cloudSecretKey, awsRegion, minioEndpoint);
      }
    } else if (ModelDBUtils.isEnvSet(ModelDBConstants.AWS_ROLE_ARN)
        && ModelDBUtils.isEnvSet(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE)) {
      LOGGER.debug("temporary token based s3 client");
      fetchCredentialsAndInitializeS3ClientWithTemporaryCredentials(awsRegion);
    } else {
      LOGGER.debug("environment credentials based s3 client");
      // reads credential from OS Environment
      s3Client = AmazonS3ClientBuilder.standard().withRegion(awsRegion).build();
    }
  }

  private void initializeMinioClient(
      String cloudAccessKey, String cloudSecretKey, Regions awsRegion, String minioEndpoint) {
    AWSCredentials awsCreds = new BasicAWSCredentials(cloudAccessKey, cloudSecretKey);
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("AWSS3V4SignerType");

    this.s3Client =
        AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(minioEndpoint, awsRegion.getName()))
            .withPathStyleAccessEnabled(true)
            .withClientConfiguration(clientConfiguration)
            .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
            .build();
  }

  private void initializeS3ClientWithAccessKey(
      String cloudAccessKey, String cloudSecretKey, Regions awsRegion) {
    BasicAWSCredentials awsCreds = new BasicAWSCredentials(cloudAccessKey, cloudSecretKey);
    this.s3Client =
        AmazonS3ClientBuilder.standard()
            .withRegion(awsRegion)
            .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
            .build();
  }

  private void fetchCredentialsAndInitializeS3ClientWithTemporaryCredentials(Regions clientRegion) {

    try {
      RefreshCredentialsAndSchedule(clientRegion.toString());

      initializeS3ClientWithTemporaryCredentials(clientRegion);

    } catch (AmazonServiceException e) {
      // The call was transmitted successfully, but Amazon S3 couldn't process
      // it, so it returned an error response.
      e.printStackTrace();
    } catch (SdkClientException e) {
      // Amazon S3 couldn't be contacted for a response, or the client
      // couldn't parse the response from Amazon S3.
      e.printStackTrace();
    }
  }

  private void initializeS3ClientWithTemporaryCredentials(Regions clientRegion) {
    // Create a BasicSessionCredentials object that contains the credentials you just retrieved.
    BasicSessionCredentials awsCredentials =
        new BasicSessionCredentials(
            temporarySessionCredentials.getAccessKeyId(),
            temporarySessionCredentials.getSecretAccessKey(),
            temporarySessionCredentials.getSessionToken());

    // Provide temporary security credentials so that the Amazon S3 client
    // can send authenticated requests to Amazon S3. You create the client
    // using the sessionCredentials object.
    s3Client =
        AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .withRegion(clientRegion)
            .build();
    LOGGER.debug("s3client refreshed with temporary credentials");
  }

  private void RefreshCredentialsAndSchedule(String region) {
    LOGGER.debug("fetching token for s3 access");

    LOGGER.debug(
        "credentials before refresh {}",
        temporarySessionCredentials != null ? temporarySessionCredentials.hashCode() : null);

    TimerTask task = new FetchTemporaryS3Token(region);
    task.run();

    LOGGER.debug(
        "credentials after refresh {}",
        temporarySessionCredentials != null ? temporarySessionCredentials.hashCode() : null);
    LOGGER.debug("fetched token for s3 access");

    Date expiration = temporarySessionCredentials.getExpiration();
    Date now = new Date();
    long diffInSec = Math.abs(expiration.getTime() - now.getTime()) / 1000;
    long delay = diffInSec / 2;
    LOGGER.info("sceduled cron for credentail refresh in {} seconds", delay);
    ModelDBUtils.scheduleTask(task, delay, delay, TimeUnit.SECONDS);
    LOGGER.debug("scheduled periodic task to fetch token for s3 access");
  }

  private Boolean doesBucketExist(String bucketName) {
    try {
      return s3Client.doesBucketExistV2(bucketName);
    } catch (AmazonServiceException e) {
      // If token based access is configured and getting issues checking bucket existence then try
      // refreshing credentials
      if (ModelDBUtils.isEnvSet(ModelDBConstants.AWS_ROLE_ARN)
          && ModelDBUtils.isEnvSet(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE)) {
        // this may spiral into an infinite loop due to incorrect configuration
        LOGGER.info("Fetching temporary credentails ");
        LOGGER.warn(e.getErrorMessage());
        initializeS3ClientWithTemporaryCredentials(awsRegion);
      }
      return doesBucketExist(bucketName);
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
    InitiateMultipartUploadResult result =
        s3Client.initiateMultipartUpload(initiateMultipartUploadRequest);
    return Optional.ofNullable(result.getUploadId());
  }

  @Override
  public String generatePresignedUrl(String s3Key, String method, long partNumber, String uploadId)
      throws ModelDBException {
    if (app.isS3presignedURLEnabled()) {
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
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      LOGGER.info(errorMessage);
      throw StatusProto.toStatusRuntimeException(status);
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

    return s3Client.generatePresignedUrl(request).toString();
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
    try {
      CompleteMultipartUploadResult result =
          s3Client.completeMultipartUpload(completeMultipartUploadRequest);
      LOGGER.info("upload result: {}", result);
    } catch (AmazonS3Exception e) {
      if (e.getStatusCode() == HttpStatusCodes.STATUS_CODE_BAD_REQUEST) {
        LOGGER.info("message: {} additional details: {}", e.getMessage(), e.getAdditionalDetails());
        throw new ModelDBException(e.getErrorMessage(), Code.FAILED_PRECONDITION);
      }
      throw e;
    }
  }

  public static void setTemporarySessionCredentials(Credentials temporarySessionCredentials) {
    S3Service.temporarySessionCredentials = temporarySessionCredentials;
  }

  public String uploadFile(
      String artifactPath, HttpServletRequest request, Long partNumber, String uploadId)
      throws ModelDBException, IOException {
    try {
      Boolean exist = doesBucketExist(bucketName);
      if (!exist) {
        throw new ModelDBException("Bucket does not exists", Code.UNAVAILABLE);
      }

      if (partNumber != 0 && uploadId != null && !uploadId.isEmpty()) {
        UploadPartRequest uploadRequest =
            new UploadPartRequest()
                .withBucketName(bucketName)
                .withKey(artifactPath)
                .withUploadId(uploadId)
                .withPartNumber(partNumber.intValue())
                .withInputStream(request.getInputStream())
                .withPartSize(request.getContentLength());
        UploadPartResult uploadPartResult = s3Client.uploadPart(uploadRequest);
        return uploadPartResult.getPartETag().getETag();
      } else {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(request.getContentType());
        metadata.setContentLength(request.getContentLength());

        int maxUploadThreads = 5;
        TransferManager transferManager =
            TransferManagerBuilder.standard()
                .withS3Client(s3Client)
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

  public Resource loadFileAsResource(String artifactPath) throws ModelDBException {
    LOGGER.trace("S3Service - loadFileAsResource called");
    try {
      if (s3Client.doesObjectExist(bucketName, artifactPath)) {
        LOGGER.trace("S3Service - loadFileAsResource - resource exists");
        LOGGER.trace("S3Service - loadFileAsResource returned");
        return new InputStreamResource(
            s3Client.getObject(bucketName, artifactPath).getObjectContent());
      } else {
        String errorMessage = "File not found " + artifactPath;
        LOGGER.warn(errorMessage);
        throw new ModelDBException(errorMessage);
      }
    } catch (ModelDBException ex) {
      String errorMessage = "File not found " + artifactPath;
      LOGGER.warn(errorMessage, ex);
      throw new ModelDBException(errorMessage, ex);
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
          app.getArtifactStoreUrlProtocol(),
          app.getStoreArtifactEndpoint(),
          app.getPickArtifactStoreHostFromConfig(),
          app.getArtifactStoreServerHost());
    } else if (method.equalsIgnoreCase(ModelDBConstants.GET)) {
      LOGGER.trace("S3Service - generatePresignedUrl - get url returned");
      return getDownloadUrl(
          parameters,
          app.getArtifactStoreUrlProtocol(),
          app.getGetArtifactEndpoint(),
          app.getPickArtifactStoreHostFromConfig(),
          app.getArtifactStoreServerHost());
    } else {
      String errorMessage = "Unsupported HTTP Method for S3 Presigned URL";
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      LOGGER.info(errorMessage);
      throw StatusProto.toStatusRuntimeException(status);
    }
  }
}
