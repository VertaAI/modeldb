package ai.verta.modeldb.entities;

import ai.verta.modeldb.GitSnapshot;
import java.util.Collections;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "git_snapshot")
public class GitSnapshotEntity {

  public GitSnapshotEntity() {}

  public GitSnapshotEntity(String fieldType, GitSnapshot gitSnapshot) {

    setFilepaths(gitSnapshot.getFilepathsList());
    setRepo(gitSnapshot.getRepo());
    setHash(gitSnapshot.getHash());
    setIs_dirty(gitSnapshot.getIsDirtyValue());

    this.field_type = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @ElementCollection
  @CollectionTable(
      name = "git_snapshot_file_paths",
      joinColumns = @JoinColumn(name = "git_snapshot_id"))
  @Column(name = "file_paths")
  private List<String> filepaths = Collections.emptyList(); // paths to relevant source code

  @Column(name = "repo")
  private String repo; // URL to remote repository

  @Column(name = "hash")
  private String hash; // commit hash

  @Column(name = "is_dirty")
  private Integer is_dirty = 0;

  @Column(name = "field_type", length = 50)
  private String field_type;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public List<String> getFilepaths() {
    return filepaths;
  }

  public void setFilepaths(List<String> filepaths) {
    this.filepaths = filepaths;
  }

  public String getRepo() {
    return repo;
  }

  public void setRepo(String repo) {
    this.repo = repo;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public Integer getIs_dirty() {
    return is_dirty;
  }

  public void setIs_dirty(Integer is_dirty) {
    this.is_dirty = is_dirty;
  }

  public GitSnapshot getProtoObject() {
    return GitSnapshot.newBuilder()
        .addAllFilepaths(filepaths)
        .setRepo(repo)
        .setHash(hash)
        .setIsDirtyValue(is_dirty)
        .build();
  }
}
