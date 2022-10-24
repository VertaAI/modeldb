package ai.verta.modeldb.common.futures;

import io.grpc.Context;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.springframework.lang.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class FutureExecutor implements Executor {
        final Executor other;

    FutureExecutor(Executor other) {
            this.other = io.opentelemetry.context.Context.taskWrapping(other);
        }

    // Wraps an Executor and make it compatible with grpc's context
    public static FutureExecutor makeCompatibleExecutor(Executor ex) {
        return new FutureExecutor(ex);
    }

    public static FutureExecutor initializeExecutor(Integer threadCount) {
        return makeCompatibleExecutor(
                new ForkJoinPool(
                        threadCount,
                        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                        Thread.getDefaultUncaughtExceptionHandler(),
                        true));
    }

    public static FutureExecutor newSingleThreadExecutor() {
        return makeCompatibleExecutor(Executors.newSingleThreadExecutor());
    }

        @Override
        public void execute(@NonNull Runnable r) {
            if (GlobalTracer.isRegistered()) {
                final var tracer = GlobalTracer.get();
                final var span = tracer.scopeManager().activeSpan();
                other.execute(
                        Context.current()
                                .wrap(
                                        () -> {
                                            try (Scope s = tracer.scopeManager().activate(span)) {
                                                r.run();
                                            }
                                        }));
            } else {
                other.execute(Context.current().wrap(r));
            }
        }
}
