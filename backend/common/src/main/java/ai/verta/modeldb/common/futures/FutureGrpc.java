package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.CommonUtils;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.stub.StreamObserver;

@SuppressWarnings({"squid:S100"})
public class FutureGrpc {
  private FutureGrpc() {}

  public static <T extends GeneratedMessageV3> void serverResponse(
      StreamObserver<T> observer, Future<T> f) {
    f.whenComplete(
        (v, t) -> {
          if (t == null) {
            observer.onNext(v);
            observer.onCompleted();
          } else {
            CommonUtils.observeError(observer, t);
          }
        });
  }
}
