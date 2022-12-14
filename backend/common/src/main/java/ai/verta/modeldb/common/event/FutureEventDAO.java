package ai.verta.modeldb.common.event;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import com.google.gson.JsonObject;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FutureEventDAO {
  private static final Logger LOGGER = LogManager.getLogger(FutureEventDAO.class);
  private final FutureExecutor executor;
  private final FutureJdbi jdbi;
  private final Config config;
  private final String serviceType;

  public FutureEventDAO(
      FutureExecutor executor, FutureJdbi jdbi, Config config, String serviceType) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.config = config;
    this.serviceType = serviceType;
  }

  public void addLocalEventWithBlocking(
      String resourceType, String eventType, long workspaceId, JsonObject eventMetadata) {
    try {
      addLocalEvent(resourceType, eventType, workspaceId, eventMetadata).get();
    } catch (Exception e) {
      throw new ModelDBException(e);
    }
  }

  public InternalFuture<Void> addLocalEventWithAsync(
      String resourceType, String eventType, long workspaceId, JsonObject eventMetadata) {
    return addLocalEvent(resourceType, eventType, workspaceId, eventMetadata);
  }

  private InternalFuture<Void> addLocalEvent(
      String resourceType, String eventType, long workspaceId, JsonObject eventMetadata) {
    if (!config.isEvent_system_enabled()) {
      LOGGER.info("Event system is not enabled");
      return InternalFuture.completedInternalFuture(null);
    }

    if (workspaceId == 0) {
      LOGGER.info("workspaceId not found while logging event");
      return InternalFuture.failedStage(new ModelDBException("Workspace id should not be empty"));
    }

    eventMetadata.addProperty("service", serviceType);
    eventMetadata.addProperty("resource_type", resourceType);
    eventMetadata.addProperty("logged_time", new Date().getTime());

    return jdbi.useHandle(
            handle ->
                handle
                    .createUpdate(
                        "insert into event (event_uuid, event_type, workspace_id, event_metadata) values (:event_uuid, :event_type, :workspace_id, :event_metadata) ")
                    .bind("event_uuid", UUID.randomUUID().toString())
                    .bind("event_type", eventType)
                    .bind("workspace_id", workspaceId)
                    .bind("event_metadata", eventMetadata.toString())
                    .execute())
        .thenAccept(unused -> LOGGER.debug("Event added successfully"), executor);
  }

  public Void deleteLocalEventWithBlocking(List<String> eventUUIDs) {
    try {
      return deleteLocalEvent(eventUUIDs).get();
    } catch (Exception e) {
      throw new ModelDBException(e);
    }
  }

  public InternalFuture<Void> deleteLocalEventWithAsync(List<String> eventUUIDs) {
    return deleteLocalEvent(eventUUIDs);
  }

  private InternalFuture<Void> deleteLocalEvent(List<String> eventUUIDList) {
    if (!config.isEvent_system_enabled()) {
      LOGGER.info("Event system is not enabled");
      return InternalFuture.completedInternalFuture(null);
    }

    Set<String> eventUUIDs =
        eventUUIDList.stream()
            .filter(eventUUID -> !eventUUID.isEmpty())
            .collect(Collectors.toSet());

    if (eventUUIDs.isEmpty()) {
      LOGGER.debug("0 out of 0 events deleted");
      return InternalFuture.completedInternalFuture(null);
    }

    return jdbi.useHandle(
            handle ->
                handle
                    .createUpdate("DELETE FROM event WHERE event_uuid IN (<eventUUIDs>) ")
                    .bindList("eventUUIDs", eventUUIDs)
                    .execute())
        .thenAccept(unused -> LOGGER.debug("Events deleted successfully"), executor);
  }
}
