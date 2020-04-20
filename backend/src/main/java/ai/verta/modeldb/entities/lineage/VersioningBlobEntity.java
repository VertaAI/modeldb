package ai.verta.modeldb.entities.lineage;

import ai.verta.modeldb.lineage.LineageElement;
import ai.verta.modeldb.lineage.VersioningBlobElement;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "lineage_versioning_blob")
public class VersioningBlobEntity {

  @Id
  @Column(name = "id")
  private Long id;

  @Column(name = "repository_id")
  private Long repositoryId;

  @Column(name = "commit_sha")
  private String commitSha;

  @Column(name = "location")
  private String location;

  public VersioningBlobEntity(Long repositoryId, String commitSha, String location) {
    this.repositoryId = repositoryId;
    this.commitSha = commitSha;
    this.location = location;
  }

  public Long getId() {
    return id;
  }

  public LineageElement getElement() {
    return new VersioningBlobElement(repositoryId, commitSha, location);
  }
}
