package ai.verta.modeldb.data;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class ModelDataServiceImpl extends ModelDataServiceGrpc.ModelDataServiceImplBase {
  private static final Logger LOGGER = LogManager.getLogger(ModelDataServiceImpl.class);

  private final String modelDataStoragePath;

  public ModelDataServiceImpl(String modelDataStoragePath) {
    this.modelDataStoragePath = modelDataStoragePath;
  }

  @Override
  public void storeModelData(
      StoreModelDataRequest request,
      StreamObserver<StoreModelDataRequest.Response> responseObserver) {
    LOGGER.info("StoreModelData: " + request);
    super.storeModelData(request, responseObserver);
  }
    private String buildFileName(ModelDataMetadata metadata) {
        final String modelId = metadata.getModelId();
        final Long timestampMillis = metadata.getTimestampMillis();
        final String endpoint = metadata.getEndpoint();
        return modelDataStoragePath + "/" + modelId + "-" + endpoint + "-" + timestampMillis;
    }

    @Override
    public void storeModelData(StoreModelDataRequest request, StreamObserver<StoreModelDataRequest.Response> responseObserver) {
        LOGGER.info("StoreModelData: " + request);
        final ModelDataMetadata metadata = request.getModelData().getMetadata();
        final String data = request.getModelData().getData();
        final String fileName = buildFileName(metadata);
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(data);
        } catch (IOException ex) {
            LOGGER.error(ex);
        }
    }

  @Override
  public void getModelData(
      GetModelDataRequest request, StreamObserver<GetModelDataRequest.Response> responseObserver) {
    LOGGER.info("GetModelData: " + request);
    super.getModelData(request, responseObserver);
  }

  @Override
  public void getModelDataDiff(
      GetModelDataRequest request, StreamObserver<GetModelDataRequest.Response> responseObserver) {
    LOGGER.info("GetModelDataDiff: " + request);
    super.getModelDataDiff(request, responseObserver);
  }
}
