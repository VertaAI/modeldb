package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * Fetch temporary credentials from AWS and store them in a file
 */
public class FetchTemporaryS3Token extends TimerTask {
  public FetchTemporaryS3Token(String clientRegion) {
    super();
    this.clientRegion = clientRegion;
  }

  private static final Logger LOGGER = LogManager.getLogger(FetchTemporaryS3Token.class);
  private String roleARN;
  private String clientRegion;

  /** The action to be performed by this timer task. */
  @Override
  public void run() {

    if (!ModelDBUtils.isEnvSet(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE)) {
      LOGGER.error("TokenFile location no found in env Var, skipping token generaration");
      return;
    }

    String roleSessionName = "modelDB" + Calendar.getInstance().getTimeInMillis();
    AWSSecurityTokenService stsClient =
        AWSSecurityTokenServiceClientBuilder.standard()
            .withCredentials(new ProfileCredentialsProvider())
            .withRegion(clientRegion)
            .build();

    initializeRole();

    if (roleARN == null || roleARN.isEmpty()) {
      LOGGER.error("Could not find roleARN value in env Var {}", ModelDBConstants.AWS_ROLE_ARN);
      LOGGER.error("Can not initialize s3 artifact store");
      return;
    }

    // Obtain credentials for the IAM role. Note that you cannot assume the role of an AWS root
    // account;
    // Amazon S3 will deny access. You must use credentials for an IAM user or an IAM role.
    AssumeRoleRequest roleRequest =
        new AssumeRoleRequest().withRoleArn(roleARN).withRoleSessionName(roleSessionName);

    AssumeRoleResult roleResponse = stsClient.assumeRole(roleRequest);
    Credentials credentials = roleResponse.getCredentials();

    try {
      writeCredentialsToFile(
          ModelDBUtils.appendOptionalTelepresencePath(
              System.getenv(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE)),
          credentials);
    } catch (IOException e) {
      LOGGER.error(
          "Token file pointed by {} at {} not found",
          ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE,
          System.getenv(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE));
      return;
    }
  }

  private void writeCredentialsToFile(String filePath, Credentials credentials) throws IOException {
    // Get the file reference
    Path path = Paths.get(filePath);

    // Use try-with-resource to get auto-closeable writer instance
    try (BufferedWriter writer = Files.newBufferedWriter(path)) {
      writer.write(credentials.getAccessKeyId());
      writer.newLine();
      writer.write(credentials.getSecretAccessKey());
      writer.newLine();
      writer.write(credentials.getSessionToken());
    }
  }

  private void initializeRole() {
    if (roleARN != null && !roleARN.isEmpty()) {
      roleARN = System.getenv(ModelDBConstants.AWS_ROLE_ARN);
    }
  }
}
