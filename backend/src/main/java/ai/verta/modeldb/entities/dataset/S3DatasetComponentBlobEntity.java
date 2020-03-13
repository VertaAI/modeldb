package ai.verta.modeldb.entities.dataset;

import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.S3DatasetComponentBlob;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "s3_dataset_component_blob")
public class S3DatasetComponentBlobEntity {
  public S3DatasetComponentBlobEntity() {}

  public S3DatasetComponentBlobEntity(
      String blobHash, String blobHashDataset, S3DatasetComponentBlob s3DatasetComponentBlob) {

    PathDatasetComponentBlob pathDatasetComponentBlob = s3DatasetComponentBlob.getPath();
    this.id = new S3DatasetComponentBlobId(blobHash, blobHashDataset);
    this.path = pathDatasetComponentBlob.getPath();
    this.size = pathDatasetComponentBlob.getSize();
    this.last_modified_at_source = pathDatasetComponentBlob.getLastModifiedAtSource();
    this.sha256 = pathDatasetComponentBlob.getSha256();
    this.md5 = pathDatasetComponentBlob.getMd5();
  }

  @EmbeddedId private S3DatasetComponentBlobId id;

  @Column(name = "path", columnDefinition = "TEXT")
  private String path;

  @Column(name = "size")
  private Long size;

  @Column(name = "last_modified_at_source")
  private Long last_modified_at_source;

  @Column(name = "sha256", columnDefinition = "text")
  private String sha256;

  @Column(name = "md5", columnDefinition = "text")
  private String md5;

  public String getPath() {
    return path;
  }

  public Long getSize() {
    return size;
  }

  public Long getLast_modified_at_source() {
    return last_modified_at_source;
  }

  public String getSha256() {
    return sha256;
  }

  public String getMd5() {
    return md5;
  }

  public S3DatasetComponentBlob toProto() {
    return S3DatasetComponentBlob.newBuilder()
        .setPath(
            PathDatasetComponentBlob.newBuilder()
                .setPath(this.path)
                .setSize(this.size)
                .setLastModifiedAtSource(this.last_modified_at_source)
                .setSha256(this.sha256)
                .setMd5(this.md5)
                .build())
        .build();
  }
}

@Embeddable
class S3DatasetComponentBlobId implements Serializable {

  @Column(name = "blob_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String blob_hash;

  @Column(name = "s3_dataset_blob_id", nullable = false, columnDefinition = "varchar", length = 64)
  private String s3_dataset_blob_id;

  public S3DatasetComponentBlobId(String blobHash, String blobHashDataset) {
    this.blob_hash = blobHash;
    this.s3_dataset_blob_id = blobHashDataset;
  }

  private S3DatasetComponentBlobId() {}

  public String getBlob_hash() {
    return blob_hash;
  }

  public String getS3_dataset_blob_id() {
    return s3_dataset_blob_id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof S3DatasetComponentBlobId)) return false;
    S3DatasetComponentBlobId that = (S3DatasetComponentBlobId) o;
    return Objects.equals(getBlob_hash(), that.getBlob_hash())
        && Objects.equals(getS3_dataset_blob_id(), that.getS3_dataset_blob_id());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBlob_hash(), getS3_dataset_blob_id());
  }
}
