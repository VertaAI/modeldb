package ai.verta.modeldb.utils;

import static java.time.format.DateTimeFormatter.ofPattern;

import ai.verta.modeldb.App;
import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.artifactStore.storageservice.s3.S3SignatureUtil;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.exceptions.ModelDBException;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.uac.UserInfo;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrialUtils {

  private static final Logger LOGGER = LogManager.getLogger(TrialUtils.class);

  private TrialUtils() {}

  public static void validateMaxArtifactsForTrial(
      App app, int newArtifactsCount, int existingArtifactsCount) throws ModelDBException {
    if (app.getTrialEnabled()) {
      if (app.getMaxArtifactPerRun() != null
          && existingArtifactsCount + newArtifactsCount > app.getMaxArtifactPerRun()) {
        throw new ModelDBException(
            ModelDBConstants.LIMIT_RUN_ARTIFACT_NUMBER
                + "“Number of artifacts exceeded”: You are allowed to log upto "
                + app.getMaxArtifactPerRun()
                + " artifacts per experiment run.",
            Code.RESOURCE_EXHAUSTED);
      }
    }
  }

  public static void validateExperimentRunPerWorkspaceForTrial(
      App app,
      ProjectDAO projectDAO,
      RoleService roleService,
      ExperimentRunDAO experimentRunDAO,
      String projectId,
      UserInfo userInfo)
      throws InvalidProtocolBufferException, ModelDBException {
    if (app.getTrialEnabled()) {
      Project project = projectDAO.getProjectByID(projectId);
      if (project.getWorkspaceId() != null && !project.getWorkspaceId().isEmpty()) {
        // TODO: We can be replaced by a count(*) query instead .setIdsOnly(true)
        FindExperimentRuns findExperimentRuns =
            FindExperimentRuns.newBuilder().setIdsOnly(true).setProjectId(projectId).build();
        ExperimentRunPaginationDTO paginationDTO =
            experimentRunDAO.findExperimentRuns(projectDAO, userInfo, findExperimentRuns);
        if (app.getMaxExperimentRunPerWorkspace() != null
            && paginationDTO.getTotalRecords() >= app.getMaxExperimentRunPerWorkspace()) {
          throw new ModelDBException(
              ModelDBConstants.LIMIT_RUN_NUMBER
                  + "“Number of experiment runs exceeded”: Your trial account allows you to log upto "
                  + app.getMaxExperimentRunPerWorkspace()
                  + " experiment runs. Try deleting prior experiment runs in order to proceed.",
              Code.RESOURCE_EXHAUSTED);
        }
      }
    }
  }

  public static void validateArtifactSizeForTrial(App app, String artifactPath, int artifactSize)
      throws ModelDBException {
    if (app.getTrialEnabled()) {
      double uploadedArtifactSize = ((double) artifactSize / 1024); // In KB
      if (app.getMaxArtifactSizeMB() != null
          && uploadedArtifactSize > ((double) app.getMaxArtifactSizeMB() * 1024)) {
        throw new ModelDBException(
            ModelDBConstants.LIMIT_RUN_ARTIFACT_SIZE
                + "“"
                + artifactPath
                + " exceeds maximum allowed artifact size of "
                + app.getMaxArtifactSizeMB()
                + "MB”: The artifact you are trying to log exceeds the allowable limit for your trial account",
            Code.RESOURCE_EXHAUSTED);
      }
    }
  }

  public static Map<String, String> getBodyParameterMapForTrialPresignedURL(
      App app,
      AWSCredentials awsCredentials,
      String bucketName,
      String s3Key,
      int maxArtifactSize) {
    LocalDateTime localDateTime = LocalDateTime.now();
    String dateTimeStr = localDateTime.format(ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    String date = localDateTime.format(ofPattern("yyyyMMdd"));

    S3SignatureUtil s3SignatureUtil =
        new S3SignatureUtil(awsCredentials, app.getAwsRegion(), ModelDBConstants.S3.toLowerCase());

    String policy = s3SignatureUtil.readPolicy(bucketName, maxArtifactSize, awsCredentials);
    String signature = s3SignatureUtil.getSignature(policy, localDateTime);

    Map<String, String> bodyParametersMap = new HashMap<>();
    // TODO: add expiration
    bodyParametersMap.put("key", s3Key);
    bodyParametersMap.put("Policy", policy);
    bodyParametersMap.put("X-Amz-Signature", signature);
    bodyParametersMap.put("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
    bodyParametersMap.put("X-Amz-Date", dateTimeStr);
    bodyParametersMap.put(
        "X-Amz-Credential",
        String.format(
            "%s/%s/%s/s3/aws4_request",
            awsCredentials.getAWSAccessKeyId(), date, app.getAwsRegion()));
    if (awsCredentials instanceof AWSSessionCredentials) {
      AWSSessionCredentials sessionCreds = (AWSSessionCredentials) awsCredentials;
      bodyParametersMap.put("X-Amz-Security-Token", sessionCreds.getSessionToken());
    }
    return bodyParametersMap;
  }
}
