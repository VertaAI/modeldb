package ai.verta.modeldb.entities.lineage;

import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.LineageEntry.DescriptionCase;
import ai.verta.modeldb.Location;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "lineage_versioning")
public class LineageEntity implements Serializable {

  public LineageEntity() {}

  public LineageEntity(Long id, LineageEntry input, LineageEntry output)
      throws InvalidProtocolBufferException {
    outputType = output.getDescriptionCase().getNumber();
    switch (output.getDescriptionCase()) {
      case EXPERIMENT_RUN:
        outputExperimentId = output.getExperimentRun();
        break;
      case BLOB:
        VersioningLineageEntry blob = output.getBlob();
        outputRepositoryId = blob.getRepositoryId();
        outputCommitSha = blob.getCommitSha();
        outputLocation = ModelDBUtils.getStringFromProtoObject(Location.newBuilder().addAllLocation(blob.getLocationList()));
    }
    this.id = id;
  }

  @Id
  Long id;

  @Id
  @Column(name = "input_experiment_id")
  private String inputExperimentId;
  
  @Id
  @Column(name = "input_repository_id")
  Long inputRepositoryId;
  
  @Id
  @Column(name = "input_commit_sha")
  String inputCommitSha;
  
  @Id
  @Column(name = "input_location")
  String inputLocation;

  @Id
  @Column(name = "input_type")
  private Integer inputType;

  @Id
  @Column(name = "output_experiment_id")
  private String outputExperimentId;

  @Id
  @Column(name = "output_repository_id")
  Long outputRepositoryId;

  @Id
  @Column(name = "output_commit_sha")
  String outputCommitSha;

  @Id
  @Column(name = "output_location")
  String outputLocation;
  
  @Id
  @Column(name = "output_type")
  private Integer outputType;

  public String getInputExperimentId() {
    return inputExperimentId;
  }

  public Long getInputRepositoryId() {
    return inputRepositoryId;
  }

  public String getInputCommitSha() {
    return inputCommitSha;
  }

  public String getInputLocation() {
    return inputLocation;
  }

  public Integer getInputType() {
    return inputType;
  }

  public String getOutputExperimentId() {
    return outputExperimentId;
  }

  public Long getOutputRepositoryId() {
    return outputRepositoryId;
  }

  public String getOutputCommitSha() {
    return outputCommitSha;
  }

  public String getOutputLocation() {
    return outputLocation;
  }

  public Integer getOutputType() {
    return outputType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LineageEntity that = (LineageEntity) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(inputExperimentId, that.inputExperimentId) &&
        Objects.equals(inputRepositoryId, that.inputRepositoryId) &&
        Objects.equals(inputCommitSha, that.inputCommitSha) &&
        Objects.equals(inputLocation, that.inputLocation) &&
        Objects.equals(inputType, that.inputType) &&
        Objects.equals(outputExperimentId, that.outputExperimentId) &&
        Objects.equals(outputRepositoryId, that.outputRepositoryId) &&
        Objects.equals(outputCommitSha, that.outputCommitSha) &&
        Objects.equals(outputLocation, that.outputLocation) &&
        Objects.equals(outputType, that.outputType);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, inputExperimentId, inputRepositoryId, inputCommitSha, inputLocation, inputType,
            outputExperimentId, outputRepositoryId, outputCommitSha, outputLocation, outputType);
  }

  public VersioningLineageEntry getInputBlob() throws InvalidProtocolBufferException {
    if (inputType == DescriptionCase.BLOB.getNumber()) {
      Location.Builder builder = Location.newBuilder();
      ModelDBUtils.getProtoObjectFromString(getInputLocation(), builder);
      return VersioningLineageEntry.newBuilder().setRepositoryId(getInputRepositoryId())
          .setCommitSha(getInputCommitSha()).addAllLocation(builder.getLocationList()).build();
    }
    return null;
  }

  public VersioningLineageEntry getOutputBlob() throws InvalidProtocolBufferException {
    if (outputType == DescriptionCase.BLOB.getNumber()) {
      Location.Builder builder = Location.newBuilder();
      ModelDBUtils.getProtoObjectFromString(getOutputLocation(), builder);
      return VersioningLineageEntry.newBuilder().setRepositoryId(getOutputRepositoryId())
          .setCommitSha(getOutputCommitSha()).addAllLocation(builder.getLocationList()).build();
    }
    return null;
  }
}
