package ai.verta.modeldb.entities.versioning;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "blob_info")
public class BlobInfoEntity {
  @Id
  @Column(name = "blob_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String blobHash;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  public BlobInfoEntity() {}

  public BlobInfoEntity(String sha, String description) {
    blobHash = sha;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
