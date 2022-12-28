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
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureGrpc;
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
import java.util.function.Function;
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

  private Future<Void> addEvent(
      String entityId,
      Optional<String> experimentId,
      String projectId,
      String eventType,
      Optional<String> updatedField,
      Map<String, Object> extraFieldsMap,
      String eventMessage) {

    if (!App.getInstance().mdbConfig.isEvent_system_enabled()) {
      return Future.of(null);
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
                futureEventDAO
                    .addLocalEventWithAsync(
                        ModelDBResourceEnum.ModelDBServiceResourceTypes.EXPERIMENT_RUN.name(),
                        eventType,
                        projectResource.getWorkspaceId(),
                        eventMetadata)
                    .toFuture());
  }

  @Override
  public void createExperimentRun(
      CreateExperimentRun request, StreamObserver<CreateExperimentRun.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture =
          futureExperimentRunDAO
              .createExperimentRun(request)
              .thenCompose(
                  experimentRun ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            experimentRun.getId(),
                            Optional.of(experimentRun.getExperimentId()),
                            experimentRun.getProjectId(),
                            ADD_EVENT_TYPE,
                            Optional.empty(),
                            Collections.emptyMap(),
                            "experiment_run added successfully")
                        .thenCompose(unused -> Future.of(experimentRun));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture.<CreateExperimentRun.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super ExperimentRun, ? extends CreateExperimentRun.Response>)
                              experimentRun ->
                                  CreateExperimentRun.Response.newBuilder()
                                      .setExperimentRun(experimentRun)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentRun(
      DeleteExperimentRun request, StreamObserver<DeleteExperimentRun.Response> responseObserver) {
    try {
      Future<List<Void>> listFuture =
          futureExperimentRunDAO
              .deleteExperimentRuns(
                  DeleteExperimentRuns.newBuilder().addIds(request.getId()).build())
              .thenCompose(
                  unused ->
                      loggedDeleteExperimentRunEvents(Collections.singletonList(request.getId())));
      final var response =
          listFuture.<DeleteExperimentRun.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<Void>, ? extends DeleteExperimentRun.Response>)
                              unused ->
                                  DeleteExperimentRun.Response.newBuilder().setStatus(true).build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  private Future<List<Void>> loggedDeleteExperimentRunEvents(List<String> runIds) {
    // Add succeeded event in local DB
    List<Future<Void>> futureList = new ArrayList<>();
    Map<String, String> cacheMap = new ConcurrentHashMap<>();
    Future<String> projectIdFuture;
    for (String runId : runIds) {
      if (cacheMap.containsKey(runId)) {
        String thing = cacheMap.get(runId);
        projectIdFuture = Future.of(thing);
      } else {
        Future<String> stringFuture = futureExperimentRunDAO.getProjectIdByExperimentRunId(runId);
        projectIdFuture =
            stringFuture.thenCompose(
                t ->
                    Future.of(
                        ((Function<? super String, ? extends String>)
                                projectId -> {
                                  cacheMap.put(runId, projectId);
                                  return projectId;
                                })
                            .apply(t)));
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
                      "experiment_run deleted successfully"));
      futureList.add(eventFuture);
    }
    return Future.sequence(futureList);
  }

  @Override
  public void getExperimentRunsInProject(
      GetExperimentRunsInProject request,
      StreamObserver<GetExperimentRunsInProject.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getProjectId().isEmpty()) {
                  var errorMessage = "Project ID not present";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<FindExperimentRuns.Response> responseFuture =
          requestValidationFuture.thenCompose(
              unused ->
                  futureExperimentRunDAO.findExperimentRuns(
                      FindExperimentRuns.newBuilder()
                          .setProjectId(request.getProjectId())
                          .setPageLimit(request.getPageLimit())
                          .setPageNumber(request.getPageNumber())
                          .setAscending(request.getAscending())
                          .setSortKey(request.getSortKey())
                          .build()));
      final var response =
          responseFuture.<GetExperimentRunsInProject.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<
                                  ? super FindExperimentRuns.Response,
                                  ? extends GetExperimentRunsInProject.Response>)
                              findResponse ->
                                  GetExperimentRunsInProject.Response.newBuilder()
                                      .addAllExperimentRuns(findResponse.getExperimentRunsList())
                                      .setTotalRecords(findResponse.getTotalRecords())
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
      FutureGrpc.serverResponse(responseObserver, response);
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
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "ExperimentRun ID not present";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<FindExperimentRuns.Response> responseFuture =
          requestValidationFuture.thenCompose(
              unused ->
                  futureExperimentRunDAO.findExperimentRuns(
                      FindExperimentRuns.newBuilder()
                          .addExperimentRunIds(request.getId())
                          .build()));
      final var response =
          responseFuture.<GetExperimentRunById.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<
                                  ? super FindExperimentRuns.Response,
                                  ? extends GetExperimentRunById.Response>)
                              findResponse -> {
                                if (findResponse.getExperimentRunsCount() > 1) {
                                  throw new InternalErrorException(
                                      "More than one ExperimentRun found for ID: "
                                          + request.getId());
                                } else if (findResponse.getExperimentRunsCount() == 0) {
                                  throw new NotFoundException(
                                      "ExperimentRun not found for the ID: " + request.getId());
                                } else {
                                  return GetExperimentRunById.Response.newBuilder()
                                      .setExperimentRun(findResponse.getExperimentRuns(0))
                                      .build();
                                }
                              })
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
          Future.runAsync(
              () -> {
                if (request.getName().isEmpty()) {
                  var errorMessage = "ExperimentRun name not present";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getExperimentId().isEmpty()) {
                  var errorMessage = "Experiment ID not present";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<FindExperimentRuns.Response> responseFuture =
          requestValidationFuture.thenCompose(
              unused ->
                  futureExperimentRunDAO.findExperimentRuns(
                      FindExperimentRuns.newBuilder()
                          .setExperimentId(request.getExperimentId())
                          .addPredicates(
                              KeyValueQuery.newBuilder()
                                  .setKey("name")
                                  .setValue(
                                      Value.newBuilder().setStringValue(request.getName()).build())
                                  .setOperator(OperatorEnum.Operator.EQ)
                                  .setValueType(ValueTypeEnum.ValueType.STRING)
                                  .build())
                          .build()));
      final var response =
          responseFuture.<GetExperimentRunByName.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<
                                  ? super FindExperimentRuns.Response,
                                  ? extends GetExperimentRunByName.Response>)
                              findResponse -> {
                                if (findResponse.getExperimentRunsCount() == 0) {
                                  throw new NotFoundException(
                                      "ExperimentRun not found for the name: " + request.getName());
                                } else {
                                  return GetExperimentRunByName.Response.newBuilder()
                                      .setExperimentRun(findResponse.getExperimentRuns(0))
                                      .build();
                                }
                              })
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
      Future<ExperimentRun> experimentRunFuture =
          futureExperimentRunDAO
              .updateExperimentRunDescription(request)
              .thenCompose(
                  experimentRun ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            experimentRun.getId(),
                            Optional.of(experimentRun.getExperimentId()),
                            experimentRun.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("description"),
                            Collections.emptyMap(),
                            "experiment_run description added successfully")
                        .thenCompose(unused -> Future.of(experimentRun));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture.<UpdateExperimentRunDescription.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<
                                  ? super ExperimentRun,
                                  ? extends UpdateExperimentRunDescription.Response>)
                              experimentRun ->
                                  UpdateExperimentRunDescription.Response.newBuilder()
                                      .setExperimentRun(experimentRun)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentRunTags(
      AddExperimentRunTags request,
      StreamObserver<AddExperimentRunTags.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture =
          futureExperimentRunDAO
              .addTags(request)
              .thenCompose(
                  experimentRun ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
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
                        .thenCompose(unused -> Future.of(experimentRun));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture.<AddExperimentRunTags.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super ExperimentRun, ? extends AddExperimentRunTags.Response>)
                              experimentRun ->
                                  AddExperimentRunTags.Response.newBuilder()
                                      .setExperimentRun(experimentRun)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
              .thenCompose(
                  tags -> Future.of(GetTags.Response.newBuilder().addAllTags(tags).build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentRunTags(
      DeleteExperimentRunTags request,
      StreamObserver<DeleteExperimentRunTags.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture =
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
                        .thenCompose(unused -> Future.of(experimentRun));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture.<DeleteExperimentRunTags.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super ExperimentRun, ? extends DeleteExperimentRunTags.Response>)
                              experimentRun ->
                                  DeleteExperimentRunTags.Response.newBuilder()
                                      .setExperimentRun(experimentRun)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentRunTag(
      AddExperimentRunTag request, StreamObserver<AddExperimentRunTag.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture =
          futureExperimentRunDAO
              .addTags(
                  AddExperimentRunTags.newBuilder()
                      .setId(request.getId())
                      .addTags(request.getTag())
                      .build())
              .thenCompose(
                  experimentRun ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
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
                        .thenCompose(eventLoggedStatus -> Future.of(experimentRun));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture.<AddExperimentRunTag.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super ExperimentRun, ? extends AddExperimentRunTag.Response>)
                              experimentRun ->
                                  AddExperimentRunTag.Response.newBuilder()
                                      .setExperimentRun(experimentRun)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentRunTag(
      DeleteExperimentRunTag request,
      StreamObserver<DeleteExperimentRunTag.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture =
          futureExperimentRunDAO
              .deleteTags(
                  DeleteExperimentRunTags.newBuilder()
                      .setId(request.getId())
                      .addTags(request.getTag())
                      .build())
              .thenCompose(
                  experimentRun ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
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
                        .thenCompose(eventLoggedStatus -> Future.of(experimentRun));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture.<DeleteExperimentRunTag.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super ExperimentRun, ? extends DeleteExperimentRunTag.Response>)
                              experimentRun ->
                                  DeleteExperimentRunTag.Response.newBuilder()
                                      .setExperimentRun(experimentRun)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
                  })
              .thenCompose(unused -> Future.of(LogObservation.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
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
                  })
              .thenCompose(unused -> Future.of(LogObservations.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getObservations(
      GetObservations request, StreamObserver<GetObservations.Response> responseObserver) {
    try {
      Future<List<Observation>> listFuture = futureExperimentRunDAO.getObservations(request);
      final var response =
          listFuture.<GetObservations.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<Observation>, ? extends GetObservations.Response>)
                              observations ->
                                  GetObservations.Response.newBuilder()
                                      .addAllObservations(observations)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
                  })
              .thenCompose(unused -> Future.of(DeleteObservations.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
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
                          "experiment_run metric added successfully"))
              .thenCompose(unused -> Future.of(LogMetric.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
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
                          "experiment_run metrics added successfully"))
              .thenCompose(unused -> Future.of(LogMetrics.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getMetrics(GetMetrics request, StreamObserver<GetMetrics.Response> responseObserver) {
    try {
      Future<List<KeyValue>> listFuture = futureExperimentRunDAO.getMetrics(request);
      final var response =
          listFuture.<GetMetrics.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<KeyValue>, ? extends GetMetrics.Response>)
                              metrics ->
                                  GetMetrics.Response.newBuilder().addAllMetrics(metrics).build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
                  })
              .thenCompose(unused -> Future.of(DeleteMetrics.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
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
                  })
              .thenCompose(unused -> Future.of(LogDataset.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
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
                  })
              .thenCompose(unused -> Future.of(LogDatasets.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getDatasets(
      GetDatasets request, StreamObserver<GetDatasets.Response> responseObserver) {
    try {
      Future<List<Artifact>> listFuture = futureExperimentRunDAO.getDatasets(request);
      final var response =
          listFuture.<GetDatasets.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<Artifact>, ? extends GetDatasets.Response>)
                              datasets ->
                                  GetDatasets.Response.newBuilder()
                                      .addAllDatasets(datasets)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
                          "experiment_run hyperparameter added successfully"))
              .thenCompose(unused -> Future.of(LogHyperparameter.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
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
                          "experiment_run hyperparameters added successfully"))
              .thenCompose(unused -> Future.of(LogHyperparameters.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getHyperparameters(
      GetHyperparameters request, StreamObserver<GetHyperparameters.Response> responseObserver) {
    try {
      Future<List<KeyValue>> listFuture = futureExperimentRunDAO.getHyperparameters(request);
      final var response =
          listFuture.<GetHyperparameters.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<KeyValue>, ? extends GetHyperparameters.Response>)
                              hyperparameters ->
                                  GetHyperparameters.Response.newBuilder()
                                      .addAllHyperparameters(hyperparameters)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteHyperparameters(
      DeleteHyperparameters request,
      StreamObserver<DeleteHyperparameters.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture =
          futureExperimentRunDAO.deleteHyperparameters(request);
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
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
                      })
              .thenCompose(
                  unused -> Future.of(DeleteHyperparameters.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logAttribute(
      LogAttribute request, StreamObserver<LogAttribute.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture =
          futureExperimentRunDAO.logAttributes(
              LogAttributes.newBuilder()
                  .setId(request.getId())
                  .addAttributes(request.getAttribute())
                  .build());
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
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
                              "experiment_run attribute added successfully"))
              .thenCompose(unused -> Future.of(LogAttribute.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logAttributes(
      LogAttributes request, StreamObserver<LogAttributes.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture = futureExperimentRunDAO.logAttributes(request);
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
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
                              "experiment_run attributes added successfully"))
              .thenCompose(unused -> Future.of(LogAttributes.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentRunAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      Future<List<KeyValue>> listFuture = futureExperimentRunDAO.getAttributes(request);
      final var response =
          listFuture.<GetAttributes.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<KeyValue>, ? extends GetAttributes.Response>)
                              attributes ->
                                  GetAttributes.Response.newBuilder()
                                      .addAllAttributes(attributes)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentRunAttributes(
      AddExperimentRunAttributes request,
      StreamObserver<AddExperimentRunAttributes.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture =
          futureExperimentRunDAO.logAttributes(
              LogAttributes.newBuilder()
                  .setId(request.getId())
                  .addAllAttributes(request.getAttributesList())
                  .build());
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
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
                              "experiment_run attributes added successfully"))
              .thenCompose(
                  unused -> Future.of(AddExperimentRunAttributes.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentRunAttributes(
      DeleteExperimentRunAttributes request,
      StreamObserver<DeleteExperimentRunAttributes.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture = futureExperimentRunDAO.deleteAttributes(request);
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
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
                      })
              .thenCompose(
                  unused -> Future.of(DeleteExperimentRunAttributes.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logEnvironment(
      LogEnvironment request, StreamObserver<LogEnvironment.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture = futureExperimentRunDAO.logEnvironment(request);
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
                      experimentRun ->
                          // Add succeeded event in local DB
                          addEvent(
                              experimentRun.getId(),
                              Optional.of(experimentRun.getExperimentId()),
                              experimentRun.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.of("environment"),
                              Collections.emptyMap(),
                              "experiment_run environment added successfully"))
              .thenCompose(unused -> Future.of(LogEnvironment.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logExperimentRunCodeVersion(
      LogExperimentRunCodeVersion request,
      StreamObserver<LogExperimentRunCodeVersion.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture = futureExperimentRunDAO.logCodeVersion(request);
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
                      experimentRun ->
                          // Add succeeded event in local DB
                          addEvent(
                              experimentRun.getId(),
                              Optional.of(experimentRun.getExperimentId()),
                              experimentRun.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.of("code_version"),
                              Collections.emptyMap(),
                              "experiment_run code_version added successfully"))
              .thenCompose(
                  unused -> Future.of(LogExperimentRunCodeVersion.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentRunCodeVersion(
      GetExperimentRunCodeVersion request,
      StreamObserver<GetExperimentRunCodeVersion.Response> responseObserver) {
    try {
      Future<Optional<CodeVersion>> optionalFuture = futureExperimentRunDAO.getCodeVersion(request);
      final var response =
          optionalFuture.<GetExperimentRunCodeVersion.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<
                                  ? super Optional<CodeVersion>,
                                  ? extends GetExperimentRunCodeVersion.Response>)
                              codeVersion ->
                                  GetExperimentRunCodeVersion.Response.newBuilder()
                                      .setCodeVersion(
                                          codeVersion.orElseGet(
                                              () -> CodeVersion.newBuilder().build()))
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logArtifact(
      LogArtifact request, StreamObserver<LogArtifact.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture =
          futureExperimentRunDAO.logArtifacts(
              LogArtifacts.newBuilder()
                  .setId(request.getId())
                  .addArtifacts(request.getArtifact())
                  .build());
      // Add succeeded event in local DB
      final var futureResponse =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
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
                              "experiment_run artifact added successfully"))
              .thenCompose(unused -> Future.of(LogArtifact.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logArtifacts(
      LogArtifacts request, StreamObserver<LogArtifacts.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture = futureExperimentRunDAO.logArtifacts(request);
      // Add succeeded event in local DB
      final var futureResponse =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
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
                              "experiment_run artifacts added successfully"))
              .thenCompose(unused -> Future.of(LogArtifacts.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {
      Future<List<Artifact>> listFuture = futureExperimentRunDAO.getArtifacts(request);
      final var futureResponse =
          listFuture.<GetArtifacts.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<Artifact>, ? extends GetArtifacts.Response>)
                              artifacts ->
                                  GetArtifacts.Response.newBuilder()
                                      .addAllArtifacts(artifacts)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteArtifact(
      DeleteArtifact request, StreamObserver<DeleteArtifact.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture = futureExperimentRunDAO.deleteArtifacts(request);
      // Add succeeded event in local DB
      final var futureResponse =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
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
                              "experiment_run artifact deleted successfully"))
              .thenCompose(deleted -> Future.of(DeleteArtifact.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    try {
      final var futureResponse = futureExperimentRunDAO.getUrlForArtifact(request);
      FutureGrpc.serverResponse(responseObserver, futureResponse);
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
              .thenCompose(unused -> Future.of(CommitArtifactPart.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
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
      FutureGrpc.serverResponse(responseObserver, futureResponse);
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
              .thenCompose(
                  unused -> Future.of(CommitMultipartArtifact.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void findExperimentRuns(
      FindExperimentRuns request, StreamObserver<FindExperimentRuns.Response> responseObserver) {
    try {
      final var futureResponse = futureExperimentRunDAO.findExperimentRuns(request);
      FutureGrpc.serverResponse(responseObserver, futureResponse);
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
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentRuns(
      DeleteExperimentRuns request,
      StreamObserver<DeleteExperimentRuns.Response> responseObserver) {
    try {
      Future<List<Void>> listFuture =
          futureExperimentRunDAO
              .deleteExperimentRuns(request)
              .thenCompose(unused -> loggedDeleteExperimentRunEvents(request.getIdsList()));
      final var response =
          listFuture.<DeleteExperimentRuns.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<Void>, ? extends DeleteExperimentRuns.Response>)
                              unused ->
                                  DeleteExperimentRuns.Response.newBuilder()
                                      .setStatus(true)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logVersionedInput(
      LogVersionedInput request, StreamObserver<LogVersionedInput.Response> responseObserver) {
    try {
      Future<ExperimentRun> experimentRunFuture =
          futureExperimentRunDAO.logVersionedInputs(request);
      // Add succeeded event in local DB
      final var response =
          experimentRunFuture
              .thenCompose(
                  (Function<? super ExperimentRun, Future<Void>>)
                      experimentRun ->
                          // Add succeeded event in local DB
                          addEvent(
                              experimentRun.getId(),
                              Optional.of(experimentRun.getExperimentId()),
                              experimentRun.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.of("version_input"),
                              Collections.emptyMap(),
                              "experiment_run version_input added successfully"))
              .thenCompose(unused -> Future.of(LogVersionedInput.Response.newBuilder().build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getVersionedInputs(
      GetVersionedInput request, StreamObserver<GetVersionedInput.Response> responseObserver) {
    try {
      Future<VersioningEntry> versioningEntryFuture =
          futureExperimentRunDAO.getVersionedInputs(request);
      final var response =
          versioningEntryFuture.<GetVersionedInput.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super VersioningEntry, ? extends GetVersionedInput.Response>)
                              versionedInputs -> {
                                var builder = GetVersionedInput.Response.newBuilder();
                                if (versionedInputs != null) {
                                  builder.setVersionedInputs(versionedInputs);
                                }
                                return builder.build();
                              })
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
      Future<CloneExperimentRun.Response> responseFuture =
          futureExperimentRunDAO.cloneExperimentRun(request);
      // Add succeeded event in local DB
      final var response =
          responseFuture.thenCompose(
              (Function<? super CloneExperimentRun.Response, Future<CloneExperimentRun.Response>>)
                  returnResponse ->
                  // Add succeeded event in local DB
                  {
                    Future<Void> voidFuture =
                        addEvent(
                            returnResponse.getRun().getId(),
                            Optional.of(returnResponse.getRun().getExperimentId()),
                            returnResponse.getRun().getProjectId(),
                            ADD_EVENT_TYPE,
                            Optional.empty(),
                            Collections.emptyMap(),
                            "experiment_run cloned successfully");
                    return voidFuture.thenCompose(
                        t ->
                            Future.of(
                                ((Function<? super Void, ? extends CloneExperimentRun.Response>)
                                        unused -> returnResponse)
                                    .apply(t)));
                  });
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }
}
