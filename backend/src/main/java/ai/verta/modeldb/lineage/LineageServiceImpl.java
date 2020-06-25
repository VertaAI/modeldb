package ai.verta.modeldb.lineage;

import ai.verta.modeldb.AddLineage;
import ai.verta.modeldb.DeleteLineage;
import ai.verta.modeldb.FindAllInputs;
import ai.verta.modeldb.FindAllInputsOutputs;
import ai.verta.modeldb.FindAllOutputs;
import ai.verta.modeldb.LineageEntryEnum.LineageEntryType;
import ai.verta.modeldb.LineageServiceGrpc.LineageServiceImplBase;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.CommitDAO;
import io.grpc.Status.Code;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

public class LineageServiceImpl extends LineageServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(LineageServiceImpl.class);
  private final ExperimentRunDAO experimentDAO;
  private final CommitDAO commitDAO;
  private LineageDAO lineageDAO;

  public LineageServiceImpl(
      LineageDAO lineageDAO, ExperimentRunDAO experimentRunDAO, CommitDAO commitDAO) {
    this.lineageDAO = lineageDAO;
    this.experimentDAO = experimentRunDAO;
    this.commitDAO = commitDAO;
  }

  @Override
  public void addLineage(AddLineage request, StreamObserver<AddLineage.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      if (request.getInputCount() == 0 && request.getOutputCount() == 0) {
        throw new ModelDBException("Input and output not specified", Code.INVALID_ARGUMENT);
      } else {
        if (request.getInputCount() == 0) {
          throw new ModelDBException("Input not specified", Code.INVALID_ARGUMENT);
        } else if (request.getOutputCount() == 0) {
          throw new ModelDBException("Output not specified", Code.INVALID_ARGUMENT);
        }
      }
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
        AddLineage.Response response = lineageDAO.addLineage(request, this::isResourceExists);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, AddLineage.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteLineage(
      DeleteLineage request, StreamObserver<DeleteLineage.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      if (request.getInputCount() == 0 && request.getOutputCount() == 0) {
        throw new ModelDBException("Input and output not specified", Code.INVALID_ARGUMENT);
      } else {
        if (request.getInputCount() == 0) {
          throw new ModelDBException("Input not specified", Code.INVALID_ARGUMENT);
        } else if (request.getOutputCount() == 0) {
          throw new ModelDBException("Output not specified", Code.INVALID_ARGUMENT);
        }
      }
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
        DeleteLineage.Response response = lineageDAO.deleteLineage(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, DeleteLineage.Response.getDefaultInstance());
    }
  }

  @Override
  public void findAllInputs(
      FindAllInputs request, StreamObserver<FindAllInputs.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      if (request.getItemsCount() == 0) {
        throw new ModelDBException("Items not specified", Code.INVALID_ARGUMENT);
      }
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
        FindAllInputs.Response response = lineageDAO.findAllInputs(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, FindAllInputs.Response.getDefaultInstance());
    }
  }

  @Override
  public void findAllOutputs(
      FindAllOutputs request, StreamObserver<FindAllOutputs.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      if (request.getItemsCount() == 0) {
        throw new ModelDBException("Items not specified", Code.INVALID_ARGUMENT);
      }
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
        FindAllOutputs.Response response = lineageDAO.findAllOutputs(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, FindAllOutputs.Response.getDefaultInstance());
    }
  }

  @Override
  public void findAllInputsOutputs(
      FindAllInputsOutputs request,
      StreamObserver<FindAllInputsOutputs.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      if (request.getItemsCount() == 0) {
        throw new ModelDBException("Items not specified", Code.INVALID_ARGUMENT);
      }
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
        FindAllInputsOutputs.Response response = lineageDAO.findAllInputsOutputs(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, FindAllInputsOutputs.Response.getDefaultInstance());
    }
  }

  private boolean isResourceExists(Session session, String id, LineageEntryType type)
      throws ModelDBException {
    switch (type) {
      case EXPERIMENT_RUN:
        return experimentDAO.isExperimentRunExists(session, id);
      case DATASET_VERSION:
        return commitDAO.isCommitExists(session, id);
      default:
        throw new ModelDBException("Unexpected type", Code.INTERNAL);
    }
  }
}
