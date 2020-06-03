package ai.verta.modeldb.entities.versioning;

import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.Repository.Builder;
import ai.verta.modeldb.versioning.RepositoryAccessModifierEnum.RepositoryAccessModifier;
import ai.verta.modeldb.versioning.RepositoryVisibilityEnum.RepositoryVisibility;
import ai.verta.modeldb.versioning.SetRepository;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "repository")
public class RepositoryEntity {

  public RepositoryEntity() {}

  public RepositoryEntity(
      String name,
      WorkspaceDTO workspaceDTO,
      String owner,
      RepositoryVisibility repositoryVisibility,
      RepositoryAccessModifier repositoryAccessModifier) {
    this.name = name;
    this.date_created = new Date().getTime();
    this.date_updated = new Date().getTime();
    if (repositoryVisibility != null) {
      this.repository_visibility = repositoryVisibility.getNumber();
    }
    if (repositoryAccessModifier != null) {
      this.repositoryAccessModifier = repositoryAccessModifier.getNumber();
    }
    if (workspaceDTO.getWorkspaceId() != null) {
      this.workspace_id = workspaceDTO.getWorkspaceId();
      this.workspace_type = workspaceDTO.getWorkspaceType().getNumber();
      this.owner = owner;
    } else {
      this.workspace_id = "";
      this.workspace_type = 0;
      this.owner = "";
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UNSIGNED")
  private Long id;

  @Column(name = "name", columnDefinition = "varchar", length = 50)
  private String name;

  @Column(name = "date_created")
  private Long date_created;

  @Column(name = "date_updated")
  private Long date_updated;

  @Column(name = "workspace_id")
  private String workspace_id;

  @Column(name = "workspace_type", columnDefinition = "varchar")
  private Integer workspace_type;

  @OrderBy("date_created")
  @ManyToMany(mappedBy = "repository")
  private Set<CommitEntity> commits = new HashSet<>();

  @Column(name = "owner")
  private String owner;

  @Column(name = "repository_visibility")
  private Integer repository_visibility = null;

  @Column(name = "repository_access_modifier")
  private Integer repositoryAccessModifier = null;

  @Column(name = "deleted")
  private Boolean deleted = false;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Long getDate_created() {
    return date_created;
  }

  public Long getDate_updated() {
    return date_updated;
  }

  public void setDate_updated(Long date_updated) {
    this.date_updated = date_updated;
  }

  public String getWorkspace_id() {
    return workspace_id;
  }

  public Set<CommitEntity> getCommits() {
    return commits;
  }

  public Integer getWorkspace_type() {
    return workspace_type;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  public Repository toProto() {
    final Builder builder =
        Repository.newBuilder()
            .setId(this.id)
            .setName(this.name)
            .setDateCreated(this.date_created)
            .setDateUpdated(this.date_updated)
            .setWorkspaceId(this.workspace_id)
            .setWorkspaceTypeValue(this.workspace_type);
    if (repository_visibility != null) {
      builder.setRepositoryVisibilityValue(repository_visibility);
    }
    if (repositoryAccessModifier != null) {
      builder.setRepositoryAccessModifierValue(repositoryAccessModifier);
    }
    if (owner != null) {
      builder.setOwner(owner);
    }
    return builder.build();
  }

  public void update(SetRepository request) {
    final Repository repository = request.getRepository();
    this.name = repository.getName();
    this.date_updated = new Date().getTime();
    this.repository_visibility = repository.getRepositoryVisibilityValue();
    repositoryAccessModifier = repository.getRepositoryAccessModifierValue();
  }

  public String getOwner() {
    return owner;
  }

  public Integer getRepository_visibility() {
    return repository_visibility;
  }

  public Integer getRepositoryAccessModifier() {
    return repositoryAccessModifier;
  }

  public boolean isProtected() {
    return Objects.equals(repositoryAccessModifier, RepositoryAccessModifier.PROTECTED.getNumber());
  }
}
