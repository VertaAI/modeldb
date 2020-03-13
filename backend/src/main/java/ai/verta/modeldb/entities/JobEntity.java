package ai.verta.modeldb.entities;

import ai.verta.modeldb.Job;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.utils.RdbmsUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "job")
public class JobEntity {

  public JobEntity() {}

  public JobEntity(Job job) throws InvalidProtocolBufferException {
    setId(job.getId());
    setDescription(job.getDescription());
    setStart_time(job.getStartTime());
    setEnd_time(job.getEndTime());
    setAttributeMapping(
        RdbmsUtils.convertAttributesFromAttributeEntityList(
            this, ModelDBConstants.METADATA, job.getMetadataList()));
    setJob_status(job.getJobStatusValue());
    setJob_type(job.getJobTypeValue());
    setOwner(job.getOwner());
  }

  @Id
  @Column(name = "id", unique = true)
  private String id;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "start_time")
  private String start_time;

  @Column(name = "end_time")
  private String end_time;

  @OneToMany(targetEntity = KeyValueEntity.class, mappedBy = "jobEntity", cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<KeyValueEntity> keyValueMapping;

  @OneToMany(
      targetEntity = AttributeEntity.class,
      mappedBy = "jobEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<AttributeEntity> attributeMapping;

  @Column(name = "job_status")
  private Integer job_status;

  @Column(name = "job_type")
  private Integer job_type;

  @Column(name = "owner")
  private String owner;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStart_time() {
    return start_time;
  }

  public void setStart_time(String startTime) {
    this.start_time = startTime;
  }

  public String getEnd_time() {
    return end_time;
  }

  public void setEnd_time(String endTime) {
    this.end_time = endTime;
  }

  public Integer getJob_status() {
    return job_status;
  }

  public void setJob_status(Integer jobStatus) {
    this.job_status = jobStatus;
  }

  public Integer getJob_type() {
    return job_type;
  }

  public void setJob_type(Integer jobType) {
    this.job_type = jobType;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
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

  public Job getProtoObject() throws InvalidProtocolBufferException {
    return Job.newBuilder()
        .setId(getId())
        .setDescription(getDescription())
        .setStartTime(getStart_time())
        .setEndTime(getEnd_time())
        .addAllMetadata(RdbmsUtils.convertAttributeEntityListFromAttributes(getAttributeMapping()))
        .setJobStatusValue(getJob_status())
        .setJobTypeValue(getJob_type())
        .setOwner(getOwner())
        .build();
  }
}
