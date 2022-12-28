package ai.verta.modeldb.experiment;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
import ai.verta.modeldb.DeleteExperimentAttributes.Response;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceImplBase;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.modeldb.utils.UACApisUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FutureExperimentServiceImpl extends ExperimentServiceImplBase {

  private static final String UPDATE_EVENT_TYPE =
      "update.resource.experiment.update_experiment_succeeded";
  private static final String DELETE_EXPERIMENT_EVENT_TYPE =
      "delete.resource.experiment.delete_experiment_succeeded";
  private final FutureExecutor executor;
  private final FutureProjectDAO futureProjectDAO;
  private final FutureExperimentDAO futureExperimentDAO;
  private final FutureEventDAO futureEventDAO;
  private final UACApisUtil uacApisUtil;

  public FutureExperimentServiceImpl(DAOSet daoSet, FutureExecutor executor) {
    this.executor = executor;
    this.futureProjectDAO = daoSet.getFutureProjectDAO();
    this.futureExperimentDAO = daoSet.getFutureExperimentDAO();
    this.futureEventDAO = daoSet.getFutureEventDAO();
    this.uacApisUtil = daoSet.getUacApisUtil();
  }

  private Future<Void> addEvent(
      String entityId,
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
                        ModelDBServiceResourceTypes.EXPERIMENT.name(),
                        eventType,
                        projectResource.getWorkspaceId(),
                        eventMetadata)
                    .toFuture());
  }

  @Override
  public void createExperiment(
      CreateExperiment request, StreamObserver<CreateExperiment.Response> responseObserver) {
    try {
      Future<Experiment> experimentFuture =
          futureExperimentDAO
              .createExperiment(request)
              .thenCompose(
                  createdExperiment -> {
                    return addEvent(
                            createdExperiment.getId(),
                            createdExperiment.getProjectId(),
                            "add.resource.experiment.add_experiment_succeeded",
                            Optional.empty(),
                            Collections.emptyMap(),
                            "experiment logged successfully")
                        .thenCompose(eventLoggedStatus -> Future.of(createdExperiment));
                  });
      final var futureResponse =
          experimentFuture.<CreateExperiment.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends CreateExperiment.Response>)
                              createdExperiment ->
                                  CreateExperiment.Response.newBuilder()
                                      .setExperiment(createdExperiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentsInProject(
      GetExperimentsInProject request,
      StreamObserver<GetExperimentsInProject.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getProjectId().isEmpty()) {
                  var errorMessage = "Project ID not found in GetExperimentsInProject request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<FindExperiments.Response> responseFuture =
          requestValidationFuture
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getProjectId()))
              .thenCompose(
                  unused ->
                      futureExperimentDAO.findExperiments(
                          FindExperiments.newBuilder()
                              .setProjectId(request.getProjectId())
                              .setPageLimit(request.getPageLimit())
                              .setPageNumber(request.getPageNumber())
                              .setAscending(request.getAscending())
                              .setSortKey(request.getSortKey())
                              .build()));
      final var response =
          responseFuture.<GetExperimentsInProject.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<
                                  ? super FindExperiments.Response,
                                  ? extends GetExperimentsInProject.Response>)
                              findResponse ->
                                  GetExperimentsInProject.Response.newBuilder()
                                      .addAllExperiments(findResponse.getExperimentsList())
                                      .setTotalRecords(findResponse.getTotalRecords())
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentById(
      GetExperimentById request, StreamObserver<GetExperimentById.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in GetExperimentById request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<FindExperiments.Response> responseFuture =
          requestValidationFuture.thenCompose(
              unused ->
                  futureExperimentDAO.findExperiments(
                      FindExperiments.newBuilder().addExperimentIds(request.getId()).build()));
      final var response =
          responseFuture.<GetExperimentById.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<
                                  ? super FindExperiments.Response,
                                  ? extends GetExperimentById.Response>)
                              findResponse -> {
                                if (findResponse.getExperimentsCount() > 1) {
                                  throw new InternalErrorException(
                                      "More than one Experiment found for ID: " + request.getId());
                                } else if (findResponse.getExperimentsCount() == 0) {
                                  throw new NotFoundException(
                                      "Experiment not found for the ID: " + request.getId());
                                } else {
                                  return GetExperimentById.Response.newBuilder()
                                      .setExperiment(findResponse.getExperiments(0))
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
  public void getExperimentByName(
      GetExperimentByName request, StreamObserver<GetExperimentByName.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getProjectId().isEmpty()) {
                  var errorMessage1 = "Project ID not found in GetExperimentByName request";
                  throw new InvalidArgumentException(errorMessage1);
                } else if (request.getName().isEmpty()) {
                  var errorMessage1 = "Experiment name not found in GetExperimentByName request";
                  throw new InvalidArgumentException(errorMessage1);
                }
              });
      Future<FindExperiments.Response> responseFuture =
          requestValidationFuture.thenCompose(
              unused ->
                  futureExperimentDAO.findExperiments(
                      FindExperiments.newBuilder()
                          .setProjectId(request.getProjectId())
                          .addPredicates(
                              KeyValueQuery.newBuilder()
                                  .setKey(ModelDBConstants.NAME)
                                  .setValue(
                                      Value.newBuilder().setStringValue(request.getName()).build())
                                  .build())
                          .build()));
      final var response =
          responseFuture.<GetExperimentByName.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<
                                  ? super FindExperiments.Response,
                                  ? extends GetExperimentByName.Response>)
                              findResponse -> {
                                var experiments = findResponse.getExperimentsList();
                                if (experiments.isEmpty()) {
                                  var errorMessage =
                                      "Experiment with name "
                                          + request.getName()
                                          + " not found in project "
                                          + request.getProjectId();
                                  throw new ModelDBException(errorMessage, Code.NOT_FOUND);
                                }
                                if (experiments.size() != 1) {
                                  var errorMessage =
                                      "Multiple experiments with name "
                                          + request.getName()
                                          + " found in project "
                                          + request.getProjectId();
                                  throw new ModelDBException(errorMessage, Code.INTERNAL);
                                }
                                return GetExperimentByName.Response.newBuilder()
                                    .setExperiment(experiments.get(0))
                                    .build();
                              })
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateExperimentNameOrDescription(
      UpdateExperimentNameOrDescription request,
      StreamObserver<UpdateExperimentNameOrDescription.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage =
                      "Experiment ID not found in UpdateExperimentNameOrDescription request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.updateExperimentNameOrDescription(request))
              .thenCompose(
                  updatedExperiment -> {
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.empty(),
                            Collections.emptyMap(),
                            "experiment updated successfully")
                        .thenCompose(eventLoggedStatus -> Future.of(updatedExperiment));
                  });
      final var response =
          experimentFuture.<UpdateExperimentNameOrDescription.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<
                                  ? super Experiment,
                                  ? extends UpdateExperimentNameOrDescription.Response>)
                              experiment ->
                                  UpdateExperimentNameOrDescription.Response.newBuilder()
                                      .setExperiment(experiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateExperimentName(
      UpdateExperimentName request,
      StreamObserver<UpdateExperimentName.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in UpdateExperimentName request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.updateExperimentName(request))
              .thenCompose(
                  updatedExperiment -> {
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("name"),
                            Collections.singletonMap("name", updatedExperiment.getName()),
                            "experiment name updated successfully")
                        .thenCompose(eventLoggedStatus -> Future.of(updatedExperiment));
                  });
      final var response =
          experimentFuture.<UpdateExperimentName.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends UpdateExperimentName.Response>)
                              experiment ->
                                  UpdateExperimentName.Response.newBuilder()
                                      .setExperiment(experiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateExperimentDescription(
      UpdateExperimentDescription request,
      StreamObserver<UpdateExperimentDescription.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage =
                      "Experiment ID not found in UpdateExperimentDescription request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.updateExperimentDescription(request))
              .thenCompose(
                  updatedExperiment -> {
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("description"),
                            Collections.emptyMap(),
                            "experiment description updated successfully")
                        .thenCompose(eventLoggedStatus -> Future.of(updatedExperiment));
                  });
      final var response =
          experimentFuture.<UpdateExperimentDescription.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<
                                  ? super Experiment,
                                  ? extends UpdateExperimentDescription.Response>)
                              experiment ->
                                  UpdateExperimentDescription.Response.newBuilder()
                                      .setExperiment(experiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentTags(
      AddExperimentTags request, StreamObserver<AddExperimentTags.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in AddExperimentTags request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getTagsList().isEmpty()) {
                  var errorMessage = "Experiment tags not found in AddExperimentTags request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture =
          requestValidationFuture
              .thenCompose(
                  unused -> futureExperimentDAO.addTags(request.getId(), request.getTagsList()))
              .thenCompose(
                  updatedExperiment ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("tags"),
                            Collections.singletonMap(
                                "tags",
                                new Gson()
                                    .toJsonTree(
                                        request.getTagsList(),
                                        new TypeToken<ArrayList<String>>() {}.getType())),
                            "experiment tags added successfully")
                        .thenCompose(eventLoggedStatus -> Future.of(updatedExperiment));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentFuture.<AddExperimentTags.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends AddExperimentTags.Response>)
                              experiment ->
                                  AddExperimentTags.Response.newBuilder()
                                      .setExperiment(experiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentTag(
      AddExperimentTag request, StreamObserver<AddExperimentTag.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in AddExperimentTag request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getTag().isEmpty()) {
                  var errorMessage = "Experiment Tag not found in AddExperimentTag request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.addTags(
                          request.getId(), Collections.singletonList(request.getTag())))
              .thenCompose(
                  updatedExperiment ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("tags"),
                            Collections.singletonMap(
                                "tags",
                                new Gson()
                                    .toJsonTree(
                                        Collections.singletonList(request.getTag()),
                                        new TypeToken<ArrayList<String>>() {}.getType())),
                            "experiment tag added successfully")
                        .thenCompose(eventLoggedStatus -> Future.of(updatedExperiment));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentFuture.<AddExperimentTag.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends AddExperimentTag.Response>)
                              experiment ->
                                  AddExperimentTag.Response.newBuilder()
                                      .setExperiment(experiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in GetTags request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      final var response =
          requestValidationFuture.thenCompose(
              unused -> futureExperimentDAO.getTags(request.getId()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentTags(
      DeleteExperimentTags request,
      StreamObserver<DeleteExperimentTags.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                String errorMessage = null;
                if (request.getId().isEmpty()
                    && request.getTagsList().isEmpty()
                    && !request.getDeleteAll()) {
                  errorMessage =
                      "Experiment ID and Experiment tags not found in DeleteExperimentTags request";
                } else if (request.getId().isEmpty()) {
                  errorMessage = "Experiment ID not found in DeleteExperimentTags request";
                } else if (request.getTagsList().isEmpty() && !request.getDeleteAll()) {
                  errorMessage = "Experiment tags not found in DeleteExperimentTags request";
                }

                if (errorMessage != null) {
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.deleteTags(
                          request.getId(), request.getTagsList(), request.getDeleteAll()))
              .thenCompose(
                  updatedExperiment -> {
                    Map<String, Object> extraField = new HashMap<>();
                    if (request.getDeleteAll()) {
                      extraField.put("tags_delete_all", true);
                    } else {
                      extraField.put(
                          "tags",
                          new Gson()
                              .toJsonTree(
                                  request.getTagsList(),
                                  new TypeToken<ArrayList<String>>() {}.getType()));
                    }
                    // Add succeeded event in local DB
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("tags"),
                            extraField,
                            "experiment tags deleted successfully")
                        .thenCompose(unused -> Future.of(updatedExperiment));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentFuture.<DeleteExperimentTags.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends DeleteExperimentTags.Response>)
                              experiment ->
                                  DeleteExperimentTags.Response.newBuilder()
                                      .setExperiment(experiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentTag(
      DeleteExperimentTag request, StreamObserver<DeleteExperimentTag.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                String errorMessage = null;
                if (request.getId().isEmpty() && request.getTag().isEmpty()) {
                  errorMessage =
                      "Experiment ID and Experiment tag not found in DeleteExperimentTag request";
                } else if (request.getId().isEmpty()) {
                  errorMessage = "Experiment ID not found in DeleteExperimentTag request";
                } else if (request.getTag().isEmpty()) {
                  errorMessage = "Experiment tag not found in DeleteExperimentTag request";
                }

                if (errorMessage != null) {
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.deleteTags(
                          request.getId(), Collections.singletonList(request.getTag()), false))
              .thenCompose(
                  updatedExperiment ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("tags"),
                            Collections.singletonMap(
                                "tags",
                                new Gson()
                                    .toJsonTree(
                                        Collections.singletonList(request.getTag()),
                                        new TypeToken<ArrayList<String>>() {}.getType())),
                            "experiment tag deleted successfully")
                        .thenCompose(unused -> Future.of(updatedExperiment));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentFuture.<DeleteExperimentTag.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends DeleteExperimentTag.Response>)
                              experiment ->
                                  DeleteExperimentTag.Response.newBuilder()
                                      .setExperiment(experiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addAttribute(
      AddAttributes request, StreamObserver<AddAttributes.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in AddAttributes request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.logAttributes(
                          request.getId(), Collections.singletonList(request.getAttribute())))
              .thenCompose(
                  updatedExperiment ->
                      // Add succeeded event in local DB
                      addEvent(
                          updatedExperiment.getId(),
                          updatedExperiment.getProjectId(),
                          UPDATE_EVENT_TYPE,
                          Optional.of("attributes"),
                          Collections.singletonMap(
                              "attribute_keys",
                              new Gson()
                                  .toJsonTree(
                                      Stream.of(request.getAttribute())
                                          .map(KeyValue::getKey)
                                          .collect(Collectors.toSet()),
                                      new TypeToken<ArrayList<String>>() {}.getType())),
                          "experiment attribute added successfully"))
              .thenCompose(
                  unused -> Future.of(AddAttributes.Response.newBuilder().setStatus(true).build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentAttributes(
      AddExperimentAttributes request,
      StreamObserver<AddExperimentAttributes.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                String errorMessage = null;
                if (request.getId().isEmpty() && request.getAttributesList().isEmpty()) {
                  errorMessage =
                      "Experiment ID and Experiment Attributes not found in AddExperimentAttributes request";
                } else if (request.getId().isEmpty()) {
                  errorMessage = "Experiment ID not found in AddExperimentAttributes request";
                } else if (request.getAttributesList().isEmpty()) {
                  errorMessage =
                      "Experiment Attributes not found in AddExperimentAttributes request";
                }

                if (errorMessage != null) {
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.logAttributes(
                          request.getId(), request.getAttributesList()))
              .thenCompose(
                  updatedExperiment ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("attributes"),
                            Collections.singletonMap(
                                "attribute_keys",
                                new Gson()
                                    .toJsonTree(
                                        request.getAttributesList().stream()
                                            .map(KeyValue::getKey)
                                            .collect(Collectors.toSet()),
                                        new TypeToken<ArrayList<String>>() {}.getType())),
                            "experiment attributes added successfully")
                        .thenCompose(eventLoggedStatus -> Future.of(updatedExperiment));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentFuture.<AddExperimentAttributes.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends AddExperimentAttributes.Response>)
                              experiment ->
                                  AddExperimentAttributes.Response.newBuilder()
                                      .setExperiment(experiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                List<String> errorMessages = new ArrayList<>();
                if (request.getId().isEmpty()) {
                  errorMessages.add("Experiment ID not found in GetAttributes request");
                }
                if (request.getAttributeKeysList().isEmpty() && !request.getGetAll()) {
                  errorMessages.add("Experiment Attribute keys not found in GetAttributes request");
                }
                if (!errorMessages.isEmpty()) {
                  throw new InvalidArgumentException(String.join("\n", errorMessages));
                }
              });
      final var response =
          requestValidationFuture.thenCompose(
              unused -> futureExperimentDAO.getExperimentAttributes(request));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentAttributes(
      DeleteExperimentAttributes request, StreamObserver<Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage =
                      "Experiment ID not found in DeleteExperimentAttributes request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getAttributeKeysList().isEmpty() && !request.getDeleteAll()) {
                  var errorMessage =
                      "Experiment Attribute keys not found in DeleteExperimentAttributes request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.deleteAttributes(request))
              .thenCompose(
                  updatedExperiment ->
                  // Add succeeded event in local DB
                  {
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
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("attributes"),
                            extraFieldValue,
                            "Experiment attributes deleted successfully")
                        .thenCompose(unused -> Future.of(updatedExperiment));
                  });
      // Add succeeded event in local DB
      // Add succeeded event in local DB
      final var response =
          experimentFuture.<Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends Response>)
                              experiment -> Response.newBuilder().setExperiment(experiment).build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperiment(
      DeleteExperiment request, StreamObserver<DeleteExperiment.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessages = "Experiment ID not found in request";
                  throw new InvalidArgumentException(errorMessages);
                }
              });
      Future<Future<List<Void>>> futureFuture =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.deleteExperiments(
                          DeleteExperiments.newBuilder().addIds(request.getId()).build()))
              .thenCompose(a -> Future.of(this.loggedDeleteExperimentEvents(a)));
      final var response =
          futureFuture.<DeleteExperiment.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Future<List<Void>>, ? extends DeleteExperiment.Response>)
                              unused ->
                                  DeleteExperiment.Response.newBuilder().setStatus(true).build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logExperimentCodeVersion(
      LogExperimentCodeVersion request,
      StreamObserver<LogExperimentCodeVersion.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessages = "Experiment ID not found in request";
                  throw new InvalidArgumentException(errorMessages);
                }
              });
      Future<Experiment> experimentFuture =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.logCodeVersion(request))
              .thenCompose(
                  updatedExperiment ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("code_version"),
                            Collections.emptyMap(),
                            "experiment code_version added successfully")
                        .thenCompose(unused -> Future.of(updatedExperiment));
                  });
      // Add succeeded event in local DB
      final var response =
          experimentFuture.<LogExperimentCodeVersion.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends LogExperimentCodeVersion.Response>)
                              updatedExperiment ->
                                  LogExperimentCodeVersion.Response.newBuilder()
                                      .setExperiment(updatedExperiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentCodeVersion(
      GetExperimentCodeVersion request,
      StreamObserver<GetExperimentCodeVersion.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in GetExperimentCodeVersion request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      final var response =
          requestValidationFuture.thenCompose(
              unused -> futureExperimentDAO.getExperimentCodeVersion(request));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void findExperiments(
      FindExperiments request, StreamObserver<FindExperiments.Response> responseObserver) {
    try {
      final var futureResponse = futureExperimentDAO.findExperiments(request);
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in GetUrlForArtifact request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getKey().isEmpty()) {
                  var errorMessage = "Artifact Key not found in GetUrlForArtifact request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getMethod().isEmpty()) {
                  var errorMessage = "Method is not found in GetUrlForArtifact request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      final var futureResponse =
          requestValidationFuture.thenCompose(
              unused -> futureExperimentDAO.getUrlForArtifact(request));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logArtifacts(
      LogExperimentArtifacts request,
      StreamObserver<LogExperimentArtifacts.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in LogArtifacts request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getArtifactsList().isEmpty()) {
                  var errorMessage = "Artifacts not found in LogArtifacts request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture1 =
          requestValidationFuture.thenCompose(unused -> futureExperimentDAO.logArtifacts(request));
      // Add succeeded event in local DB
      Future<Experiment> experimentFuture =
          experimentFuture1.thenCompose(
              (Function<? super Experiment, Future<Experiment>>)
                  updatedExperiment ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
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
                            "experiment artifacts added successfully")
                        .thenCompose(unused -> Future.of(updatedExperiment));
                  });
      // Add succeeded event in local DB
      final var futureResponse =
          experimentFuture.<LogExperimentArtifacts.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends LogExperimentArtifacts.Response>)
                              experiment ->
                                  LogExperimentArtifacts.Response.newBuilder()
                                      .setExperiment(experiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in GetArtifacts request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<List<Artifact>> listFuture =
          requestValidationFuture.thenCompose(unused -> futureExperimentDAO.getArtifacts(request));
      final var response =
          listFuture.<GetArtifacts.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<Artifact>, ? extends GetArtifacts.Response>)
                              artifacts ->
                                  GetArtifacts.Response.newBuilder()
                                      .addAllArtifacts(artifacts)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteArtifact(
      DeleteExperimentArtifact request,
      StreamObserver<DeleteExperimentArtifact.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in DeleteArtifact request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getKey().isEmpty()) {
                  var errorMessage = "Artifact key not found in DeleteArtifact request";
                  throw new InvalidArgumentException(errorMessage);
                }
              });
      Future<Experiment> experimentFuture1 =
          requestValidationFuture.thenCompose(
              unused -> futureExperimentDAO.deleteArtifacts(request));
      // Add succeeded event in local DB
      Future<ai.verta.modeldb.Experiment> experimentFuture =
          experimentFuture1.thenCompose(
              (Function<? super Experiment, Future<Experiment>>)
                  updatedExperiment ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            updatedExperiment.getId(),
                            updatedExperiment.getProjectId(),
                            UPDATE_EVENT_TYPE,
                            Optional.of("artifacts"),
                            Collections.singletonMap(
                                "artifact_keys",
                                new Gson()
                                    .toJsonTree(
                                        Collections.singletonList(request.getKey()),
                                        new TypeToken<ArrayList<String>>() {}.getType())),
                            "experiment artifact deleted successfully")
                        .thenCompose(unused -> Future.of(updatedExperiment));
                  });
      // Add succeeded event in local DB
      final var futureResponse =
          experimentFuture.<DeleteExperimentArtifact.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Experiment, ? extends DeleteExperimentArtifact.Response>)
                              experiment ->
                                  DeleteExperimentArtifact.Response.newBuilder()
                                      .setExperiment(experiment)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperiments(
      DeleteExperiments request, StreamObserver<DeleteExperiments.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          Future.runAsync(
              () -> {
                if (request.getIdsList().isEmpty()) {
                  var errorMessages = "Experiment IDs not found in request";
                  throw new InvalidArgumentException(errorMessages);
                }
              });
      Future<Future<List<Void>>> futureFuture =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.deleteExperiments(request))
              .thenCompose(a -> Future.of(this.loggedDeleteExperimentEvents(a)));
      final var response =
          futureFuture.<DeleteExperiments.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Future<List<Void>>, ? extends DeleteExperiments.Response>)
                              unused ->
                                  DeleteExperiments.Response.newBuilder().setStatus(true).build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  private Future<List<Void>> loggedDeleteExperimentEvents(Map<String, String> experimentIdsMap) {
    // Add succeeded event in local DB
    List<Future<Void>> futureList = new ArrayList<>();
    for (var entry : experimentIdsMap.entrySet()) {
      addEvent(
          entry.getKey(),
          entry.getValue(),
          DELETE_EXPERIMENT_EVENT_TYPE,
          Optional.empty(),
          Collections.emptyMap(),
          "experiment deleted successfully");
    }
    return Future.sequence(futureList);
  }
}
