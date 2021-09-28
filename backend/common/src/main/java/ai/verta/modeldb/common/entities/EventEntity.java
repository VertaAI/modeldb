package ai.verta.modeldb.common.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class EventEntity implements Serializable {
  private EventEntity() {}

  public EventEntity(String eventType, Long workspaceId, String eventMetadata) {
    this.event_uuid = UUID.randomUUID().toString();
    this.event_type = eventType;
    this.workspace_id = workspaceId;
    this.event_metadata = eventMetadata;
  }

  String event_uuid; // uuid for this event to handle deduplication
  String event_type; // Service-specific name for this type of event
  Long workspace_id; // Required
  String event_metadata;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EventEntity)) return false;
    EventEntity that = (EventEntity) o;
    return event_uuid.equals(that.event_uuid)
        && event_type.equals(that.event_type)
        && workspace_id.equals(that.workspace_id)
        && Objects.equals(event_metadata, that.event_metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(event_uuid, event_type, workspace_id, event_metadata);
  }
}
