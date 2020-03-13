package ai.verta.modeldb.entities.code;

import ai.verta.modeldb.entities.environment.EnvironmentBlobEntity;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "notebook_code_blob")
public class NotebookCodeBlob {

  public NotebookCodeBlob() {}

  @Id
  @Column(name = "blob_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String blob_hash;

  @ManyToOne(targetEntity = GitCodeBlob.class, cascade = CascadeType.ALL)
  @JoinColumn(name = "git_code_blob_hash")
  private EnvironmentBlobEntity gitCodeBlobHash;

  @Column(name = "path_dataset_blob_hash", columnDefinition = "varchar", length = 64)
  private String path_dataset_blob_hash;
}
