package ai.verta.modeldb.experimentRun;

import ai.verta.common.Artifact;
import ai.verta.common.CodeVersion;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.common.Pagination;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.AddExperimentRunTags;
import ai.verta.modeldb.CloneExperimentRun;
import ai.verta.modeldb.CommitArtifactPart;
import ai.verta.modeldb.CommitMultipartArtifact;
import ai.verta.modeldb.CreateExperimentRun;
import ai.verta.modeldb.DeleteArtifact;
import ai.verta.modeldb.DeleteExperimentRunAttributes;
import ai.verta.modeldb.DeleteExperimentRunTags;
import ai.verta.modeldb.DeleteExperimentRuns;
import ai.verta.modeldb.DeleteHyperparameters;
import ai.verta.modeldb.DeleteMetrics;
import ai.verta.modeldb.DeleteObservations;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.Feature;
import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetCommittedArtifactParts;
import ai.verta.modeldb.GetDatasets;
import ai.verta.modeldb.GetExperimentRunCodeVersion;
import ai.verta.modeldb.GetExperimentRunsByDatasetVersionId;
import ai.verta.modeldb.GetExperimentRunsInExperiment;
import ai.verta.modeldb.GetHyperparameters;
import ai.verta.modeldb.GetMetrics;
import ai.verta.modeldb.GetObservations;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.GetVersionedInput;
import ai.verta.modeldb.LogArtifacts;
import ai.verta.modeldb.LogAttributes;
import ai.verta.modeldb.LogDatasets;
import ai.verta.modeldb.LogEnvironment;
import ai.verta.modeldb.LogExperimentRunCodeVersion;
import ai.verta.modeldb.LogHyperparameters;
import ai.verta.modeldb.LogMetrics;
import ai.verta.modeldb.LogObservations;
import ai.verta.modeldb.LogVersionedInput;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.Observation;
import ai.verta.modeldb.UpdateExperimentRunDescription;
import ai.verta.modeldb.VersioningEntry;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.FutureUtil;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.handlers.TagsHandlerBase;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.common.subtypes.MapSubtypes;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionFromBlobHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.CreateExperimentRunHandler;
import ai.verta.modeldb.experimentRun.subtypes.DatasetHandler;
import ai.verta.modeldb.experimentRun.subtypes.EnvironmentHandler;
import ai.verta.modeldb.experimentRun.subtypes.FeatureHandler;
import ai.verta.modeldb.experimentRun.subtypes.FilterPrivilegedDatasetsHandler;
import ai.verta.modeldb.experimentRun.subtypes.FilterPrivilegedVersionedInputsHandler;
import ai.verta.modeldb.experimentRun.subtypes.HyperparametersFromConfigHandler;
import ai.verta.modeldb.experimentRun.subtypes.KeyValueBaseHandler;
import ai.verta.modeldb.experimentRun.subtypes.ObservationHandler;
import ai.verta.modeldb.experimentRun.subtypes.PredicatesHandler;
import ai.verta.modeldb.experimentRun.subtypes.SortingHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.modeldb.experimentRun.subtypes.VersionInputHandler;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.utils.UACApisUtil;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.uac.Action;
import ai.verta.uac.Empty;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ResourceType;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.jdbi.v3.core.statement.Query;

public class FutureExperimentRunDAO {
  private static Logger LOGGER = LogManager.getLogger(FutureExperimentRunDAO.class);
  private static final String EXPERIMENT_RUN_ENTITY_NAME = "ExperimentRunEntity";

  private final FutureExecutor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;

  private final AttributeHandler attributeHandler;
  private final KeyValueBaseHandler hyperparametersHandler;
  private final KeyValueBaseHandler metricsHandler;
  private final ObservationHandler observationHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final CodeVersionHandler codeVersionHandler;
  private final DatasetHandler datasetHandler;
  private final PredicatesHandler predicatesHandler;
  private final SortingHandler sortingHandler;
  private final FeatureHandler featureHandler;
  private final EnvironmentHandler environmentHandler;
  private final FilterPrivilegedDatasetsHandler privilegedDatasetsHandler;
  private final VersionInputHandler versionInputHandler;
  private final FilterPrivilegedVersionedInputsHandler privilegedVersionedInputsHandler;
  private final CreateExperimentRunHandler createExperimentRunHandler;
  private final HyperparametersFromConfigHandler hyperparametersFromConfigHandler;
  private final MDBConfig config;
  private final CodeVersionFromBlobHandler codeVersionFromBlobHandler;
  private final UACApisUtil uacApisUtil;

  public FutureExperimentRunDAO(
      FutureExecutor executor,
      FutureJdbi jdbi,
      MDBConfig config,
      UAC uac,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetVersionDAO datasetVersionDAO,
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      BlobDAO blobDAO,
      UACApisUtil uacApisUtil) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;
    this.config = config;
    this.uacApisUtil = uacApisUtil;

    attributeHandler = new AttributeHandler(executor, jdbi, EXPERIMENT_RUN_ENTITY_NAME);
    hyperparametersHandler =
        new KeyValueBaseHandler(executor, jdbi, "hyperparameters", EXPERIMENT_RUN_ENTITY_NAME);
    metricsHandler = new KeyValueBaseHandler(executor, jdbi, "metrics", EXPERIMENT_RUN_ENTITY_NAME);
    observationHandler = new ObservationHandler(executor, jdbi);
    tagsHandler = new TagsHandler(executor, jdbi, EXPERIMENT_RUN_ENTITY_NAME);
    codeVersionHandler = new CodeVersionHandler(executor, jdbi, "experiment_run");
    datasetHandler = new DatasetHandler(executor, jdbi, EXPERIMENT_RUN_ENTITY_NAME, config);
    artifactHandler =
        new ArtifactHandler(
            executor,
            jdbi,
            EXPERIMENT_RUN_ENTITY_NAME,
            codeVersionHandler,
            datasetHandler,
            artifactStoreDAO,
            datasetVersionDAO,
            config);
    predicatesHandler =
        new PredicatesHandler(executor, "experiment_run", "experiment_run", uacApisUtil);
    sortingHandler = new SortingHandler("experiment_run");
    featureHandler = new FeatureHandler(executor, jdbi, EXPERIMENT_RUN_ENTITY_NAME);
    environmentHandler = new EnvironmentHandler(executor, jdbi, EXPERIMENT_RUN_ENTITY_NAME);
    privilegedDatasetsHandler = new FilterPrivilegedDatasetsHandler(executor, jdbi);
    versionInputHandler =
        new VersionInputHandler(
            executor, jdbi, EXPERIMENT_RUN_ENTITY_NAME, repositoryDAO, commitDAO, blobDAO);
    privilegedVersionedInputsHandler = new FilterPrivilegedVersionedInputsHandler(executor, jdbi);
    createExperimentRunHandler =
        new CreateExperimentRunHandler(
            executor,
            jdbi,
            config,
            uac,
            attributeHandler,
            hyperparametersHandler,
            metricsHandler,
            observationHandler,
            tagsHandler,
            artifactHandler,
            featureHandler,
            datasetHandler,
            versionInputHandler);
    hyperparametersFromConfigHandler =
        new HyperparametersFromConfigHandler(
            executor, jdbi, "hyperparameters", EXPERIMENT_RUN_ENTITY_NAME);
    codeVersionFromBlobHandler =
        new CodeVersionFromBlobHandler(
            executor, jdbi, config.isPopulateConnectionsBasedOnPrivileges());
  }

  public InternalFuture<ExperimentRun> deleteObservations(DeleteObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getObservationKeysList());

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> observationHandler.deleteObservations(runId, maybeKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<List<Observation>> getObservations(GetObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var key = request.getObservationKey();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> observationHandler.getObservations(runId, key), executor)
        .thenApply(
            observations ->
                observations.stream()
                    .sorted(Comparator.comparing(RdbmsUtils::getObservationCompareKey))
                    .collect(Collectors.toList()),
            executor);
  }

  public InternalFuture<ExperimentRun> logObservations(LogObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var observations = request.getObservationsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> observationHandler.logObservations(handle, runId, observations, now)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<ExperimentRun> deleteMetrics(DeleteMetrics request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getMetricKeysList());

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused ->
                jdbi.useHandle(handle -> metricsHandler.deleteKeyValues(handle, runId, maybeKeys)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<ExperimentRun> deleteHyperparameters(DeleteHyperparameters request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll()
            ? Optional.empty()
            : Optional.of(request.getHyperparameterKeysList());

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> hyperparametersHandler.deleteKeyValues(handle, runId, maybeKeys)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<ExperimentRun> deleteAttributes(DeleteExperimentRunAttributes request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getAttributeKeysList());

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> attributeHandler.deleteKeyValues(handle, runId, maybeKeys)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<List<KeyValue>> getMetrics(GetMetrics request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(
            unused -> metricsHandler.getKeyValues(runId, Collections.emptyList(), true), executor);
  }

  public InternalFuture<List<KeyValue>> getHyperparameters(GetHyperparameters request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(
            unused -> hyperparametersHandler.getKeyValues(runId, Collections.emptyList(), true),
            executor);
  }

  public InternalFuture<List<KeyValue>> getAttributes(GetAttributes request) {
    final var runId = request.getId();
    final var keys = request.getAttributeKeysList();
    final var getAll = request.getGetAll();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> attributeHandler.getKeyValues(runId, keys, getAll), executor);
  }

  public InternalFuture<ExperimentRun> logMetrics(LogMetrics request) {
    final var runId = request.getId();
    final var metrics = request.getMetricsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused -> jdbi.useHandle(handle -> metricsHandler.logKeyValues(handle, runId, metrics)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<ExperimentRun> logHyperparameters(LogHyperparameters request) {
    final var runId = request.getId();
    final var hyperparameters = request.getHyperparametersList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> hyperparametersHandler.logKeyValues(handle, runId, hyperparameters)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<ExperimentRun> logAttributes(LogAttributes request) {
    final var runId = request.getId();
    final var attributes = request.getAttributesList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused ->
                jdbi.useHandle(handle -> attributeHandler.logKeyValues(handle, runId, attributes)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<ExperimentRun> addTags(AddExperimentRunTags request) {
    final var runId = request.getId();
    final var tags = request.getTagsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle ->
                        tagsHandler.addTags(
                            handle, runId, TagsHandlerBase.checkEntityTagsLength(tags))),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<ExperimentRun> deleteTags(DeleteExperimentRunTags request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeTags =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getTagsList());

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused -> jdbi.useHandle(handle -> tagsHandler.deleteTags(handle, runId, maybeTags)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<List<String>> getTags(GetTags request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> tagsHandler.getTags(runId), executor);
  }

  private InternalFuture<Void> updateModifiedTimestamp(String runId, Long now) {
    return jdbi.useHandle(
        handle -> {
          final var currentDateUpdated =
              handle
                  .createQuery("SELECT date_updated FROM experiment_run WHERE id=:run_id")
                  .bind("run_id", runId)
                  .mapTo(Long.class)
                  .one();
          final var dateUpdated = Math.max(currentDateUpdated, now);
          handle
              .createUpdate("update experiment_run set date_updated=:date_updated where id=:run_id")
              .bind("run_id", runId)
              .bind("date_updated", dateUpdated)
              .execute();
        });
  }

  private InternalFuture<Void> updateVersionNumber(String erId) {
    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "update experiment_run set version_number=(version_number + 1) where id=:er_id")
                .bind("er_id", erId)
                .execute());
  }

  private InternalFuture<Boolean> getEntityPermissionBasedOnResourceTypes(
      List<String> entityIds,
      ModelDBActionEnum.ModelDBServiceActions action,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return FutureUtil.clientRequest(
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
                                .addAllResourceIds(entityIds))
                        .build()),
            executor)
        .thenCompose(
            response -> InternalFuture.completedInternalFuture(response.getAllowed()), executor);
  }

  private InternalFuture<Void> checkPermission(
      List<String> runIds, ModelDBActionEnum.ModelDBServiceActions action) {
    // If request do not have ids and we are passing request.getId() then it will create list record
    // with `""` string
    final var finalRunIds = runIds.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
    if (finalRunIds.isEmpty()) {
      return InternalFuture.failedStage(
          new InvalidArgumentException("Experiment run IDs is missing"));
    }

    var futureMaybeProjectIds =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT project_id FROM experiment_run WHERE id IN (<ids>) AND deleted=:deleted")
                    .bindList("ids", finalRunIds)
                    .bind("deleted", 0)
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
              return getEntityPermissionBasedOnResourceTypes(
                      maybeProjectIds,
                      ModelDBActionEnum.ModelDBServiceActions.UPDATE,
                      ModelDBServiceResourceTypes.PROJECT)
                  .thenAccept(
                      allowed -> {
                        if (!allowed) {
                          throw new PermissionDeniedException(ModelDBMessages.PERMISSION_DENIED);
                        }
                      },
                      executor);
            default:
              return getEntityPermissionBasedOnResourceTypes(
                      maybeProjectIds, action, ModelDBServiceResourceTypes.PROJECT)
                  .thenAccept(
                      allowed -> {
                        if (!allowed) {
                          throw new PermissionDeniedException(ModelDBMessages.PERMISSION_DENIED);
                        }
                      },
                      executor);
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

  public InternalFuture<ExperimentRun> logArtifacts(LogArtifacts request) {
    final var runId = request.getId();
    final var artifacts = request.getArtifactsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> artifactHandler.logArtifacts(handle, runId, artifacts, false)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<List<Artifact>> getArtifacts(GetArtifacts request) {
    final var runId = request.getId();
    final var key = request.getKey();
    Optional<String> maybeKey = key.isEmpty() ? Optional.empty() : Optional.of(key);

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> artifactHandler.getArtifacts(runId, maybeKey), executor);
  }

  public InternalFuture<ExperimentRun> deleteArtifacts(DeleteArtifact request) {
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
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<ExperimentRun> logDatasets(LogDatasets request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused ->
                privilegedDatasetsHandler.filterAndGetPrivilegedDatasetsOnly(
                    request.getDatasetsList(), true, this::getEntityPermissionBasedOnResourceTypes),
            executor)
        .thenCompose(
            privilegedDatasets ->
                jdbi.useHandle(
                    handle ->
                        datasetHandler.logArtifacts(
                            handle, runId, privilegedDatasets, request.getOverwrite())),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<List<Artifact>> getDatasets(GetDatasets request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> datasetHandler.getArtifacts(runId, Optional.empty()), executor);
  }

  public InternalFuture<ExperimentRun> logCodeVersion(LogExperimentRunCodeVersion request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();
    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle ->
                        codeVersionHandler.logCodeVersion(
                            handle, runId, request.getOverwrite(), request.getCodeVersion())),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<Optional<CodeVersion>> getCodeVersion(GetExperimentRunCodeVersion request) {
    final var runId = request.getId();
    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> codeVersionHandler.getCodeVersion(request.getId()), executor);
  }

  public InternalFuture<GetUrlForArtifact.Response> getUrlForArtifact(GetUrlForArtifact request) {
    final var runId = request.getId();

    InternalFuture<Void> permissionCheck;
    if (request.getMethod().toUpperCase().equals("GET")) {
      permissionCheck =
          checkPermission(
              Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ);
    } else {
      permissionCheck =
          checkPermission(
              Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE);
    }

    return permissionCheck.thenCompose(
        unused -> artifactHandler.getUrlForArtifact(request), executor);
  }

  public InternalFuture<GetCommittedArtifactParts.Response> getCommittedArtifactParts(
      GetCommittedArtifactParts request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> artifactHandler.getCommittedArtifactParts(request), executor);
  }

  public InternalFuture<Void> commitArtifactPart(CommitArtifactPart request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> artifactHandler.commitArtifactPart(request), executor);
  }

  public InternalFuture<Void> commitMultipartArtifact(CommitMultipartArtifact request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> artifactHandler.commitMultipartArtifact(request), executor);
  }

  public InternalFuture<FindExperimentRuns.Response> findExperimentRuns(
      FindExperimentRuns request) {
    // TODO: handle ids only?
    // TODO: filter by permission

    final var futureLocalContext =
        InternalFuture.supplyAsync(
            () -> {
              final var localQueryContext = new QueryFilterContext();
              localQueryContext.getConditions().add("experiment_run.deleted = :deleted");
              localQueryContext.getConditions().add("p.deleted = :deleted");
              localQueryContext.getConditions().add("e.deleted = :deleted");
              localQueryContext.getBinds().add(q -> q.bind("deleted", false));

              if (!request.getProjectId().isEmpty()) {
                localQueryContext
                    .getConditions()
                    .add("experiment_run.project_id=:request_project_id");
                localQueryContext
                    .getBinds()
                    .add(q -> q.bind("request_project_id", request.getProjectId()));
              }

              if (!request.getExperimentId().isEmpty()) {
                localQueryContext
                    .getConditions()
                    .add("experiment_run.experiment_id=:request_experiment_id");
                localQueryContext
                    .getBinds()
                    .add(q -> q.bind("request_experiment_id", request.getExperimentId()));
              }

              if (!request.getExperimentRunIdsList().isEmpty()) {
                localQueryContext
                    .getConditions()
                    .add("experiment_run.id in (<request_experiment_run_ids>)");
                localQueryContext
                    .getBinds()
                    .add(
                        q ->
                            q.bindList(
                                "request_experiment_run_ids", request.getExperimentRunIdsList()));
              }

              return localQueryContext;
            },
            executor);

    // futurePredicatesContext
    final var futurePredicatesContext =
        predicatesHandler.processPredicates(request.getPredicatesList(), executor);

    // futureSortingContext
    final var futureSortingContext =
        sortingHandler.processSort(request.getSortKey(), request.getAscending());

    final InternalFuture<QueryFilterContext> futureProjectIds =
        getAccessibleProjectIdsQueryFilterContext(
            request.getWorkspaceName(), request.getProjectId());

    final var futureExperimentRuns =
        futureProjectIds.thenCompose(
            accessibleProjectIdsQueryContext -> {
              // accessibleProjectIdsQueryContext == null means not allowed anything
              if (accessibleProjectIdsQueryContext == null) {
                return InternalFuture.completedInternalFuture(new ArrayList<ExperimentRun>());
              } else {
                final var futureProjectIdsContext =
                    InternalFuture.completedInternalFuture(accessibleProjectIdsQueryContext);
                return InternalFuture.sequence(
                        Arrays.asList(
                            futureLocalContext,
                            futurePredicatesContext,
                            futureSortingContext,
                            futureProjectIdsContext),
                        executor)
                    .thenApply(QueryFilterContext::combine, executor)
                    .thenCompose(
                        queryContext -> {
                          // TODO: get environment
                          // TODO: get features?
                          // TODO: get versioned inputs
                          // TODO: get code version from blob
                          return jdbi.withHandle(
                                  handle -> {
                                    var sql =
                                        "select experiment_run.id, experiment_run.date_created, experiment_run.date_updated, experiment_run.experiment_id, experiment_run.name, experiment_run.project_id, experiment_run.description, experiment_run.start_time, experiment_run.end_time, experiment_run.owner, experiment_run.environment, experiment_run.code_version, experiment_run.job_id, experiment_run.version_number from experiment_run";

                                    sql +=
                                        " inner join project p ON p.id = experiment_run.project_id ";
                                    sql +=
                                        " inner join experiment e ON e.id = experiment_run.experiment_id ";

                                    Query query =
                                        CommonUtils.buildQueryFromQueryContext(
                                            "experiment_run",
                                            Pagination.newBuilder()
                                                .setPageNumber(request.getPageNumber())
                                                .setPageLimit(request.getPageLimit())
                                                .build(),
                                            queryContext,
                                            handle,
                                            sql,
                                            config.getDatabase().getRdbConfiguration().isMssql());

                                    return query
                                        .map(
                                            (rs, ctx) -> {
                                              var runBuilder =
                                                  ExperimentRun.newBuilder()
                                                      .setId(rs.getString("id"))
                                                      .setProjectId(rs.getString("project_id"))
                                                      .setExperimentId(
                                                          rs.getString("experiment_id"))
                                                      .setName(rs.getString("name"))
                                                      .setDescription(rs.getString("description"))
                                                      .setDateUpdated(rs.getLong("date_updated"))
                                                      .setDateCreated(rs.getLong("date_created"))
                                                      .setStartTime(rs.getLong("start_time"))
                                                      .setEndTime(rs.getLong("end_time"))
                                                      .setOwner(rs.getString("owner"))
                                                      .setCodeVersion(rs.getString("code_version"))
                                                      .setJobId(rs.getString("job_id"))
                                                      .setVersionNumber(
                                                          rs.getLong("version_number"));

                                              var environment = rs.getString("environment");
                                              if (environment != null && !environment.isEmpty()) {
                                                var environmentBlobBuilder =
                                                    EnvironmentBlob.newBuilder();
                                                CommonUtils.getProtoObjectFromString(
                                                    environment, environmentBlobBuilder);
                                                runBuilder.setEnvironment(
                                                    environmentBlobBuilder.build());
                                              }

                                              return runBuilder;
                                            })
                                        .list();
                                  })
                              .thenCompose(
                                  builders -> {
                                    if (builders == null || builders.isEmpty()) {
                                      return InternalFuture.completedInternalFuture(
                                          new LinkedList<ExperimentRun>());
                                    }

                                    var futureBuildersStream =
                                        InternalFuture.completedInternalFuture(builders.stream());
                                    final var ids =
                                        builders.stream()
                                            .map(x -> x.getId())
                                            .collect(Collectors.toSet());

                                    // Get tags
                                    final var futureTags = tagsHandler.getTagsMap(ids);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureTags,
                                            (stream, tags) ->
                                                stream.map(
                                                    builder ->
                                                        builder.addAllTags(
                                                            tags.get(builder.getId()))),
                                            executor);

                                    // Get hyperparams
                                    final var futureHyperparams =
                                        hyperparametersHandler.getKeyValuesMap(ids);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureHyperparams,
                                            (stream, hyperparams) ->
                                                stream.map(
                                                    builder ->
                                                        builder.addAllHyperparameters(
                                                            hyperparams.get(builder.getId()))),
                                            executor);

                                    final var futureHyperparamsFromConfigBlobs =
                                        getFutureHyperparamsFromConfigBlobs(ids);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureHyperparamsFromConfigBlobs,
                                            (stream, hyperparamsFromConfigBlob) ->
                                                stream.map(
                                                    builder -> {
                                                      List<KeyValue> hypFromConfigs =
                                                          hyperparamsFromConfigBlob.get(
                                                              builder.getId());
                                                      if (hypFromConfigs != null) {
                                                        builder.addAllHyperparameters(
                                                            hypFromConfigs);
                                                      }
                                                      return builder;
                                                    }),
                                            executor);

                                    final var futureCodeVersionFromBlob =
                                        getFutureCodeVersionFromBlob(ids);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureCodeVersionFromBlob,
                                            (stream, runCodeVersionConfigBlob) ->
                                                stream.map(
                                                    builder -> {
                                                      if (!runCodeVersionConfigBlob.isEmpty()
                                                          && runCodeVersionConfigBlob.containsKey(
                                                              builder.getId())) {
                                                        builder.putAllCodeVersionFromBlob(
                                                            runCodeVersionConfigBlob.get(
                                                                builder.getId()));
                                                      }
                                                      return builder;
                                                    }),
                                            executor);

                                    // Get metrics
                                    final var futureMetrics = metricsHandler.getKeyValuesMap(ids);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureMetrics,
                                            (stream, metrics) ->
                                                stream.map(
                                                    builder ->
                                                        builder.addAllMetrics(
                                                            metrics.get(builder.getId()))),
                                            executor);

                                    // Get attributes
                                    final var futureAttributes =
                                        attributeHandler.getKeyValuesMap(ids);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureAttributes,
                                            (stream, attributes) ->
                                                stream.map(
                                                    builder ->
                                                        builder.addAllAttributes(
                                                            attributes.get(builder.getId()))),
                                            executor);

                                    // Get artifacts
                                    final var futureArtifacts =
                                        artifactHandler.getArtifactsMap(ids);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureArtifacts,
                                            (stream, artifacts) ->
                                                stream.map(
                                                    builder ->
                                                        builder.addAllArtifacts(
                                                            artifacts.get(builder.getId()))),
                                            executor);

                                    // Get datasets
                                    final var futureDatasetsMap =
                                        datasetHandler.getArtifactsMap(ids);
                                    final var filterDatasetsMap =
                                        futureDatasetsMap.thenCompose(
                                            artifactMapSubtypes -> {
                                              List<InternalFuture<Map<String, List<Artifact>>>>
                                                  internalFutureList = new ArrayList<>();
                                              for (ExperimentRun.Builder builder : builders) {
                                                internalFutureList.add(
                                                    privilegedDatasetsHandler
                                                        .filterAndGetPrivilegedDatasetsOnly(
                                                            artifactMapSubtypes.get(
                                                                builder.getId()),
                                                            false,
                                                            this
                                                                ::getEntityPermissionBasedOnResourceTypes)
                                                        .thenCompose(
                                                            artifacts ->
                                                                InternalFuture
                                                                    .completedInternalFuture(
                                                                        Collections.singletonMap(
                                                                            builder.getId(),
                                                                            artifacts)),
                                                            executor));
                                              }
                                              return InternalFuture.sequence(
                                                      internalFutureList, executor)
                                                  .thenCompose(
                                                      maps -> {
                                                        Map<String, List<Artifact>>
                                                            finalDatasetMap = new HashMap<>();
                                                        maps.forEach(finalDatasetMap::putAll);
                                                        return InternalFuture
                                                            .completedInternalFuture(
                                                                finalDatasetMap);
                                                      },
                                                      executor);
                                            },
                                            executor);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            filterDatasetsMap,
                                            (stream, datasets) ->
                                                stream.map(
                                                    builder -> {
                                                      List<Artifact> datasetList =
                                                          datasets.get(builder.getId());
                                                      if (datasetList != null
                                                          && !datasetList.isEmpty()) {
                                                        return builder
                                                            .clearDatasets()
                                                            .addAllDatasets(datasetList);
                                                      }
                                                      return builder;
                                                    }),
                                            executor);

                                    // Get observations
                                    final var futureObservations =
                                        observationHandler.getObservationsMap(ids);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureObservations,
                                            (stream, observations) ->
                                                stream.map(
                                                    builder ->
                                                        builder.addAllObservations(
                                                            observations.get(builder.getId()))),
                                            executor);

                                    // Get features
                                    final var futureFeatures = featureHandler.getFeaturesMap(ids);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureFeatures,
                                            (stream, features) ->
                                                stream.map(
                                                    builder ->
                                                        builder.addAllFeatures(
                                                            features.get(builder.getId()))),
                                            executor);

                                    // Get code version snapshot
                                    final var futureCodeVersionSnapshots =
                                        codeVersionHandler.getCodeVersionMap(new ArrayList<>(ids));
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureCodeVersionSnapshots,
                                            (stream, codeVersionsMap) ->
                                                stream.peek(
                                                    builder -> {
                                                      if (codeVersionsMap.containsKey(
                                                          builder.getId())) {
                                                        builder.setCodeVersionSnapshot(
                                                            codeVersionsMap.get(builder.getId()));
                                                      } else {
                                                        builder.setCodeVersionSnapshot(
                                                            CodeVersion.getDefaultInstance());
                                                      }
                                                    }),
                                            executor);

                                    // Get VersionedInputs
                                    final var futureVersionedInputs =
                                        versionInputHandler.getVersionedInputs(ids);
                                    final InternalFuture<Map<String, VersioningEntry>>
                                        filterPrivilegeVersionedInputMap =
                                            privilegedVersionedInputsHandler
                                                .filterVersionedInputsBasedOnPrivileges(
                                                    ids,
                                                    futureVersionedInputs,
                                                    this::getEntityPermissionBasedOnResourceTypes);
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            filterPrivilegeVersionedInputMap,
                                            (stream, versionInputsMap) ->
                                                stream.map(
                                                    builder -> {
                                                      VersioningEntry finalVersionedInputs =
                                                          versionInputsMap.get(builder.getId());
                                                      if (finalVersionedInputs != null) {
                                                        builder.setVersionedInputs(
                                                            finalVersionedInputs);
                                                      } else {
                                                        builder.clearVersionedInputs();
                                                      }
                                                      return builder;
                                                    }),
                                            executor);

                                    return futureBuildersStream.thenApply(
                                        experimentRunBuilders ->
                                            experimentRunBuilders
                                                .map(ExperimentRun.Builder::build)
                                                .collect(Collectors.toList()),
                                        executor);
                                  },
                                  executor);
                        },
                        executor);
              }
            },
            executor);

    final var futureCount =
        futureProjectIds.thenCompose(
            accessibleProjectIdsQueryContext -> {
              // accessibleProjectIdsQueryContext == null means not allowed anything
              if (accessibleProjectIdsQueryContext == null) {
                return InternalFuture.completedInternalFuture(0L);
              } else {
                final var futureProjectIdsContext =
                    InternalFuture.completedInternalFuture(accessibleProjectIdsQueryContext);
                return InternalFuture.sequence(
                        Arrays.asList(
                            futureLocalContext, futurePredicatesContext, futureProjectIdsContext),
                        executor)
                    .thenApply(QueryFilterContext::combine, executor)
                    .thenCompose(
                        queryContext ->
                            jdbi.withHandle(
                                handle -> {
                                  var sql = "select count(experiment_run.id) from experiment_run";

                                  sql +=
                                      " inner join project p ON p.id = experiment_run.project_id ";
                                  sql +=
                                      " inner join experiment e ON e.id = experiment_run.experiment_id ";

                                  if (!queryContext.getConditions().isEmpty()) {
                                    sql +=
                                        " WHERE "
                                            + String.join(" AND ", queryContext.getConditions());
                                  }

                                  var query = handle.createQuery(sql);
                                  queryContext.getBinds().forEach(b -> b.accept(query));

                                  return query.mapTo(Long.class).one();
                                }),
                        executor);
              }
            },
            executor);

    return futureExperimentRuns
        .thenApply(this::sortExperimentRunFields, executor)
        .thenCombine(
            futureCount,
            (runs, count) ->
                FindExperimentRuns.Response.newBuilder()
                    .addAllExperimentRuns(runs)
                    .setTotalRecords(count)
                    .build(),
            executor);
  }

  private List<ExperimentRun> sortExperimentRunFields(List<ExperimentRun> experimentRuns) {
    List<ExperimentRun> sortedRuns = new LinkedList<>();
    for (ExperimentRun run : experimentRuns) {
      var experimentRunBuilder = ExperimentRun.newBuilder(run);
      experimentRunBuilder
          .clearTags()
          .addAllTags(run.getTagsList().stream().sorted().collect(Collectors.toList()))
          .clearAttributes()
          .addAllAttributes(
              run.getAttributesList().stream()
                  .sorted(Comparator.comparing(KeyValue::getKey))
                  .collect(Collectors.toList()))
          .clearHyperparameters()
          .addAllHyperparameters(
              run.getHyperparametersList().stream()
                  .sorted(Comparator.comparing(KeyValue::getKey))
                  .collect(Collectors.toList()))
          .clearArtifacts()
          .addAllArtifacts(
              run.getArtifactsList().stream()
                  .sorted(Comparator.comparing(Artifact::getKey))
                  .collect(Collectors.toList()))
          .clearDatasets()
          .addAllDatasets(
              run.getDatasetsList().stream()
                  .sorted(Comparator.comparing(Artifact::getKey))
                  .collect(Collectors.toList()))
          .clearMetrics()
          .addAllMetrics(
              run.getMetricsList().stream()
                  .sorted(Comparator.comparing(KeyValue::getKey))
                  .collect(Collectors.toList()))
          .clearObservations()
          .addAllObservations(
              run.getObservationsList().stream()
                  .sorted(Comparator.comparing(RdbmsUtils::getObservationCompareKey))
                  .collect(Collectors.toList()))
          .clearFeatures()
          .addAllFeatures(
              run.getFeaturesList().stream()
                  .sorted(Comparator.comparing(Feature::getName))
                  .collect(Collectors.toList()));
      sortedRuns.add(experimentRunBuilder.build());
    }
    return sortedRuns;
  }

  private InternalFuture<QueryFilterContext> getAccessibleProjectIdsQueryFilterContext(
      String workspaceName, String requestedProjectId) {
    if (workspaceName.isEmpty()) {
      return uacApisUtil
          .getAllowedEntitiesByResourceType(
              ModelDBActionEnum.ModelDBServiceActions.READ, ModelDBServiceResourceTypes.PROJECT)
          .thenApply(
              resources -> {
                boolean allowedAllResources = RoleServiceUtils.checkAllResourceAllowed(resources);
                if (allowedAllResources) {
                  return new QueryFilterContext();
                } else {
                  Set<String> accessibleProjectIds = RoleServiceUtils.getResourceIds(resources);
                  if (accessibleProjectIds.isEmpty()) {
                    return null;
                  } else {
                    return new QueryFilterContext()
                        .addCondition("experiment_run.project_id in (<authz_project_ids>)")
                        .addBind(q -> q.bindList("authz_project_ids", accessibleProjectIds));
                  }
                }
              },
              executor);
    } else {
      // futureProjectIds based on workspace
      return uacApisUtil
          .getAccessibleProjectIdsBasedOnWorkspace(workspaceName, Optional.of(requestedProjectId))
          .thenApply(
              accessibleProjectIds -> {
                if (accessibleProjectIds.isEmpty()) {
                  return null;
                } else {
                  return new QueryFilterContext()
                      .addCondition("experiment_run.project_id in (<authz_project_ids>)")
                      .addBind(q -> q.bindList("authz_project_ids", accessibleProjectIds));
                }
              },
              executor);
    }
  }

  private InternalFuture<MapSubtypes<String, KeyValue>> getFutureHyperparamsFromConfigBlobs(
      Set<String> ids) {
    return getRepositoryResourcesForPopulateConnectionsBasedOnPrivileges()
        .thenCompose(
            resources -> {
              boolean allowedAllResources = RoleServiceUtils.checkAllResourceAllowed(resources);
              // For all repositories are accessible
              if (allowedAllResources) {
                return hyperparametersFromConfigHandler.getExperimentRunHyperparameterConfigBlobMap(
                    new ArrayList<>(ids), Collections.emptyList(), true);
              } else {
                // If all repositories are not accessible then need to extract accessible from list
                // of resources
                Set<String> selfAllowedRepositoryIds = RoleServiceUtils.getResourceIds(resources);
                // If self allowed repositories list is empty then return response by this method
                // will return empty list otherwise return as per selfAllowedRepositoryIds
                return hyperparametersFromConfigHandler.getExperimentRunHyperparameterConfigBlobMap(
                    new ArrayList<>(ids), selfAllowedRepositoryIds, false);
              }
            },
            executor);
  }

  private InternalFuture<Map<String, Map<String, CodeVersion>>> getFutureCodeVersionFromBlob(
      Set<String> ids) {
    return getRepositoryResourcesForPopulateConnectionsBasedOnPrivileges()
        .thenCompose(
            resources -> {
              boolean allowedAllResources = RoleServiceUtils.checkAllResourceAllowed(resources);
              // For all repositories are accessible
              if (allowedAllResources) {
                return codeVersionFromBlobHandler.getExperimentRunCodeVersionMap(
                    ids, Collections.emptyList(), true);
              } else {
                // If all repositories are not accessible then need to extract accessible from list
                // of resources
                Set<String> selfAllowedRepositoryIds = RoleServiceUtils.getResourceIds(resources);
                // If self allowed repositories list is empty then return response by this method
                // will return empty list otherwise return as per selfAllowedRepositoryIds
                return codeVersionFromBlobHandler.getExperimentRunCodeVersionMap(
                    ids, selfAllowedRepositoryIds, false);
              }
            },
            executor);
  }

  private InternalFuture<List<Resources>>
      getRepositoryResourcesForPopulateConnectionsBasedOnPrivileges() {
    return InternalFuture.completedInternalFuture(config.isPopulateConnectionsBasedOnPrivileges())
        .thenCompose(
            populateConnectionsBasedOnPrivileges -> {
              // If populateConnectionsBasedOnPrivileges = true then fetch all accessible
              // repositories from UAC
              if (populateConnectionsBasedOnPrivileges) {
                return uacApisUtil.getAllowedEntitiesByResourceType(
                    ModelDBActionEnum.ModelDBServiceActions.READ,
                    ModelDBServiceResourceTypes.REPOSITORY);
              } else {
                // return empty list if populateConnectionsBasedOnPrivileges = false
                return InternalFuture.completedInternalFuture(new ArrayList<>());
              }
            },
            executor);
  }

  public InternalFuture<ExperimentRun> createExperimentRun(CreateExperimentRun request) {
    // Validate arguments
    var futureTask =
        InternalFuture.runAsync(
            () -> {
              if (request.getProjectId().isEmpty()) {
                throw new InvalidArgumentException("Project ID not present");
              } else if (request.getExperimentId().isEmpty()) {
                throw new InvalidArgumentException("Experiment ID not present");
              }
            },
            executor);
    return futureTask
        .thenCompose(
            unused ->
                getEntityPermissionBasedOnResourceTypes(
                    Collections.singletonList(request.getProjectId()),
                    ModelDBActionEnum.ModelDBServiceActions.UPDATE,
                    ModelDBServiceResourceTypes.PROJECT),
            executor)
        .thenAccept(
            allowed -> {
              if (!allowed) {
                throw new PermissionDeniedException(ModelDBMessages.PERMISSION_DENIED);
              }
            },
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> {
                      Long count =
                          handle
                              .createQuery("SELECT COUNT(id) FROM experiment where id = :id")
                              .bind("id", request.getExperimentId())
                              .mapTo(Long.class)
                              .one();
                      if (count == 0) {
                        throw new NotFoundException(
                            "Experiment not found for given ID: " + request.getExperimentId());
                      }
                    }),
            executor)
        .thenCompose(
            unused ->
                privilegedDatasetsHandler
                    .filterAndGetPrivilegedDatasetsOnly(
                        request.getDatasetsList(),
                        true,
                        this::getEntityPermissionBasedOnResourceTypes)
                    .thenApply(
                        privilegedDatasets ->
                            request
                                .toBuilder()
                                .clearDatasets()
                                .addAllDatasets(privilegedDatasets)
                                .build(),
                        executor),
            executor)
        .thenCompose(unused -> createExperimentRunHandler.convertCreateRequest(request), executor)
        .thenCompose(
            experimentRun ->
                findExperimentRuns(
                        FindExperimentRuns.newBuilder()
                            .setIdsOnly(true)
                            .setProjectId(experimentRun.getProjectId())
                            .build())
                    .thenApply(runsResponse -> experimentRun, executor),
            executor)
        .thenCompose(
            experimentRun ->
                createExperimentRunHandler
                    .insertExperimentRun(experimentRun)
                    .thenApply(
                        createdExperimentRun ->
                            sortExperimentRunFields(Collections.singletonList(createdExperimentRun))
                                .get(0),
                        executor),
            executor);
  }

  public InternalFuture<ExperimentRun> logEnvironment(LogEnvironment request) {
    final var runId = request.getId();

    if (!request.hasEnvironment()) {
      return InternalFuture.failedStage(
          new InvalidArgumentException("Environment should not be empty"));
    }

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(
            unused -> environmentHandler.logEnvironment(request.getId(), request.getEnvironment()),
            executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<ExperimentRun> logVersionedInputs(LogVersionedInput request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();
    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused -> versionInputHandler.getVersionedInputs(Collections.singleton(runId)),
            executor)
        .thenAccept(
            existingVersioningEntryMap -> {
              var existingVersioningEntry = existingVersioningEntryMap.get(request.getId());
              if (existingVersioningEntry != null) {
                if (existingVersioningEntry.getRepositoryId()
                        != request.getVersionedInputs().getRepositoryId()
                    || !existingVersioningEntry
                        .getCommit()
                        .equals(request.getVersionedInputs().getCommit())) {
                  throw new AlreadyExistsException(
                      ModelDBConstants.DIFFERENT_REPOSITORY_OR_COMMIT_MESSAGE);
                }
              }
            },
            executor)
        .thenCompose(
            unused -> versionInputHandler.validateVersioningEntity(request.getVersionedInputs()),
            executor)
        .thenCompose(
            locationBlobWithHashMap ->
                jdbi.useHandle(
                    handle ->
                        versionInputHandler.validateAndInsertVersionedInputs(
                            handle,
                            request.getId(),
                            request.getVersionedInputs(),
                            locationBlobWithHashMap)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<VersioningEntry> getVersionedInputs(GetVersionedInput request) {
    final var futureVersionedInputs =
        versionInputHandler.getVersionedInputs(Collections.singleton(request.getId()));
    return privilegedVersionedInputsHandler
        .filterVersionedInputsBasedOnPrivileges(
            Collections.singleton(request.getId()),
            futureVersionedInputs,
            this::getEntityPermissionBasedOnResourceTypes)
        .thenApply(versioningEntryMap -> versioningEntryMap.get(request.getId()), executor);
  }

  public InternalFuture<GetExperimentRunsByDatasetVersionId.Response>
      getExperimentRunsByDatasetVersionId(GetExperimentRunsByDatasetVersionId request) {
    var validationFuture =
        InternalFuture.runAsync(
            () -> {
              // Validate request parameter
              if (request.getDatasetVersionId().isEmpty()) {
                throw new ModelDBException(
                    "DatasetVersion Id should not be empty", Code.INVALID_ARGUMENT);
              }
            },
            executor);
    return validationFuture
        .thenAccept(
            unused ->
                // Validate requested dataset version exists
                jdbi.useHandle(
                    handle -> {
                      var query =
                          "SELECT COUNT(commit_hash) FROM commit WHERE commit_hash = :commitHash";
                      if (config.getDatabase().getRdbConfiguration().isMssql()) {
                        query =
                            "SELECT COUNT(commit_hash) FROM \"commit\" WHERE commit_hash = :commitHash";
                      }
                      Optional<Long> count =
                          handle
                              .createQuery(query)
                              .bind("commitHash", request.getDatasetVersionId())
                              .mapTo(Long.class)
                              .findOne();
                      if (count.isEmpty() || count.get() == 0) {
                        throw new NotFoundException("DatasetVersion not found");
                      }

                      // Validate requested dataset version mappings with datasets
                      List<Long> mappingDatasetIds =
                          handle
                              .createQuery(
                                  "SELECT repository_id FROM repository_commit WHERE commit_hash = :id")
                              .bind("id", request.getDatasetVersionId())
                              .mapTo(Long.class)
                              .list();

                      if (mappingDatasetIds.size() == 0) {
                        throw new InternalErrorException(
                            "DatasetVersion not attached with the dataset");
                      } else if (mappingDatasetIds.size() > 1) {
                        throw new InternalErrorException(
                            "DatasetVersion '"
                                + request.getDatasetVersionId()
                                + "' associated with multiple datasets");
                      }
                    }),
            executor)
        .thenApply(
            unused -> {
              // Create find ER request using dataset version predicate and requested pagination
              // parameters
              KeyValueQuery entityKeyValuePredicate =
                  KeyValueQuery.newBuilder()
                      .setKey(ModelDBConstants.DATASETS + "." + ModelDBConstants.LINKED_ARTIFACT_ID)
                      .setValue(
                          Value.newBuilder().setStringValue(request.getDatasetVersionId()).build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build();

              return FindExperimentRuns.newBuilder()
                  .setPageNumber(request.getPageNumber())
                  .setPageLimit(request.getPageLimit())
                  .setAscending(request.getAscending())
                  .setSortKey(request.getSortKey())
                  .addPredicates(entityKeyValuePredicate)
                  .build();
            },
            executor)
        .thenCompose(this::findExperimentRuns, executor)
        .thenApply(
            findExperimentRunsResponse -> {
              // Build GetExperimentRunsByDatasetVersionId response from find ER response
              LOGGER.trace(
                  "Final return experiment_run count for dataset version: {}",
                  findExperimentRunsResponse.getExperimentRunsCount());
              LOGGER.trace(
                  "Final return total record count : {}",
                  findExperimentRunsResponse.getTotalRecords());
              return GetExperimentRunsByDatasetVersionId.Response.newBuilder()
                  .addAllExperimentRuns(findExperimentRunsResponse.getExperimentRunsList())
                  .setTotalRecords(findExperimentRunsResponse.getTotalRecords())
                  .build();
            },
            executor);
  }

  public InternalFuture<ExperimentRun> updateExperimentRunDescription(
      UpdateExperimentRunDescription request) {
    final var runId = request.getId();
    final var description = request.getDescription();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenAccept(
            unused ->
                jdbi.useHandle(
                    handle ->
                        handle
                            .createUpdate(
                                "UPDATE experiment_run SET description = :description WHERE id = :id")
                            .bind("id", runId)
                            .bind("description", description)
                            .execute()),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor)
        .thenCompose(unused -> updateVersionNumber(runId), executor)
        .thenCompose(unused -> getExperimentRunById(runId), executor);
  }

  public InternalFuture<GetExperimentRunsInExperiment.Response> getExperimentRunsInExperiment(
      GetExperimentRunsInExperiment request) {
    final var requestValidationFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getExperimentId().isEmpty()) {
                var errorMessage = "Experiment ID not present";
                throw new InvalidArgumentException(errorMessage);
              }
            },
            executor);
    return requestValidationFuture
        .thenCompose(
            unused ->
                jdbi.withHandle(
                    handle ->
                        handle
                            .createQuery("SELECT COUNT(id) FROM experiment WHERE id = :id")
                            .bind("id", request.getExperimentId())
                            .mapTo(Long.class)
                            .one()),
            executor)
        .thenAccept(
            count -> {
              if (count == 0) {
                throw new NotFoundException("Experiment not found");
              }
            },
            executor)
        .thenCompose(
            unused ->
                findExperimentRuns(
                    FindExperimentRuns.newBuilder()
                        .setExperimentId(request.getExperimentId())
                        .setSortKey(request.getSortKey())
                        .setAscending(request.getAscending())
                        .setPageLimit(request.getPageLimit())
                        .setPageNumber(request.getPageNumber())
                        .build()),
            executor)
        .thenApply(
            findResponse ->
                GetExperimentRunsInExperiment.Response.newBuilder()
                    .addAllExperimentRuns(findResponse.getExperimentRunsList())
                    .setTotalRecords(findResponse.getTotalRecords())
                    .build(),
            executor);
  }

  public InternalFuture<CloneExperimentRun.Response> cloneExperimentRun(
      CloneExperimentRun request) {
    var validateRequestParamFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getSrcExperimentRunId().isEmpty()) {
                throw new InvalidArgumentException("Source ExperimentRun Id should not be empty");
              }
            },
            executor);

    return validateRequestParamFuture
        .thenCompose(
            unused ->
                FutureUtil.clientRequest(
                    uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor),
            executor)
        .thenCompose(
            userInfo ->
                findExperimentRuns(
                        FindExperimentRuns.newBuilder()
                            .addExperimentRunIds(request.getSrcExperimentRunId())
                            .build())
                    .thenApply(
                        findExperimentRuns -> {
                          if (findExperimentRuns.getExperimentRunsList().isEmpty()) {
                            throw new NotFoundException("Source experiment run not found");
                          }
                          var srcExperimentRun = findExperimentRuns.getExperimentRuns(0);
                          return srcExperimentRun.toBuilder().clone();
                        },
                        executor)
                    .thenCompose(
                        experimentRun ->
                            findExperimentRuns(
                                    FindExperimentRuns.newBuilder()
                                        .setIdsOnly(true)
                                        .setProjectId(experimentRun.getProjectId())
                                        .build())
                                .thenApply(runsResponse -> experimentRun, executor),
                        executor)
                    .thenCompose(
                        cloneExperimentRunBuilder ->
                            checkPermissionAndPopulateFieldsBasedOnDestExperimentId(
                                request, cloneExperimentRunBuilder),
                        executor)
                    .thenCompose(
                        desExperimentRunBuilder -> {
                          desExperimentRunBuilder
                              .setId(UUID.randomUUID().toString())
                              .setDateCreated(Calendar.getInstance().getTimeInMillis())
                              .setDateUpdated(Calendar.getInstance().getTimeInMillis())
                              .setStartTime(Calendar.getInstance().getTimeInMillis())
                              .setEndTime(Calendar.getInstance().getTimeInMillis());

                          if (!request.getDestExperimentRunName().isEmpty()) {
                            desExperimentRunBuilder.setName(request.getDestExperimentRunName());
                          } else {
                            desExperimentRunBuilder.setName(
                                desExperimentRunBuilder.getName() + " - " + new Date().getTime());
                          }

                          if (userInfo != null) {
                            desExperimentRunBuilder
                                .clearOwner()
                                .setOwner(userInfo.getVertaInfo().getUserId());
                          }
                          return createExperimentRunHandler
                              .insertExperimentRun(desExperimentRunBuilder.build())
                              .thenApply(
                                  unused1 ->
                                      CloneExperimentRun.Response.newBuilder()
                                          .setRun(desExperimentRunBuilder.build())
                                          .build(),
                                  executor);
                        },
                        executor),
            executor);
  }

  private InternalFuture<ExperimentRun.Builder>
      checkPermissionAndPopulateFieldsBasedOnDestExperimentId(
          CloneExperimentRun request, ExperimentRun.Builder cloneExperimentRunBuilder) {
    if (!request.getDestExperimentId().isEmpty()) {
      if (!cloneExperimentRunBuilder.getExperimentId().equals(request.getDestExperimentId())) {
        return jdbi.withHandle(
                handle ->
                    handle
                        .createQuery("SELECT project_id FROM experiment where id = :id")
                        .bind("id", request.getDestExperimentId())
                        .mapTo(String.class)
                        .findOne())
            .thenApply(
                projectId -> {
                  if (projectId.isEmpty()) {
                    throw new NotFoundException(
                        "Experiment not found for given ID: " + request.getDestExperimentId());
                  }
                  return projectId.get();
                },
                executor)
            .thenCompose(
                projectId ->
                    getEntityPermissionBasedOnResourceTypes(
                            Collections.singletonList(projectId),
                            ModelDBActionEnum.ModelDBServiceActions.UPDATE,
                            ModelDBServiceResourceTypes.PROJECT)
                        .thenCompose(
                            allowed -> {
                              if (!allowed) {
                                throw new PermissionDeniedException(
                                    "Destination project is not accessible");
                              }
                              return InternalFuture.completedInternalFuture(projectId);
                            },
                            executor),
                executor)
            .thenApply(
                projectId ->
                    cloneExperimentRunBuilder
                        .setExperimentId(request.getDestExperimentId())
                        .setProjectId(projectId),
                executor);
      }
    }
    return InternalFuture.completedInternalFuture(cloneExperimentRunBuilder);
  }

  private InternalFuture<ExperimentRun> getExperimentRunById(String experimentRunId) {
    try {
      var validateArgumentFuture =
          InternalFuture.runAsync(
              () -> {
                if (experimentRunId.isEmpty()) {
                  throw new InvalidArgumentException("ExperimentRun ID not present");
                }
              },
              executor);
      return validateArgumentFuture
          .thenCompose(
              unused ->
                  findExperimentRuns(
                      FindExperimentRuns.newBuilder()
                          .addExperimentRunIds(experimentRunId)
                          .setPageLimit(1)
                          .setPageNumber(1)
                          .build()),
              executor)
          .thenApply(
              response -> {
                if (response.getExperimentRunsList().isEmpty()) {
                  throw new NotFoundException("ExperimentRun not found for given Id");
                } else if (response.getExperimentRunsCount() > 1) {
                  throw new InternalErrorException("More then one experiments found");
                }
                return response.getExperimentRuns(0);
              },
              executor);
    } catch (Exception e) {
      return InternalFuture.failedStage(e);
    }
  }

  public InternalFuture<String> getProjectIdByExperimentRunId(String experimentRunId) {
    return jdbi.withHandle(
        handle -> {
          try (var query =
              handle.createQuery("SELECT project_id from experiment_run where id = :id ")) {
            var projectId = query.bind("id", experimentRunId).mapTo(String.class).findOne();
            return projectId.orElse("");
          }
        });
  }

  public void deleteLogVersionedInputs(Session session, List<Long> repoIds) {
    String fetchAllExpRunLogVersionedInputsHqlBuilder =
        String.format(
            "DELETE FROM VersioningModeldbEntityMapping vm WHERE vm.repository_id IN (:repoIds) "
                + " AND vm.entity_type = '%s'",
            ExperimentRunEntity.class.getSimpleName());
    var query =
        session
            .createQuery(fetchAllExpRunLogVersionedInputsHqlBuilder)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    query.setParameter("repoIds", repoIds);
    query.executeUpdate();
    LOGGER.debug("ExperimentRun versioning deleted successfully");
  }
}
