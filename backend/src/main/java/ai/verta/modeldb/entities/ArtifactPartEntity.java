package ai.verta.modeldb.entities;

import ai.verta.common.ArtifactPart;
import com.amazonaws.services.s3.model.PartETag;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "artifact_part")
public class ArtifactPartEntity implements Serializable {

  public static final int EXP_RUN_ARTIFACT = 0;
  public static final int VERSION_BLOB_ARTIFACT = 1;

  public ArtifactPartEntity() {}

  public ArtifactPartEntity(String artifactId, int artifactType, long partNumber, String etag) {
    this.artifact_id = artifactId;
    this.artifact_type = artifactType;
    this.partNumber = partNumber;
    this.etag = etag;
  }

  @Id
  @Column(name = "artifact_id", nullable = false)
  private String artifact_id;

  @Id
  @Column(name = "artifact_type", nullable = false)
  private Integer artifact_type;

  @Id
  @Column(name = "part_number", nullable = false)
  private long partNumber;

  @Column private String etag;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ArtifactPartEntity that = (ArtifactPartEntity) o;
    return partNumber == that.partNumber
        && Objects.equals(artifact_id, that.artifact_id)
        && Objects.equals(artifact_type, that.artifact_type)
        && Objects.equals(etag, that.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifact_id, artifact_type, partNumber, etag);
  }

  public ArtifactPart toProto() {
    return ArtifactPart.newBuilder().setPartNumber(partNumber).setEtag(etag).build();
  }

  public PartETag toPartETag() {
    return new PartETag((int) partNumber, etag);
  }

  public long getPartNumber() {
    return partNumber;
  }

  public String getEtag() {
    return etag;
  }
}
