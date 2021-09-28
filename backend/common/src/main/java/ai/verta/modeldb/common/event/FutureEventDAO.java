package ai.verta.modeldb.common.event;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FutureEventDAO {
  private static final Logger LOGGER = LogManager.getLogger(FutureEventDAO.class);
  private final Executor executor;
  private final FutureJdbi jdbi;
  private final Config config;

  public FutureEventDAO(Executor executor, FutureJdbi jdbi, Config config) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.config = config;
  }

  public Boolean addLocalEventWithBlocking(
      String eventType, Long workspaceId, String eventMetadata) {
    return addLocalEvent(eventType, workspaceId, eventMetadata).get();
  }

  public InternalFuture<Boolean> addLocalEventWithAsync(
      String eventType, Long workspaceId, String eventMetadata) {
    return addLocalEvent(eventType, workspaceId, eventMetadata);
  }

  private InternalFuture<Boolean> addLocalEvent(
      String eventType, Long workspaceId, String eventMetadata) {
    if (!config.isEvent_system_enabled()) {
      LOGGER.info("Event system is not enabled");
      return InternalFuture.completedInternalFuture(false);
    }

    return jdbi.withHandle(
            handle ->
                handle
                    .createUpdate(
                        "insert into event (event_uuid, event_type, workspace_id, event_metadata) values (:event_uuid, :event_type, :workspace_id, :event_metadata) ")
                    .bind("event_uuid", UUID.randomUUID().toString())
                    .bind("event_type", eventType)
                    .bind("workspace_id", workspaceId)
                    .bind("event_metadata", eventMetadata)
                    .execute())
        .thenApply(
            insertedRowCount -> {
              LOGGER.debug("Event added successfully");
              return insertedRowCount > 0;
            },
            executor);
  }

  public Boolean deleteLocalEventWithBlocking(String eventUUID) {
    return deleteLocalEvent(eventUUID).get();
  }

  public InternalFuture<Boolean> deleteLocalEventWithAsync(String eventUUID) {
    return deleteLocalEvent(eventUUID);
  }

  private InternalFuture<Boolean> deleteLocalEvent(String eventUUID) {
    if (!config.isEvent_system_enabled()) {
      LOGGER.info("Event system is not enabled");
      return InternalFuture.completedInternalFuture(false);
    }

    return jdbi.withHandle(
            handle ->
                handle
                    .createUpdate("DELETE FROM event WHERE event_uuid = :eventUUID ")
                    .bind("eventUUID", eventUUID)
                    .execute())
        .thenApply(
            insertedRowCount -> {
              LOGGER.debug("Event deleted successfully");
              return insertedRowCount > 0;
            },
            executor);
  }
}
