package ai.verta.modeldb.entities.metadata;

import ai.verta.modeldb.metadata.IdentificationType;
import ai.verta.modeldb.metadata.VersioningCompositeIdentifier;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.*;

@Entity
@Table(name = "labels_mapping_description")
public class LabelsMappingDescriptionEntity implements LabelsMappingEntityBase {
  public LabelsMappingDescriptionEntity() {}

  public LabelsMappingDescriptionEntity(IdentificationType id, String label) {
    this.id = new LabelMappingId(id.getCompositeId());
  }

  @Column(name = "label", columnDefinition = "TEXT")
  private String label;

  @EmbeddedId private LabelMappingId id;

  public LabelMappingId getId() {
    return id;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Embeddable
  public static class LabelMappingId implements Serializable {

    @Column(name = "repository_id")
    private Long repositoryId;

    @Column(name = "commit_sha")
    private String commitSha;

    @Column(name = "location")
    private String location;

    public LabelMappingId() {}

    private LabelMappingId(VersioningCompositeIdentifier compositeId) {
      repositoryId = compositeId.getRepoId();
      commitSha = compositeId.getCommitHash();
      location = ModelDBUtils.getJoinedLocation(compositeId.getLocationList());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      LabelMappingId that = (LabelMappingId) o;
      return Objects.equals(repositoryId, that.repositoryId)
          && Objects.equals(commitSha, that.commitSha)
          && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
      return Objects.hash(repositoryId, commitSha, location);
    }
  }
}
