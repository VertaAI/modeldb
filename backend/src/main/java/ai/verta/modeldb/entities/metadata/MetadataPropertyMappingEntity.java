package ai.verta.modeldb.entities.metadata;

import ai.verta.modeldb.metadata.IdentificationType;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.VersioningUtils;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "metadata_property_mapping")
public class MetadataPropertyMappingEntity {
  public MetadataPropertyMappingEntity() {}

  @Column(name = "metadata_value", columnDefinition = "TEXT")
  private String value;

  @EmbeddedId private LabelMappingId id;

  public MetadataPropertyMappingEntity(LabelMappingId id0, String value) {
    id = id0;
    this.value = value;
  }

  public static LabelMappingId createId(IdentificationType id, String key) {
    return new LabelMappingId(id.getStringId(), key);
  }

  public LabelMappingId getId() {
    return id;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Embeddable
  public static class LabelMappingId implements Serializable {

    @Column(name = "repository_id")
    private Long repositoryId;

    @Column(name = "commit_sha")
    private String commitSha;

    @Column(name = "location")
    private String location;

    @Column(name = "metadata_key")
    private String key;

    public LabelMappingId() {}

    private LabelMappingId(String compositeId, String key) {
      String[] compositeIdArr = VersioningUtils.getDatasetVersionBlobCompositeIdString(compositeId);
      repositoryId = Long.parseLong(compositeIdArr[0]);
      commitSha = compositeIdArr[1];
      location =
          ModelDBUtils.getJoinedLocation(
              ModelDBUtils.getLocationWithSplitSlashOperator(compositeIdArr[2]));
      this.key = key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LabelMappingId that = (LabelMappingId) o;
      return Objects.equals(repositoryId, that.repositoryId)
          && Objects.equals(commitSha, that.commitSha)
          && Objects.equals(location, that.location)
          && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
      return Objects.hash(repositoryId, commitSha, location, key);
    }
  }
}
