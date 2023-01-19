package ai.verta.modeldb.metadata;

import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.metadata.MetadataServiceGrpc.MetadataServiceImplBase;
import com.google.rpc.Code;
import com.oblac.nomen.Nomen;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MetadataServiceImpl extends MetadataServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(MetadataServiceImpl.class);
  private final MetadataDAO metadataDAO;

  public MetadataServiceImpl(DAOSet daoSet) {
    this.metadataDAO = daoSet.getMetadataDAO();
  }

  @Override
  public void getLabels(
      GetLabelsRequest request, StreamObserver<GetLabelsRequest.Response> responseObserver) {
    try {
      if (request.getId() == null
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        var errorMessage = "Invalid parameter set in GetLabelsRequest.Id";
        throw new InvalidArgumentException(errorMessage);
      }

      List<String> labels = metadataDAO.getLabels(request.getId());
      responseObserver.onNext(GetLabelsRequest.Response.newBuilder().addAllLabels(labels).build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addLabels(
      AddLabelsRequest request, StreamObserver<AddLabelsRequest.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId() == null
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        errorMessage = "Invalid parameter set in AddLabelsRequest.Id";
      } else if (request.getLabelsList().isEmpty()) {
        errorMessage = "labels not found in AddLabelsRequest request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      boolean status = metadataDAO.addLabels(request.getId(), request.getLabelsList());
      responseObserver.onNext(AddLabelsRequest.Response.newBuilder().setStatus(status).build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateLabels(
      AddLabelsRequest request, StreamObserver<AddLabelsRequest.Response> responseObserver) {
    try {
      if (request.getId() == null
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        throw new ModelDBException(
            "Invalid parameter set in AddLabelsRequest.Id", Code.INVALID_ARGUMENT);
      }

      boolean status = metadataDAO.updateLabels(request.getId(), request.getLabelsList());
      responseObserver.onNext(AddLabelsRequest.Response.newBuilder().setStatus(status).build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getLabelIds(
      GetLabelIdsRequest request, StreamObserver<GetLabelIdsRequest.Response> responseObserver) {
    try {
      if (request.getLabelsList().isEmpty()) {
        throw new ModelDBException("Labels not found in GetLabelIdsRequest", Code.INVALID_ARGUMENT);
      }

      List<IdentificationType> labelIds =
          metadataDAO.getLabelIds(request.getLabelsList(), request.getOperator());
      responseObserver.onNext(GetLabelIdsRequest.Response.newBuilder().addAllIds(labelIds).build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e);
    }
  }

  @Override
  public void deleteLabels(
      DeleteLabelsRequest request, StreamObserver<DeleteLabelsRequest.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId() == null
          || (request.getId().getIntId() == 0 && request.getId().getStringId().isEmpty())) {
        errorMessage = "Invalid parameter set in GetLabelsRequest.Id";
      } else if (request.getLabelsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Labels not found in GetLabelsRequest";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      boolean status =
          metadataDAO.deleteLabels(
              request.getId(), request.getLabelsList(), request.getDeleteAll());
      responseObserver.onNext(DeleteLabelsRequest.Response.newBuilder().setStatus(status).build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e);
    }
  }

  @Override
  public void addKeyValueProperties(
      AddKeyValuePropertiesRequest request,
      StreamObserver<AddKeyValuePropertiesRequest.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      metadataDAO.addKeyValueProperties(request);
      responseObserver.onNext(AddKeyValuePropertiesRequest.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e);
    }
  }

  @Override
  public void getKeyValueProperties(
      GetKeyValuePropertiesRequest request,
      StreamObserver<GetKeyValuePropertiesRequest.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      List<KeyValueStringProperty> keyValues = metadataDAO.getKeyValueProperties(request);
      responseObserver.onNext(
          GetKeyValuePropertiesRequest.Response.newBuilder()
              .addAllKeyValueProperty(keyValues)
              .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e);
    }
  }

  @Override
  public void deleteKeyValueProperties(
      DeleteKeyValuePropertiesRequest request,
      StreamObserver<DeleteKeyValuePropertiesRequest.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      metadataDAO.deleteKeyValueProperties(request);
      responseObserver.onNext(DeleteKeyValuePropertiesRequest.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e);
    }
  }

  public static String createRandomName() {
    return Nomen.est().adjective().color().animal().withSeparator("-").get();
  }

  @Override
  public void generateRandomName(
      GenerateRandomNameRequest request,
      StreamObserver<GenerateRandomNameRequest.Response> responseObserver) {
    try {
      responseObserver.onNext(
          GenerateRandomNameRequest.Response.newBuilder()
              .setName(MetadataServiceImpl.createRandomName())
              .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e);
    }
  }
}
