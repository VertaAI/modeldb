package ai.verta.modeldb.entities;

import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.VersioningEntry;
import ai.verta.modeldb.entities.versioning.VersioningModeldbEntityMapping;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "experiment_run")
public class ExperimentRunEntity {

  private static Logger LOGGER = LogManager.getLogger(ExperimentRunEntity.class);

  public ExperimentRunEntity() {}

  public ExperimentRunEntity(ExperimentRun experimentRun) throws InvalidProtocolBufferException {
    setId(experimentRun.getId());
    setProject_id(experimentRun.getProjectId());
    setExperiment_id(experimentRun.getExperimentId());
    setName(experimentRun.getName());
    setDescription(experimentRun.getDescription());
    setDate_created(experimentRun.getDateCreated());
    setDate_updated(experimentRun.getDateUpdated());
    setStart_time(experimentRun.getStartTime());
    setEnd_time(experimentRun.getEndTime());
    setCode_version(experimentRun.getCodeVersion());
    setTags(RdbmsUtils.convertTagListFromTagMappingList(this, experimentRun.getTagsList()));
    setAttributeMapping(
        RdbmsUtils.convertAttributesFromAttributeEntityList(
            this, ModelDBConstants.ATTRIBUTES, experimentRun.getAttributesList()));
    setKeyValueMapping(
        RdbmsUtils.convertKeyValuesFromKeyValueEntityList(
            this, ModelDBConstants.HYPERPARAMETERS, experimentRun.getHyperparametersList()));
    setArtifactMapping(
        RdbmsUtils.convertArtifactsFromArtifactEntityList(
            this, ModelDBConstants.ARTIFACTS, experimentRun.getArtifactsList()));
    setArtifactMapping(
        RdbmsUtils.convertArtifactsFromArtifactEntityList(
            this, ModelDBConstants.DATASETS, experimentRun.getDatasetsList()));
    setKeyValueMapping(
        RdbmsUtils.convertKeyValuesFromKeyValueEntityList(
            this, ModelDBConstants.METRICS, experimentRun.getMetricsList()));
    setObservationMapping(
        RdbmsUtils.convertObservationsFromObservationEntityList(
            this, ModelDBConstants.OBSERVATIONS, experimentRun.getObservationsList()));
    setFeatures(
        RdbmsUtils.convertFeatureListFromFeatureMappingList(this, experimentRun.getFeaturesList()));
    setJob_id(experimentRun.getJobId());
    setParent_id(experimentRun.getParentId());
    setOwner(experimentRun.getOwner());
    if (experimentRun.getCodeVersionSnapshot().hasCodeArchive()
        || experimentRun.getCodeVersionSnapshot().hasGitSnapshot()) {
      setCode_version_snapshot(
          RdbmsUtils.generateCodeVersionEntity(
              ModelDBConstants.CODE_VERSION, experimentRun.getCodeVersionSnapshot()));
    }

    if (experimentRun.getVersionedInputs() != null && experimentRun.hasVersionedInputs()) {
      VersioningEntry versioningEntry = experimentRun.getVersionedInputs();
      this.versioningModeldbEntityMappings =
          RdbmsUtils.getVersioningMappingFromVersioningInput(versioningEntry, this);
    }
  }

  @Id
  @Column(name = "id", unique = true)
  private String id;

  @Column(name = "project_id", nullable = false)
  private String project_id;

  @Column(name = "experiment_id", nullable = false)
  private String experiment_id;

  @Column(name = "name")
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "date_created")
  private Long date_created;

  @Column(name = "date_updated")
  private Long date_updated;

  @Column(name = "start_time")
  private Long start_time;

  @Column(name = "end_time")
  private Long end_time;

  @Column(name = "code_version")
  private String code_version;

  @OneToMany(
      targetEntity = TagsMapping.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<TagsMapping> tags;

  @OneToMany(
      targetEntity = KeyValueEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<KeyValueEntity> keyValueMapping;

  @OneToMany(
      targetEntity = AttributeEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<AttributeEntity> attributeMapping;

  @OneToMany(
      targetEntity = ArtifactEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<ArtifactEntity> artifactMapping;

  @OneToMany(
      targetEntity = ObservationEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<ObservationEntity> observationMapping;

  @OneToMany(
      targetEntity = FeatureEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<FeatureEntity> features;

  @Column(name = "job_id")
  private String job_id;

  @Column(name = "parent_id")
  private String parent_id;

  @Column(name = "owner")
  private String owner;

  @OneToOne(targetEntity = CodeVersionEntity.class, cascade = CascadeType.ALL)
  @OrderBy("id")
  private CodeVersionEntity code_version_snapshot;

  @OneToMany(
      targetEntity = VersioningModeldbEntityMapping.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  private List<VersioningModeldbEntityMapping> versioningModeldbEntityMappings = new ArrayList<>();

  @Transient private Map<String, List<KeyValueEntity>> keyValueEntityMap = new HashMap<>();

  @Transient private Map<String, List<ArtifactEntity>> artifactEntityMap = new HashMap<>();

  @Transient private Map<String, List<ObservationEntity>> observationEntityMap = new HashMap<>();

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

  public String getExperiment_id() {
    return experiment_id;
  }

  public void setExperiment_id(String experiment_id) {
    this.experiment_id = experiment_id;
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

  public Long getStart_time() {
    return start_time;
  }

  public void setStart_time(Long startTime) {
    this.start_time = startTime;
  }

  public Long getEnd_time() {
    return end_time;
  }

  public void setEnd_time(Long endTime) {
    this.end_time = endTime;
  }

  public String getCode_version() {
    return code_version;
  }

  public void setCode_version(String codeVersion) {
    this.code_version = codeVersion;
  }

  public List<TagsMapping> getTags() {
    return tags;
  }

  public void setTags(List<TagsMapping> tags) {
    this.tags = tags;
  }

  private void addKeyValueOnMap(List<KeyValueEntity> keyValueMapping) {
    for (KeyValueEntity keyValueEntity : keyValueMapping) {
      List<KeyValueEntity> keyValueEntities = keyValueEntityMap.get(keyValueEntity.getField_type());
      if (keyValueEntities == null) {
        keyValueEntities = new ArrayList<>();
      }
      keyValueEntities.add(keyValueEntity);
      keyValueEntityMap.put(keyValueEntity.getField_type(), keyValueEntities);
    }
  }

  public void setKeyValueMapping(List<KeyValueEntity> keyValueMapping) {
    if (this.keyValueMapping == null) {
      this.keyValueMapping = new ArrayList<>();
    }

    if (keyValueMapping != null) {
      this.keyValueMapping.addAll(keyValueMapping);
      if (keyValueEntityMap.size() == 0) {
        // Add all keyvalue on the map
        addKeyValueOnMap(this.keyValueMapping);
      } else {
        // Add new keyvalue on the map
        addKeyValueOnMap(keyValueMapping);
      }
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

  private void addObservationMappingOnMap(List<ObservationEntity> observationMapping) {
    for (ObservationEntity observationEntity : observationMapping) {
      List<ObservationEntity> observationEntities =
          observationEntityMap.get(observationEntity.getField_type());
      if (observationEntities == null) {
        observationEntities = new ArrayList<>();
      }
      observationEntities.add(observationEntity);
      observationEntityMap.put(observationEntity.getField_type(), observationEntities);
    }
  }

  public void setObservationMapping(List<ObservationEntity> observationMapping) {
    if (this.observationMapping == null) {
      this.observationMapping = new ArrayList<>();
    }

    if (observationMapping != null) {
      this.observationMapping.addAll(observationMapping);
      if (observationEntityMap.size() == 0) {
        // Add all observation in the map
        addObservationMappingOnMap(this.observationMapping);
      } else {
        // Add new observation in the map
        addObservationMappingOnMap(observationMapping);
      }
    }
  }

  public List<FeatureEntity> getFeatures() {
    return features;
  }

  public void setFeatures(List<FeatureEntity> features) {
    this.features = features;
  }

  public String getJob_id() {
    return job_id;
  }

  public void setJob_id(String jobId) {
    this.job_id = jobId;
  }

  public String getParent_id() {
    return parent_id;
  }

  public void setParent_id(String parent_id) {
    this.parent_id = parent_id;
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

  public List<VersioningModeldbEntityMapping> getVersioningModeldbEntityMappings() {
    return versioningModeldbEntityMappings;
  }

  public void setVersioningModeldbEntityMappings(
      List<VersioningModeldbEntityMapping> versioningModeldbEntityMappings) {
    this.versioningModeldbEntityMappings = versioningModeldbEntityMappings;
  }

  public ExperimentRun getProtoObject() throws InvalidProtocolBufferException {
    LOGGER.trace("starting conversion");
    if (keyValueEntityMap.size() == 0) {
      addKeyValueOnMap(keyValueMapping);
    }
    List<AttributeEntity> attributeList =
        (getAttributeMapping() != null && !getAttributeMapping().isEmpty())
            ? getAttributeMapping()
            : Collections.emptyList();
    List<KeyValueEntity> hyperParameterList =
        (keyValueMapping != null && keyValueEntityMap.containsKey(ModelDBConstants.HYPERPARAMETERS))
            ? keyValueEntityMap.get(ModelDBConstants.HYPERPARAMETERS)
            : Collections.emptyList();
    List<KeyValueEntity> metricList =
        (keyValueMapping != null && keyValueEntityMap.containsKey(ModelDBConstants.METRICS))
            ? keyValueEntityMap.get(ModelDBConstants.METRICS)
            : Collections.emptyList();

    if (artifactEntityMap.size() == 0) {
      addArtifactInMap(artifactMapping);
    }
    List<ArtifactEntity> artifactList =
        (artifactMapping != null && artifactEntityMap.containsKey(ModelDBConstants.ARTIFACTS))
            ? artifactEntityMap.get(ModelDBConstants.ARTIFACTS)
            : Collections.emptyList();
    List<ArtifactEntity> datasetList =
        (artifactMapping != null && artifactEntityMap.containsKey(ModelDBConstants.DATASETS))
            ? artifactEntityMap.get(ModelDBConstants.DATASETS)
            : Collections.emptyList();

    if (observationEntityMap.size() == 0) {
      // add observation in the map
      addObservationMappingOnMap(this.observationMapping);
    }
    List<ObservationEntity> observationList =
        (observationMapping != null
                && observationEntityMap.containsKey(ModelDBConstants.OBSERVATIONS))
            ? observationEntityMap.get(ModelDBConstants.OBSERVATIONS)
            : Collections.emptyList();
    ExperimentRun.Builder experimentRunBuilder =
        ExperimentRun.newBuilder()
            .setId(id)
            .setProjectId(project_id)
            .setExperimentId(experiment_id)
            .setName(name)
            .setDescription(description)
            .setDateCreated(date_created)
            .setDateUpdated(date_updated)
            .setStartTime(start_time)
            .setEndTime(end_time)
            .setCodeVersion(code_version)
            .setCodeVersionSnapshot(
                code_version_snapshot == null
                    ? CodeVersion.getDefaultInstance()
                    : code_version_snapshot.getProtoObject())
            .addAllTags(RdbmsUtils.convertTagsMappingListFromTagList(tags))
            .addAllAttributes(RdbmsUtils.convertAttributeEntityListFromAttributes(attributeList))
            .addAllHyperparameters(
                RdbmsUtils.convertKeyValueEntityListFromKeyValues(hyperParameterList))
            .addAllArtifacts(RdbmsUtils.convertArtifactEntityListFromArtifacts(artifactList))
            .addAllDatasets(RdbmsUtils.convertArtifactEntityListFromArtifacts(datasetList))
            .addAllMetrics(RdbmsUtils.convertKeyValueEntityListFromKeyValues(metricList))
            .addAllObservations(
                RdbmsUtils.convertObservationEntityListFromObservations(observationList))
            .addAllFeatures(RdbmsUtils.convertFeatureEntityListFromFeatureList(features))
            .setJobId(job_id)
            .setParentId(parent_id)
            .setOwner(owner);

    if (code_version_snapshot != null) {
      experimentRunBuilder.setCodeVersionSnapshot(code_version_snapshot.getProtoObject());
    }
    if (versioningModeldbEntityMappings != null && versioningModeldbEntityMappings.size() > 0) {
      VersioningEntry versioningEntry =
          RdbmsUtils.getVersioningEntryFromList(versioningModeldbEntityMappings);
      experimentRunBuilder.setVersionedInputs(versioningEntry);
    }
    LOGGER.trace("Returning converted ExperimentRun");
    return experimentRunBuilder.build();
  }
}
