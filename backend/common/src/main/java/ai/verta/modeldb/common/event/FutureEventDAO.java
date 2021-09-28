package ai.verta.modeldb.common.event;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

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
      String eventType, long workspaceId, String eventMetadata) {
    return addLocalEvent(eventType, workspaceId, eventMetadata).get();
  }

  public InternalFuture<Boolean> addLocalEventWithAsync(
      String eventType, long workspaceId, String eventMetadata) {
    return addLocalEvent(eventType, workspaceId, eventMetadata);
  }

  private InternalFuture<Boolean> addLocalEvent(
      String eventType, long workspaceId, String eventMetadata) {
    if (!config.isEvent_system_enabled()) {
      LOGGER.info("Event system is not enabled");
      return InternalFuture.completedInternalFuture(false);
    }

    if (workspaceId == 0){
      LOGGER.info("workspaceId not found while logging event");
      return InternalFuture.failedStage(new ModelDBException("Workspace id should not be empty"));
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

  public Boolean deleteLocalEventWithBlocking(List<String> eventUUIDs) {
    return deleteLocalEvent(eventUUIDs).get();
  }

  public InternalFuture<Boolean> deleteLocalEventWithAsync(List<String> eventUUIDs) {
    return deleteLocalEvent(eventUUIDs);
  }

  private InternalFuture<Boolean> deleteLocalEvent(List<String> eventUUIDList) {
    if (!config.isEvent_system_enabled()) {
      LOGGER.info("Event system is not enabled");
      return InternalFuture.completedInternalFuture(false);
    }

    Set<String> eventUUIDs = eventUUIDList.stream().filter(eventUUID -> !eventUUID.isEmpty()).collect(Collectors.toSet());

    if (eventUUIDs.isEmpty()){
      return InternalFuture.failedStage(new ModelDBException("Event UUID not found for deletion"));
    }

    return jdbi.withHandle(
            handle ->
                handle
                    .createUpdate("DELETE FROM event WHERE event_uuid IN (<eventUUIDs>) ")
                    .bindList("eventUUIDs", eventUUIDs)
                    .execute())
        .thenApply(
            insertedRowCount -> {
              LOGGER.debug("Events deleted successfully, Events UUID are {}", eventUUIDList);
              return insertedRowCount > 0;
            },
            executor);
  }
}
