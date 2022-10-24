package ai.verta.modeldb.experiment;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.AddAttributes;
import ai.verta.modeldb.AddExperimentAttributes;
import ai.verta.modeldb.AddExperimentTag;
import ai.verta.modeldb.AddExperimentTags;
import ai.verta.modeldb.App;
import ai.verta.modeldb.CreateExperiment;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.DeleteExperiment;
import ai.verta.modeldb.DeleteExperimentArtifact;
import ai.verta.modeldb.DeleteExperimentAttributes;
import ai.verta.modeldb.DeleteExperimentAttributes.Response;
import ai.verta.modeldb.DeleteExperimentTag;
import ai.verta.modeldb.DeleteExperimentTags;
import ai.verta.modeldb.DeleteExperiments;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceImplBase;
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetExperimentById;
import ai.verta.modeldb.GetExperimentByName;
import ai.verta.modeldb.GetExperimentCodeVersion;
import ai.verta.modeldb.GetExperimentsInProject;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.LogExperimentArtifacts;
import ai.verta.modeldb.LogExperimentCodeVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.UpdateExperimentDescription;
import ai.verta.modeldb.UpdateExperimentName;
import ai.verta.modeldb.UpdateExperimentNameOrDescription;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.InternalFuture;
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
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FutureExperimentServiceImpl extends ExperimentServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(FutureExperimentServiceImpl.class);
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

  private InternalFuture<Void> addEvent(
      String entityId,
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
                    ModelDBServiceResourceTypes.EXPERIMENT.name(),
                    eventType,
                    projectResource.getWorkspaceId(),
                    eventMetadata),
            executor);
  }

  @Override
  public void createExperiment(
      CreateExperiment request, StreamObserver<CreateExperiment.Response> responseObserver) {
    try {
      final var futureResponse =
          futureExperimentDAO
              .createExperiment(request)
              .thenCompose(
                  createdExperiment ->
                      addEvent(
                              createdExperiment.getId(),
                              createdExperiment.getProjectId(),
                              "add.resource.experiment.add_experiment_succeeded",
                              Optional.empty(),
                              Collections.emptyMap(),
                              "experiment logged successfully")
                          .thenApply(eventLoggedStatus -> createdExperiment, executor),
                  executor)
              .thenApply(
                  createdExperiment ->
                      CreateExperiment.Response.newBuilder()
                          .setExperiment(createdExperiment)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
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
          InternalFuture.runAsync(
              () -> {
                if (request.getProjectId().isEmpty()) {
                  var errorMessage = "Project ID not found in GetExperimentsInProject request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused -> futureProjectDAO.getProjectById(request.getProjectId()), executor)
              .thenCompose(
                  unused ->
                      futureExperimentDAO.findExperiments(
                          FindExperiments.newBuilder()
                              .setProjectId(request.getProjectId())
                              .setPageLimit(request.getPageLimit())
                              .setPageNumber(request.getPageNumber())
                              .setAscending(request.getAscending())
                              .setSortKey(request.getSortKey())
                              .build()),
                  executor)
              .thenApply(
                  findResponse ->
                      GetExperimentsInProject.Response.newBuilder()
                          .addAllExperiments(findResponse.getExperimentsList())
                          .setTotalRecords(findResponse.getTotalRecords())
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentById(
      GetExperimentById request, StreamObserver<GetExperimentById.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in GetExperimentById request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.findExperiments(
                          FindExperiments.newBuilder().addExperimentIds(request.getId()).build()),
                  executor)
              .thenApply(
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
                  },
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentByName(
      GetExperimentByName request, StreamObserver<GetExperimentByName.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getProjectId().isEmpty()) {
                  var errorMessage = "Project ID not found in GetExperimentByName request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getName().isEmpty()) {
                  var errorMessage = "Experiment name not found in GetExperimentByName request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.findExperiments(
                          FindExperiments.newBuilder()
                              .setProjectId(request.getProjectId())
                              .addPredicates(
                                  KeyValueQuery.newBuilder()
                                      .setKey(ModelDBConstants.NAME)
                                      .setValue(
                                          Value.newBuilder()
                                              .setStringValue(request.getName())
                                              .build())
                                      .build())
                              .build()),
                  executor)
              .thenApply(
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
                  },
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
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
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage =
                      "Experiment ID not found in UpdateExperimentNameOrDescription request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused -> futureExperimentDAO.updateExperimentNameOrDescription(request),
                  executor)
              .thenCompose(
                  updatedExperiment ->
                      addEvent(
                              updatedExperiment.getId(),
                              updatedExperiment.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.empty(),
                              Collections.emptyMap(),
                              "experiment updated successfully")
                          .thenApply(eventLoggedStatus -> updatedExperiment, executor),
                  executor)
              .thenApply(
                  experiment ->
                      UpdateExperimentNameOrDescription.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
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
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in UpdateExperimentName request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.updateExperimentName(request), executor)
              .thenCompose(
                  updatedExperiment ->
                      addEvent(
                              updatedExperiment.getId(),
                              updatedExperiment.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.of("name"),
                              Collections.singletonMap("name", updatedExperiment.getName()),
                              "experiment name updated successfully")
                          .thenApply(eventLoggedStatus -> updatedExperiment, executor),
                  executor)
              .thenApply(
                  experiment ->
                      UpdateExperimentName.Response.newBuilder().setExperiment(experiment).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
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
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage =
                      "Experiment ID not found in UpdateExperimentDescription request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused -> futureExperimentDAO.updateExperimentDescription(request), executor)
              .thenCompose(
                  updatedExperiment ->
                      addEvent(
                              updatedExperiment.getId(),
                              updatedExperiment.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.of("description"),
                              Collections.emptyMap(),
                              "experiment description updated successfully")
                          .thenApply(eventLoggedStatus -> updatedExperiment, executor),
                  executor)
              .thenApply(
                  experiment ->
                      UpdateExperimentDescription.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentTags(
      AddExperimentTags request, StreamObserver<AddExperimentTags.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in AddExperimentTags request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getTagsList().isEmpty()) {
                  var errorMessage = "Experiment tags not found in AddExperimentTags request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused -> futureExperimentDAO.addTags(request.getId(), request.getTagsList()),
                  executor)
              .thenCompose(
                  updatedExperiment ->
                      // Add succeeded event in local DB
                      addEvent(
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
                          .thenApply(eventLoggedStatus -> updatedExperiment, executor),
                  executor)
              .thenApply(
                  experiment ->
                      AddExperimentTags.Response.newBuilder().setExperiment(experiment).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addExperimentTag(
      AddExperimentTag request, StreamObserver<AddExperimentTag.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in AddExperimentTag request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getTag().isEmpty()) {
                  var errorMessage = "Experiment Tag not found in AddExperimentTag request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.addTags(
                          request.getId(), Collections.singletonList(request.getTag())),
                  executor)
              .thenCompose(
                  updatedExperiment ->
                      // Add succeeded event in local DB
                      addEvent(
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
                          .thenApply(eventLoggedStatus -> updatedExperiment, executor),
                  executor)
              .thenApply(
                  experiment ->
                      AddExperimentTag.Response.newBuilder().setExperiment(experiment).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in GetTags request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture.thenCompose(
              unused -> futureExperimentDAO.getTags(request.getId()), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
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
          InternalFuture.runAsync(
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
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.deleteTags(
                          request.getId(), request.getTagsList(), request.getDeleteAll()),
                  executor)
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
                        .thenApply(unused -> updatedExperiment, executor);
                  },
                  executor)
              .thenApply(
                  experiment ->
                      DeleteExperimentTags.Response.newBuilder().setExperiment(experiment).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentTag(
      DeleteExperimentTag request, StreamObserver<DeleteExperimentTag.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
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
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.deleteTags(
                          request.getId(), Collections.singletonList(request.getTag()), false),
                  executor)
              .thenCompose(
                  updatedExperiment ->
                      // Add succeeded event in local DB
                      addEvent(
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
                          .thenApply(unused -> updatedExperiment, executor),
                  executor)
              .thenApply(
                  experiment ->
                      DeleteExperimentTag.Response.newBuilder().setExperiment(experiment).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addAttribute(
      AddAttributes request, StreamObserver<AddAttributes.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in AddAttributes request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.logAttributes(
                          request.getId(), Collections.singletonList(request.getAttribute())),
                  executor)
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
                          "experiment attribute added successfully"),
                  executor)
              .thenApply(
                  unused -> AddAttributes.Response.newBuilder().setStatus(true).build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
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
          InternalFuture.runAsync(
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
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.logAttributes(
                          request.getId(), request.getAttributesList()),
                  executor)
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
                                          request.getAttributesList().stream()
                                              .map(KeyValue::getKey)
                                              .collect(Collectors.toSet()),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "experiment attributes added successfully")
                          .thenApply(eventLoggedStatus -> updatedExperiment, executor),
                  executor)
              .thenApply(
                  experiment ->
                      AddExperimentAttributes.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
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
              },
              executor);
      final var response =
          requestValidationFuture.thenCompose(
              unused -> futureExperimentDAO.getExperimentAttributes(request), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperimentAttributes(
      DeleteExperimentAttributes request, StreamObserver<Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
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
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.deleteAttributes(request), executor)
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
                        .thenApply(unused -> updatedExperiment, executor);
                  },
                  executor)
              .thenApply(
                  experiment ->
                      DeleteExperimentAttributes.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperiment(
      DeleteExperiment request, StreamObserver<DeleteExperiment.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessages = "Experiment ID not found in request";
                  throw new InvalidArgumentException(errorMessages);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(
                  unused ->
                      futureExperimentDAO.deleteExperiments(
                          DeleteExperiments.newBuilder().addIds(request.getId()).build()),
                  executor)
              .thenApply(this::loggedDeleteExperimentEvents, executor)
              .thenApply(
                  unused -> DeleteExperiment.Response.newBuilder().setStatus(true).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
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
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessages = "Experiment ID not found in request";
                  throw new InvalidArgumentException(errorMessages);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.logCodeVersion(request), executor)
              .thenCompose(
                  updatedExperiment ->
                      // Add succeeded event in local DB
                      addEvent(
                              updatedExperiment.getId(),
                              updatedExperiment.getProjectId(),
                              UPDATE_EVENT_TYPE,
                              Optional.of("code_version"),
                              Collections.emptyMap(),
                              "experiment code_version added successfully")
                          .thenApply(unused -> updatedExperiment, executor),
                  executor)
              .thenApply(
                  updatedExperiment ->
                      LogExperimentCodeVersion.Response.newBuilder()
                          .setExperiment(updatedExperiment)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
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
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in GetExperimentCodeVersion request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture.thenCompose(
              unused -> futureExperimentDAO.getExperimentCodeVersion(request), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void findExperiments(
      FindExperiments request, StreamObserver<FindExperiments.Response> responseObserver) {
    try {
      final var futureResponse = futureExperimentDAO.findExperiments(request);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
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
              },
              executor);
      final var futureResponse =
          requestValidationFuture.thenCompose(
              unused -> futureExperimentDAO.getUrlForArtifact(request), executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
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
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in LogArtifacts request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getArtifactsList().isEmpty()) {
                  var errorMessage = "Artifacts not found in LogArtifacts request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var futureResponse =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.logArtifacts(request), executor)
              .thenCompose(
                  updatedExperiment ->
                      // Add succeeded event in local DB
                      addEvent(
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
                          .thenApply(unused -> updatedExperiment, executor),
                  executor)
              .thenApply(
                  experiment ->
                      LogExperimentArtifacts.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in GetArtifacts request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.getArtifacts(request), executor)
              .thenApply(
                  artifacts ->
                      GetArtifacts.Response.newBuilder().addAllArtifacts(artifacts).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
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
          InternalFuture.runAsync(
              () -> {
                if (request.getId().isEmpty()) {
                  var errorMessage = "Experiment ID not found in DeleteArtifact request";
                  throw new InvalidArgumentException(errorMessage);
                } else if (request.getKey().isEmpty()) {
                  var errorMessage = "Artifact key not found in DeleteArtifact request";
                  throw new InvalidArgumentException(errorMessage);
                }
              },
              executor);
      final var futureResponse =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.deleteArtifacts(request), executor)
              .thenCompose(
                  updatedExperiment ->
                      // Add succeeded event in local DB
                      addEvent(
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
                          .thenApply(unused -> updatedExperiment, executor),
                  executor)
              .thenApply(
                  experiment ->
                      DeleteExperimentArtifact.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteExperiments(
      DeleteExperiments request, StreamObserver<DeleteExperiments.Response> responseObserver) {
    try {
      final var requestValidationFuture =
          InternalFuture.runAsync(
              () -> {
                if (request.getIdsList().isEmpty()) {
                  var errorMessages = "Experiment IDs not found in request";
                  throw new InvalidArgumentException(errorMessages);
                }
              },
              executor);
      final var response =
          requestValidationFuture
              .thenCompose(unused -> futureExperimentDAO.deleteExperiments(request), executor)
              .thenApply(this::loggedDeleteExperimentEvents, executor)
              .thenApply(
                  unused -> DeleteExperiments.Response.newBuilder().setStatus(true).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  private InternalFuture<List<Void>> loggedDeleteExperimentEvents(
      Map<String, String> experimentIdsMap) {
    // Add succeeded event in local DB
    List<InternalFuture<Void>> futureList = new ArrayList<>();
    for (var entry : experimentIdsMap.entrySet()) {
      addEvent(
          entry.getKey(),
          entry.getValue(),
          DELETE_EXPERIMENT_EVENT_TYPE,
          Optional.empty(),
          Collections.emptyMap(),
          "experiment deleted successfully");
    }
    return InternalFuture.sequence(futureList, executor);
  }
}
