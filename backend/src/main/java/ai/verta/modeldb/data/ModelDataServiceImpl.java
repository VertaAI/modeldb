package ai.verta.modeldb.data;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
