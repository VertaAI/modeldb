package ai.verta.modeldb.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "upload_status")
public class UploadStatusEntity {
  public static final int PATH_DATASET_COMPONENT_BLOB = 0;
  public static final int S3_DATASET_COMPONENT_BLOB = 1;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "dataset_component_blob_id", updatable = false)
  private String dataset_component_blob_id;

  @Column(name = "component_blob_type")
  private Integer component_blob_type;

  @Column(name = "upload_id")
  private String uploadId;

  @Column(name = "upload_completed")
  private boolean uploadCompleted;

  public String getDataset_component_blob_id() {
    return dataset_component_blob_id;
  }

  public void setDataset_component_blob_id(String dataset_component_blob_id) {
    this.dataset_component_blob_id = dataset_component_blob_id;
  }

  public Integer getComponent_blob_type() {
    return component_blob_type;
  }

  public void setComponent_blob_type(Integer component_blob_type) {
    this.component_blob_type = component_blob_type;
  }

  public String getUploadId() {
    return uploadId;
  }

  public void setUploadId(String uploadId) {
    this.uploadId = uploadId;
  }

  public boolean isUploadCompleted() {
    return uploadCompleted;
  }

  public void setUploadCompleted(boolean uploadCompleted) {
    this.uploadCompleted = uploadCompleted;
  }
}
