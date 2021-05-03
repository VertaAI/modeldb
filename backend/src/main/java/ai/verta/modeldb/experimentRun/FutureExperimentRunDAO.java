package ai.verta.modeldb.experimentRun;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
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
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.EnumerateList;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.CreateExperimentRunHandler;
import ai.verta.modeldb.experimentRun.subtypes.DatasetHandler;
import ai.verta.modeldb.experimentRun.subtypes.FeatureHandler;
import ai.verta.modeldb.experimentRun.subtypes.FilterPrivilegedDatasetsHandler;
import ai.verta.modeldb.experimentRun.subtypes.KeyValueHandler;
import ai.verta.modeldb.experimentRun.subtypes.ObservationHandler;
import ai.verta.modeldb.experimentRun.subtypes.PredicatesHandler;
import ai.verta.modeldb.experimentRun.subtypes.SortingHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.uac.Action;
import ai.verta.uac.GetSelfAllowedResources;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ResourceType;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
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
  private final CodeVersionHandler codeVersionHandler;
  private final DatasetHandler datasetHandler;
  private final PredicatesHandler predicatesHandler;
  private final SortingHandler sortingHandler;
  private final FeatureHandler featureHandler;
  private final FilterPrivilegedDatasetsHandler privilegedDatasetsHandler;
  private final CreateExperimentRunHandler createExperimentRunHandler;

  public FutureExperimentRunDAO(
      Executor executor,
      FutureJdbi jdbi,
      UAC uac,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetVersionDAO datasetVersionDAO) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;

    attributeHandler = new AttributeHandler(executor, jdbi, "ExperimentRunEntity");
    hyperparametersHandler =
        new KeyValueHandler(executor, jdbi, "hyperparameters", "ExperimentRunEntity");
    metricsHandler = new KeyValueHandler(executor, jdbi, "metrics", "ExperimentRunEntity");
    observationHandler = new ObservationHandler(executor, jdbi);
    tagsHandler = new TagsHandler(executor, jdbi, "ExperimentRunEntity");
    codeVersionHandler = new CodeVersionHandler(executor, jdbi);
    datasetHandler = new DatasetHandler(executor, jdbi, "ExperimentRunEntity");
    artifactHandler =
        new ArtifactHandler(
            executor,
            jdbi,
            "ExperimentRunEntity",
            codeVersionHandler,
            datasetHandler,
            artifactStoreDAO,
            datasetVersionDAO);
    predicatesHandler = new PredicatesHandler();
    sortingHandler = new SortingHandler();
    featureHandler = new FeatureHandler(executor, jdbi, "ExperimentRunEntity");
    privilegedDatasetsHandler = new FilterPrivilegedDatasetsHandler(executor, jdbi);
    createExperimentRunHandler =
        new CreateExperimentRunHandler(
            executor,
            jdbi,
            uac,
            attributeHandler,
            hyperparametersHandler,
            metricsHandler,
            observationHandler,
            tagsHandler,
            artifactHandler,
            featureHandler,
            datasetHandler);
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

  public interface CheckPermissionBasedOnResourceTypesFunction {
    InternalFuture<Void> checkPermission(
        List<String> projId,
        ModelDBActionEnum.ModelDBServiceActions action,
        ModelDBServiceResourceTypes modelDBServiceResourceTypes);
  }

  private InternalFuture<Void> checkEntityPermissionBasedOnResourceTypes(
      List<String> entityIds,
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
                                .addAllResourceIds(entityIds))
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
              return checkEntityPermissionBasedOnResourceTypes(
                  maybeProjectIds,
                  ModelDBActionEnum.ModelDBServiceActions.UPDATE,
                  ModelDBServiceResourceTypes.PROJECT);
            default:
              return checkEntityPermissionBasedOnResourceTypes(
                  maybeProjectIds, action, ModelDBServiceResourceTypes.PROJECT);
          }
        },
        executor);
  }

  private InternalFuture<List<String>> getAllowedProjects(
      ModelDBActionEnum.ModelDBServiceActions action) {
    return FutureGrpc.ClientRequest(
            uac.getAuthzService()
                .getSelfAllowedResources(
                    GetSelfAllowedResources.newBuilder()
                        .addActions(
                            Action.newBuilder()
                                .setModeldbServiceAction(action)
                                .setService(ServiceEnum.Service.MODELDB_SERVICE))
                        .setService(ServiceEnum.Service.MODELDB_SERVICE)
                        .setResourceType(
                            ResourceType.newBuilder()
                                .setModeldbServiceResourceType(
                                    ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT))
                        .build()),
            executor)
        .thenApply(
            response ->
                response.getResourcesList().stream()
                    .flatMap(x -> x.getResourceIdsList().stream())
                    .collect(Collectors.toList()),
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

  public InternalFuture<Void> logArtifacts(LogArtifacts request) {
    final var runId = request.getId();
    final var artifacts = request.getArtifactsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> artifactHandler.logArtifacts(runId, artifacts, false), executor)
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

  public InternalFuture<Void> logDatasets(LogDatasets request) {
    final var runId = request.getId();
    final var artifacts = request.getDatasetsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(
            unused -> datasetHandler.logArtifacts(runId, artifacts, request.getOverwrite()),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<List<Artifact>> getDatasets(GetDatasets request) {
    final var runId = request.getId();

    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> datasetHandler.getArtifacts(runId, Optional.empty()), executor);
  }

  public InternalFuture<Void> logCodeVersion(LogExperimentRunCodeVersion request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();
    return checkPermission(
            Collections.singletonList(runId), ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> codeVersionHandler.logCodeVersion(request), executor)
        .thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
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
    // TODO: filter by workspace

    final var futureLocalContext =
        InternalFuture.supplyAsync(
            () -> {
              final var localQueryContext = new QueryFilterContext();

              if (!request.getProjectId().isEmpty()) {
                localQueryContext.conditions.add("experiment_run.project_id=:request_project_id");
                localQueryContext.binds.add(
                    q -> q.bind("request_project_id", request.getProjectId()));
              }

              if (!request.getExperimentId().isEmpty()) {
                localQueryContext.conditions.add(
                    "experiment_run.experiment_id=:request_experiment_id");
                localQueryContext.binds.add(
                    q -> q.bind("request_experiment_id", request.getExperimentId()));
              }

              if (!request.getExperimentRunIdsList().isEmpty()) {
                localQueryContext.conditions.add(
                    "experiment_run.id in (<request_experiment_run_ids>)");
                localQueryContext.binds.add(
                    q ->
                        q.bindList(
                            "request_experiment_run_ids", request.getExperimentRunIdsList()));
              }

              return localQueryContext;
            },
            executor);

    final var futurePredicatesContext =
        predicatesHandler.processPredicates(request.getPredicatesList(), executor);

    final var futureSortingContext =
        sortingHandler.processSort(request.getSortKey(), request.getAscending());

    final var futureProjectIds =
        getAllowedProjects(ModelDBActionEnum.ModelDBServiceActions.READ)
            .thenApply(
                projIds ->
                    new QueryFilterContext()
                        .addCondition("experiment_run.project_id in (<authz_project_ids>)")
                        .addBind(q -> q.bindList("authz_project_ids", projIds)),
                executor);

    final var futureExperimentRuns =
        InternalFuture.sequence(
                Arrays.asList(
                    futureLocalContext,
                    futurePredicatesContext,
                    futureSortingContext,
                    futureProjectIds),
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
                                "select experiment_run.id, experiment_run.date_created, experiment_run.date_updated, experiment_run.experiment_id, experiment_run.name, experiment_run.project_id, experiment_run.description, experiment_run.start_time, experiment_run.end_time, experiment_run.owner, experiment_run.environment, experiment_run.code_version, experiment_run.job_id from experiment_run";

                            // Add the sorting tables
                            for (final var item :
                                new EnumerateList<>(queryContext.orderItems).getList()) {
                              if (item.getValue().getTable() != null) {
                                sql +=
                                    String.format(
                                        " left join (%s) as join_table_%d on experiment_run.id=join_table_%d.id ",
                                        item.getValue().getTable(),
                                        item.getIndex(),
                                        item.getIndex());
                              }
                            }

                            if (!queryContext.conditions.isEmpty()) {
                              sql += " WHERE " + String.join(" AND ", queryContext.conditions);
                            }

                            if (!queryContext.orderItems.isEmpty()) {
                              sql += " ORDER BY ";
                              for (final var item :
                                  new EnumerateList<>(queryContext.orderItems).getList()) {
                                if (item.getValue().getTable() != null) {
                                  sql += String.format(" join_table_%d.value ", item.getIndex());
                                } else if (item.getValue().getColumn() != null) {
                                  sql += String.format(" %s ", item.getValue().getColumn());
                                }
                                sql +=
                                    String.format(
                                        " %s ", item.getValue().getAscending() ? "ASC" : "DESC");
                              }
                            }

                            // Backwards compatibility: fetch everything
                            if (request.getPageNumber() != 0 && request.getPageLimit() != 0) {
                              final var offset =
                                  (request.getPageNumber() - 1) * request.getPageLimit();
                              final var limit = request.getPageLimit();
                              sql += " LIMIT :limit OFFSET :offset";
                              queryContext.addBind(q -> q.bind("limit", limit));
                              queryContext.addBind(q -> q.bind("offset", offset));
                            }

                            var query = handle.createQuery(sql);
                            queryContext.binds.forEach(b -> b.accept(query));

                            return query
                                .map(
                                    (rs, ctx) -> {
                                      ExperimentRun.Builder runBuilder =
                                          ExperimentRun.newBuilder()
                                              .setId(rs.getString("experiment_run.id"))
                                              .setProjectId(
                                                  rs.getString("experiment_run.project_id"))
                                              .setExperimentId(
                                                  rs.getString("experiment_run.experiment_id"))
                                              .setName(rs.getString("experiment_run.name"))
                                              .setDescription(
                                                  rs.getString("experiment_run.description"))
                                              .setDateUpdated(
                                                  rs.getLong("experiment_run.date_updated"))
                                              .setDateCreated(
                                                  rs.getLong("experiment_run.date_created"))
                                              .setStartTime(rs.getLong("experiment_run.start_time"))
                                              .setEndTime(rs.getLong("experiment_run.end_time"))
                                              .setOwner(rs.getString("experiment_run.owner"))
                                              .setCodeVersion(
                                                  rs.getString("experiment_run.code_version"))
                                              .setJobId(rs.getString("experiment_run.job_id"));

                                      var environment = rs.getString("experiment_run.environment");
                                      if (environment != null && !environment.isEmpty()) {
                                        EnvironmentBlob.Builder environmentBlobBuilder =
                                            EnvironmentBlob.newBuilder();
                                        try {
                                          CommonUtils.getProtoObjectFromString(
                                              environment, environmentBlobBuilder);
                                        } catch (InvalidProtocolBufferException e) {
                                          LOGGER.error("Error generating builder for environment");
                                          throw new ModelDBException(e);
                                        }
                                        runBuilder.setEnvironment(environmentBlobBuilder.build());
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
                                builders.stream().map(x -> x.getId()).collect(Collectors.toSet());

                            // Get tags
                            final var futureTags = tagsHandler.getTagsMap(ids);
                            futureBuildersStream =
                                futureBuildersStream.thenCombine(
                                    futureTags,
                                    (stream, tags) ->
                                        stream.map(
                                            builder ->
                                                builder.addAllTags(tags.get(builder.getId()))),
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
                            final var futureAttributes = attributeHandler.getKeyValuesMap(ids);
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
                            final var futureArtifacts = artifactHandler.getArtifactsMap(ids);
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
                            final var futureDatasetsMap = datasetHandler.getArtifactsMap(ids);
                            final var filterDatasetsMap =
                                futureDatasetsMap.thenCompose(
                                    artifactMapSubtypes -> {
                                      List<InternalFuture<Map<String, List<Artifact>>>>
                                          internalFutureList = new ArrayList<>();
                                      for (ExperimentRun.Builder builder : builders) {
                                        internalFutureList.add(
                                            privilegedDatasetsHandler
                                                .filterAndGetPrivilegedDatasetsOnly(
                                                    artifactMapSubtypes.get(builder.getId()),
                                                    true,
                                                    this::checkEntityPermissionBasedOnResourceTypes)
                                                .thenCompose(
                                                    artifacts ->
                                                        InternalFuture.completedInternalFuture(
                                                            Collections.singletonMap(
                                                                builder.getId(), artifacts)),
                                                    executor));
                                      }
                                      return InternalFuture.sequence(internalFutureList, executor)
                                          .thenCompose(
                                              maps -> {
                                                Map<String, List<Artifact>> finalDatasetMap =
                                                    new HashMap<>();
                                                maps.forEach(finalDatasetMap::putAll);
                                                return InternalFuture.completedInternalFuture(
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
                                            builder ->
                                                builder.addAllDatasets(
                                                    datasets.get(builder.getId()))),
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
                                              if (codeVersionsMap.containsKey(builder.getId())) {
                                                builder.setCodeVersionSnapshot(
                                                    codeVersionsMap.get(builder.getId()));
                                              }
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

    final var futureCount =
        InternalFuture.sequence(
                Arrays.asList(futureLocalContext, futurePredicatesContext, futureProjectIds),
                executor)
            .thenApply(QueryFilterContext::combine, executor)
            .thenCompose(
                queryContext ->
                    jdbi.withHandle(
                        handle -> {
                          var sql = "select count(experiment_run.id) from experiment_run";

                          if (!queryContext.conditions.isEmpty()) {
                            sql += " WHERE " + String.join(" AND ", queryContext.conditions);
                          }

                          var query = handle.createQuery(sql);
                          queryContext.binds.forEach(b -> b.accept(query));

                          return query.mapTo(Long.class).one();
                        }),
                executor);

    return futureExperimentRuns.thenCombine(
        futureCount,
        (runs, count) ->
            FindExperimentRuns.Response.newBuilder()
                .addAllExperimentRuns(runs)
                .setTotalRecords(count)
                .build(),
        executor);
  }

  public InternalFuture<ExperimentRun> createExperimentRun(CreateExperimentRun request) {
    return checkEntityPermissionBasedOnResourceTypes(
            Collections.singletonList(request.getProjectId()),
            ModelDBActionEnum.ModelDBServiceActions.UPDATE,
            ModelDBServiceResourceTypes.PROJECT)
        .thenCompose(
            unused ->
                privilegedDatasetsHandler
                    .filterAndGetPrivilegedDatasetsOnly(
                        request.getDatasetsList(),
                        true,
                        this::checkEntityPermissionBasedOnResourceTypes)
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
