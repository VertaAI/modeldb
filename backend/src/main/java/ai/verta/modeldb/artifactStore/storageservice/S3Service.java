package ai.verta.modeldb.artifactStore.storageservice;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
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
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.google.api.client.http.HttpStatusCodes;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class S3Service implements ArtifactStoreService {

  private static final Logger LOGGER = LogManager.getLogger(S3Service.class);
  private AmazonS3 s3Client;
  private String bucketName;
  private String roleARN;
  private String webToken;
  private AWSSecurityTokenService stsClient;

  public S3Service(String cloudBucketName) {
    App app = App.getInstance();
    String cloudAccessKey = app.getCloudAccessKey();
    String cloudSecretKey = app.getCloudSecretKey();
    String minioEndpoint = app.getMinioEndpoint();
    final Regions awsRegion = Regions.fromName(app.getAwsRegion());
    if (cloudAccessKey != null && cloudSecretKey != null) {
      if (minioEndpoint == null) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(cloudAccessKey, cloudSecretKey);
        this.s3Client =
            AmazonS3ClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
      } else {

        AWSCredentials awsCreds = new BasicAWSCredentials(cloudAccessKey, cloudSecretKey);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");

        this.s3Client =
            AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(minioEndpoint, app.getAwsRegion()))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
      }
    } else if (isEnvSet(ModelDBConstants.AWS_ROLE_ARN)
        && isEnvSet(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE)) {
      initializeS3ClientWithTemporaryCredentials(awsRegion, cloudBucketName, cloudAccessKey, cloudSecretKey);
    } else {
      // reads credential from OS Environment
      s3Client = AmazonS3ClientBuilder.standard().withRegion(awsRegion).build();
    }

    this.bucketName = cloudBucketName;
  }

  private void initializeS3ClientWithTemporaryCredentials(
      Regions awsRegion, String cloudBucketName, String cloudAccessKey, String cloudSecretKey) {
    initializeRole();
    initializeToken();
    initializes3Client(awsRegion, cloudBucketName, cloudAccessKey, cloudSecretKey);
  }

  private void initializes3Client(Regions clientRegion, String bucketName, String cloudAccessKey, String cloudSecretKey) {
    String roleSessionName = "modelDB" + Calendar.getInstance().getTimeInMillis();

    try {
    	
      // Creating the STS client is part of your trusted code. It has
      // the security credentials you use to obtain temporary security credentials.
      AWSSecurityTokenService stsClient =
          AWSSecurityTokenServiceClientBuilder.standard()
              .withCredentials(new ProfileCredentialsProvider())
              .withRegion(clientRegion)
              .build();

      // Obtain credentials for the IAM role. Note that you cannot assume the role of an AWS root
      // account;
      // Amazon S3 will deny access. You must use credentials for an IAM user or an IAM role.
      AssumeRoleRequest roleRequest =
          new AssumeRoleRequest().withRoleArn(roleARN).withRoleSessionName(roleSessionName);
      AssumeRoleResult roleResponse = stsClient.assumeRole(roleRequest);

      Credentials sessionCredentials = roleResponse.getCredentials();

      // Create a BasicSessionCredentials object that contains the credentials you just retrieved.
      BasicSessionCredentials awsCredentials =
          new BasicSessionCredentials(
              //sessionCredentials.getAccessKeyId(),
              //sessionCredentials.getSecretAccessKey(),
        		  cloudAccessKey, cloudSecretKey
              sessionCredentials.getSessionToken());

      // Provide temporary security credentials so that the Amazon S3 client
      // can send authenticated requests to Amazon S3. You create the client
      // using the sessionCredentials object.
      s3Client =
          AmazonS3ClientBuilder.standard()
              .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
              .withRegion(clientRegion)
              .build();

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

  private void initializeToken() {
    if (webToken != null && !webToken.isEmpty()) {
      String tokenFile =
          ModelDBUtils.appendOptionalTelepresencePath(
              System.getenv(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE));
      try {
        webToken = new String(Files.readAllBytes(Paths.get(tokenFile)));
      } catch (IOException e) {
        LOGGER.error(
            "Token file pointed by {} at {} not found",
            ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE,
            tokenFile);
      }
    }
  }

  private void initializeRole() {
    if (roleARN != null && !roleARN.isEmpty()) {
      roleARN = System.getenv(ModelDBConstants.AWS_ROLE_ARN);
    }
  }

  private boolean isEnvSet(String envVar) {
    String envVarVal = System.getenv(envVar);
    return envVarVal != null && !envVarVal.isEmpty();
  }

  private Boolean doesBucketExist(String bucketName) {
    return s3Client.doesBucketExistV2(bucketName);
  }

  @Override
  public Optional<String> initiateMultipart(String s3Key) throws ModelDBException {
    // Validate bucket
    Boolean exist = doesBucketExist(bucketName);
    if (!exist) {
      throw new ModelDBException("Bucket does not exists", Code.INTERNAL);
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
    // Validate bucket
    Boolean exist = doesBucketExist(bucketName);
    if (!exist) {
      throw new ModelDBException("Bucket does not exists", Code.INTERNAL);
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
    java.util.Date expiration = new java.util.Date();
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
      throw new ModelDBException("Bucket does not exists", Code.INTERNAL);
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
}
