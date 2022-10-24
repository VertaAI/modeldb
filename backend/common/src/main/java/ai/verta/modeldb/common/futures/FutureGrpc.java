package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.CommonUtils;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.Executor;

@SuppressWarnings({"squid:S100"})
public class FutureGrpc {
  private FutureGrpc() {}

  // Injects the result of the Scala future into the grpc StreamObserver as the return of the server
  public static <T extends GeneratedMessageV3> void ServerResponse(
      StreamObserver<T> observer, InternalFuture<T> f, FutureExecutor ex) {
    f.whenComplete(
        (v, t) -> {
          if (t == null) {
            observer.onNext(v);
            observer.onCompleted();
          } else {
            CommonUtils.observeError(observer, t);
          }
        },
        ex);
  }
}
