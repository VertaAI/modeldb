package ai.verta.modeldb.common.reconcilers;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.uac.CreateEventRequest;
import com.google.protobuf.Any;
import com.google.protobuf.Value;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SendEventsWithCleanUp extends Reconciler<CreateEventRequest> {
  private static final Logger LOGGER = LogManager.getLogger(SendEventsWithCleanUp.class);
  private final UAC uac;
  private final FutureEventDAO futureEventDAO;

  public SendEventsWithCleanUp(
      ReconcilerConfig config,
      UAC uac,
      FutureEventDAO futureEventDAO,
      FutureJdbi futureJdbi,
      Executor executor) {
    super(config, LOGGER, futureJdbi, executor, false);
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
  protected ReconcileResult reconcile(Set<CreateEventRequest> eventUUIDs) {
    LOGGER.debug("Reconciling events " + eventUUIDs.toString());

    return InternalFuture.completedInternalFuture(eventUUIDs)
        .thenCompose(
            createEventRequests -> {
              List<InternalFuture<String>> deletedEventUUIDFutures = new ArrayList<>();
              for (CreateEventRequest request : createEventRequests) {
                deletedEventUUIDFutures.add(
                    FutureGrpc.ClientRequest(uac.getEventService().createEvent(request), executor)
                        .thenApply(empty -> request.getEventUuid(), executor));
              }
              return InternalFuture.sequence(deletedEventUUIDFutures, executor);
            },
            executor)
        .thenCompose(
            deleteEventUUIDs -> {
              LOGGER.debug("Ready to delete local events from database: {}", deleteEventUUIDs);
              InternalFuture<Boolean> statusFuture =
                  futureEventDAO.deleteLocalEventWithAsync(deleteEventUUIDs);
              LOGGER.debug("Deleted local events from database: {}", deleteEventUUIDs);
              return statusFuture;
            },
            executor)
        .thenApply(
            status -> {
              if (status) {
                LOGGER.debug("local events sent into the global event server successfully");
              } else {
                LOGGER.warn("failed to send local events in the global event server");
              }
              return new ReconcileResult();
            },
            executor)
        .get();
  }
}
