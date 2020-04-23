package ai.verta.modeldb.entities.lineage;

import ai.verta.modeldb.lineage.LineageEntryContainer;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.Session;

@Entity
@Table(name = "lineage_connection")
public class ConnectionEntity implements Serializable {
  public enum ConnectionType {
    CONNECTION_TYPE_ANY(
        0), // used for database read operations, means that we should acquire both input and output
    CONNECTION_TYPE_INPUT(1),
    CONNECTION_TYPE_OUTPUT(2);

    private final int value;

    ConnectionType(int value) {
      this.value = value;
    }

    static ConnectionType valueOf(int value) {
      switch (value) {
        case 0:
          return CONNECTION_TYPE_ANY;
        case 1:
          return CONNECTION_TYPE_INPUT;
        case 2:
          return CONNECTION_TYPE_OUTPUT;
        default:
          return null;
      }
    }

    public int getValue() {
      return value;
    }
  }

  public static final int ENTITY_TYPE_EXPERIMENT_RUN = 1;
  public static final int ENTITY_TYPE_VERSIONING_BLOB = 2;

  public static final String ID = "id";
  public static final String ENTITY_ID = "entityId";
  public static final String CONNECTION_TYPE = "connectionType";
  public static final String ENTITY_TYPE = "entityType";

  @Id
  @Column(name = "element_id")
  private Long id;

  @Id
  @Column(name = "entity_id")
  private Long entityId;

  @Id
  @Column(name = "connection_type")
  private Integer connectionType;

  @Id
  @Column(name = "entity_type")
  private Integer entityType;

  public ConnectionEntity() {}

  public ConnectionEntity(Long id, Long entityId, Integer connectionType, Integer entityType) {
    this.id = id;
    this.entityId = entityId;
    this.connectionType = connectionType;
    this.entityType = entityType;
  }

  public Long getId() {
    return id;
  }

  public ConnectionType getConnectionType() {
    return ConnectionEntity.ConnectionType.valueOf(connectionType);
  }

  public Long getEntityId() {
    return entityId;
  }

  public Integer getEntityType() {
    return entityType;
  }

  public LineageEntryContainer getLineageElement(Session session) {
    switch (entityType) {
      case ENTITY_TYPE_EXPERIMENT_RUN:
        LineageExperimentRunEntity lineageExperimentRunEntity =
            session.get(LineageExperimentRunEntity.class, entityId);
        return lineageExperimentRunEntity.getEntry();
      case ENTITY_TYPE_VERSIONING_BLOB:
        LineageVersioningBlobEntity lineageVersioningBlobEntity =
            session.get(LineageVersioningBlobEntity.class, entityId);
        return lineageVersioningBlobEntity.getEntry();
      default:
        Status statusMessage =
            Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage("Unknown entity type")
                .build();
        throw StatusProto.toStatusRuntimeException(statusMessage);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectionEntity that = (ConnectionEntity) o;
    return Objects.equals(id, that.id)
        && Objects.equals(entityId, that.entityId)
        && Objects.equals(connectionType, that.connectionType)
        && Objects.equals(entityType, that.entityType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, entityId, connectionType, entityType);
  }
}
