package ai.verta.modeldb.entities.versioning;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "tag")
public class TagsEntity {
  public TagsEntity() {}

  public TagsEntity(Long repositoryId, String commitHash, String tag) {
    this.id = new TagId(tag, repositoryId);
    this.commit_hash = commitHash;
  }

  @EmbeddedId private TagId id;

  @Column(name = "commit_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String commit_hash;

  public TagId getId() {
    return id;
  }

  public String getCommit_hash() {
    return commit_hash;
  }

  // @Embeddable used for creating the composite key in hibernate
  @Embeddable
  public static class TagId implements Serializable {

    @Column(name = "tag", columnDefinition = "varchar")
    private String tag;

    @Column(name = "repository_id")
    private Long repository_id;

    public TagId(String tag, Long repositoryId) {
      this.tag = tag;
      this.repository_id = repositoryId;
    }

    private TagId() {}

    public String getTag() {
      return tag;
    }

    public Long getRepository_id() {
      return repository_id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof TagId)) return false;
      TagId that = (TagId) o;
      return Objects.equals(getTag(), that.getTag())
          && Objects.equals(getRepository_id(), that.getRepository_id());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getTag(), getRepository_id());
    }
  }
}
