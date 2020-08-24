package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.artifactStore.storageservice.s3.S3Service;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TimerTask;
import java.util.UUID;
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
  private String roleArn;
  private String clientRegion;

  /** The action to be performed by this timer task. */
  @Override
  public void run() {

    if (!ModelDBUtils.isEnvSet(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE)) {
      LOGGER.error("TokenFile location no found in env Var, skipping token generaration");
      return;
    }

    String roleSessionName = "modelDB" + UUID.randomUUID().toString();

    AWSSecurityTokenService stsClient =
        AWSSecurityTokenServiceClientBuilder.standard().withRegion(clientRegion).build();

    initializeRole();

    if (roleArn == null || roleArn.isEmpty()) {
      LOGGER.error("Could not find roleARN value in env Var {}", ModelDBConstants.AWS_ROLE_ARN);
      LOGGER.error("Can not initialize s3 artifact store");
      return;
    }

    String token =
        readToken(
            ModelDBUtils.appendOptionalTelepresencePath(
                System.getenv(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE)));

    LOGGER.debug("Read token of length {}", token.length());

    // Obtain credentials for the IAM role. Note that you cannot assume the role of
    // an AWS root account;
    // Amazon S3 will deny access. You must use credentials for an IAM user or an
    // IAM role.
    AssumeRoleWithWebIdentityRequest roleRequest =
        new AssumeRoleWithWebIdentityRequest()
            .withRoleArn(roleArn)
            .withWebIdentityToken(token)
            .withRoleSessionName(roleSessionName);

    AssumeRoleWithWebIdentityResult roleResponse = stsClient.assumeRoleWithWebIdentity(roleRequest);
    LOGGER.debug("Received response for AssumeRoleWithWebIdentityRequest");
    Credentials credentials = roleResponse.getCredentials();
    S3Service.setTemporarySessionCredentials(credentials);
    LOGGER.debug("Refreshed session credentials");
  }

  private String readToken(String filePath) {
    try {
      return new String(Files.readAllBytes(Paths.get(filePath)));
    } catch (IOException e) {
      LOGGER.error(
          "Token file pointed by {} at {} could not be read",
          ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE,
          System.getenv(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE));
      return null;
    }
  }

  /** Assume role arn does not change after the first read. */
  private void initializeRole() {
    if (roleArn == null || roleArn.isEmpty()) {
      roleArn = System.getenv(ModelDBConstants.AWS_ROLE_ARN);
    }
  }
}
