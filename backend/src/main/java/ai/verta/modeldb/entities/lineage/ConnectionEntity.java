package ai.verta.modeldb.entities.lineage;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.lineage.LineageElement;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.Session;

@Entity
@Table(name = "lineage_connection")
public class ConnectionEntity {
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
        ExperimentRunEntity experimentRunEntity = session.get(ExperimentRunEntity.class, entityId);
        return experimentRunEntity.getElement();
      case ENTITY_TYPE_VERSIONING_BLOB:
        VersioningBlobEntity versioningBlobEntity =
            session.get(VersioningBlobEntity.class, entityId);
        return versioningBlobEntity.getElement();
      default:
        throw new ModelDBException("Unknown entity type");
    }
  }
}
