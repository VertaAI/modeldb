package ai.verta.modeldb.common.reconcilers;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.futures.*;
import ai.verta.uac.CreateEventRequest;
import ai.verta.uac.Empty;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.Value;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;

public class SendEventsWithCleanUp extends Reconciler<CreateEventRequest> {
  private final UAC uac;
  private final FutureEventDAO futureEventDAO;

  public SendEventsWithCleanUp(
      ReconcilerConfig config,
      UAC uac,
      FutureEventDAO futureEventDAO,
      FutureJdbi futureJdbi,
      FutureExecutor executor) {
    super(config, LogManager.getLogger(SendEventsWithCleanUp.class), futureJdbi, executor, false);
    this.uac = uac;
    this.futureEventDAO = futureEventDAO;
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
  protected ReconcileResult reconcile(Set<CreateEventRequest> eventUUIDs) throws Exception {
    return Future.of(eventUUIDs)
        .thenCompose(
            createEventRequests -> {
              List<Future<String>> deletedEventUUIDFutures = new ArrayList<>();
              for (CreateEventRequest request : createEventRequests) {
                ListenableFuture<Empty> listenableFuture =
                    uac.getEventService().createEvent(request);
                deletedEventUUIDFutures.add(
                    Future.fromListenableFuture(listenableFuture)
                        .thenApply(empty -> request.getEventUuid()));
              }
              return Future.sequence(deletedEventUUIDFutures);
            })
        .thenCompose(
            deleteEventUUIDs -> {
              logger.debug("Ready to delete local events from database");
              Future<Void> statusFuture =
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
