package ai.verta.modeldb.experimentRun;

import ai.verta.common.Artifact;
import ai.verta.common.CodeVersion;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.*;
import ai.verta.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceImplBase;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.utils.UACApisUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.Value;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FutureExperimentRunServiceImpl extends ExperimentRunServiceImplBase {
  private static final String DELETE_EXPERIMENT_RUN_EVENT_TYPE =
      "delete.resource.experiment_run.delete_experiment_run_succeeded";
  private final String UPDATE_EVENT_TYPE =
      "update.resource.experiment_run.update_experiment_run_succeeded";
  private final String ADD_EVENT_TYPE = "add.resource.experiment_run.add_experiment_run_succeeded";

  private final FutureExecutor executor;
  private final FutureExperimentRunDAO futureExperimentRunDAO;
  private final FutureEventDAO futureEventDAO;
  private final UACApisUtil uacApisUtil;

  public FutureExperimentRunServiceImpl(DAOSet daoSet, FutureExecutor executor) {
    this.executor = executor;
    this.futureExperimentRunDAO = daoSet.getFutureExperimentRunDAO();
    this.futureEventDAO = daoSet.getFutureEventDAO();
    this.uacApisUtil = daoSet.getUacApisUtil();
  }

  private InternalFuture<Void> addEvent(
      String entityId,
      Optional<String> experimentId,
      String projectId,
      String eventType,
      Optional<String> updatedField,
      Map<String, Object> extraFieldsMap,
      String eventMessage) {

    if (!App.getInstance().mdbConfig.isEvent_system_enabled()) {
      return InternalFuture.completedInternalFuture(null);
    }

    // Add succeeded event in local DB
    JsonObject eventMetadata = new JsonObject();
    eventMetadata.addProperty("entity_id", entityId);
    if (experimentId.isPresent() && !experimentId.get().isEmpty()) {
      eventMetadata.addProperty("experiment_id", experimentId.get());
    }
    eventMetadata.addProperty("project_id", projectId);
    if (updatedField.isPresent() && !updatedField.get().isEmpty()) {
      eventMetadata.addProperty("updated_field", updatedField.get());
    }
    if (extraFieldsMap != null && !extraFieldsMap.isEmpty()) {
      JsonObject updatedFieldValue = new JsonObject();
      extraFieldsMap.forEach(
          (key, value) -> {
            if (value instanceof JsonElement) {
              updatedFieldValue.add(key, (JsonElement) value);
            } else {
              updatedFieldValue.addProperty(key, String.valueOf(value));
            }
          });
      eventMetadata.add("updated_field_value", updatedFieldValue);
    }
    eventMetadata.addProperty("message", eventMessage);

    return uacApisUtil
        .getEntityResource(projectId, ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT)
        .thenCompose(
            projectResource ->
                futureEventDAO.addLocalEventWithAsync(
                    ModelDBResourceEnum.ModelDBServiceResourceTypes.EXPERIMENT_RUN.name(),
                    eventType,
                    projectResource.getWorkspaceId(),
                    eventMetadata),
            executor);
  }

  @Override
  public void createExperimentRun(
      CreateExperimentRun request, StreamObserver<CreateExperimentRun.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .createExperimentRun(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                              experimentRun.getId(),
                              Optional.of(experimentRun.getExperimentId()),
                              experimentRun.getProjectId(),
                              ADD_EVENT_TYPE,
                              Optional.empty(),
                              Collections.emptyMap(),
                              "experiment_run added successfully")
                          .thenApply(unused -> experimentRun, executor),
                  executor)
              .thenApply(
                  experimentRun ->
                      CreateExperimentRun.Response.newBuilder()
                          .setExperimentRun(experimentRun)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentRun(
      DeleteExperimentRun request, StreamObserver<DeleteExperimentRun.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .deleteExperimentRuns(
                  DeleteExperimentRuns.newBuilder().addIds(request.getId()).build())
              .thenCompose(
                  unused ->
                      loggedDeleteExperimentRunEvents(Collections.singletonList(request.getId())),
                  executor)
              .thenApply(
                  unused -> DeleteExperimentRun.Response.newBuilder().setStatus(true).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  private InternalFuture<List<Void>> loggedDeleteExperimentRunEvents(List<String> runIds) {
    // Add succeeded event in local DB
    List<InternalFuture<Void>> futureList = new ArrayList<>();
    Map<String, String> cacheMap = new ConcurrentHashMap<>();
    InternalFuture<String> projectIdFuture;
    for (String runId : runIds) {
      if (cacheMap.containsKey(runId)) {
        projectIdFuture = InternalFuture.completedInternalFuture(cacheMap.get(runId));
      } else {
        projectIdFuture =
            futureExperimentRunDAO
                .getProjectIdByExperimentRunId(runId)
                .thenApply(
                    projectId -> {
                      cacheMap.put(runId, projectId);
                      return projectId;
                    },
                    executor);
      }

      var eventFuture =
          projectIdFuture.thenCompose(
              projectId ->
                  addEvent(
                      runId,
                      Optional.empty(),
                      projectId,
                      DELETE_EXPERIMENT_RUN_EVENT_TYPE,
                      Optional.empty(),
                      Collections.emptyMap(),
                      "experiment_run deleted successfully"),
              executor);
      futureList.add(eventFuture);
    }
    return InternalFuture.sequence(futureList, executor);
  }

  @Override
  public void getExperimentRunsInProject(
      GetExperimentRunsInProject request,
      StreamObserver<GetExperimentRunsInProject.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getProjectId().isEmpty()) {
                  var errorMessage = "Project ID not present";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentRunDAO.findExperimentRuns(
                          FindExperimentRuns.newBuilder()
                              .setProjectId(request.getProjectId())
                              .setPageLimit(request.getPageLimit())
                              .setPageNumber(request.getPageNumber())
                              .setAscending(request.getAscending())
                              .setSortKey(request.getSortKey())
                              .build()),
                  executor)
              .thenApply(
                  findResponse ->
                      GetExperimentRunsInProject.Response.newBuilder()
                          .addAllExperimentRuns(findResponse.getExperimentRunsList())
                          .setTotalRecords(findResponse.getTotalRecords())
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentRunsInExperiment(
      GetExperimentRunsInExperiment request,
      StreamObserver<GetExperimentRunsInExperiment.Response> responseObserver) {
    try {
      final var response = futureExperimentRunDAO.getExperimentRunsInExperiment(request);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentRunById(
      GetExperimentRunById request,
      StreamObserver<GetExperimentRunById.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "ExperimentRun ID not present";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentRunDAO.findExperimentRuns(
                          FindExperimentRuns.newBuilder()
                              .addExperimentRunIds(request.getId())
                              .build()),
                  executor)
              .thenApply(
                  findResponse -> {
                    if (findResponse.getExperimentRunsCount() > 1) {
                      throw new InternalErrorException(
                          "More than one ExperimentRun found for ID: " + request.getId());
                    } else if (findResponse.getExperimentRunsCount() == 0) {
                      throw new NotFoundException(
                          "ExperimentRun not found for the ID: " + request.getId());
                    } else {
                      return GetExperimentRunById.Response.newBuilder()
                          .setExperimentRun(findResponse.getExperimentRuns(0))
                          .build();
                    }
                  },
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentRunByName(
      GetExperimentRunByName request,
      StreamObserver<GetExperimentRunByName.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getName().isEmpty()) {
                  var errorMessage = "ExperimentRun name not present";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getExperimentId().isEmpty()) {
                  var errorMessage = "Experiment ID not present";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentRunDAO.findExperimentRuns(
                          FindExperimentRuns.newBuilder()
                              .setExperimentId(request.getExperimentId())
                              .addPredicates(
                                  KeyValueQuery.newBuilder()
                                      .setKey("name")
                                      .setValue(
                                          Value.newBuilder()
                                              .setStringValue(request.getName())
                                              .build())
                                      .setOperator(OperatorEnum.Operator.EQ)
                                      .setValueType(ValueTypeEnum.ValueType.STRING)
                                      .build())
                              .build()),
                  executor)
              .thenApply(
                  findResponse -> {
                    if (findResponse.getExperimentRunsCount() == 0) {
                      throw new NotFoundException(
                          "ExperimentRun not found for the name: " + request.getName());
                    } else {
                      return GetExperimentRunByName.Response.newBuilder()
                          .setExperimentRun(findResponse.getExperimentRuns(0))
                          .build();
                    }
                  },
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateExperimentRunName(
      UpdateExperimentRunName request,
      StreamObserver<UpdateExperimentRunName.Response> responseObserver) {
    responseObserver.onError(
        Status.UNIMPLEMENTED.withDescription(ModelDBMessages.UNIMPLEMENTED).asRuntimeException());
  }

  @Override
  public void updateExperimentRunDescription(
      UpdateExperimentRunDescription request,
      StreamObserver<UpdateExperimentRunDescription.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .updateExperimentRunDescription(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                              experimentRun.getId(),
                              Optional.of(experimentRun.getExperimentId()),
                              experimentRun.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.of("description"),
                              Collections.emptyMap(),
                              "experiment_run description added successfully")
                          .thenApply(unused -> experimentRun, executor),
                  executor)
              .thenApply(
                  experimentRun ->
                      UpdateExperimentRunDescription.Response.newBuilder()
                          .setExperimentRun(experimentRun)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentRunTags(
      AddExperimentRunTags request,
      StreamObserver<AddExperimentRunTags.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .addTags(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                              experimentRun.getId(),
                              Optional.of(experimentRun.getExperimentId()),
                              experimentRun.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.of("tags"),
                              Collections.singletonMap(
                                  "tags",
                                  new Gson()
                                      .toJsonTree(
                                          request.getTagsList(),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "experiment_run tags added successfully")
                          .thenApply(unused -> experimentRun, executor),
                  executor)
              .thenApply(
                  experimentRun ->
                      AddExperimentRunTags.Response.newBuilder()
                          .setExperimentRun(experimentRun)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentRunTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .getTags(request)
              .thenApply(tags -> GetTags.Response.newBuilder().addAllTags(tags).build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentRunTags(
      DeleteExperimentRunTags request,
      StreamObserver<DeleteExperimentRunTags.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .deleteTags(request)
              .thenCompose(
                  experimentRun -> {
                    // Add succeeded event in local DB
                    Map<String, Object> extraFieldValue = new HashMap<>();
                    if (request.getDeleteAll()) {
                      extraFieldValue.put("tags_deleted_all", true);
                    } else {
                      extraFieldValue.put(
                          "tags",
                          new Gson()
                              .toJsonTree(
                                  request.getTagsList(),
                                  new TypeToken<ArrayList<String>>() {}.getType()));
                    }
                    return addEvent(
                            experimentRun.getId(),
                            Optional.of(experimentRun.getExperimentId()),
                            experimentRun.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("tags"),
                            extraFieldValue,
                            "experiment_run tags deleted successfully")
                        .thenApply(unused -> experimentRun, executor);
                  },
                  executor)
              .thenApply(
                  experimentRun ->
                      DeleteExperimentRunTags.Response.newBuilder()
                          .setExperimentRun(experimentRun)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentRunTag(
      AddExperimentRunTag request, StreamObserver<AddExperimentRunTag.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .addTags(
                  AddExperimentRunTags.newBuilder()
                      .setId(request.getId())
                      .addTags(request.getTag())
                      .build())
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                              experimentRun.getId(),
                              Optional.of(experimentRun.getExperimentId()),
                              experimentRun.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.of("tags"),
                              Collections.singletonMap(
                                  "tags",
                                  new Gson()
                                      .toJsonTree(
                                          Collections.singletonList(request.getTag()),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "experiment_run tag added successfully")
                          .thenApply(eventLoggedStatus -> experimentRun, executor),
                  executor)
              .thenApply(
                  experimentRun ->
                      AddExperimentRunTag.Response.newBuilder()
                          .setExperimentRun(experimentRun)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentRunTag(
      DeleteExperimentRunTag request,
      StreamObserver<DeleteExperimentRunTag.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .deleteTags(
                  DeleteExperimentRunTags.newBuilder()
                      .setId(request.getId())
                      .addTags(request.getTag())
                      .build())
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                              experimentRun.getId(),
                              Optional.of(experimentRun.getExperimentId()),
                              experimentRun.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.of("tags"),
                              Collections.singletonMap(
                                  "tags",
                                  new Gson()
                                      .toJsonTree(
                                          Collections.singletonList(request.getTag()),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "experiment_run tag deleted successfully")
                          .thenApply(eventLoggedStatus -> experimentRun, executor),
                  executor)
              .thenApply(
                  experimentRun ->
                      DeleteExperimentRunTag.Response.newBuilder()
                          .setExperimentRun(experimentRun)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logObservation(
      LogObservation request, StreamObserver<LogObservation.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logObservations(
                  LogObservations.newBuilder()
                      .setId(request.getId())
                      .addObservations(request.getObservation())
                      .build())
              .thenCompose(
                  experimentRun -> {
                    // Add succeeded event in local DB
                    Set<String> keys =
                        Stream.of(request.getObservation())
                            .map(
                                observation -> {
                                  if (observation.hasAttribute()) {
                                    return observation.getAttribute().getKey();
                                  }
                                  return observation.getArtifact().getKey();
                                })
                            .collect(Collectors.toSet());
                    return addEvent(
                        experimentRun.getId(),
                        Optional.of(experimentRun.getExperimentId()),
                        experimentRun.getProjectId(),
                        UPDATE_EVENT_TYPE,
                        Optional.of("observations"),
                        Collections.singletonMap(
                            "observations",
                            new Gson()
                                .toJsonTree(keys, new TypeToken<ArrayList<String>>() {}.getType())),
                        "experiment_run observation added successfully");
                  },
                  executor)
              .thenApply(unused -> LogObservation.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logObservations(
      LogObservations request, StreamObserver<LogObservations.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logObservations(request)
              .thenCompose(
                  experimentRun -> {
                    // Add succeeded event in local DB
                    Set<String> keys =
                        request.getObservationsList().stream()
                            .map(
                                observation -> {
                                  if (observation.hasAttribute()) {
                                    return observation.getAttribute().getKey();
                                  }
                                  return observation.getArtifact().getKey();
                                })
                            .collect(Collectors.toSet());
                    return addEvent(
                        experimentRun.getId(),
                        Optional.of(experimentRun.getExperimentId()),
                        experimentRun.getProjectId(),
                        UPDATE_EVENT_TYPE,
                        Optional.of("observations"),
                        Collections.singletonMap(
                            "observations",
                            new Gson()
                                .toJsonTree(keys, new TypeToken<ArrayList<String>>() {}.getType())),
                        "experiment_run observations added successfully");
                  },
                  executor)
              .thenApply(unused -> LogObservations.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getObservations(
      GetObservations request, StreamObserver<GetObservations.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .getObservations(request)
              .thenApply(
                  observations ->
                      GetObservations.Response.newBuilder()
                          .addAllObservations(observations)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteObservations(
      DeleteObservations request, StreamObserver<DeleteObservations.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .deleteObservations(request)
              .thenCompose(
                  experimentRun -> {
                    // Add succeeded event in local DB
                    Map<String, Object> extraFieldValue = new HashMap<>();
                    if (request.getDeleteAll()) {
                      extraFieldValue.put("observations_deleted_all", true);
                    } else {
                      extraFieldValue.put(
                          "observation_keys",
                          new Gson()
                              .toJsonTree(
                                  request.getObservationKeysList(),
                                  new TypeToken<ArrayList<String>>() {}.getType()));
                    }
                    return addEvent(
                        experimentRun.getId(),
                        Optional.of(experimentRun.getExperimentId()),
                        experimentRun.getProjectId(),
                        UPDATE_EVENT_TYPE,
                        Optional.of("observations"),
                        extraFieldValue,
                        "experiment_run observations deleted successfully");
                  },
                  executor)
              .thenApply(unused -> DeleteObservations.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logMetric(LogMetric request, StreamObserver<LogMetric.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logMetrics(
                  LogMetrics.newBuilder()
                      .setId(request.getId())
                      .addMetrics(request.getMetric())
                      .build())
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("metrics"),
                          Collections.singletonMap(
                              "metrics",
                              new Gson()
                                  .toJsonTree(
                                      Stream.of(request.getMetric())
                                          .map(KeyValue::getKey)
                                          .collect(Collectors.toSet()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment_run metric added successfully"),
                  executor)
              .thenApply(unused -> LogMetric.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logMetrics(LogMetrics request, StreamObserver<LogMetrics.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logMetrics(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("metrics"),
                          Collections.singletonMap(
                              "metrics",
                              new Gson()
                                  .toJsonTree(
                                      request.getMetricsList().stream()
                                          .map(KeyValue::getKey)
                                          .collect(Collectors.toSet()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment_run metrics added successfully"),
                  executor)
              .thenApply(unused -> LogMetrics.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getMetrics(GetMetrics request, StreamObserver<GetMetrics.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .getMetrics(request)
              .thenApply(
                  metrics -> GetMetrics.Response.newBuilder().addAllMetrics(metrics).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteMetrics(
      DeleteMetrics request, StreamObserver<DeleteMetrics.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .deleteMetrics(request)
              .thenCompose(
                  experimentRun -> {
                    // Add succeeded event in local DB
                    Map<String, Object> extraFieldValue = new HashMap<>();
                    if (request.getDeleteAll()) {
                      extraFieldValue.put("metrics_deleted_all", true);
                    } else {
                      extraFieldValue.put(
                          "metric_keys",
                          new Gson()
                              .toJsonTree(
                                  request.getMetricKeysList(),
                                  new TypeToken<ArrayList<String>>() {}.getType()));
                    }
                    return addEvent(
                        experimentRun.getId(),
                        Optional.of(experimentRun.getExperimentId()),
                        experimentRun.getProjectId(),
                        UPDATE_EVENT_TYPE,
                        Optional.of("metrics"),
                        extraFieldValue,
                        "experiment_run metrics deleted successfully");
                  },
                  executor)
              .thenApply(unused -> DeleteMetrics.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logDataset(LogDataset request, StreamObserver<LogDataset.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logDatasets(
                  LogDatasets.newBuilder()
                      .setId(request.getId())
                      .addDatasets(request.getDataset())
                      .setOverwrite(request.getOverwrite())
                      .build())
              .thenCompose(
                  experimentRun -> {
                    // Add succeeded event in local DB
                    Map<String, Object> extraFieldValue = new HashMap<>();
                    if (request.getOverwrite()) {
                      extraFieldValue.put("datasets_overwrite_all", true);
                    } else {
                      extraFieldValue.put(
                          "dataset_keys",
                          new Gson()
                              .toJsonTree(
                                  Stream.of(request.getDataset())
                                      .map(Artifact::getKey)
                                      .collect(Collectors.toSet()),
                                  new TypeToken<ArrayList<String>>() {}.getType()));
                    }
                    return addEvent(
                        experimentRun.getId(),
                        Optional.of(experimentRun.getExperimentId()),
                        experimentRun.getProjectId(),
                        UPDATE_EVENT_TYPE,
                        Optional.of("datasets"),
                        extraFieldValue,
                        "experiment_run datasets added successfully");
                  },
                  executor)
              .thenApply(unused -> LogDataset.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logDatasets(
      LogDatasets request, StreamObserver<LogDatasets.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logDatasets(request)
              .thenCompose(
                  experimentRun -> {
                    // Add succeeded event in local DB
                    Map<String, Object> extraFieldValue = new HashMap<>();
                    if (request.getOverwrite()) {
                      extraFieldValue.put("datasets_overwrite_all", true);
                    } else {
                      extraFieldValue.put(
                          "dataset_keys",
                          new Gson()
                              .toJsonTree(
                                  request.getDatasetsList().stream()
                                      .map(Artifact::getKey)
                                      .collect(Collectors.toSet()),
                                  new TypeToken<ArrayList<String>>() {}.getType()));
                    }
                    return addEvent(
                        experimentRun.getId(),
                        Optional.of(experimentRun.getExperimentId()),
                        experimentRun.getProjectId(),
                        UPDATE_EVENT_TYPE,
                        Optional.of("datasets"),
                        extraFieldValue,
                        "experiment_run datasets added successfully");
                  },
                  executor)
              .thenApply(unused -> LogDatasets.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getDatasets(
      GetDatasets request, StreamObserver<GetDatasets.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .getDatasets(request)
              .thenApply(
                  datasets -> GetDatasets.Response.newBuilder().addAllDatasets(datasets).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logHyperparameter(
      LogHyperparameter request, StreamObserver<LogHyperparameter.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logHyperparameters(
                  LogHyperparameters.newBuilder()
                      .setId(request.getId())
                      .addHyperparameters(request.getHyperparameter())
                      .build())
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("hyperparameters"),
                          Collections.singletonMap(
                              "hyperparameters",
                              new Gson()
                                  .toJsonTree(
                                      Stream.of(request.getHyperparameter())
                                          .map(KeyValue::getKey)
                                          .collect(Collectors.toSet()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment_run hyperparameter added successfully"),
                  executor)
              .thenApply(unused -> LogHyperparameter.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logHyperparameters(
      LogHyperparameters request, StreamObserver<LogHyperparameters.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logHyperparameters(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("hyperparameters"),
                          Collections.singletonMap(
                              "hyperparameters",
                              new Gson()
                                  .toJsonTree(
                                      request.getHyperparametersList().stream()
                                          .map(KeyValue::getKey)
                                          .collect(Collectors.toSet()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment_run hyperparameters added successfully"),
                  executor)
              .thenApply(unused -> LogHyperparameters.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getHyperparameters(
      GetHyperparameters request, StreamObserver<GetHyperparameters.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .getHyperparameters(request)
              .thenApply(
                  hyperparameters ->
                      GetHyperparameters.Response.newBuilder()
                          .addAllHyperparameters(hyperparameters)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteHyperparameters(
      DeleteHyperparameters request,
      StreamObserver<DeleteHyperparameters.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .deleteHyperparameters(request)
              .thenCompose(
                  experimentRun -> {
                    // Add succeeded event in local DB
                    Map<String, Object> extraFieldValue = new HashMap<>();
                    if (request.getDeleteAll()) {
                      extraFieldValue.put("hyperparameters_deleted_all", true);
                    } else {
                      extraFieldValue.put(
                          "hyperparameter_keys",
                          new Gson()
                              .toJsonTree(
                                  request.getHyperparameterKeysList(),
                                  new TypeToken<ArrayList<String>>() {}.getType()));
                    }
                    return addEvent(
                        experimentRun.getId(),
                        Optional.of(experimentRun.getExperimentId()),
                        experimentRun.getProjectId(),
                        UPDATE_EVENT_TYPE,
                        Optional.of("hyperparameters"),
                        extraFieldValue,
                        "experiment_run hyperparameters deleted successfully");
                  },
                  executor)
              .thenApply(unused -> DeleteHyperparameters.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logAttribute(
      LogAttribute request, StreamObserver<LogAttribute.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logAttributes(
                  LogAttributes.newBuilder()
                      .setId(request.getId())
                      .addAttributes(request.getAttribute())
                      .build())
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("attributes"),
                          Collections.singletonMap(
                              "attributes",
                              new Gson()
                                  .toJsonTree(
                                      Stream.of(request.getAttribute())
                                          .map(KeyValue::getKey)
                                          .collect(Collectors.toSet()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment_run attribute added successfully"),
                  executor)
              .thenApply(unused -> LogAttribute.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logAttributes(
      LogAttributes request, StreamObserver<LogAttributes.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logAttributes(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("attributes"),
                          Collections.singletonMap(
                              "attributes",
                              new Gson()
                                  .toJsonTree(
                                      request.getAttributesList().stream()
                                          .map(KeyValue::getKey)
                                          .collect(Collectors.toSet()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment_run attributes added successfully"),
                  executor)
              .thenApply(unused -> LogAttributes.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentRunAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .getAttributes(request)
              .thenApply(
                  attributes ->
                      GetAttributes.Response.newBuilder().addAllAttributes(attributes).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentRunAttributes(
      AddExperimentRunAttributes request,
      StreamObserver<AddExperimentRunAttributes.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logAttributes(
                  LogAttributes.newBuilder()
                      .setId(request.getId())
                      .addAllAttributes(request.getAttributesList())
                      .build())
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("attributes"),
                          Collections.singletonMap(
                              "attributes",
                              new Gson()
                                  .toJsonTree(
                                      request.getAttributesList().stream()
                                          .map(KeyValue::getKey)
                                          .collect(Collectors.toSet()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment_run attributes added successfully"),
                  executor)
              .thenApply(
                  unused -> AddExperimentRunAttributes.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentRunAttributes(
      DeleteExperimentRunAttributes request,
      StreamObserver<DeleteExperimentRunAttributes.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .deleteAttributes(request)
              .thenCompose(
                  experimentRun -> {
                    // Add succeeded event in local DB
                    Map<String, Object> extraFieldValue = new HashMap<>();
                    if (request.getDeleteAll()) {
                      extraFieldValue.put("attributes_deleted_all", true);
                    } else {
                      extraFieldValue.put(
                          "attribute_keys",
                          new Gson()
                              .toJsonTree(
                                  request.getAttributeKeysList(),
                                  new TypeToken<ArrayList<String>>() {}.getType()));
                    }
                    return addEvent(
                        experimentRun.getId(),
                        Optional.of(experimentRun.getExperimentId()),
                        experimentRun.getProjectId(),
                        UPDATE_EVENT_TYPE,
                        Optional.of("attributes"),
                        extraFieldValue,
                        "experiment_run attributes deleted successfully");
                  },
                  executor)
              .thenApply(
                  unused -> DeleteExperimentRunAttributes.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logEnvironment(
      LogEnvironment request, StreamObserver<LogEnvironment.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logEnvironment(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("environment"),
                          Collections.emptyMap(),
                          "experiment_run environment added successfully"),
                  executor)
              .thenApply(unused -> LogEnvironment.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logExperimentRunCodeVersion(
      LogExperimentRunCodeVersion request,
      StreamObserver<LogExperimentRunCodeVersion.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logCodeVersion(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("code_version"),
                          Collections.emptyMap(),
                          "experiment_run code_version added successfully"),
                  executor)
              .thenApply(
                  unused -> LogExperimentRunCodeVersion.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentRunCodeVersion(
      GetExperimentRunCodeVersion request,
      StreamObserver<GetExperimentRunCodeVersion.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .getCodeVersion(request)
              .thenApply(
                  codeVersion ->
                      GetExperimentRunCodeVersion.Response.newBuilder()
                          .setCodeVersion(
                              codeVersion.orElseGet(() -> CodeVersion.newBuilder().build()))
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logArtifact(
      LogArtifact request, StreamObserver<LogArtifact.Response> responseObserver) {
    try {
      final var futureResponse =
          futureExperimentRunDAO
              .logArtifacts(
                  LogArtifacts.newBuilder()
                      .setId(request.getId())
                      .addArtifacts(request.getArtifact())
                      .build())
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("artifacts"),
                          Collections.singletonMap(
                              "artifacts",
                              new Gson()
                                  .toJsonTree(
                                      Stream.of(request.getArtifact())
                                          .map(Artifact::getKey)
                                          .collect(Collectors.toSet()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment_run artifact added successfully"),
                  executor)
              .thenApply(unused -> LogArtifact.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logArtifacts(
      LogArtifacts request, StreamObserver<LogArtifacts.Response> responseObserver) {
    try {
      final var futureResponse =
          futureExperimentRunDAO
              .logArtifacts(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("artifacts"),
                          Collections.singletonMap(
                              "artifacts",
                              new Gson()
                                  .toJsonTree(
                                      request.getArtifactsList().stream()
                                          .map(Artifact::getKey)
                                          .collect(Collectors.toSet()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment_run artifacts added successfully"),
                  executor)
              .thenApply(unused -> LogArtifacts.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {
      final var futureResponse =
          futureExperimentRunDAO
              .getArtifacts(request)
              .thenApply(
                  artifacts ->
                      GetArtifacts.Response.newBuilder().addAllArtifacts(artifacts).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteArtifact(
      DeleteArtifact request, StreamObserver<DeleteArtifact.Response> responseObserver) {
    try {
      final var futureResponse =
          futureExperimentRunDAO
              .deleteArtifacts(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("artifacts"),
                          Collections.singletonMap(
                              "artifacts",
                              new Gson()
                                  .toJsonTree(
                                      Collections.singletonList(request.getKey()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment_run artifact deleted successfully"),
                  executor)
              .thenApply(deleted -> DeleteArtifact.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    try {
      final var futureResponse = futureExperimentRunDAO.getUrlForArtifact(request);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void commitArtifactPart(
      CommitArtifactPart request, StreamObserver<CommitArtifactPart.Response> responseObserver) {
    try {
      final var futureResponse =
          futureExperimentRunDAO
              .commitArtifactPart(request)
              .thenApply(unused -> CommitArtifactPart.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getCommittedArtifactParts(
      GetCommittedArtifactParts request,
      StreamObserver<GetCommittedArtifactParts.Response> responseObserver) {
    try {
      final var futureResponse = futureExperimentRunDAO.getCommittedArtifactParts(request);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void commitMultipartArtifact(
      CommitMultipartArtifact request,
      StreamObserver<CommitMultipartArtifact.Response> responseObserver) {
    try {
      final var futureResponse =
          futureExperimentRunDAO
              .commitMultipartArtifact(request)
              .thenApply(unused -> CommitMultipartArtifact.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void findExperimentRuns(
      FindExperimentRuns request, StreamObserver<FindExperimentRuns.Response> responseObserver) {
    try {
      final var futureResponse = futureExperimentRunDAO.findExperimentRuns(request);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void sortExperimentRuns(
      SortExperimentRuns request, StreamObserver<SortExperimentRuns.Response> responseObserver) {
    responseObserver.onError(
        Status.UNIMPLEMENTED.withDescription(ModelDBMessages.UNIMPLEMENTED).asRuntimeException());
  }

  @Override
  public void getTopExperimentRuns(
      TopExperimentRunsSelector request,
      StreamObserver<TopExperimentRunsSelector.Response> responseObserver) {
    responseObserver.onError(
        Status.UNIMPLEMENTED.withDescription(ModelDBMessages.UNIMPLEMENTED).asRuntimeException());
  }

  @Override
  public void logJobId(LogJobId request, StreamObserver<LogJobId.Response> responseObserver) {
    responseObserver.onError(
        Status.UNIMPLEMENTED.withDescription(ModelDBMessages.UNIMPLEMENTED).asRuntimeException());
  }

  @Override
  public void getJobId(GetJobId request, StreamObserver<GetJobId.Response> responseObserver) {
    responseObserver.onError(
        Status.UNIMPLEMENTED.withDescription(ModelDBMessages.UNIMPLEMENTED).asRuntimeException());
  }

  @Override
  public void getChildrenExperimentRuns(
      GetChildrenExperimentRuns request,
      StreamObserver<GetChildrenExperimentRuns.Response> responseObserver) {
    responseObserver.onError(
        Status.UNIMPLEMENTED.withDescription(ModelDBMessages.UNIMPLEMENTED).asRuntimeException());
  }

  @Override
  public void setParentExperimentRunId(
      SetParentExperimentRunId request,
      StreamObserver<SetParentExperimentRunId.Response> responseObserver) {
    responseObserver.onError(
        Status.UNIMPLEMENTED.withDescription(ModelDBMessages.UNIMPLEMENTED).asRuntimeException());
  }

  @Override
  public void getExperimentRunsByDatasetVersionId(
      GetExperimentRunsByDatasetVersionId request,
      StreamObserver<GetExperimentRunsByDatasetVersionId.Response> responseObserver) {
    try {
      final var response = futureExperimentRunDAO.getExperimentRunsByDatasetVersionId(request);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentRuns(
      DeleteExperimentRuns request,
      StreamObserver<DeleteExperimentRuns.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .deleteExperimentRuns(request)
              .thenCompose(
                  unused -> loggedDeleteExperimentRunEvents(request.getIdsList()), executor)
              .thenApply(
                  unused -> DeleteExperimentRuns.Response.newBuilder().setStatus(true).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logVersionedInput(
      LogVersionedInput request, StreamObserver<LogVersionedInput.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .logVersionedInputs(request)
              .thenCompose(
                  experimentRun ->
                      // Add succeeded event in local DB
                      addEvent(
                          experimentRun.getId(),
                          Optional.of(experimentRun.getExperimentId()),
                          experimentRun.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("version_input"),
                          Collections.emptyMap(),
                          "experiment_run version_input added successfully"),
                  executor)
              .thenApply(unused -> LogVersionedInput.Response.newBuilder().build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getVersionedInputs(
      GetVersionedInput request, StreamObserver<GetVersionedInput.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .getVersionedInputs(request)
              .thenApply(
                  versionedInputs -> {
                    var builder = GetVersionedInput.Response.newBuilder();
                    if (versionedInputs != null) {
                      builder.setVersionedInputs(versionedInputs);
                    }
                    return builder.build();
                  },
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void listCommitExperimentRuns(
      ListCommitExperimentRunsRequest request,
      StreamObserver<ListCommitExperimentRunsRequest.Response> responseObserver) {
    responseObserver.onError(
        Status.UNIMPLEMENTED.withDescription(ModelDBMessages.UNIMPLEMENTED).asRuntimeException());
  }

  @Override
  public void listBlobExperimentRuns(
      ListBlobExperimentRunsRequest request,
      StreamObserver<ListBlobExperimentRunsRequest.Response> responseObserver) {
    responseObserver.onError(
        Status.UNIMPLEMENTED.withDescription(ModelDBMessages.UNIMPLEMENTED).asRuntimeException());
  }

  @Override
  public void cloneExperimentRun(
      CloneExperimentRun request, StreamObserver<CloneExperimentRun.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .cloneExperimentRun(request)
              .thenCompose(
                  returnResponse ->
                      // Add succeeded event in local DB
                      addEvent(
                              returnResponse.getRun().getId(),
                              Optional.of(returnResponse.getRun().getExperimentId()),
                              returnResponse.getRun().getProjectId(),
                              ADD_EVENT_TYPE,
                              Optional.empty(),
                              Collections.emptyMap(),
                              "experiment_run cloned successfully")
                          .thenApply(unused -> returnResponse, executor),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }
}
