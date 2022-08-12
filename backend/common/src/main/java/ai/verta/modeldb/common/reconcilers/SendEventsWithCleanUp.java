package ai.verta.modeldb.common.reconcilers;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.futures.InternalFutureWithDefaultExecutor;
import ai.verta.uac.CreateEventRequest;
import com.google.protobuf.Any;
import com.google.protobuf.Value;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;

public class SendEventsWithCleanUp extends Reconciler<CreateEventRequest> {
  private final UAC uac;
  private final FutureEventDAO futureEventDAO;

  private final InternalFutureWithDefaultExecutor.FactoryWithExecutor futureFactory;

  public SendEventsWithCleanUp(
      ReconcilerConfig config,
      UAC uac,
      FutureEventDAO futureEventDAO,
      FutureJdbi futureJdbi,
      Executor executor) {
    super(config, LogManager.getLogger(SendEventsWithCleanUp.class), futureJdbi, executor, false);
    this.uac = uac;
    this.futureEventDAO = futureEventDAO;
    this.futureFactory = InternalFuture.withExecutor(executor);
  }

  @Override
  public void resync() {
    futureJdbi.useHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT event_uuid, event_type, workspace_id, event_metadata FROM event")
                .setFetchSize(config.getMaxSync())
                .map(
                    (rs, ctx) -> {
                      Value.Builder valueBuilder = Value.newBuilder();
                      CommonUtils.getProtoObjectFromString(
                          rs.getString("event_metadata"), valueBuilder);
                      return CreateEventRequest.newBuilder()
                          .setEventUuid(rs.getString("event_uuid"))
                          .setEventType(rs.getString("event_type"))
                          .setWorkspaceId(rs.getLong("workspace_id"))
                          .setEventMetadata(Any.pack(valueBuilder.build()))
                          .build();
                    })
                .stream()
                .forEach(this::insert));
  }

  @Override
  protected ReconcileResult reconcile(Set<CreateEventRequest> eventUUIDs) {
    return futureFactory
        .completedInternalFuture(eventUUIDs)
        .thenCompose(
            createEventRequests -> {
              List<InternalFuture<String>> deletedEventUUIDFutures = new ArrayList<>();
              for (CreateEventRequest request : createEventRequests) {
                deletedEventUUIDFutures.add(
                    futureFactory
                        .from(uac.getEventService().createEvent(request))
                        .thenApply(empty -> request.getEventUuid()));
              }
              return futureFactory.sequence(deletedEventUUIDFutures);
            })
        .thenCompose(
            deleteEventUUIDs -> {
              logger.debug("Ready to delete local events from database");
              InternalFuture<Void> statusFuture =
                  futureEventDAO.deleteLocalEventWithAsync(deleteEventUUIDs);
              logger.debug("Deleted local events from database");
              return statusFuture;
            })
        .thenApply(
            status -> {
              logger.debug("local events sent into the global event server successfully");
              return new ReconcileResult();
            })
        .get();
  }
}
