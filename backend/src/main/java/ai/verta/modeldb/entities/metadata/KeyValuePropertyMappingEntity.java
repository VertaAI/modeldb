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
@Table(name = "key_value_property_mapping")
public class KeyValuePropertyMappingEntity {
  public KeyValuePropertyMappingEntity() {}

  public KeyValuePropertyMappingEntity(KeyValuePropertyMappingId id, String value) {
    this.id = id;
    this.value = value;
  }

  @EmbeddedId private KeyValuePropertyMappingId id;

  @Column(name = "kv_value", columnDefinition = "TEXT")
  private String value;

  public static KeyValuePropertyMappingId createId(
      IdentificationType id, String key, String propertyName) {
    if (id.getIdCase().equals(IdentificationType.IdCase.INT_ID)) {
      return new KeyValuePropertyMappingId(String.valueOf(id.getIntId()), key, propertyName);
    } else if (id.getIdCase().equals(IdentificationType.IdCase.STRING_ID)) {
      return new KeyValuePropertyMappingId(id.getStringId(), key, propertyName);
    } else {
      throw new StatusRuntimeException(Status.INVALID_ARGUMENT);
    }
  }

  public KeyValuePropertyMappingId getId() {
    return id;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }

  @Embeddable
  public static class KeyValuePropertyMappingId implements Serializable {

    @Column(name = "entity_hash", nullable = false, columnDefinition = "varchar")
    private String entity_hash;

    @Column(name = "kv_key", length = 50)
    private String key;

    @Column(name = "property_name")
    private String property_name;

    public KeyValuePropertyMappingId(String entityHash, String key, String propertyName) {
      this.entity_hash = entityHash;
      this.key = key;
      this.property_name = propertyName;
    }

    private KeyValuePropertyMappingId() {}

    public String getEntity_hash() {
      return entity_hash;
    }

    public String getKey() {
      return key;
    }

    public String getProperty_name() {
      return property_name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof KeyValuePropertyMappingId)) return false;
      KeyValuePropertyMappingId that = (KeyValuePropertyMappingId) o;
      return Objects.equals(getEntity_hash(), that.getEntity_hash())
          && Objects.equals(getKey(), that.getKey())
          && Objects.equals(getProperty_name(), that.getProperty_name());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getEntity_hash(), getKey(), getProperty_name());
    }
  }
}
