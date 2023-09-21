package ai.verta.modeldb.experimentRun;

import static org.apache.commons.lang3.StringUtils.isBlank;

import ai.verta.common.CodeVersion;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.*;
import ai.verta.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceImplBase;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.InternalFuture;
import com.google.protobuf.Value;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class FutureExperimentRunServiceImpl extends ExperimentRunServiceImplBase {

  private final FutureExecutor executor;
  private final FutureExperimentRunDAO futureExperimentRunDAO;

  public FutureExperimentRunServiceImpl(
      DAOSet daoSet, ServiceSet serviceSet, FutureExecutor executor) {
    this.executor = executor;
    this.futureExperimentRunDAO = daoSet.getFutureExperimentRunDAO();
  }

  @Override
  public void createExperimentRun(
      CreateExperimentRun request, StreamObserver<CreateExperimentRun.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .createExperimentRun(request)
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
              .thenApply(
                  unused -> DeleteExperimentRun.Response.newBuilder().setStatus(true).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
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
    try {

      if (isBlank(request.getName())) {
        var errorMessage = "Name is not present";
        throw new InvalidArgumentException(errorMessage);
      }

      final var response =
          futureExperimentRunDAO
              .updateExperimentRunName(request)
              .thenSupply(() -> Future.of(UpdateExperimentRunName.Response.getDefaultInstance()));

      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateExperimentRunDescription(
      UpdateExperimentRunDescription request,
      StreamObserver<UpdateExperimentRunDescription.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .updateExperimentRunDescription(request)
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
      final var response = futureExperimentRunDAO.cloneExperimentRun(request);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }
}
