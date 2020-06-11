package ai.verta.modeldb.entities;

import ai.verta.modeldb.ArtifactPart;
import com.amazonaws.services.s3.model.PartETag;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "artifact_part")
public class ArtifactPartEntity implements Serializable {

  public ArtifactPartEntity() {}

  public ArtifactPartEntity(ArtifactEntity artifactEntity, long partNumber, String etag) {
    this.artifactEntity = artifactEntity;
    this.partNumber = partNumber;
    this.etag = etag;
  }

  @Id
  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "artifact_id")
  private ArtifactEntity artifactEntity;

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
        && Objects.equals(artifactEntity, that.artifactEntity)
        && Objects.equals(etag, that.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifactEntity, partNumber, etag);
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
