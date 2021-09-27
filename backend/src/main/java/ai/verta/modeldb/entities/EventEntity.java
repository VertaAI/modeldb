package ai.verta.modeldb.entities;

import java.io.Serializable;
import java.util.Objects;

public class EventEntity implements Serializable {
  private EventEntity() {}

  String eventUUID; // uuid for this event to handle deduplication
  String eventType; // Service-specific name for this type of event
  Long workspaceId; // Required
  String eventMetadata;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EventEntity)) return false;
    EventEntity that = (EventEntity) o;
    return eventUUID.equals(that.eventUUID)
        && eventType.equals(that.eventType)
        && workspaceId.equals(that.workspaceId)
        && eventMetadata.equals(that.eventMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventUUID, eventType, workspaceId, eventMetadata);
  }
}
