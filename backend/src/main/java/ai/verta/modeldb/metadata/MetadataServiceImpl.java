package ai.verta.modeldb.metadata;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.metadata.MetadataServiceGrpc.MetadataServiceImplBase;
import com.oblac.nomen.Nomen;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MetadataServiceImpl extends MetadataServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(MetadataServiceImpl.class);

  public MetadataServiceImpl() {}

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
          responseObserver, e, GenerateRandomNameRequest.Response.getDefaultInstance());
    }
  }
}
