package ai.verta.modeldb.entities.versioning;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEnums.RepositoryModifierEnum;
import ai.verta.modeldb.entities.versioning.RepositoryEnums.RepositoryTypeEnum;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.Repository.Builder;
import com.google.api.client.util.Objects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "repository")
public class RepositoryEntity {

  public RepositoryEntity() {}

  public RepositoryEntity(
      Repository repository, WorkspaceDTO workspaceDTO, RepositoryTypeEnum repositoryTypeEnum)
      throws InvalidProtocolBufferException {
    this.name = repository.getName();
    this.description = repository.getDescription();
    this.date_created = new Date().getTime();
    this.date_updated = new Date().getTime();
    this.repository_visibility = repository.getRepositoryVisibilityValue();
    if (workspaceDTO.getWorkspaceId() != null) {
      this.workspace_id = workspaceDTO.getWorkspaceId();
      this.workspace_type = workspaceDTO.getWorkspaceType().getNumber();
      this.owner = repository.getOwner();
    } else {
      this.workspace_id = "";
      this.workspace_type = 0;
      this.owner = "";
    }
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

  @Column(name = "name", columnDefinition = "varchar", length = 50)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

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

  @OneToMany(
      targetEntity = AttributeEntity.class,
      mappedBy = "repositoryEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<AttributeEntity> attributeMapping;

  @OneToOne(mappedBy = "repositoryEntity", cascade = CascadeType.ALL)
  private DatasetRepositoryMappingEntity datasetRepositoryMappingEntity;

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

  public Repository toProto() throws InvalidProtocolBufferException {
    final Builder builder = Repository.newBuilder().setId(this.id);
    builder
        .setName(this.name)
        .setDateCreated(this.date_created)
        .setDateUpdated(this.date_updated)
        .setWorkspaceId(this.workspace_id)
        .setWorkspaceTypeValue(this.workspace_type)
        .addAllAttributes(
            RdbmsUtils.convertAttributeEntityListFromAttributes(getAttributeMapping()));
    if (repository_visibility != null) {
      builder.setRepositoryVisibilityValue(repository_visibility);
    }
    if (owner != null) {
      builder.setOwner(owner);
    }
    if (description != null) {
      builder.setDescription(description);
    }
    return builder.build();
  }

  public void update(Repository repository) throws InvalidProtocolBufferException {
    this.name = repository.getName();
    this.description = repository.getDescription();
    this.repository_visibility = repository.getRepositoryVisibilityValue();
    this.workspace_id = repository.getWorkspaceId();
    this.workspace_type = repository.getWorkspaceTypeValue();
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
