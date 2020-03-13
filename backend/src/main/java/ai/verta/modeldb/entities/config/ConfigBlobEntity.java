package ai.verta.modeldb.entities.config;

import ai.verta.modeldb.ModelDBException;
import io.grpc.Status;
import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "config_blob")
public class ConfigBlobEntity implements Serializable {

  public static final int HYPERPARAMETER_SET = 0;
  public static final int HYPERPARAMETER = 1;

  private ConfigBlobEntity() {}

  public ConfigBlobEntity(String blobHash, Integer configSeqNumber, Object blobEntity)
      throws ModelDBException {
    this.blob_hash = blobHash;
    this.config_seq_number = configSeqNumber;

    if (blobEntity instanceof HyperparameterSetConfigBlobEntity) {
      this.hyperparameterSetConfigBlobEntity = (HyperparameterSetConfigBlobEntity) blobEntity;
      this.hyperparameter_type = HYPERPARAMETER_SET;
    } else if (blobEntity instanceof HyperparameterElementConfigBlobEntity) {
      this.hyperparameterElementConfigBlobEntity =
          (HyperparameterElementConfigBlobEntity) blobEntity;
      this.hyperparameter_type = HYPERPARAMETER;
    } else {
      throw new ModelDBException("Invalid blob object found", Status.Code.INVALID_ARGUMENT);
    }
  }

  @Id
  @Column(name = "blob_hash", columnDefinition = "varchar", length = 64, nullable = false)
  private String blob_hash;

  @Id
  @Column(name = "config_seq_number")
  private Integer config_seq_number;

  @Column(name = "hyperparameter_type")
  private Integer hyperparameter_type;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "hyperparameter_set_config_blob_hash")
  private HyperparameterSetConfigBlobEntity hyperparameterSetConfigBlobEntity;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "hyperparameter_element_config_blob_hash")
  private HyperparameterElementConfigBlobEntity hyperparameterElementConfigBlobEntity;

  public void setBlobHash(String blobHash) {
    blob_hash = blobHash;
  }

  public Integer getConfigSeqNumber() {
    return config_seq_number;
  }

  public Integer getHyperparameter_type() {
    return hyperparameter_type;
  }

  public String getComponentBlobHash() {
    if (hyperparameter_type == HYPERPARAMETER_SET) {
      return hyperparameterSetConfigBlobEntity.getBlobHash();
    } else {
      return hyperparameterElementConfigBlobEntity.getBlobHash();
    }
  }
}
