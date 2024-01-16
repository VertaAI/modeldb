package ai.verta.modeldb.experiment;

import ai.verta.common.KeyValueQuery;
import ai.verta.modeldb.*;
import ai.verta.modeldb.DeleteExperimentAttributes.Response;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceImplBase;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.modeldb.utils.InternalFutureGrpc;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FutureExperimentServiceImpl extends ExperimentServiceImplBase {

  private final FutureExecutor executor;
  private final FutureProjectDAO futureProjectDAO;
  private final FutureExperimentDAO futureExperimentDAO;

  public FutureExperimentServiceImpl(
      DAOSet daoSet, ServiceSet serviceSet, FutureExecutor executor) {
    this.executor = executor;
    this.futureProjectDAO = daoSet.getFutureProjectDAO();
    this.futureExperimentDAO = daoSet.getFutureExperimentDAO();
  }

  @Override
  public void createExperiment(
      CreateExperiment request, StreamObserver<CreateExperiment.Response> responseObserver) {
    try {
      final var futureResponse =
          futureExperimentDAO
              .createExperiment(request)
              .thenApply(
                  createdExperiment ->
                      CreateExperiment.Response.newBuilder()
                          .setExperiment(createdExperiment)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
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
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  experiment ->
                      UpdateExperimentNameOrDescription.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  experiment ->
                      UpdateExperimentName.Response.newBuilder().setExperiment(experiment).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  experiment ->
                      UpdateExperimentDescription.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  experiment ->
                      AddExperimentTags.Response.newBuilder().setExperiment(experiment).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  experiment ->
                      AddExperimentTag.Response.newBuilder().setExperiment(experiment).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  experiment ->
                      DeleteExperimentTags.Response.newBuilder().setExperiment(experiment).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  experiment ->
                      DeleteExperimentTag.Response.newBuilder().setExperiment(experiment).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  unused -> AddAttributes.Response.newBuilder().setStatus(true).build(), executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  experiment ->
                      AddExperimentAttributes.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  experiment ->
                      DeleteExperimentAttributes.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  unused -> DeleteExperiment.Response.newBuilder().setStatus(true).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  updatedExperiment ->
                      LogExperimentCodeVersion.Response.newBuilder()
                          .setExperiment(updatedExperiment)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void findExperiments(
      FindExperiments request, StreamObserver<FindExperiments.Response> responseObserver) {
    try {
      final var futureResponse = futureExperimentDAO.findExperiments(request);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
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
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
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
              .thenApply(
                  experiment ->
                      LogExperimentArtifacts.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
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
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
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
              .thenApply(
                  experiment ->
                      DeleteExperimentArtifact.Response.newBuilder()
                          .setExperiment(experiment)
                          .build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, futureResponse, executor);
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
              .thenApply(
                  unused -> DeleteExperiments.Response.newBuilder().setStatus(true).build(),
                  executor);
      InternalFutureGrpc.serverResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }
}
