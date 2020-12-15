package ai.verta.modeldb.data;

import io.grpc.stub.StreamObserver;

public class ModelDataServiceImpl extends ModelDataServiceGrpc.ModelDataServiceImplBase {

  @Override
  public void storeModelData(
      StoreModelDataRequest request,
      StreamObserver<StoreModelDataRequest.Response> responseObserver) {
    super.storeModelData(request, responseObserver);
  }

  @Override
  public void getModelData(
      GetModelDataRequest request, StreamObserver<GetModelDataRequest.Response> responseObserver) {
    super.getModelData(request, responseObserver);
  }

  @Override
  public void getModelDataDiff(
      GetModelDataRequest request, StreamObserver<GetModelDataRequest.Response> responseObserver) {
    super.getModelDataDiff(request, responseObserver);
  }
}
