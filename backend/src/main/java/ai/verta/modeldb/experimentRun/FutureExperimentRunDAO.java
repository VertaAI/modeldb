package ai.verta.modeldb.experimentRun;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.AddExperimentRunTags;
import ai.verta.modeldb.CreateExperimentRun;
import ai.verta.modeldb.DeleteExperimentRunAttributes;
import ai.verta.modeldb.DeleteExperimentRunTags;
import ai.verta.modeldb.DeleteHyperparameters;
import ai.verta.modeldb.DeleteMetrics;
import ai.verta.modeldb.DeleteObservations;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetHyperparameters;
import ai.verta.modeldb.GetMetrics;
import ai.verta.modeldb.GetObservations;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.LogAttributes;
import ai.verta.modeldb.LogHyperparameters;
import ai.verta.modeldb.LogMetrics;
import ai.verta.modeldb.LogObservations;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.Observation;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.KeyValueHandler;
import ai.verta.modeldb.experimentRun.subtypes.ObservationHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.TrialUtils;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.PythonEnvironmentBlob;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import ai.verta.uac.Action;
import ai.verta.uac.Empty;
import ai.verta.uac.Entities;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ResourceType;
import ai.verta.uac.Resources;
import ai.verta.uac.RoleBinding;
import ai.verta.uac.RoleScope;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.SetRoleBinding;
import ai.verta.uac.UserInfo;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FutureExperimentRunDAO {
  private static Logger LOGGER = LogManager.getLogger(FutureExperimentRunDAO.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;

  private final AttributeHandler attributeHandler;
  private final KeyValueHandler hyperparametersHandler;
  private final KeyValueHandler metricsHandler;
  private final ObservationHandler observationHandler;
  private final TagsHandler tagsHandler;
  private final Config config = Config.getInstance();

  public FutureExperimentRunDAO(Executor executor, FutureJdbi jdbi, UAC uac) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;

    attributeHandler = new AttributeHandler(executor, jdbi, "ExperimentRunEntity");
    hyperparametersHandler =
        new KeyValueHandler(executor, jdbi, "hyperparameters", "ExperimentRunEntity");
    metricsHandler = new KeyValueHandler(executor, jdbi, "metrics", "ExperimentRunEntity");
    observationHandler = new ObservationHandler(executor, jdbi);
    tagsHandler = new TagsHandler(executor, jdbi, "ExperimentRunEntity");
  }

  public InternalFuture<Void> deleteObservations(DeleteObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getObservationKeysList());

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> observationHandler.deleteObservations(runId, maybeKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<List<Observation>> getObservations(GetObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var key = request.getObservationKey();

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> observationHandler.getObservations(runId, key), executor);
  }

  public InternalFuture<Void> logObservations(LogObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var observations = request.getObservationsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused -> observationHandler.logObservations(runId, observations, now), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteMetrics(DeleteMetrics request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getMetricKeysList());

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> metricsHandler.deleteKeyValues(runId, maybeKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteHyperparameters(DeleteHyperparameters request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll()
            ? Optional.empty()
            : Optional.of(request.getHyperparameterKeysList());

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> hyperparametersHandler.deleteKeyValues(runId, maybeKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteAttributes(DeleteExperimentRunAttributes request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getAttributeKeysList());

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> attributeHandler.deleteKeyValues(runId, maybeKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<List<KeyValue>> getMetrics(GetMetrics request) {
    final var runId = request.getId();

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> metricsHandler.getKeyValues(runId), executor);
  }

  public InternalFuture<List<KeyValue>> getHyperparameters(GetHyperparameters request) {
    final var runId = request.getId();

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> hyperparametersHandler.getKeyValues(runId), executor);
  }

  public InternalFuture<List<KeyValue>> getAttributes(GetAttributes request) {
    final var runId = request.getId();

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> attributeHandler.getKeyValues(runId), executor);
  }

  public InternalFuture<Void> logMetrics(LogMetrics request) {
    final var runId = request.getId();
    final var metrics = request.getMetricsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> metricsHandler.logKeyValues(runId, metrics), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> logHyperparameters(LogHyperparameters request) {
    final var runId = request.getId();
    final var hyperparameters = request.getHyperparametersList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused -> hyperparametersHandler.logKeyValues(runId, hyperparameters), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> logAttributes(LogAttributes request) {
    final var runId = request.getId();
    final var attributes = request.getAttributesList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> attributeHandler.logKeyValues(runId, attributes), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> addTags(AddExperimentRunTags request) {
    final var runId = request.getId();
    final var tags = request.getTagsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> tagsHandler.addTags(runId, tags), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteTags(DeleteExperimentRunTags request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeTags =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getTagsList());

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> tagsHandler.deleteTags(runId, maybeTags), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<List<String>> getTags(GetTags request) {
    final var runId = request.getId();

    return checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> tagsHandler.getTags(runId), executor);
  }

  private InternalFuture<Void> updateModifiedTimestamp(String runId, Long now) {
    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "update experiment_run set date_updated=greatest(date_updated, :now) where id=:run_id")
                .bind("run_id", runId)
                .bind("now", now)
                .execute());
  }

  private InternalFuture<Void> checkEntityPermission(
      String projId,
      ModelDBActionEnum.ModelDBServiceActions action,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return FutureGrpc.ClientRequest(
            uac.getAuthzService()
                .isSelfAllowed(
                    IsSelfAllowed.newBuilder()
                        .addActions(
                            Action.newBuilder()
                                .setModeldbServiceAction(action)
                                .setService(ServiceEnum.Service.MODELDB_SERVICE))
                        .addResources(
                            Resources.newBuilder()
                                .setService(ServiceEnum.Service.MODELDB_SERVICE)
                                .setResourceType(
                                    ResourceType.newBuilder()
                                        .setModeldbServiceResourceType(modelDBServiceResourceTypes))
                                .addResourceIds(projId))
                        .build()),
            executor)
        .thenAccept(
            response -> {
              if (!response.getAllowed()) {
                throw new PermissionDeniedException("Permission denied");
              }
            },
            executor);
  }

  private InternalFuture<Void> checkPermission(
      String runId, ModelDBActionEnum.ModelDBServiceActions action) {
    if (runId.isEmpty()) {
      return InternalFuture.failedStage(
          new InvalidArgumentException("Experiment run ID is missing"));
    }

    var futureMaybeProjectId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("select project_id from experiment_run where id=:id and deleted=0")
                    .bind("id", runId)
                    .mapTo(String.class)
                    .findOne());

    return futureMaybeProjectId.thenCompose(
        maybeProjectId -> {
          if (maybeProjectId.isEmpty()) {
            throw new NotFoundException("Experiment run not found");
          }

          switch (action) {
            default:
              return checkEntityPermission(
                  maybeProjectId.get(), action, ModelDBServiceResourceTypes.PROJECT);
          }
        },
        executor);
  }

  private InternalFuture<UserInfo> getCurrentLoginUserInfo() {
    return FutureGrpc.ClientRequest(
        uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor);
  }

  public InternalFuture<ExperimentRun> createExperimentRun(final CreateExperimentRun request) {
    // Validate arguments
    var futureTask =
        InternalFuture.runAsync(
            () -> {
              String errorMessage = null;
              if (request.getProjectId().isEmpty() && request.getExperimentId().isEmpty()) {
                errorMessage =
                    "Project ID and Experiment ID not found in CreateExperimentRun request";
              } else if (request.getProjectId().isEmpty()) {
                errorMessage = "Project ID not found in CreateExperimentRun request";
              } else if (request.getExperimentId().isEmpty()) {
                errorMessage = "Experiment ID not found in CreateExperimentRun request";
              }

              if (errorMessage != null) {
                throw new InvalidArgumentException(errorMessage);
              }
            },
            executor);

    return futureTask
        .thenCompose(
            unused ->
                checkEntityPermission(
                    request.getProjectId(),
                    ModelDBActionEnum.ModelDBServiceActions.UPDATE,
                    ModelDBServiceResourceTypes.PROJECT),
            executor)
        .thenCompose(
            unused ->
                TrialUtils.futureValidateExperimentRunPerWorkspaceForTrial(config.trial, executor),
            executor)
        .thenCompose(unused -> getCurrentLoginUserInfo(), executor)
        .thenCompose(
            currentLoginUserInfo -> getExperimentRunFromRequest(request, currentLoginUserInfo),
            executor)
        .thenCompose(
            experimentRun ->
                TrialUtils.futureValidateMaxArtifactsForTrial(
                    config.trial, experimentRun, 0, executor),
            executor)
        .thenCompose(experimentRun -> checkIfEntityAlreadyExists(experimentRun, true), executor)
        .thenCompose(
            experimentRun -> {
              if (experimentRun.getDatasetsCount() > 0
                  && config.populateConnectionsBasedOnPrivileges) {
                experimentRun = checkDatasetVersionBasedOnPrivileges(experimentRun, true);
              }
              return InternalFuture.completedInternalFuture(experimentRun);
            },
            executor)
        .thenCompose(
            experimentRun -> {
              // TODO: Fix populating logic of setVersioned_inputs,
              // setHyperparameter_element_mappings here
              return InternalFuture.completedInternalFuture(experimentRun);
            },
            executor)
        .thenCompose(this::insertExperimentRun, executor)
        .thenCompose(experimentRun -> createRoleBindingsForExperimentRun(experimentRun), executor);
  }

  /**
   * @param errorOut : Throw error while creation (true) otherwise we will keep it silent (false)
   */
  private ExperimentRun checkDatasetVersionBasedOnPrivileges(
      ExperimentRun experimentRun, boolean errorOut) {
    ExperimentRun.Builder experimentRunBuilder = experimentRun.toBuilder();
    List<Artifact> accessibleDatasetVersions =
        getPrivilegedDatasets(experimentRun.getDatasetsList(), errorOut);
    experimentRunBuilder.clearDatasets().addAllDatasets(accessibleDatasetVersions);
    return experimentRunBuilder.build();
  }

  /**
   * @param newDatasets : new datasets for privilege check
   * @param errorOut : Throw error while creation (true) otherwise we will keep it silent (false)
   * @return {@link List} : accessible datasets
   * @throws ModelDBException: modelDBException
   */
  private List<Artifact> getPrivilegedDatasets(List<Artifact> newDatasets, boolean errorOut) {
    List<Artifact> accessibleDatasets = new ArrayList<>();
    List<String> accessibleDatasetVersionIds = new ArrayList<>();
    List<String> newDatasetLinkedIds = new ArrayList<>();
    for (Artifact dataset : newDatasets) {
      String datasetVersionId = dataset.getLinkedArtifactId();
      if (!datasetVersionId.isEmpty()) {
        newDatasetLinkedIds.add(datasetVersionId);
      }
    }

    jdbi.useHandle(
        handle -> {
          Map<String, Long> datasetVersionDatasetMap = new HashMap<>();
          var query =
              handle
                  .createQuery(
                      " SELECT commit_hash, repository_id FROM repository_commit WHERE commit_hash IN (<datasetIds>) ")
                  .bind("datasetIds", newDatasetLinkedIds);
          query
              .map(
                  (rs, ctx) ->
                      datasetVersionDatasetMap.put(
                          rs.getString("commit_hash"), rs.getLong("repository_id")))
              .list();

          for (Artifact dataset : newDatasets) {
            String datasetVersionId = dataset.getLinkedArtifactId();
            if (!datasetVersionId.isEmpty()
                && !accessibleDatasetVersionIds.contains(datasetVersionId)) {
              try {
                if (datasetVersionDatasetMap.containsKey(datasetVersionId)
                    && datasetVersionDatasetMap.get(datasetVersionId) != null) {
                  Long datasetId = datasetVersionDatasetMap.get(datasetVersionId);
                  checkEntityPermission(
                      String.valueOf(datasetId),
                      ModelDBActionEnum.ModelDBServiceActions.READ,
                      ModelDBServiceResourceTypes.DATASET);
                  accessibleDatasets.add(dataset);
                  accessibleDatasetVersionIds.add(datasetVersionId);
                } else {
                  throw new InvalidArgumentException(
                      "Dataset not found for dataset version: " + datasetVersionId);
                }
              } catch (Exception ex) {
                LOGGER.debug(ex.getMessage());
                if (errorOut) {
                  throw ex;
                }
              }
            } else {
              accessibleDatasets.add(dataset);
            }
          }
        });

    return accessibleDatasets;
  }

  /**
   * Convert CreateExperimentRun request to Experiment object. This method generate the
   * ExperimentRun Id using UUID and put it in ExperimentRun object.
   *
   * @param request : CreateExperimentRun request
   * @param userInfo : current login UserInfo
   * @return ExperimentRun : experimentRun
   */
  private InternalFuture<ExperimentRun> getExperimentRunFromRequest(
      CreateExperimentRun request, UserInfo userInfo) {

    /*
     * Create ExperimentRun entity from given CreateExperimentRun request. generate UUID and put as
     * id in ExperimentRun for uniqueness.
     */
    if (request.getName().isEmpty()) {
      request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
    }

    ExperimentRun.Builder experimentRunBuilder =
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
            .addAllTags(ModelDBUtils.checkEntityTagsLength(request.getTagsList()))
            .addAllAttributes(request.getAttributesList())
            .addAllHyperparameters(request.getHyperparametersList())
            .addAllArtifacts(request.getArtifactsList())
            .addAllDatasets(request.getDatasetsList())
            .addAllMetrics(request.getMetricsList())
            .addAllObservations(request.getObservationsList())
            .addAllFeatures(request.getFeaturesList());

    if (request.getDateCreated() != 0L) {
      experimentRunBuilder
          .setDateCreated(request.getDateCreated())
          .setDateUpdated(request.getDateCreated());
    } else {
      experimentRunBuilder
          .setDateCreated(Calendar.getInstance().getTimeInMillis())
          .setDateUpdated(Calendar.getInstance().getTimeInMillis());
    }

    if (request.getCodeVersionSnapshot() != null) {
      experimentRunBuilder.setCodeVersionSnapshot(request.getCodeVersionSnapshot());
    }
    if (request.getVersionedInputs() != null && request.hasVersionedInputs()) {
      experimentRunBuilder.setVersionedInputs(request.getVersionedInputs());
    }
    if (userInfo != null) {

      experimentRunBuilder.setOwner(userInfo.getVertaInfo().getUserId());
    }

    return InternalFuture.completedInternalFuture(experimentRunBuilder.build());
  }

  private InternalFuture<ExperimentRun> insertExperimentRun(ExperimentRun newExperimentRun) {
    final var now = Calendar.getInstance().getTimeInMillis();
    return jdbi.withHandle(
            handle -> {
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
              // TODO: code version snapshot
              /*runValueMap.put("code_version_snapshot_id", newExperimentRun.getCodeVersionSnapshot());*/
              runValueMap.put("job_id", newExperimentRun.getJobId());
              runValueMap.put("parent_id", newExperimentRun.getParentId());
              runValueMap.put("owner", newExperimentRun.getOwner());

              EnvironmentBlob environmentBlob =
                  sortPythonEnvironmentBlob(newExperimentRun.getEnvironment());
              runValueMap.put(
                  "environment", ModelDBUtils.getStringFromProtoObject(environmentBlob));
              runValueMap.put("deleted", false);

              String[] fieldsArr = runValueMap.keySet().toArray(new String[0]);
              String commaFields = String.join(",", fieldsArr);

              StringBuilder queryStrBuilder =
                  new StringBuilder("insert into experiment_run ( ")
                      .append(commaFields)
                      .append(") values (");

              for (int i = 0; i < fieldsArr.length; i++) {
                queryStrBuilder.append(":").append(fieldsArr[i]);
                if (i < fieldsArr.length - 1) {
                  queryStrBuilder.append(",");
                }
              }
              queryStrBuilder.append(" ) ");

              LOGGER.trace("insert experiment run query string: " + queryStrBuilder.toString());
              var query = handle.createUpdate(queryStrBuilder.toString());
              for (Map.Entry<String, Object> objectEntry : runValueMap.entrySet()) {
                query.bind(objectEntry.getKey(), objectEntry.getValue());
              }
              query.execute();

              return newExperimentRun;
            })
        .thenCompose(
            handle -> tagsHandler.addTags(newExperimentRun.getId(), newExperimentRun.getTagsList()),
            executor)
        .thenCompose(
            handle ->
                attributeHandler.logKeyValues(
                    newExperimentRun.getId(), newExperimentRun.getAttributesList()),
            executor)
        .thenCompose(
            handle ->
                hyperparametersHandler.logKeyValues(
                    newExperimentRun.getId(), newExperimentRun.getHyperparametersList()),
            executor)
        .thenCompose(
            handle ->
                metricsHandler.logKeyValues(
                    newExperimentRun.getId(), newExperimentRun.getMetricsList()),
            executor)
        // TODO .thenCompose(handle -> artifactHandler.logArtifacts(newExperimentRun.getId(),
        // newExperimentRun.getArtifactsList()), executor)
        // TODO .thenCompose(handle -> datasetHandler.logDatasets(newExperimentRun.getId(),
        // newExperimentRun.getDatasetsList()), executor)
        .thenCompose(
            handle ->
                observationHandler.logObservations(
                    newExperimentRun.getId(), newExperimentRun.getObservationsList(), now),
            executor)
        // TODO .thenCompose(handle -> featureHandler.logFeatures(newExperimentRun.getId(),
        // newExperimentRun.getFeaturesList()), executor)
        // TODO .thenCompose(handle -> addCodeVersionSnapShot(), executor)
        // TODO .thenCompose(handle -> versioned_inputs, executor)

        .thenCompose(unused -> InternalFuture.completedInternalFuture(newExperimentRun), executor);
  }

  private EnvironmentBlob sortPythonEnvironmentBlob(EnvironmentBlob environmentBlob) {
    EnvironmentBlob.Builder builder = environmentBlob.toBuilder();
    if (builder.hasPython()) {
      PythonEnvironmentBlob.Builder pythonEnvironmentBlobBuilder = builder.getPython().toBuilder();

      // Compare requirementEnvironmentBlobs
      List<PythonRequirementEnvironmentBlob> requirementEnvironmentBlobs =
          new ArrayList<>(pythonEnvironmentBlobBuilder.getRequirementsList());
      requirementEnvironmentBlobs.sort(
          Comparator.comparing(PythonRequirementEnvironmentBlob::getLibrary));
      pythonEnvironmentBlobBuilder
          .clearRequirements()
          .addAllRequirements(requirementEnvironmentBlobs);

      // Compare
      List<PythonRequirementEnvironmentBlob> constraintsBlobs =
          new ArrayList<>(pythonEnvironmentBlobBuilder.getConstraintsList());
      constraintsBlobs.sort(Comparator.comparing(PythonRequirementEnvironmentBlob::getLibrary));
      pythonEnvironmentBlobBuilder.clearConstraints().addAllConstraints(constraintsBlobs);

      builder.setPython(pythonEnvironmentBlobBuilder.build());
    }
    return builder.build();
  }

  private InternalFuture<ExperimentRun> checkIfEntityAlreadyExists(
      ExperimentRun experimentRun, Boolean isInsert) {
    return jdbi.useHandle(
            handle -> {
              try {
                String queryStr;
                if (isInsert) {
                  queryStr =
                      "SELECT id FROM experiment_run ere WHERE "
                          + " ere."
                          + ModelDBConstants.NAME
                          + " = :experimentRunName "
                          + " AND ere."
                          + ModelDBConstants.PROJECT_ID
                          + " = :projectId "
                          + " AND ere."
                          + ModelDBConstants.EXPERIMENT_ID
                          + " = :experimentId "
                          + " AND ere."
                          + ModelDBConstants.DELETED
                          + " = false ";
                } else {
                  queryStr =
                      "SELECT id FROM experiment_run ere WHERE "
                          + " ere."
                          + ModelDBConstants.ID
                          + " = :experimentRunId "
                          + " AND ere."
                          + ModelDBConstants.DELETED
                          + " = false ";
                }

                var query = handle.createQuery(queryStr);

                if (isInsert) {
                  query.bind("experimentRunName", experimentRun.getName());
                  query.bind("projectId", experimentRun.getProjectId());
                  query.bind("experimentId", experimentRun.getExperimentId());
                } else {
                  query.bind(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRun.getId());
                }

                long count = query.mapTo(String.class).stream().count();
                boolean existStatus = false;
                if (count > 0) {
                  existStatus = true;
                }

                // Throw error if it is an insert request and ExperimentRun with same name already
                // exists
                if (existStatus && isInsert) {
                  throw new AlreadyExistsException("ExperimentRun already exists in database");
                } else if (!existStatus && !isInsert) {
                  // Throw error if it is an update request and ExperimentRun with given name does
                  // not exist
                  throw new NotFoundException("ExperimentRun does not exist in database");
                }
              } catch (Exception ex) {
                if (ModelDBUtils.needToRetry(ex)) {
                  checkIfEntityAlreadyExists(experimentRun, isInsert);
                } else {
                  throw ex;
                }
              }
            })
        .thenCompose(unused -> InternalFuture.completedInternalFuture(experimentRun), executor);
  }

  private String buildRoleBindingName(
      String roleName, String resourceId, String vertaId, String resourceTypeName) {
    return roleName + "_" + resourceTypeName + "_" + resourceId + "_" + "User_" + vertaId;
  }

  private InternalFuture<ExperimentRun> createRoleBindingsForExperimentRun(
      final ExperimentRun experimentRun) {
    ModelDBServiceResourceTypes modelDBServiceResourceType =
        ModelDBServiceResourceTypes.EXPERIMENT_RUN;
    String roleName = ModelDBConstants.ROLE_EXPERIMENT_RUN_OWNER;
    return FutureGrpc.ClientRequest(
            uac.getRoleService()
                .setRoleBinding(
                    SetRoleBinding.newBuilder()
                        .setRoleBinding(
                            RoleBinding.newBuilder()
                                .setName(
                                    buildRoleBindingName(
                                        roleName,
                                        experimentRun.getId(),
                                        experimentRun.getOwner(),
                                        modelDBServiceResourceType.name()))
                                .setScope(RoleScope.newBuilder().build())
                                .setRoleName(roleName)
                                .addEntities(
                                    Entities.newBuilder()
                                        .addUserIds(experimentRun.getOwner())
                                        .build())
                                .addResources(
                                    Resources.newBuilder()
                                        .setService(ServiceEnum.Service.MODELDB_SERVICE)
                                        .setResourceType(
                                            ResourceType.newBuilder()
                                                .setModeldbServiceResourceType(
                                                    modelDBServiceResourceType))
                                        .addResourceIds(experimentRun.getId())
                                        .build())
                                .build())
                        .build()),
            executor)
        .thenAccept(
            response -> {
              LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, response);
            },
            executor)
        .thenCompose(unused -> InternalFuture.completedInternalFuture(experimentRun), executor);
  }
}
