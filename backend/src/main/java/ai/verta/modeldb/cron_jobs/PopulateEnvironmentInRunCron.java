package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAORdbImpl;
import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.ArtifactEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.PythonEnvironmentBlob;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import ai.verta.modeldb.versioning.VersionEnvironmentBlob;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import com.google.rpc.Code;
import io.grpc.StatusRuntimeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.OptimisticLockException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class PopulateEnvironmentInRunCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(PopulateEnvironmentInRunCron.class);
  private final ModelDBHibernateUtil modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
  private final ArtifactStoreDAO artifactStoreDAO;
  private final Integer recordUpdateLimit;

  public PopulateEnvironmentInRunCron(
      ArtifactStoreService artifactStoreService, Integer recordUpdateLimit) {
    this.artifactStoreDAO = new ArtifactStoreDAORdbImpl(artifactStoreService);
    this.recordUpdateLimit = recordUpdateLimit;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("PopulateEnvironmentInRunCron wakeup");

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      // Update experimentRun
      updateExperimentRuns(session);
    } catch (Exception ex) {
      if (ex instanceof StatusRuntimeException) {
        StatusRuntimeException exception = (StatusRuntimeException) ex;
        if (exception.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
          LOGGER.warn("PopulateEnvironmentInRunCron Exception: {}", ex.getMessage());
        } else {
          LOGGER.warn("PopulateEnvironmentInRunCron Exception: ", ex);
        }
      } else {
        LOGGER.warn("PopulateEnvironmentInRunCron Exception: ", ex);
      }
    }
    LOGGER.info("PopulateEnvironmentInRunCron finish tasks and reschedule");
  }

  private void updateExperimentRuns(Session session) {
    LOGGER.trace("ExperimentRun updating");

    String getExperimentRunQueryString =
        new StringBuilder("SELECT expr FROM ")
            .append(ExperimentRunEntity.class.getSimpleName())
            .append(" expr LEFT JOIN ArtifactEntity ar ")
            .append(" ON expr.id = ar.experimentRunEntity.id ")
            .append(" WHERE expr.")
            .append(ModelDBConstants.DELETED)
            .append(" = :deleted ")
            .append(" AND ar.field_type = :fieldType")
            .append(" AND ar.key IN (:keys)")
            .append(" AND expr.environment IS NULL")
            .toString();

    Query getExperimentRunQuery = session.createQuery(getExperimentRunQueryString);
    getExperimentRunQuery.setParameter("deleted", false);
    getExperimentRunQuery.setParameter("fieldType", ModelDBConstants.ARTIFACTS);
    getExperimentRunQuery.setParameter(
        "keys", Arrays.asList(ModelDBConstants.MODEL_API_JSON, ModelDBConstants.REQUIREMENTS_TXT));
    getExperimentRunQuery.setMaxResults(this.recordUpdateLimit);
    List<ExperimentRunEntity> experimentRunEntities = getExperimentRunQuery.list();

    List<String> experimentRunIds = new ArrayList<>();
    if (!experimentRunEntities.isEmpty()) {
      for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
        try {
          List<ArtifactEntity> experimentRunArtifacts =
              (experimentRunEntity.getArtifactEntityMap() != null
                      && experimentRunEntity
                          .getArtifactEntityMap()
                          .containsKey(ModelDBConstants.ARTIFACTS))
                  ? experimentRunEntity.getArtifactEntityMap().get(ModelDBConstants.ARTIFACTS)
                  : Collections.emptyList();
          if (!experimentRunArtifacts.isEmpty()) {
            PythonEnvironmentBlob.Builder pythonEnvironmentBuilder =
                PythonEnvironmentBlob.newBuilder();
            createPythonEnvironmentFromArtifacts(
                artifactStoreDAO, experimentRunArtifacts, pythonEnvironmentBuilder);

            if (pythonEnvironmentBuilder.getConstraintsCount() > 0
                || pythonEnvironmentBuilder.hasVersion()) {
              EnvironmentBlob.Builder environmentBlobBuilder = EnvironmentBlob.newBuilder();
              environmentBlobBuilder.setPython(pythonEnvironmentBuilder.build());
              experimentRunEntity.setEnvironment(
                  ModelDBUtils.getStringFromProtoObject(environmentBlobBuilder.build()));
            }

            try {
              Transaction transaction = session.beginTransaction();
              session.update(experimentRunEntity);
              transaction.commit();
              experimentRunIds.add(experimentRunEntity.getId());
            } catch (OptimisticLockException ex) {
              LOGGER.info(
                  "PopulateEnvironmentInRunCron : updateExperimentRuns : Exception: {}",
                  ex.getMessage());
            }
          }
        } catch (Exception ex) {
          LOGGER.debug("PopulateEnvironmentInRunCron : updateExperimentRuns : Exception:", ex);
        }
      }
    }

    LOGGER.debug(
        "ExperimentRun updated successfully : Updated experimentRuns count {}",
        experimentRunIds.size());
  }

  private void createPythonEnvironmentFromArtifacts(
      ArtifactStoreDAO artifactStoreDAO,
      List<ArtifactEntity> artifacts,
      PythonEnvironmentBlob.Builder pythonEnvironmentBuilder)
      throws ModelDBException {
    for (ArtifactEntity artifact : artifacts) {
      if (artifact.getKey().equals(ModelDBConstants.MODEL_API_JSON)) {
        addVersionInPythonEnvironmentBlob(artifactStoreDAO, pythonEnvironmentBuilder, artifact);
      } else if (artifact.getKey().equals(ModelDBConstants.REQUIREMENTS_TXT)) {
        addRequirementsInPythonEnvironmentBlob(
            artifactStoreDAO, pythonEnvironmentBuilder, artifact);
      }
    }
  }

  private void addRequirementsInPythonEnvironmentBlob(
      ArtifactStoreDAO artifactStoreDAO,
      PythonEnvironmentBlob.Builder pythonEnvironmentBuilder,
      ArtifactEntity artifact)
      throws ModelDBException {
    InputStream inputStream = artifactStoreDAO.downloadArtifact(artifact.getPath());

    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
      Pattern pattern = Pattern.compile(ModelDBConstants.VER_SPEC_PATTERN);
      String line;
      while ((line = br.readLine()) != null) {
        Matcher matcher = pattern.matcher(line);
        PythonRequirementEnvironmentBlob.Builder requirementEnvironmentBlob =
            PythonRequirementEnvironmentBlob.newBuilder();
        if (matcher.find()) {
          String[] requirementArr = pattern.split(line);
          requirementEnvironmentBlob.setLibrary(requirementArr[0]);
          requirementEnvironmentBlob.setConstraint(matcher.group());
          requirementEnvironmentBlob.setVersion(getVersionEnvironmentBlob(requirementArr[1]));
        } else {
          requirementEnvironmentBlob.setLibrary(line);
        }
        pythonEnvironmentBuilder.addRequirements(requirementEnvironmentBlob.build());
      }
    } catch (IOException e) {
      LOGGER.warn(e.getMessage());
      throw new ModelDBException(e.getMessage(), Code.INTERNAL);
    }
  }

  public VersionEnvironmentBlob getVersionEnvironmentBlob(String version) {
    VersionEnvironmentBlob.Builder versionBuilder = VersionEnvironmentBlob.newBuilder();
    String[] versionArr = version.split("\\.");
    int validVersionLength = 0;
    if (versionArr.length > 0) {
      String majorStr = versionArr[0];
      try {
        versionBuilder.setMajor(Integer.parseInt(majorStr));
        validVersionLength = validVersionLength + majorStr.length();
      } catch (NumberFormatException ex) {
        String[] requirementArr = majorStr.split(ModelDBConstants.VER_NUM_PATTERN);
        if (requirementArr.length > 0 && !requirementArr[0].isEmpty()) {
          try {
            versionBuilder.setMajor(Integer.parseInt(requirementArr[0]));
            validVersionLength = validVersionLength + requirementArr[0].length();
            versionBuilder.setSuffix(version.substring(validVersionLength));
            return versionBuilder.build();
          } catch (NumberFormatException e) {
            versionBuilder.setSuffix(version);
            return versionBuilder.build();
          }
        } else {
          versionBuilder.setSuffix(version);
          return versionBuilder.build();
        }
      }
      validVersionLength = validVersionLength + 1;
    }
    if (versionArr.length > 1) {
      String minorStr = versionArr[1];
      try {
        versionBuilder.setMinor(Integer.parseInt(minorStr));
        validVersionLength = validVersionLength + minorStr.length();
      } catch (NumberFormatException ex) {
        String[] requirementArr = minorStr.split(ModelDBConstants.VER_NUM_PATTERN);
        if (requirementArr.length > 0) {
          try {
            versionBuilder.setMinor(Integer.parseInt(requirementArr[0]));
            validVersionLength = validVersionLength + requirementArr[0].length();
            versionBuilder.setSuffix(version.substring(validVersionLength));
            return versionBuilder.build();
          } catch (NumberFormatException e) {
            versionBuilder.setSuffix(version.substring(validVersionLength));
            return versionBuilder.build();
          }
        } else {
          versionBuilder.setSuffix(version.substring(validVersionLength));
          return versionBuilder.build();
        }
      }
      validVersionLength = validVersionLength + 1;
    }
    if (versionArr.length > 2) {
      String patchStr = versionArr[2];
      try {
        versionBuilder.setPatch(Integer.parseInt(patchStr));
        validVersionLength = validVersionLength + patchStr.length();
      } catch (NumberFormatException ex) {
        String[] requirementArr = patchStr.split(ModelDBConstants.VER_NUM_PATTERN);
        if (requirementArr.length > 0) {
          try {
            versionBuilder.setPatch(Integer.parseInt(requirementArr[0]));
            validVersionLength = validVersionLength + requirementArr[0].length();
            versionBuilder.setSuffix(version.substring(validVersionLength));
            return versionBuilder.build();
          } catch (NumberFormatException e) {
            versionBuilder.setSuffix(version.substring(validVersionLength));
            return versionBuilder.build();
          }
        } else {
          versionBuilder.setSuffix(version.substring(validVersionLength));
          return versionBuilder.build();
        }
      }
    }
    if (versionArr.length > 3) {
      versionBuilder.setSuffix(versionArr[3]);
    }
    return versionBuilder.build();
  }

  private void addVersionInPythonEnvironmentBlob(
      ArtifactStoreDAO artifactStoreDAO,
      PythonEnvironmentBlob.Builder pythonEnvironmentBuilder,
      ArtifactEntity artifact)
      throws ModelDBException {
    InputStream inputStream = artifactStoreDAO.downloadArtifact(artifact.getPath());

    addVersionInPythonEnvironmentBlob(pythonEnvironmentBuilder, inputStream);
  }

  public void addVersionInPythonEnvironmentBlob(
      PythonEnvironmentBlob.Builder pythonEnvironmentBuilder, InputStream inputStream)
      throws ModelDBException {

    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        sb.append(line);
      }

      Gson gson = new Gson();
      JsonObject jsonObject = gson.fromJson(sb.toString(), JsonObject.class);
      if (jsonObject.has("model_packaging")) {
        JsonObject modelPackagingObject = jsonObject.get("model_packaging").getAsJsonObject();
        if (modelPackagingObject.has("python_version")) {
          String version = modelPackagingObject.get("python_version").getAsString();
          pythonEnvironmentBuilder.setVersion(getVersionEnvironmentBlob(version));
        }
      }
    } catch (JsonSyntaxException | MalformedJsonException e) {
      String errorMessage = "model_api.json file could not be parsed";
      LOGGER.info(errorMessage);
      throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage());
      throw new ModelDBException(e);
    }
  }
}
