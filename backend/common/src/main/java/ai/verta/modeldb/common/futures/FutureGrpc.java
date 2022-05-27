package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.CommonUtils;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

@SuppressWarnings({"squid:S100"})
public class FutureGrpc extends FutureUtil {
  private FutureGrpc() {}

  // Injects the result of the Scala future into the grpc StreamObserver as the return of the server
  public static <T extends GeneratedMessageV3> void ServerResponse(
      StreamObserver<T> observer, InternalFuture<T> f, Executor ex) {
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
