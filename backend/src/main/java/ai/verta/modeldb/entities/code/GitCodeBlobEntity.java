package ai.verta.modeldb.entities.code;

import ai.verta.modeldb.versioning.GitCodeBlob;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "git_code_blob")
public class GitCodeBlobEntity {

  public GitCodeBlobEntity() {}

  public GitCodeBlobEntity(String blobHash, GitCodeBlob gitCodeBlob) {
    blob_hash = blobHash;
    repository = gitCodeBlob.getRepo();
    commit_hash = gitCodeBlob.getHash();
    branch = gitCodeBlob.getBranch();
    tag = gitCodeBlob.getTag();
    is_dirty = gitCodeBlob.getIsDirty();
  }

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

  public String getBlobHash() {
    return blob_hash;
  }

  public GitCodeBlob toProto() {
    return GitCodeBlob.newBuilder()
        .setRepo(repository)
        .setHash(commit_hash)
        .setBranch(branch)
        .setTag(tag)
        .setIsDirty(is_dirty)
        .build();
  }
}
