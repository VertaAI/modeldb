package ai.verta.modeldb.metadata;

import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.metadata.MetadataServiceGrpc.MetadataServiceImplBase;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.Status;
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
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        String errorMessage = "Invalid parameter set in GetLabelsRequest.Id";
        LOGGER.info(errorMessage);
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
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetLabelsRequest.Response.getDefaultInstance());
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
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        errorMessage = "Invalid parameter set in AddLabelsRequest.Id";
      } else if (request.getLabelsList().isEmpty()) {
        errorMessage = "labels not found in AddLabelsRequest request";
      }

      if (errorMessage != null) {
        LOGGER.info(errorMessage);
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
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, AddLabelsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateLabels(
      AddLabelsRequest request, StreamObserver<AddLabelsRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId() == null
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        throw new ModelDBException(
            "Invalid parameter set in AddLabelsRequest.Id", io.grpc.Status.Code.INVALID_ARGUMENT);
      }

      boolean status = metadataDAO.updateLabels(request.getId(), request.getLabelsList());
      responseObserver.onNext(AddLabelsRequest.Response.newBuilder().setStatus(status).build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, AddLabelsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getLabelIds(
      GetLabelIdsRequest request, StreamObserver<GetLabelIdsRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getLabelsList().isEmpty()) {
        throw new ModelDBException("Labels not found in GetLabelIdsRequest", Code.INVALID_ARGUMENT);
      }

      List<IdentificationType> labelIds =
          metadataDAO.getLabelIds(request.getLabelsList(), request.getOperator());
      responseObserver.onNext(GetLabelIdsRequest.Response.newBuilder().addAllIds(labelIds).build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetLabelIdsRequest.Response.getDefaultInstance());
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
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        errorMessage = "Invalid parameter set in GetLabelsRequest.Id";
      } else if (request.getLabelsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Labels not found in GetLabelsRequest";
      }

      if (errorMessage != null) {
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteLabelsRequest.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      boolean status =
          metadataDAO.deleteLabels(
              request.getId(), request.getLabelsList(), request.getDeleteAll());
      responseObserver.onNext(DeleteLabelsRequest.Response.newBuilder().setStatus(status).build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteLabelsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void addKeyValueProperties(
      AddKeyValuePropertiesRequest request,
      StreamObserver<AddKeyValuePropertiesRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId() == null
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        errorMessage = "Invalid parameter set in AddKeyValuePropertiesRequest.Id";
      } else if (request.getKeyValuePropertyList().isEmpty()) {
        errorMessage = "KeyValueProperties not found in AddKeyValuePropertiesRequest request";
      } else if (request.getPropertyName().isEmpty()) {
        errorMessage = "PropertyName not found in AddKeyValuePropertiesRequest request";
      }

      if (errorMessage != null) {
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddKeyValuePropertiesRequest.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      metadataDAO.addOrUpdateKeyValueProperties(request);
      responseObserver.onNext(AddKeyValuePropertiesRequest.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, AddKeyValuePropertiesRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getKeyValueProperties(
      GetKeyValuePropertiesRequest request,
      StreamObserver<GetKeyValuePropertiesRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId() == null
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        errorMessage = "Invalid parameter set in GetKeyValuePropertiesRequest.Id";
      } else if (request.getKeysList().isEmpty() && !request.getGetAll()) {
        errorMessage =
            "Keys not found OR getAll flag is false in GetKeyValuePropertiesRequest request";
      } else if (request.getPropertyName().isEmpty()) {
        errorMessage = "PropertyName not found in GetKeyValuePropertiesRequest request";
      }

      if (errorMessage != null) {
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetKeyValuePropertiesRequest.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValueStringProperty> keyValues = metadataDAO.getKeyValueProperties(request);
      responseObserver.onNext(
          GetKeyValuePropertiesRequest.Response.newBuilder()
              .addAllKeyValueProperty(keyValues)
              .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetKeyValuePropertiesRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteKeyValueProperties(
      DeleteKeyValuePropertiesRequest request,
      StreamObserver<DeleteKeyValuePropertiesRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId() == null
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        errorMessage = "Invalid parameter set in DeleteKeyValuePropertiesRequest.Id";
      } else if (request.getKeysList().isEmpty() && !request.getDeleteAll()) {
        errorMessage =
            "Keys not found OR deleteAll flag is false in DeleteKeyValuePropertiesRequest request";
      } else if (request.getPropertyName().isEmpty()) {
        errorMessage = "PropertyName not found in DeleteKeyValuePropertiesRequest request";
      }

      if (errorMessage != null) {
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteKeyValuePropertiesRequest.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      metadataDAO.deleteKeyValueProperties(request);
      responseObserver.onNext(DeleteKeyValuePropertiesRequest.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteKeyValuePropertiesRequest.Response.getDefaultInstance());
    }
  }
}
