package ai.verta.modeldb.metadata;

import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.metadata.MetadataServiceGrpc.MetadataServiceImplBase;
import ai.verta.modeldb.monitoring.ErrorCountResource;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MetadataServiceImpl extends MetadataServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(MetadataServiceImpl.class);
  private final MetadataDAO metadataDAO;

  public MetadataServiceImpl(MetadataDAO metadataDAO) {
    this.metadataDAO = metadataDAO;
  }

  @Override
  public void getLabels(
      GetLabelsRequest request, StreamObserver<GetLabelsRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getId() == null
          || request.getId().getIdTypeValue() == 0
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        String errorMessage = "Invalid parameter set in GetLabelsRequest.Id";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetLabelsRequest.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<String> labels = metadataDAO.getLabels(request.getId());
      responseObserver.onNext(GetLabelsRequest.Response.newBuilder().addAllLabels(labels).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetLabelsRequest.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addLabels(
      AddLabelsRequest request, StreamObserver<AddLabelsRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId() == null
          || request.getId().getIdTypeValue() == 0
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        errorMessage = "Invalid parameter set in AddLabelsRequest.Id";
      } else if (request.getLabelsList().isEmpty()) {
        errorMessage = "labels not found in AddLabelsRequest request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddLabelsRequest.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      boolean status = metadataDAO.addLabels(request.getId(), request.getLabelsList());
      responseObserver.onNext(AddLabelsRequest.Response.newBuilder().setStatus(status).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AddLabelsRequest.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteLabels(
      DeleteLabelsRequest request, StreamObserver<DeleteLabelsRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId() == null
          || request.getId().getIdTypeValue() == 0
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        errorMessage = "Invalid parameter set in GetLabelsRequest.Id";
      } else if (request.getLabelsList().isEmpty()) {
        errorMessage = "Labels not found in GetLabelsRequest";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetLabelsRequest.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      boolean status = metadataDAO.deleteLabels(request.getId(), request.getLabelsList());
      responseObserver.onNext(DeleteLabelsRequest.Response.newBuilder().setStatus(status).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(DeleteLabelsRequest.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }
}
