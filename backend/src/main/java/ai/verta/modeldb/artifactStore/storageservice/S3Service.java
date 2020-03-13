package ai.verta.modeldb.artifactStore.storageservice;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class S3Service implements ArtifactStoreService {

  private static final Logger LOGGER = LogManager.getLogger(S3Service.class);
  private AmazonS3 s3Client;
  private String bucketName;

  public S3Service(String cloudBucketName) {
    App app = App.getInstance();
    String cloudAccessKey = app.getCloudAccessKey();
    String cloudSecretKey = app.getCloudSecretKey();
    if (cloudAccessKey != null && cloudSecretKey != null) {
      BasicAWSCredentials awsCreds = new BasicAWSCredentials(cloudAccessKey, cloudSecretKey);
      this.s3Client =
          AmazonS3ClientBuilder.standard()
              .withRegion(Regions.US_EAST_1)
              .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
              .build();
    } else {
      // reads credential from OS Environment
      s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    }

    this.bucketName = cloudBucketName;
  }

  private Boolean doesBucketExist(String bucketName) {
    return s3Client.doesBucketExistV2(bucketName);
  }

  @Override
  public String generatePresignedUrl(String s3Key, String method) {
    // Validate bucket
    doesBucketExist(bucketName);

    HttpMethod reqMethod;
    if (method.equalsIgnoreCase(ModelDBConstants.PUT)) {
      reqMethod = HttpMethod.PUT;
    } else if (method.equalsIgnoreCase(ModelDBConstants.GET)) {
      reqMethod = HttpMethod.GET;
    } else {
      String errorMessage = "Unsupported HTTP Method for S3 Presigned URL";
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      LOGGER.warn(errorMessage);
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

    return s3Client.generatePresignedUrl(request).toString();
  }
}
