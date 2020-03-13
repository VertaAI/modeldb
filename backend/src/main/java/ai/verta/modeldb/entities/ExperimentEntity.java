package ai.verta.modeldb.entities;

import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.utils.RdbmsUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "experiment")
public class ExperimentEntity {

  public ExperimentEntity() {}

  public ExperimentEntity(Experiment experiment) throws InvalidProtocolBufferException {
    setId(experiment.getId());
    setProject_id(experiment.getProjectId());
    setName(experiment.getName());
    setDate_created(experiment.getDateCreated());
    setDate_updated(experiment.getDateUpdated());
    setDescription(experiment.getDescription());
    setAttributeMapping(
        RdbmsUtils.convertAttributesFromAttributeEntityList(
            this, ModelDBConstants.ATTRIBUTES, experiment.getAttributesList()));
    setTags(RdbmsUtils.convertTagListFromTagMappingList(this, experiment.getTagsList()));
    setArtifactMapping(
        RdbmsUtils.convertArtifactsFromArtifactEntityList(
            this, ModelDBConstants.ARTIFACTS, experiment.getArtifactsList()));
    setOwner(experiment.getOwner());
    if (experiment.getCodeVersionSnapshot().hasCodeArchive()
        || experiment.getCodeVersionSnapshot().hasGitSnapshot()) {
      setCode_version_snapshot(
          RdbmsUtils.generateCodeVersionEntity(
              ModelDBConstants.CODE_VERSION, experiment.getCodeVersionSnapshot()));
    }
  }

  @Id
  @Column(name = "id", unique = true)
  private String id;

  @Column(name = "project_id", nullable = false)
  private String project_id;

  @Column(name = "name")
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "date_created")
  private Long date_created;

  @Column(name = "date_updated")
  private Long date_updated;

  @OneToMany(
      targetEntity = KeyValueEntity.class,
      mappedBy = "experimentEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<KeyValueEntity> keyValueMapping;

  @OneToMany(
      targetEntity = AttributeEntity.class,
      mappedBy = "experimentEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<AttributeEntity> attributeMapping;

  @OneToMany(
      targetEntity = TagsMapping.class,
      mappedBy = "experimentEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<TagsMapping> tags;

  @OneToMany(
      targetEntity = ArtifactEntity.class,
      mappedBy = "experimentEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<ArtifactEntity> artifactMapping;

  @Column(name = "owner")
  private String owner;

  @OneToOne(targetEntity = CodeVersionEntity.class, cascade = CascadeType.ALL)
  @OrderBy("id")
  private CodeVersionEntity code_version_snapshot;

  @Transient private Map<String, List<ArtifactEntity>> artifactEntityMap = new HashMap<>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getProject_id() {
    return project_id;
  }

  public void setProject_id(String projectId) {
    this.project_id = projectId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getDate_created() {
    return date_created;
  }

  public void setDate_created(Long dateCreated) {
    this.date_created = dateCreated;
  }

  public Long getDate_updated() {
    return date_updated;
  }

  public void setDate_updated(Long dateUpdated) {
    this.date_updated = dateUpdated;
  }

  public List<TagsMapping> getTags() {
    return tags;
  }

  public void setTags(List<TagsMapping> tagsMapping) {
    this.tags = tagsMapping;
  }

  private List<ArtifactEntity> getArtifactMapping(String fieldType) {

    if (artifactEntityMap.size() == 0) {
      addArtifactInMap(this.artifactMapping);
    }

    if (artifactMapping != null && artifactEntityMap.containsKey(fieldType)) {
      return artifactEntityMap.get(fieldType);
    } else {
      return Collections.emptyList();
    }
  }

  private void addArtifactInMap(List<ArtifactEntity> artifactMapping) {
    for (ArtifactEntity artifactEntity : artifactMapping) {
      List<ArtifactEntity> artifactEntities = artifactEntityMap.get(artifactEntity.getField_type());
      if (artifactEntities == null) {
        artifactEntities = new ArrayList<>();
      }
      artifactEntities.add(artifactEntity);
      artifactEntityMap.put(artifactEntity.getField_type(), artifactEntities);
    }
  }

  public void setArtifactMapping(List<ArtifactEntity> artifactMapping) {
    if (this.artifactMapping == null) {
      this.artifactMapping = new ArrayList<>();
    }

    if (artifactMapping != null) {
      this.artifactMapping.addAll(artifactMapping);
      if (artifactEntityMap.size() == 0) {
        // Add all artifact in the map
        addArtifactInMap(this.artifactMapping);
      } else {
        // Add new artifact in the map
        addArtifactInMap(artifactMapping);
      }
    }
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public CodeVersionEntity getCode_version_snapshot() {
    return code_version_snapshot;
  }

  public void setCode_version_snapshot(CodeVersionEntity code_version_snapshot) {
    this.code_version_snapshot = code_version_snapshot;
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

  public Experiment getProtoObject() throws InvalidProtocolBufferException {
    Experiment.Builder experimentBuilder =
        Experiment.newBuilder()
            .setId(getId())
            .setProjectId(getProject_id())
            .setName(getName())
            .setDescription(getDescription())
            .setDateCreated(getDate_created())
            .setDateUpdated(getDate_updated())
            .addAllAttributes(
                RdbmsUtils.convertAttributeEntityListFromAttributes(getAttributeMapping()))
            .addAllTags(RdbmsUtils.convertTagsMappingListFromTagList(getTags()))
            .addAllArtifacts(
                RdbmsUtils.convertArtifactEntityListFromArtifacts(
                    getArtifactMapping(ModelDBConstants.ARTIFACTS)))
            .setOwner(getOwner());

    if (getCode_version_snapshot() != null) {
      experimentBuilder.setCodeVersionSnapshot(getCode_version_snapshot().getProtoObject());
    }
    return experimentBuilder.build();
  }
}
