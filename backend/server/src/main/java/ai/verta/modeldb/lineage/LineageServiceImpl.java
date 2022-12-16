package ai.verta.modeldb.lineage;

import ai.verta.modeldb.AddLineage;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.DeleteLineage;
import ai.verta.modeldb.FindAllInputs;
import ai.verta.modeldb.FindAllInputsOutputs;
import ai.verta.modeldb.FindAllOutputs;
import ai.verta.modeldb.LineageEntryEnum.LineageEntryType;
import ai.verta.modeldb.LineageServiceGrpc.LineageServiceImplBase;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
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
  private final LineageDAO lineageDAO;

  public LineageServiceImpl(DAOSet daoSet) {
    this.lineageDAO = daoSet.getLineageDAO();
    this.experimentDAO = daoSet.getExperimentRunDAO();
    this.commitDAO = daoSet.getCommitDAO();
  }

  @Override
  public void addLineage(AddLineage request, StreamObserver<AddLineage.Response> responseObserver) {
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
      var response = lineageDAO.addLineage(request, this::isResourceExists);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, AddLineage.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteLineage(
      DeleteLineage request, StreamObserver<DeleteLineage.Response> responseObserver) {
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
      var response = lineageDAO.deleteLineage(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, DeleteLineage.Response.getDefaultInstance());
    }
  }

  @Override
  public void findAllInputs(
      FindAllInputs request, StreamObserver<FindAllInputs.Response> responseObserver) {
    try {
      if (request.getItemsCount() == 0) {
        throw new ModelDBException(
            ModelDBMessages.ITEMS_NOT_SPECIFIED_ERROR, Code.INVALID_ARGUMENT);
      }
      var response = lineageDAO.findAllInputs(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, FindAllInputs.Response.getDefaultInstance());
    }
  }

  @Override
  public void findAllOutputs(
      FindAllOutputs request, StreamObserver<FindAllOutputs.Response> responseObserver) {
    try {
      if (request.getItemsCount() == 0) {
        throw new ModelDBException(
            ModelDBMessages.ITEMS_NOT_SPECIFIED_ERROR, Code.INVALID_ARGUMENT);
      }
      var response = lineageDAO.findAllOutputs(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, FindAllOutputs.Response.getDefaultInstance());
    }
  }

  @Override
  public void findAllInputsOutputs(
      FindAllInputsOutputs request,
      StreamObserver<FindAllInputsOutputs.Response> responseObserver) {
    try {
      if (request.getItemsCount() == 0) {
        throw new ModelDBException(
            ModelDBMessages.ITEMS_NOT_SPECIFIED_ERROR, Code.INVALID_ARGUMENT);
      }
      var response = lineageDAO.findAllInputsOutputs(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
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
        throw new ModelDBException(
            "Unexpected LineageEntryType '" + type + "' found", Code.INTERNAL);
    }
  }
}
