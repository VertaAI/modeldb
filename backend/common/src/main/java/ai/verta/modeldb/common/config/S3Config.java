package ai.verta.modeldb.common.config;

public class S3Config {
  public String cloudBucketName;
  public String cloudAccessKey;
  public String cloudSecretKey;
  public String awsRegion = "us-east-1";
  public Boolean s3presignedURLEnabled = true;

  public void Validate(String base) throws InvalidConfigException {
    if (cloudBucketName == null || cloudBucketName.isEmpty())
      throw new InvalidConfigException(base + ".cloudBucketName", Config.MISSING_REQUIRED);
  }

  public String storeTypePathPrefix() {
    return String.format("s3://%s/", cloudBucketName);
  }
}
