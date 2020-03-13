package ai.verta.modeldb.entities.code;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "git_code_blob")
public class GitCodeBlob {

  public GitCodeBlob() {}

  @Id
  @Column(name = "blob_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String blob_hash;

  @Column(name = "repo")
  private String repository;

  @Column(name = "commit_hash", columnDefinition = "varchar", length = 64)
  private String commit_hash;

  @Column(name = "branch", columnDefinition = "varchar", length = 64)
  private String branch;

  @Column(name = "tag")
  private String tag;

  @Column(name = "is_dirty")
  private Boolean is_dirty;
}
