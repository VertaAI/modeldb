package ai.verta.modeldb.utils;

import static java.time.format.DateTimeFormatter.ofPattern;

import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.common.artifactStore.storageservice.s3.S3SignatureUtil;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.config.TrialConfig;
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
}
