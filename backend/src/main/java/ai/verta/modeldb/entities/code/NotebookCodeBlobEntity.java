package ai.verta.modeldb.entities.code;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "notebook_code_blob")
public class NotebookCodeBlobEntity {

  public NotebookCodeBlobEntity() {}

  public NotebookCodeBlobEntity(
      String blobHash, GitCodeBlobEntity gitCodeBlobEntity, String pathBlobSha) {
    blob_hash = blobHash;
    this.gitCodeBlobEntity = gitCodeBlobEntity;
    this.path_dataset_blob_hash = pathBlobSha;
  }

  @Id
  @Column(name = "blob_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String blob_hash;

  @ManyToOne(targetEntity = GitCodeBlobEntity.class, cascade = CascadeType.ALL)
  @JoinColumn(name = "git_code_blob_hash")
  private GitCodeBlobEntity gitCodeBlobEntity;

  @Column(name = "path_dataset_blob_hash", columnDefinition = "varchar", length = 64)
  private String path_dataset_blob_hash;

  public GitCodeBlobEntity getGitCodeBlobEntity() {
    return gitCodeBlobEntity;
  }

  public String getPath_dataset_blob_hash() {
    return path_dataset_blob_hash;
  }
}
