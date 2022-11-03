package ai.verta.modeldb.common.futures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.grpc.Context;
import java.util.concurrent.atomic.AtomicReference;
import org.jdbi.v3.core.statement.StatementExceptions;
import org.junit.jupiter.api.Test;

class FutureJdbiTest {

  @Test
  void contextWithHandle() throws Exception {
    InternalJdbi internalJdbi = createMockInternalJdbi(mock(Handle.class));

    FutureJdbi subject = new FutureJdbi(internalJdbi, FutureExecutor.newSingleThreadExecutor());

    Context.Key<String> testKey = Context.key("cheese");

    Context contextWithData = Context.ROOT.withValue(testKey, "cheddar");
    AtomicReference<String> captured = new AtomicReference<>();
    InternalFuture<String> result =
        contextWithData
            .wrap(
                () ->
                    subject.withHandle(
                        h -> {
                          captured.set(testKey.get());
                          return "foo";
                        }))
            .call();
    assertThat(result.get()).isEqualTo("foo");
    assertThat(captured).hasValue("cheddar");
  }

  @Test
  void contextUseHandle() throws Exception {
    InternalJdbi internalJdbi = createMockInternalJdbi(mock(Handle.class));

    FutureJdbi subject = new FutureJdbi(internalJdbi, FutureExecutor.newSingleThreadExecutor());

    Context.Key<String> testKey = Context.key("cheese");

    Context contextWithData = Context.ROOT.withValue(testKey, "cheddar");
    AtomicReference<String> captured = new AtomicReference<>();
    contextWithData
        .wrap(
            () ->
                subject.useHandle(
                    h -> {
                      captured.set(testKey.get());
                    }))
        .call()
        .get();
    assertThat(captured).hasValue("cheddar");
  }

  @SuppressWarnings("unchecked")
  private static InternalJdbi createMockInternalJdbi(Handle handle) {
    InternalJdbi internalJdbi = mock(InternalJdbi.class);
    when(internalJdbi.getConfig(StatementExceptions.class))
        .thenReturn(mock(StatementExceptions.class));
    doAnswer(
            invocation -> {
              HandleConsumer<?> callback = invocation.getArgument(0);
              callback.useHandle(handle);
              return null;
            })
        .when(internalJdbi)
        .useHandle(isA(HandleConsumer.class));
    doAnswer(
            invocation -> {
              HandleCallback<?, ?> callback = invocation.getArgument(0);
              return callback.withHandle(handle);
            })
        .when(internalJdbi)
        .withHandle(isA(HandleCallback.class));
    return internalJdbi;
  }
}
