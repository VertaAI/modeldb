package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.CommonMessages;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter(AccessLevel.NONE)
@SuppressWarnings({"squid:S100"})
public class S3Config {
  @JsonProperty private String cloudBucketName;
  @JsonProperty private String cloudBucketPrefix;
  @JsonProperty private String cloudAccessKey;
  @JsonProperty private String cloudSecretKey;
  @JsonProperty private String minioEndpoint;
  @JsonProperty private String awsRegion = "us-east-1";
  @JsonProperty private Boolean s3presignedURLEnabled = true;

  public void validate(String base) throws InvalidConfigException {
    if (cloudBucketName == null || cloudBucketName.isEmpty())
      throw new InvalidConfigException(base + ".cloudBucketName", CommonMessages.MISSING_REQUIRED);
  }

  public String storeTypePathPrefix() {
    return String.format("s3://%s/", cloudBucketName);
  }

  public String getCloudBucketPrefix() {
    return cloudBucketPrefix == null ? "" : cloudBucketPrefix + "/";
  }
}
