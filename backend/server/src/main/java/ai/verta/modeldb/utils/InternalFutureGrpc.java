package ai.verta.modeldb.utils;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.futures.FutureExecutor;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.stub.StreamObserver;

public class InternalFutureGrpc {
  // Injects the result of the InternalFuture into the grpc StreamObserver as the return of the
  // server
  public static <T extends GeneratedMessageV3> void serverResponse(
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
