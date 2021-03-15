package ai.verta.modeldb.entities.versioning;

import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEnums.RepositoryModifierEnum;
import ai.verta.modeldb.entities.versioning.RepositoryEnums.RepositoryTypeEnum;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.Repository.Builder;
import ai.verta.modeldb.versioning.RepositoryVisibilityEnum.RepositoryVisibility;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Workspace;
import com.google.api.client.util.Objects;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "repository")
public class RepositoryEntity {

  public RepositoryEntity() {}

  public RepositoryEntity(Repository repository, RepositoryTypeEnum repositoryTypeEnum)
      throws InvalidProtocolBufferException {
    this.name = ModelDBUtils.checkEntityNameLength(repository.getName());
    this.description = repository.getDescription();
    if (repository.getDateCreated() != 0L) {
      this.date_created = repository.getDateCreated();
    } else {
      this.date_created = new Date().getTime();
    }
    if (repository.getDateUpdated() != 0L) {
      this.date_updated = repository.getDateUpdated();
    } else {
      this.date_updated = new Date().getTime();
    }
    this.repositoryVisibility = repository.getVisibility();

    setAttributeMapping(
        RdbmsUtils.convertAttributesFromAttributeEntityList(
            this, ModelDBConstants.ATTRIBUTES, repository.getAttributesList()));
    switch (repositoryTypeEnum) {
      case DATASET:
        this.datasetRepositoryMappingEntity = new DatasetRepositoryMappingEntity(this);
        this.repositoryAccessModifier = RepositoryModifierEnum.PROTECTED.ordinal();
        break;
      default:
        this.repositoryAccessModifier = RepositoryModifierEnum.REGULAR.ordinal();
        break;
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UNSIGNED")
  private Long id;

  @Column(name = "name", columnDefinition = "varchar", length = 256)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "date_created")
  private Long date_created;

  @Column(name = "date_updated")
  private Long date_updated;

  @Column(name = "workspace_service_id")
  private Long workspaceServiceId;

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

  @Transient private ResourceVisibility repositoryVisibility = ResourceVisibility.PRIVATE;

  @Column(name = "repository_access_modifier")
  private Integer repositoryAccessModifier = null;

  @Column(name = "deleted")
  private Boolean deleted = false;

  @OneToMany(
      targetEntity = AttributeEntity.class,
      mappedBy = "repositoryEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<AttributeEntity> attributeMapping;

  @OneToOne(mappedBy = "repositoryEntity", cascade = CascadeType.ALL)
  private DatasetRepositoryMappingEntity datasetRepositoryMappingEntity;

  @Column(name = "created")
  private Boolean created = false;

  @Column(name = "visibility_migration")
  private Boolean visibility_migration = false;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
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

  public Long getWorkspaceServiceId() {
    return workspaceServiceId;
  }

  public void setWorkspaceServiceId(Long workspaceServiceId) {
    this.workspaceServiceId = workspaceServiceId;
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

  public List<AttributeEntity> getAttributeMapping() {
    return attributeMapping;
  }

  public void setAttributeMapping(List<AttributeEntity> attributeMapping) {
    if (this.attributeMapping == null) {
      this.attributeMapping = new ArrayList<>();
    }
    this.attributeMapping.addAll(attributeMapping);
  }

  public void setCreated(Boolean created) {
    this.created = created;
  }

  public ResourceVisibility getRepositoryVisibility() {
    return repositoryVisibility;
  }

  public void setVisibility_migration(Boolean visibility_migration) {
    this.visibility_migration = visibility_migration;
  }

  public ListenableFuture<Repository> toProto(RoleService roleService, AuthService authService)
      throws InvalidProtocolBufferException {
    final Builder builder = Repository.newBuilder().setId(this.id);
    builder
        .setName(this.name)
        .setDateCreated(this.date_created)
        .setDateUpdated(this.date_updated)
        .addAllAttributes(
            RdbmsUtils.convertAttributeEntityListFromAttributes(getAttributeMapping()));

    ModelDBServiceResourceTypes modelDBServiceResourceTypes =
        ModelDBUtils.getModelDBServiceResourceTypesFromRepository(this);

    ListenableFuture<GetResourcesResponseItem> futureRepositoryResource =
        roleService.getEntityResource(
            Optional.of(String.valueOf(this.id)), Optional.empty(), modelDBServiceResourceTypes);
    ListenableFuture<Repository> futureBuilder =
        Futures.transform(
            futureRepositoryResource,
            repository -> {
              builder.setVisibility(repository.getVisibility());
              builder.setWorkspaceServiceId(repository.getWorkspaceId());
              builder.setOwner(String.valueOf(repository.getOwnerId()));
              builder.setCustomPermission(repository.getCustomPermission());

              RepositoryVisibility visibility;
              if (isDataset()) {
                DatasetVisibility datasetVisibility =
                    (DatasetVisibility)
                        ModelDBUtils.getOldVisibility(
                            modelDBServiceResourceTypes, repository.getVisibility());
                visibility = RepositoryVisibility.forNumber(datasetVisibility.getNumber());
              } else {
                visibility =
                    (RepositoryVisibility)
                        ModelDBUtils.getOldVisibility(
                            modelDBServiceResourceTypes, repository.getVisibility());
              }

              builder.setRepositoryVisibility(visibility);

              Workspace workspace = authService.workspaceById(false, repository.getWorkspaceId());
              switch (workspace.getInternalIdCase()) {
                case ORG_ID:
                  builder.setWorkspaceId(workspace.getOrgId());
                  builder.setWorkspaceTypeValue(WorkspaceTypeEnum.WorkspaceType.ORGANIZATION_VALUE);
                  break;
                case USER_ID:
                  builder.setWorkspaceId(workspace.getUserId());
                  builder.setWorkspaceTypeValue(WorkspaceTypeEnum.WorkspaceType.USER_VALUE);
                  break;
              }

              if (description != null) {
                builder.setDescription(description);
              }

              return builder.build();
            },
            MoreExecutors.directExecutor());

    return futureBuilder;
  }

  public void update(Repository repository) throws InvalidProtocolBufferException {
    this.name = ModelDBUtils.checkEntityNameLength(repository.getName());
    this.description = repository.getDescription();
    this.repositoryVisibility = repository.getVisibility();
    update();
    updateAttribute(repository.getAttributesList());
  }

  public void update() {
    this.date_updated = new Date().getTime();
  }

  public String getOwner() {
    return owner;
  }

  public Integer getRepository_visibility() {
    return repository_visibility;
  }

  private void updateAttribute(List<KeyValue> attributes) throws InvalidProtocolBufferException {
    if (attributes != null && !attributes.isEmpty()) {
      for (KeyValue attribute : attributes) {
        AttributeEntity updatedAttributeObj =
            RdbmsUtils.generateAttributeEntity(this, ModelDBConstants.ATTRIBUTES, attribute);

        List<AttributeEntity> existingAttributes = this.getAttributeMapping();
        if (!existingAttributes.isEmpty()) {
          boolean doesExist = false;
          for (AttributeEntity existingAttribute : existingAttributes) {
            if (existingAttribute.getKey().equals(attribute.getKey())) {
              existingAttribute.setKey(updatedAttributeObj.getKey());
              existingAttribute.setValue(updatedAttributeObj.getValue());
              existingAttribute.setValue_type(updatedAttributeObj.getValue_type());
              doesExist = true;
              break;
            }
          }
          if (!doesExist) {
            this.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
          }
        } else {
          this.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
        }
      }
    }
  }

  public boolean isDataset() {
    return datasetRepositoryMappingEntity != null;
  }

  public boolean isProtected() {
    return Objects.equal(repositoryAccessModifier, RepositoryModifierEnum.PROTECTED.ordinal());
  }
}
