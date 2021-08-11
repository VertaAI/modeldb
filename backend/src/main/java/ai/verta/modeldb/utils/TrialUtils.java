package ai.verta.modeldb.utils;

import static java.time.format.DateTimeFormatter.ofPattern;

import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.artifactStore.storageservice.s3.S3SignatureUtil;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.config.TrialConfig;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrialUtils {

  private static final Logger LOGGER = LogManager.getLogger(TrialUtils.class);

  private TrialUtils() {}

  public static void validateMaxArtifactsForTrial(
      TrialConfig config, int newArtifactsCount, int existingArtifactsCount)
      throws ModelDBException {
    if (config != null) {
      if (config.restrictions.max_artifact_per_run != null
          && existingArtifactsCount + newArtifactsCount
              > config.restrictions.max_artifact_per_run) {
        throw new ModelDBException(
            ModelDBConstants.LIMIT_RUN_ARTIFACT_NUMBER
                + "“Number of artifacts exceeded”: You are allowed to log upto "
                + config.restrictions.max_artifact_per_run
                + " artifacts per experiment run.",
            Code.RESOURCE_EXHAUSTED);
      }
    }
  }

  public static void validateExperimentRunPerWorkspaceForTrial(
      TrialConfig config,
      ProjectDAO projectDAO,
      RoleService roleService,
      ExperimentRunDAO experimentRunDAO,
      String projectId,
      UserInfo userInfo)
      throws InvalidProtocolBufferException, ModelDBException, ExecutionException,
          InterruptedException {
    if (config != null) {
      Project project = projectDAO.getProjectByID(projectId);
      if (project.getWorkspaceId() != null && !project.getWorkspaceId().isEmpty()) {
        // TODO: We can be replaced by a count(*) query instead .setIdsOnly(true)
        FindExperimentRuns findExperimentRuns =
            FindExperimentRuns.newBuilder().setIdsOnly(true).setProjectId(projectId).build();
        ExperimentRunPaginationDTO paginationDTO =
            experimentRunDAO.findExperimentRuns(projectDAO, userInfo, findExperimentRuns);
        if (config.restrictions.max_experiment_run_per_workspace != null
            && paginationDTO.getTotalRecords()
                >= config.restrictions.max_experiment_run_per_workspace) {
          throw new ModelDBException(
              ModelDBConstants.LIMIT_RUN_NUMBER
                  + "“Number of experiment runs exceeded”: Your trial account allows you to log upto "
                  + config.restrictions.max_experiment_run_per_workspace
                  + " experiment runs. Try deleting prior experiment runs in order to proceed.",
              Code.RESOURCE_EXHAUSTED);
        }
      }
    }
  }

  public static InternalFuture<Void> futureValidateExperimentRunPerWorkspaceForTrial(
      TrialConfig config, Executor executor) {
    // TODO: Implement trial support using InternalFuture
    return InternalFuture.runAsync(() -> {}, executor);
  }

  public static void validateArtifactSizeForTrial(
      TrialConfig config, String artifactPath, int artifactSize) throws ModelDBException {
    if (config != null) {
      double uploadedArtifactSize = ((double) artifactSize / 1024); // In KB
      if (config.restrictions.max_artifact_size_MB != null
          && uploadedArtifactSize > ((double) config.restrictions.max_artifact_size_MB * 1024)) {
        throw new ModelDBException(
            ModelDBConstants.LIMIT_RUN_ARTIFACT_SIZE
                + "“"
                + artifactPath
                + " exceeds maximum allowed artifact size of "
                + config.restrictions.max_artifact_size_MB
                + "MB”: The artifact you are trying to log exceeds the allowable limit for your trial account",
            Code.RESOURCE_EXHAUSTED);
      }
    }
  }

  public static Map<String, String> getBodyParameterMapForTrialPresignedURL(
      AWSCredentials awsCredentials,
      String bucketName,
      String s3Key,
      String region,
      int maxArtifactSize) {
    LocalDateTime localDateTime = LocalDateTime.now();
    String dateTimeStr = localDateTime.format(ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    String date = localDateTime.format(ofPattern("yyyyMMdd"));

    S3SignatureUtil s3SignatureUtil =
        new S3SignatureUtil(awsCredentials, region, ModelDBConstants.S3.toLowerCase());

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
            "%s/%s/%s/s3/aws4_request", awsCredentials.getAWSAccessKeyId(), date, region));
    if (awsCredentials instanceof AWSSessionCredentials) {
      AWSSessionCredentials sessionCreds = (AWSSessionCredentials) awsCredentials;
      bodyParametersMap.put("X-Amz-Security-Token", sessionCreds.getSessionToken());
    }
    return bodyParametersMap;
  }
}
