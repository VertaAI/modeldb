package ai.verta.modeldb.experimentRun;

import ai.verta.modeldb.*;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.futures.FutureGrpc;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.Executor;

public class FutureExperimentRunServiceImpl extends ExperimentRunServiceImpl {
  private final Executor executor;
  private final FutureExperimentRunDAO futureExperimentRunDAO;

  public FutureExperimentRunServiceImpl(ServiceSet serviceSet, DAOSet daoSet, Executor executor) {
    super(serviceSet, daoSet);
    this.executor = executor;
    this.futureExperimentRunDAO = daoSet.futureExperimentRunDAO;
  }

  @Override
  public void createExperimentRun(
      CreateExperimentRun request, StreamObserver<CreateExperimentRun.Response> responseObserver) {
    super.createExperimentRun(request, responseObserver);
  }

  @Override
  public void deleteExperimentRun(
      DeleteExperimentRun request, StreamObserver<DeleteExperimentRun.Response> responseObserver) {
    super.deleteExperimentRun(request, responseObserver);
  }

  @Override
  public void getExperimentRunsInProject(
      GetExperimentRunsInProject request,
      StreamObserver<GetExperimentRunsInProject.Response> responseObserver) {
    super.getExperimentRunsInProject(request, responseObserver);
  }

  @Override
  public void getExperimentRunsInExperiment(
      GetExperimentRunsInExperiment request,
      StreamObserver<GetExperimentRunsInExperiment.Response> responseObserver) {
    super.getExperimentRunsInExperiment(request, responseObserver);
  }

  @Override
  public void getExperimentRunById(
      GetExperimentRunById request,
      StreamObserver<GetExperimentRunById.Response> responseObserver) {
    super.getExperimentRunById(request, responseObserver);
  }

  @Override
  public void getExperimentRunByName(
      GetExperimentRunByName request,
      StreamObserver<GetExperimentRunByName.Response> responseObserver) {
    super.getExperimentRunByName(request, responseObserver);
  }

  @Override
  public void updateExperimentRunName(
      UpdateExperimentRunName request,
      StreamObserver<UpdateExperimentRunName.Response> responseObserver) {
    super.updateExperimentRunName(request, responseObserver);
  }

  @Override
  public void updateExperimentRunDescription(
      UpdateExperimentRunDescription request,
      StreamObserver<UpdateExperimentRunDescription.Response> responseObserver) {
    super.updateExperimentRunDescription(request, responseObserver);
  }

  @Override
  public void addExperimentRunTags(
      AddExperimentRunTags request,
      StreamObserver<AddExperimentRunTags.Response> responseObserver) {
    try {
      final var response =
          futureExperimentRunDAO
              .addTags(request)
              .thenApply(unused -> AddExperimentRunTags.Response.newBuilder().build(), executor);
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
              .thenApply(unused -> DeleteExperimentRunTags.Response.newBuilder().build(), executor);
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
              .thenApply(unused -> AddExperimentRunTag.Response.newBuilder().build(), executor);
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
              .thenApply(unused -> DeleteExperimentRunTag.Response.newBuilder().build(), executor);
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
    super.logDataset(request, responseObserver);
  }

  @Override
  public void logDatasets(
      LogDatasets request, StreamObserver<LogDatasets.Response> responseObserver) {
    super.logDatasets(request, responseObserver);
  }

  @Override
  public void getDatasets(
      GetDatasets request, StreamObserver<GetDatasets.Response> responseObserver) {
    super.getDatasets(request, responseObserver);
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
    super.logEnvironment(request, responseObserver);
  }

  @Override
  public void logExperimentRunCodeVersion(
      LogExperimentRunCodeVersion request,
      StreamObserver<LogExperimentRunCodeVersion.Response> responseObserver) {
    super.logExperimentRunCodeVersion(request, responseObserver);
  }

  @Override
  public void getExperimentRunCodeVersion(
      GetExperimentRunCodeVersion request,
      StreamObserver<GetExperimentRunCodeVersion.Response> responseObserver) {
    super.getExperimentRunCodeVersion(request, responseObserver);
  }

  @Override
  public void logArtifact(
      LogArtifact request, StreamObserver<LogArtifact.Response> responseObserver) {
    super.logArtifact(request, responseObserver);
  }

  @Override
  public void logArtifacts(
      LogArtifacts request, StreamObserver<LogArtifacts.Response> responseObserver) {
    super.logArtifacts(request, responseObserver);
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    super.getArtifacts(request, responseObserver);
  }

  @Override
  public void deleteArtifact(
      DeleteArtifact request, StreamObserver<DeleteArtifact.Response> responseObserver) {
    super.deleteArtifact(request, responseObserver);
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    super.getUrlForArtifact(request, responseObserver);
  }

  @Override
  public void commitArtifactPart(
      CommitArtifactPart request, StreamObserver<CommitArtifactPart.Response> responseObserver) {
    super.commitArtifactPart(request, responseObserver);
  }

  @Override
  public void getCommittedArtifactParts(
      GetCommittedArtifactParts request,
      StreamObserver<GetCommittedArtifactParts.Response> responseObserver) {
    super.getCommittedArtifactParts(request, responseObserver);
  }

  @Override
  public void commitMultipartArtifact(
      CommitMultipartArtifact request,
      StreamObserver<CommitMultipartArtifact.Response> responseObserver) {
    super.commitMultipartArtifact(request, responseObserver);
  }

  @Override
  public void findExperimentRuns(
      FindExperimentRuns request, StreamObserver<FindExperimentRuns.Response> responseObserver) {
    super.findExperimentRuns(request, responseObserver);
  }

  @Override
  public void sortExperimentRuns(
      SortExperimentRuns request, StreamObserver<SortExperimentRuns.Response> responseObserver) {
    super.sortExperimentRuns(request, responseObserver);
  }

  @Override
  public void getTopExperimentRuns(
      TopExperimentRunsSelector request,
      StreamObserver<TopExperimentRunsSelector.Response> responseObserver) {
    super.getTopExperimentRuns(request, responseObserver);
  }

  @Override
  public void logJobId(LogJobId request, StreamObserver<LogJobId.Response> responseObserver) {
    super.logJobId(request, responseObserver);
  }

  @Override
  public void getJobId(GetJobId request, StreamObserver<GetJobId.Response> responseObserver) {
    super.getJobId(request, responseObserver);
  }

  @Override
  public void getChildrenExperimentRuns(
      GetChildrenExperimentRuns request,
      StreamObserver<GetChildrenExperimentRuns.Response> responseObserver) {
    super.getChildrenExperimentRuns(request, responseObserver);
  }

  @Override
  public void setParentExperimentRunId(
      SetParentExperimentRunId request,
      StreamObserver<SetParentExperimentRunId.Response> responseObserver) {
    super.setParentExperimentRunId(request, responseObserver);
  }

  @Override
  public void getExperimentRunsByDatasetVersionId(
      GetExperimentRunsByDatasetVersionId request,
      StreamObserver<GetExperimentRunsByDatasetVersionId.Response> responseObserver) {
    super.getExperimentRunsByDatasetVersionId(request, responseObserver);
  }

  @Override
  public void deleteExperimentRuns(
      DeleteExperimentRuns request,
      StreamObserver<DeleteExperimentRuns.Response> responseObserver) {
    super.deleteExperimentRuns(request, responseObserver);
  }

  @Override
  public void logVersionedInput(
      LogVersionedInput request, StreamObserver<LogVersionedInput.Response> responseObserver) {
    super.logVersionedInput(request, responseObserver);
  }

  @Override
  public void getVersionedInputs(
      GetVersionedInput request, StreamObserver<GetVersionedInput.Response> responseObserver) {
    super.getVersionedInputs(request, responseObserver);
  }

  @Override
  public void listCommitExperimentRuns(
      ListCommitExperimentRunsRequest request,
      StreamObserver<ListCommitExperimentRunsRequest.Response> responseObserver) {
    super.listCommitExperimentRuns(request, responseObserver);
  }

  @Override
  public void listBlobExperimentRuns(
      ListBlobExperimentRunsRequest request,
      StreamObserver<ListBlobExperimentRunsRequest.Response> responseObserver) {
    super.listBlobExperimentRuns(request, responseObserver);
  }

  @Override
  public void cloneExperimentRun(
      CloneExperimentRun request, StreamObserver<CloneExperimentRun.Response> responseObserver) {
    super.cloneExperimentRun(request, responseObserver);
  }
}
