package ai.verta.modeldb.entities.metadata;

import ai.verta.modeldb.metadata.IdentificationType;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "labels_mapping")
public class LabelsMappingEntity {
  public LabelsMappingEntity() {}

  public LabelsMappingEntity(LabelMappingId id) {
    this.id = id;
  }

  @EmbeddedId private LabelMappingId id;

  public static LabelMappingId createId(IdentificationType id, String label) {
    if (id.getIdCase().equals(IdentificationType.IdCase.INT_ID)) {
      return new LabelMappingId(String.valueOf(id.getIntId()), id.getIdTypeValue(), label);
    } else if (id.getIdCase().equals(IdentificationType.IdCase.STRING_ID)) {
      return new LabelMappingId(id.getStringId(), id.getIdTypeValue(), label);
    } else {
      throw new StatusRuntimeException(Status.INVALID_ARGUMENT);
    }
  }

  public LabelMappingId getId() {
    return id;
  }

  public String getValue() {
    return id.getLabel();
  }

  @Embeddable
  public static class LabelMappingId implements Serializable {

    @Column(name = "label", length = 50)
    private String label;

    @Column(name = "entity_hash", nullable = false, columnDefinition = "varchar")
    private String entity_hash;

    @Column(name = "entity_type")
    private Integer entity_type;

    public LabelMappingId(String entityHash, Integer entityType, String label) {
      this.entity_hash = entityHash;
      this.entity_type = entityType;
      this.label = label;
    }

    private LabelMappingId() {}

    public String getEntity_hash() {
      return entity_hash;
    }

    public Integer getEntity_type() {
      return entity_type;
    }

    public String getLabel() {
      return label;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof LabelMappingId)) return false;
      LabelMappingId that = (LabelMappingId) o;
      return Objects.equals(getEntity_hash(), that.getEntity_hash())
          && Objects.equals(getEntity_type(), that.getEntity_type())
          && Objects.equals(getLabel(), that.getLabel());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getEntity_hash(), getEntity_type(), getLabel());
    }
  }
}
