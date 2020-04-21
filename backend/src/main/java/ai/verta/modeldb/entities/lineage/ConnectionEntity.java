package ai.verta.modeldb.entities.lineage;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.lineage.LineageElement;
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
  public static final int CONNECTION_TYPE_ANY = 0;
  public static final int CONNECTION_TYPE_INPUT = 1;
  public static final int CONNECTION_TYPE_OUTPUT = 2;

  public static final int ENTITY_TYPE_EXPERIMENT_RUN = 1;
  public static final int ENTITY_TYPE_VERSIONING_BLOB = 2;

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

  public Integer getConnectionType() {
    return connectionType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public Integer getEntityType() {
    return entityType;
  }

  public LineageElement getLineageElement(Session session) throws ModelDBException {
    switch (entityType) {
      case ENTITY_TYPE_EXPERIMENT_RUN:
        LineageExperimentRunEntity lineageExperimentRunEntity =
            session.get(LineageExperimentRunEntity.class, entityId);
        return lineageExperimentRunEntity.getElement();
      case ENTITY_TYPE_VERSIONING_BLOB:
        LineageVersioningBlobEntity lineageVersioningBlobEntity =
            session.get(LineageVersioningBlobEntity.class, entityId);
        return lineageVersioningBlobEntity.getElement();
      default:
        throw new ModelDBException("Unknown entity type");
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
