package ai.verta.modeldb.experimentRun;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.AddExperimentRunTags;
import ai.verta.modeldb.CreateExperimentRun;
import ai.verta.modeldb.DeleteArtifact;
import ai.verta.modeldb.DeleteExperimentRunAttributes;
import ai.verta.modeldb.DeleteExperimentRunTags;
import ai.verta.modeldb.DeleteExperimentRuns;
import ai.verta.modeldb.DeleteHyperparameters;
import ai.verta.modeldb.DeleteMetrics;
import ai.verta.modeldb.DeleteObservations;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetHyperparameters;
import ai.verta.modeldb.GetMetrics;
import ai.verta.modeldb.GetObservations;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.LogArtifacts;
import ai.verta.modeldb.LogAttributes;
import ai.verta.modeldb.LogHyperparameters;
import ai.verta.modeldb.LogMetrics;
import ai.verta.modeldb.LogObservations;
import ai.verta.modeldb.Observation;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CreateExperimentRunHandler;
import ai.verta.modeldb.experimentRun.subtypes.KeyValueHandler;
import ai.verta.modeldb.experimentRun.subtypes.ObservationHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.uac.Action;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ResourceType;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum;
import java.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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
  private final ArtifactHandler artifactHandler;
  private final CreateExperimentRunHandler createExperimentRunHandler;

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
    artifactHandler = new ArtifactHandler(executor, jdbi, "artifacts", "ExperimentRunEntity");
    createExperimentRunHandler = new CreateExperimentRunHandler(executor, jdbi, uac);
  }

  public InternalFuture<Void> deleteObservations(DeleteObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getObservationKeysList());

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> observationHandler.deleteObservations(runId, maybeKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<List<Observation>> getObservations(GetObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var key = request.getObservationKey();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> observationHandler.getObservations(runId, key), executor);
  }

  public InternalFuture<Void> logObservations(LogObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var observations = request.getObservationsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused -> observationHandler.logObservations(runId, observations, now), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteMetrics(DeleteMetrics request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getMetricKeysList());

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
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

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> hyperparametersHandler.deleteKeyValues(runId, maybeKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteAttributes(DeleteExperimentRunAttributes request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getAttributeKeysList());

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> attributeHandler.deleteKeyValues(runId, maybeKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<List<KeyValue>> getMetrics(GetMetrics request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> metricsHandler.getKeyValues(runId), executor);
  }

  public InternalFuture<List<KeyValue>> getHyperparameters(GetHyperparameters request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> hyperparametersHandler.getKeyValues(runId), executor);
  }

  public InternalFuture<List<KeyValue>> getAttributes(GetAttributes request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> attributeHandler.getKeyValues(runId), executor);
  }

  public InternalFuture<Void> logMetrics(LogMetrics request) {
    final var runId = request.getId();
    final var metrics = request.getMetricsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> metricsHandler.logKeyValues(runId, metrics), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> logHyperparameters(LogHyperparameters request) {
    final var runId = request.getId();
    final var hyperparameters = request.getHyperparametersList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused -> hyperparametersHandler.logKeyValues(runId, hyperparameters), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> logAttributes(LogAttributes request) {
    final var runId = request.getId();
    final var attributes = request.getAttributesList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> attributeHandler.logKeyValues(runId, attributes), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> addTags(AddExperimentRunTags request) {
    final var runId = request.getId();
    final var tags = request.getTagsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> tagsHandler.addTags(runId, tags), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteTags(DeleteExperimentRunTags request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeTags =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getTagsList());

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> tagsHandler.deleteTags(runId, maybeTags), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<List<String>> getTags(GetTags request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
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
      List<String> projId,
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
                                .addAllResourceIds(projId))
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
      List<String> runIds, ModelDBActionEnum.ModelDBServiceActions action) {
    if (runIds.isEmpty()) {
      return InternalFuture.failedStage(
          new InvalidArgumentException("Experiment run IDs is missing"));
    }

    var futureMaybeProjectIds =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT project_id FROM experiment_run WHERE id IN (<ids>) AND deleted=0")
                    .bindList("ids", runIds)
                    .mapTo(String.class)
                    .list());

    return futureMaybeProjectIds.thenCompose(
        maybeProjectIds -> {
          if (maybeProjectIds.isEmpty()) {
            throw new NotFoundException("Project ids not found for given experiment runs");
          }

          switch (action) {
            case DELETE:
              // TODO: check if we should using DELETE for the ER itself
              return checkEntityPermission(
                  maybeProjectIds,
                  ModelDBActionEnum.ModelDBServiceActions.UPDATE,
                  ModelDBServiceResourceTypes.PROJECT);
            default:
              return checkEntityPermission(
                  maybeProjectIds, action, ModelDBServiceResourceTypes.PROJECT);
          }
        },
        executor);
  }

  public InternalFuture<Void> deleteExperimentRuns(DeleteExperimentRuns request) {
    final var runIds = request.getIdsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    var futureDeleteTask =
        InternalFuture.runAsync(
            () -> {
              if (runIds.isEmpty()) {
                throw new InvalidArgumentException("ExperimentRun IDs not found in request");
              }
            },
            executor);

    return futureDeleteTask
        .thenCompose(
            unused ->
                checkPermission(
                    request.getIdsList(), ModelDBActionEnum.ModelDBServiceActions.DELETE),
            executor)
        .thenCompose(unused -> deleteExperimentRuns(runIds), executor);
  }

  /**
   * @param newDatasets : new datasets for privilege check
   * @param errorOut : Throw error while creation (true) otherwise we will keep it silent (false)
   * @return {@link List} : accessible datasets
   */
  private InternalFuture<List<Artifact>> filterAndGetPrivilegedDatasetsOnly(
      List<Artifact> newDatasets, boolean errorOut) {
    List<String> newDatasetLinkedIds = new ArrayList<>();
    for (Artifact dataset : newDatasets) {
      String datasetVersionId = dataset.getLinkedArtifactId();
      if (!datasetVersionId.isEmpty()) {
        newDatasetLinkedIds.add(datasetVersionId);
      }
    }

    if (newDatasetLinkedIds.isEmpty()) {
      return InternalFuture.completedInternalFuture(newDatasets);
    }
    return jdbi.withHandle(
            handle -> {
              Map<String, Long> datasetVersionDatasetMap = new HashMap<>();
              handle
                  .createQuery(
                      " SELECT commit_hash, repository_id FROM repository_commit WHERE commit_hash IN (<datasetIds>) ")
                  .bindList("datasetIds", newDatasetLinkedIds)
                  .map(
                      (rs, ctx) ->
                          datasetVersionDatasetMap.put(
                              rs.getString("commit_hash"), rs.getLong("repository_id")))
                  .list();
              return datasetVersionDatasetMap;
            })
        .thenCompose(
            datasetVersionDatasetMap -> {
              List<String> accessibleDatasetVersionIds = new ArrayList<>();
              List<InternalFuture<Artifact>> internalFutures = new LinkedList<>();
              for (Artifact dataset : newDatasets) {
                String datasetVersionId = dataset.getLinkedArtifactId();
                if (!datasetVersionId.isEmpty()
                    && !accessibleDatasetVersionIds.contains(datasetVersionId)) {
                  try {
                    if (datasetVersionDatasetMap.containsKey(datasetVersionId)
                        && datasetVersionDatasetMap.get(datasetVersionId) != null) {
                      Long datasetId = datasetVersionDatasetMap.get(datasetVersionId);

                      internalFutures.add(
                          checkEntityPermission(
                                  Collections.singletonList(String.valueOf(datasetId)),
                                  ModelDBActionEnum.ModelDBServiceActions.READ,
                                  ModelDBServiceResourceTypes.DATASET)
                              .thenCompose(
                                  unused -> {
                                    accessibleDatasetVersionIds.add(datasetVersionId);
                                    return InternalFuture.completedInternalFuture(dataset);
                                  },
                                  executor));
                    } else {
                      return InternalFuture.failedStage(
                          new InvalidArgumentException(
                              "Dataset not found for dataset version: " + datasetVersionId));
                    }
                  } catch (Exception ex) {
                    LOGGER.trace(ex.getMessage());
                    if (errorOut) {
                      return InternalFuture.failedStage(ex);
                    }
                  }
                } else {
                  internalFutures.add(InternalFuture.completedInternalFuture(dataset));
                }
              }
              return InternalFuture.sequence(internalFutures, executor);
            },
            executor);
  }

  private InternalFuture<Void> deleteExperimentRuns(List<String> runIds) {
    return InternalFuture.runAsync(
        () ->
            jdbi.withHandle(
                handle ->
                    handle
                        .createUpdate(
                            "Update experiment_run SET deleted = :deleted WHERE id IN (<ids>)")
                        .bindList("ids", runIds)
                        .bind("deleted", true)
                        .execute()),
        executor);
  }

  public InternalFuture<Void> logArtifacts(LogArtifacts request) {
    final var runId = request.getId();
    final var artifacts = request.getArtifactsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> artifactHandler.logArtifacts(runId, artifacts), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<List<Artifact>> getArtifacts(GetArtifacts request) {
    final var runId = request.getId();
    final var key = request.getKey();
    Optional<String> maybeKey = key.isEmpty() ? Optional.empty() : Optional.of(key);

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> artifactHandler.getArtifacts(runId, maybeKey), executor);
  }

  public InternalFuture<Void> deleteArtifacts(DeleteArtifact request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();
    final var keys =
        request.getKey().isEmpty()
            ? new ArrayList<String>()
            : Collections.singletonList(request.getKey());
    Optional<List<String>> optionalKeys = keys.isEmpty() ? Optional.empty() : Optional.of(keys);

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> artifactHandler.deleteArtifacts(runId, optionalKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<ExperimentRun> createExperimentRun(CreateExperimentRun request) {
    return checkEntityPermission(
            Collections.singletonList(request.getProjectId()),
            ModelDBActionEnum.ModelDBServiceActions.UPDATE,
            ModelDBServiceResourceTypes.PROJECT)
        .thenCompose(
            unused ->
                filterAndGetPrivilegedDatasetsOnly(request.getDatasetsList(), true)
                    .thenApply(
                        privilegedDatasets ->
                            request
                                .toBuilder()
                                .clearDatasets()
                                .addAllDatasets(privilegedDatasets)
                                .build(),
                        executor),
            executor)
        .thenCompose(createExperimentRunHandler::createExperimentRun, executor);
  }
}
