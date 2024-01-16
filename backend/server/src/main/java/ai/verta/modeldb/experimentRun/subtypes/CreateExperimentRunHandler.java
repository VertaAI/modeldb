package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.CreateExperimentRun;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.futures.*;
import ai.verta.modeldb.common.handlers.TagsHandlerBase;
import ai.verta.modeldb.common.subtypes.KeyValueHandler;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.utils.InternalFuture;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.*;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

public class CreateExperimentRunHandler extends HandlerUtil {

  private static final Logger LOGGER = LogManager.getLogger(CreateExperimentRunHandler.class);

  private final FutureExecutor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;
  private final Config config;

  private final AttributeHandler attributeHandler;
  private final KeyValueHandler hyperparametersHandler;
  private final KeyValueHandler metricsHandler;
  private final ObservationHandler observationHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final FeatureHandler featureHandler;
  private final CodeVersionHandler codeVersionHandler;
  private final DatasetHandler datasetHandler;
  private final VersionInputHandler versionInputHandler;

  public CreateExperimentRunHandler(
      FutureExecutor executor,
      FutureJdbi jdbi,
      Config config,
      UAC uac,
      AttributeHandler attributeHandler,
      KeyValueHandler hyperparametersHandler,
      KeyValueHandler metricsHandler,
      ObservationHandler observationHandler,
      TagsHandler tagsHandler,
      ArtifactHandler artifactHandler,
      FeatureHandler featureHandler,
      DatasetHandler datasetHandler,
      VersionInputHandler versionInputHandler) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.config = config;
    this.uac = uac;

    this.attributeHandler = attributeHandler;
    this.hyperparametersHandler = hyperparametersHandler;
    this.metricsHandler = metricsHandler;
    this.observationHandler = observationHandler;
    this.tagsHandler = tagsHandler;
    this.artifactHandler = artifactHandler;
    this.featureHandler = featureHandler;
    this.codeVersionHandler = new CodeVersionHandler(executor, jdbi, "experiment_run");
    this.datasetHandler = datasetHandler;
    this.versionInputHandler = versionInputHandler;
  }

  public InternalFuture<ExperimentRun> convertCreateRequest(final CreateExperimentRun request) {
    return InternalFuture.clientRequest(
            uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor)
        .thenCompose(
            currentLoginUserInfo -> {
              final var experimentRun = getExperimentRunFromRequest(request, currentLoginUserInfo);

              return InternalFuture.completedInternalFuture(experimentRun);
            },
            executor);
    /*.thenCompose(
    experimentRun -> {
      // TODO: Fix below logic for checking privileges of linked dataset versions
      */
    /*if (experimentRun.getDatasetsCount() > 0 && config.populateConnectionsBasedOnPrivileges) {
      experimentRun = checkDatasetVersionBasedOnPrivileges(experimentRun, true);
    }*/
    /*
          return InternalFuture.completedInternalFuture(experimentRun);
        },
        executor)
    .thenCompose(
        experimentRun -> {
          // TODO: Fix populating logic of setVersioned_inputs,
          // setHyperparameter_element_mappings here
          return InternalFuture.completedInternalFuture(experimentRun);
        },
        executor)*/
  }

  /**
   * Convert CreateExperimentRun request to Experiment object. This method generate the
   * ExperimentRun Id using UUID and put it in ExperimentRun object.
   *
   * @param request : CreateExperimentRun request
   * @param userInfo : current login UserInfo
   * @return ExperimentRun : experimentRun
   */
  private ExperimentRun getExperimentRunFromRequest(
      CreateExperimentRun request, UserInfo userInfo) {

    /*
     * Create ExperimentRun entity from given CreateExperimentRun request. generate UUID and put as
     * id in ExperimentRun for uniqueness.
     */
    if (request.getName().isEmpty()) {
      request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
    }

    var experimentRunBuilder =
        ExperimentRun.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setProjectId(request.getProjectId())
            .setExperimentId(request.getExperimentId())
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setDescription(request.getDescription())
            .setStartTime(request.getStartTime())
            .setEndTime(request.getEndTime())
            .setCodeVersion(request.getCodeVersion())
            .setParentId(request.getParentId())
            .addAllTags(TagsHandlerBase.checkEntityTagsLength(request.getTagsList()))
            .addAllAttributes(request.getAttributesList())
            .addAllHyperparameters(request.getHyperparametersList())
            .addAllArtifacts(request.getArtifactsList())
            .addAllDatasets(request.getDatasetsList())
            .addAllMetrics(request.getMetricsList())
            .addAllObservations(request.getObservationsList())
            .addAllFeatures(request.getFeaturesList())
            .setVersionNumber(1L);

    var now = Calendar.getInstance().getTimeInMillis();
    if (request.getDateCreated() != 0L) {
      experimentRunBuilder.setDateCreated(request.getDateCreated());
    } else {
      experimentRunBuilder.setDateCreated(now);
    }

    if (request.getDateUpdated() != 0L) {
      experimentRunBuilder.setDateUpdated(request.getDateCreated());
    } else {
      experimentRunBuilder.setDateUpdated(now);
    }

    experimentRunBuilder.setCodeVersionSnapshot(request.getCodeVersionSnapshot());
    if (request.getVersionedInputs() != null && request.hasVersionedInputs()) {
      experimentRunBuilder.setVersionedInputs(request.getVersionedInputs());
    }
    if (userInfo != null) {

      experimentRunBuilder.setOwner(userInfo.getVertaInfo().getUserId());
    }

    return experimentRunBuilder.build();
  }

  public InternalFuture<ExperimentRun> insertExperimentRun(ExperimentRun newExperimentRun) {
    final var now = Calendar.getInstance().getTimeInMillis();
    Map<String, Object> runValueMap = new LinkedHashMap<>();
    runValueMap.put("id", newExperimentRun.getId());
    runValueMap.put("project_id", newExperimentRun.getProjectId());
    runValueMap.put("experiment_id", newExperimentRun.getExperimentId());
    runValueMap.put("name", newExperimentRun.getName());
    runValueMap.put("description", newExperimentRun.getDescription());
    runValueMap.put("date_created", newExperimentRun.getDateCreated());
    runValueMap.put("date_updated", newExperimentRun.getDateUpdated());
    runValueMap.put("start_time", newExperimentRun.getStartTime());
    runValueMap.put("end_time", newExperimentRun.getEndTime());
    runValueMap.put("code_version", newExperimentRun.getCodeVersion());
    runValueMap.put("job_id", newExperimentRun.getJobId());
    runValueMap.put("parent_id", newExperimentRun.getParentId());
    runValueMap.put("owner", newExperimentRun.getOwner());
    runValueMap.put("version_number", newExperimentRun.getVersionNumber());

    runValueMap.put("environment", null);
    runValueMap.put("deleted", false);
    runValueMap.put("created", false);

    return InternalFuture.completedInternalFuture(true)
        .thenCompose(
            unused -> {
              if (newExperimentRun.getVersionedInputs().getRepositoryId() != 0) {
                return versionInputHandler.validateVersioningEntity(
                    newExperimentRun.getVersionedInputs());
              }
              return InternalFuture.completedInternalFuture(new HashMap<>());
            },
            executor)
        .thenCompose(
            locationBlobWithHashMap -> { // Inserting fields arguments based on the keys and value
              // of map
              // take a brief pause before resubmitting its query/transaction
              // Time in ms
              return InternalFuture.fromFuture(
                  jdbi.inTransaction(
                      handle -> {
                        final var builder = newExperimentRun.toBuilder();
                        Boolean exists = checkInsertedEntityAlreadyExists(handle, newExperimentRun);
                        if (exists) {
                          throw new AlreadyExistsException(
                              "ExperimentRun '"
                                  + builder.getName()
                                  + "' already exists in database");
                        }

                        String queryString = buildInsertQuery(runValueMap, "experiment_run");

                        LOGGER.trace("insert experiment run query string: " + queryString);
                        var query = handle.createUpdate(queryString);

                        // Inserting fields arguments based on the keys and value of map
                        for (Map.Entry<String, Object> objectEntry : runValueMap.entrySet()) {
                          query.bind(objectEntry.getKey(), objectEntry.getValue());
                        }

                        try {
                          int count = query.execute();
                          LOGGER.trace("ExperimentRun Inserted : " + (count > 0));
                        } catch (UnableToExecuteStatementException exception) {
                          // take a brief pause before resubmitting its query/transaction
                          Thread.sleep(config.getJdbi_retry_time()); // Time in ms
                          LOGGER.trace("Retry to insert ExperimentRun");
                          int count = query.execute();
                          LOGGER.trace("ExperimentRun Inserted after retry : " + (count > 0));
                        }

                        if (!builder.getTagsList().isEmpty()) {
                          tagsHandler.addTags(handle, builder.getId(), builder.getTagsList());
                        }
                        if (!builder.getAttributesList().isEmpty()) {
                          attributeHandler.logKeyValues(
                              handle, builder.getId(), builder.getAttributesList());
                        }
                        if (!builder.getHyperparametersList().isEmpty()) {
                          hyperparametersHandler.logKeyValues(
                              handle, builder.getId(), builder.getHyperparametersList());
                        }
                        if (!builder.getMetricsList().isEmpty()) {
                          metricsHandler.logKeyValues(
                              handle, builder.getId(), builder.getMetricsList());
                        }
                        if (!builder.getObservationsList().isEmpty()) {
                          observationHandler.logObservations(
                              handle, builder.getId(), builder.getObservationsList(), now);
                        }
                        if (!builder.getArtifactsList().isEmpty()) {
                          var updatedArtifacts =
                              artifactHandler.logArtifacts(
                                  handle, builder.getId(), builder.getArtifactsList(), false);
                          builder.clearArtifacts().addAllArtifacts(updatedArtifacts);
                        }
                        if (!builder.getFeaturesList().isEmpty()) {
                          featureHandler.logFeatures(
                              handle, builder.getId(), builder.getFeaturesList());
                        }
                        if (builder.getCodeVersionSnapshot().hasCodeArchive()
                            || builder.getCodeVersionSnapshot().hasGitSnapshot()) {
                          codeVersionHandler.logCodeVersion(
                              handle, builder.getId(), false, builder.getCodeVersionSnapshot());
                        }
                        if (!builder.getDatasetsList().isEmpty()) {
                          var updatedDatasets =
                              datasetHandler.logArtifacts(
                                  handle, builder.getId(), builder.getDatasetsList(), false);
                          builder.clearDatasets().addAllDatasets(updatedDatasets);
                        }

                        if (builder.getVersionedInputs().getRepositoryId() != 0) {
                          versionInputHandler.validateAndInsertVersionedInputs(
                              handle,
                              builder.getId(),
                              builder.getVersionedInputs(),
                              locationBlobWithHashMap);
                        }
                        return builder.build();
                      }));
            },
            executor)
        .thenCompose(
            createdExperimentRun ->
                InternalFuture.fromFuture(
                        jdbi.run(
                            handle ->
                                handle
                                    .createUpdate(
                                        "UPDATE experiment_run SET created=:created WHERE id=:id")
                                    .bind("created", true)
                                    .bind("id", newExperimentRun.getId())
                                    .execute()))
                    .thenApply(unused -> createdExperimentRun, executor),
            executor);
  }

  private Boolean checkInsertedEntityAlreadyExists(Handle handle, ExperimentRun experimentRun) {
    String queryStr =
        "SELECT count(id) FROM experiment_run WHERE "
            + " name = :experimentRunName "
            + " AND project_id = :projectId "
            + " AND experiment_id = :experimentId "
            + " AND deleted = :deleted ";

    try (var query = handle.createQuery(queryStr)) {
      query.bind("experimentRunName", experimentRun.getName());
      query.bind("projectId", experimentRun.getProjectId());
      query.bind("experimentId", experimentRun.getExperimentId());
      query.bind("deleted", false);

      long count = query.mapTo(Long.class).one();
      return count > 0;
    }
  }
}
