package ai.verta.modeldb.entities.versioning;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "branch")
public class BranchEntity {
  public BranchEntity() {}

  public BranchEntity(Long repositoryId, String commitHash, String branchName) {
    this.id = new BranchId(branchName, repositoryId);
    this.commit_hash = commitHash;
  }

  @EmbeddedId private BranchId id;

  @Column(name = "commit_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String commit_hash;

  public BranchId getId() {
    return id;
  }

  public String getCommit_hash() {
    return commit_hash;
  }

  // @Embeddable used for creating the composite key in hibernate
  @Embeddable
  public static class BranchId implements Serializable {

    @Column(name = "branch", columnDefinition = "varchar")
    private String branch;

    @Column(name = "repository_id")
    private Long repository_id;

    public BranchId(String branchName, Long repositoryId) {
      this.branch = branchName;
      this.repository_id = repositoryId;
    }

    private BranchId() {}

    public String getBranch() {
      return branch;
    }

    public Long getRepository_id() {
      return repository_id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof BranchId)) return false;
      BranchId that = (BranchId) o;
      return Objects.equals(getBranch(), that.getBranch())
          && Objects.equals(getRepository_id(), that.getRepository_id());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getBranch(), getRepository_id());
    }
  }
}
