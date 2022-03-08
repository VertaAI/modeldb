package ai.verta.modeldb.common.config;

@SuppressWarnings({"squid:S100"})
public class S3Config {
  private String cloudBucketName;
  private String cloudBucketPrefix;
  private String cloudAccessKey;
  private String cloudSecretKey;
  private String minioEndpoint;
  private String awsRegion = "us-east-1";
  private Boolean s3presignedURLEnabled = true;

  public void Validate(String base) throws InvalidConfigException {
    if (cloudBucketName == null || cloudBucketName.isEmpty())
      throw new InvalidConfigException(base + ".cloudBucketName", Config.MISSING_REQUIRED);
  }

  public String storeTypePathPrefix() {
    var pathPrefix = String.format("s3://%s/", cloudBucketName);
    if (cloudBucketPrefix != null && !cloudBucketPrefix.isEmpty()) {
      pathPrefix += cloudBucketPrefix + "/";
    }
    return pathPrefix;
  }

  public String getCloudBucketName() {
    return cloudBucketName;
  }

  public String getCloudAccessKey() {
    return cloudAccessKey;
  }

  public String getCloudSecretKey() {
    return cloudSecretKey;
  }

  public String getMinioEndpoint() {
    return minioEndpoint;
  }

  public String getAwsRegion() {
    return awsRegion;
  }

  public Boolean getS3presignedURLEnabled() {
    return s3presignedURLEnabled;
  }

  public String getCloudBucketPrefix() {
    return cloudBucketPrefix;
  }
}
