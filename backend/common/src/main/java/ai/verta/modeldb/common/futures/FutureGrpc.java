package ai.verta.modeldb.common.futures;

import ai.verta.modeldb.common.CommonUtils;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FutureGrpc {
  // Converts a ListenableFuture, returned by a non-blocking call via grpc, to our custom
  // InternalFuture
  public static <T> InternalFuture<T> ClientRequest(ListenableFuture<T> f, Executor ex) {
    CompletableFuture<T> promise = new CompletableFuture<T>();
    Futures.addCallback(f, new Callback<T>(promise), ex);
    return InternalFuture.from(promise);
  }

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

  // Wraps an Executor and make it compatible with grpc's context
  private static Executor makeCompatibleExecutor(Executor ex) {
    return Context.currentContextExecutor(ex);
  }

  public static Executor initializeExecutor(Integer threadCount) {
    return FutureGrpc.makeCompatibleExecutor(
        Executors.newFixedThreadPool(threadCount, Executors.defaultThreadFactory()));
  }

  // Callback for a ListenableFuture to satisfy a promise
  private static class Callback<T> implements com.google.common.util.concurrent.FutureCallback<T> {
    final CompletableFuture<T> promise;

    private Callback(CompletableFuture<T> promise) {
      this.promise = promise;
    }

    @Override
    public void onSuccess(@NullableDecl T t) {
      promise.complete(t);
    }

    @Override
    public void onFailure(Throwable t) {
      promise.completeExceptionally(t);
    }
  }
}
