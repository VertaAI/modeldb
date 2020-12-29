package ai.verta.modeldb.entities.dataset;

import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "path_dataset_component_blob")
public class PathDatasetComponentBlobEntity {
  public PathDatasetComponentBlobEntity() {}

  public PathDatasetComponentBlobEntity(
      String blobHash, String blobHashDataset, PathDatasetComponentBlob pathDatasetComponentBlob) {

    this.id = new PathDatasetComponentBlobId(blobHash, blobHashDataset);
    this.path = pathDatasetComponentBlob.getPath();
    this.size = pathDatasetComponentBlob.getSize();
    this.last_modified_at_source = pathDatasetComponentBlob.getLastModifiedAtSource();
    this.sha256 = pathDatasetComponentBlob.getSha256();
    this.md5 = pathDatasetComponentBlob.getMd5();
    this.internal_versioned_path = pathDatasetComponentBlob.getInternalVersionedPath();
    this.base_path = pathDatasetComponentBlob.getBasePath();
  }

  @EmbeddedId private PathDatasetComponentBlobId id;

  @Column(name = "path", columnDefinition = "TEXT")
  private String path;

  @Column(name = "internal_versioned_path", columnDefinition = "TEXT")
  private String internal_versioned_path;

  @Column(name = "size")
  private Long size;

  @Column(name = "last_modified_at_source")
  private Long last_modified_at_source;

  @Column(name = "sha256", columnDefinition = "text")
  private String sha256;

  @Column(name = "md5", columnDefinition = "text")
  private String md5;

  @Column(name = "base_path", columnDefinition = "TEXT")
  private String base_path;

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

  public PathDatasetComponentBlob toProto() {
    PathDatasetComponentBlob.Builder pathDatasetComponentBlob =
        PathDatasetComponentBlob.newBuilder()
            .setPath(this.path)
            .setSize(this.size)
            .setLastModifiedAtSource(this.last_modified_at_source)
            .setSha256(this.sha256)
            .setMd5(this.md5);
    if (base_path != null) {
      pathDatasetComponentBlob.setBasePath(base_path);
    }
    if (this.internal_versioned_path != null) {
      pathDatasetComponentBlob.setInternalVersionedPath(this.internal_versioned_path);
    }
    return pathDatasetComponentBlob.build();
  }
}

@Embeddable
class PathDatasetComponentBlobId implements Serializable {

  @Column(name = "blob_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String blob_hash;

  @Column(
      name = "path_dataset_blob_id",
      nullable = false,
      columnDefinition = "varchar",
      length = 64)
  private String path_dataset_blob_id;

  public PathDatasetComponentBlobId(String blobHash, String datasetBlobHash) {
    this.blob_hash = blobHash;
    path_dataset_blob_id = datasetBlobHash;
  }

  private PathDatasetComponentBlobId() {}

  public String getBlob_hash() {
    return blob_hash;
  }

  public String getPath_dataset_blob_id() {
    return path_dataset_blob_id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof S3DatasetComponentBlobId)) return false;
    S3DatasetComponentBlobId that = (S3DatasetComponentBlobId) o;
    return Objects.equals(getBlob_hash(), that.getBlob_hash())
        && Objects.equals(getPath_dataset_blob_id(), that.getS3_dataset_blob_id());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBlob_hash(), getPath_dataset_blob_id());
  }
}
