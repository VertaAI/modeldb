package ai.verta.modeldb.entities.versioning;

import ai.verta.modeldb.versioning.Commit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

@Entity
@Table(name = "commit")
public class CommitEntity {
  public CommitEntity() {}

  public CommitEntity(
      RepositoryEntity repositoryEntity,
      Map<Integer, CommitEntity> parentCommits,
      Commit internalCommit,
      String rootSha) {
    this.commit_hash = internalCommit.getCommitSha();
    this.date_created = internalCommit.getDateCreated();
    this.message = internalCommit.getMessage();
    this.repository.add(repositoryEntity);
    this.author = internalCommit.getAuthor();
    this.rootSha = rootSha;

    if (parentCommits != null) {
      this.parent_commits.putAll(parentCommits);
    }
  }

  @Id
  @Column(name = "commit_hash", columnDefinition = "varchar", length = 64, nullable = false)
  private String commit_hash;

  @Column(name = "message", columnDefinition = "text")
  private String message;

  @Column(name = "date_created")
  private Long date_created;

  @Column(name = "author", columnDefinition = "varchar", length = 50)
  private String author;

  @Column(name = "root_sha", columnDefinition = "varchar", length = 64)
  private String rootSha;

  // Repo fork
  @ManyToMany(targetEntity = RepositoryEntity.class, cascade = CascadeType.PERSIST)
  @JoinTable(
      name = "repository_commit",
      joinColumns = @JoinColumn(name = "commit_hash"),
      inverseJoinColumns = @JoinColumn(name = "repository_id"))
  private Set<RepositoryEntity> repository = new HashSet<>();

  // merge commit have multiple parents
  @ManyToMany(targetEntity = CommitEntity.class, cascade = CascadeType.PERSIST)
  @JoinTable(
      name = "commit_parent",
      joinColumns = {@JoinColumn(name = "child_hash", referencedColumnName = "commit_hash")},
      inverseJoinColumns = {
        @JoinColumn(name = "parent_hash", referencedColumnName = "commit_hash")
      })
  @MapKeyColumn(name = "parent_order")
  private Map<Integer, CommitEntity> parent_commits = new HashMap<>();

  @ManyToMany(mappedBy = "parent_commits")
  private Set<CommitEntity> child_commits = new HashSet<>();

  public String getCommit_hash() {
    return commit_hash;
  }

  public String getMessage() {
    return message;
  }

  public Long getDate_created() {
    return date_created;
  }

  public String getAuthor() {
    return author;
  }

  public Set<RepositoryEntity> getRepository() {
    return repository;
  }

  public Map<Integer, CommitEntity> getParent_commits() {
    return parent_commits;
  }

  public String getRootSha() {
    return rootSha;
  }

  public Set<CommitEntity> getChild_commits() {
    return child_commits;
  }

  private List<String> getParentCommitIds() {
    return parent_commits.values().stream()
        .map(CommitEntity::getCommit_hash)
        .collect(Collectors.toList());
  }

  public Commit toCommitProto() {
    return Commit.newBuilder()
        .setCommitSha(this.commit_hash)
        .addAllParentShas(getParentCommitIds())
        .setDateCreated(this.date_created)
        .setMessage(this.message)
        .setAuthor(this.author)
        .build();
  }
}
