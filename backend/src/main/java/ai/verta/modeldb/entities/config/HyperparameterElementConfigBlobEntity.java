package ai.verta.modeldb.entities.config;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob;
import io.grpc.Status;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "hyperparameter_element_config_blob")
public class HyperparameterElementConfigBlobEntity {
  private HyperparameterElementConfigBlobEntity() {}

  public HyperparameterElementConfigBlobEntity(
      String blobHash,
      String commitHash,
      String name,
      HyperparameterValuesConfigBlob hyperparameterValuesConfigBlob)
      throws ModelDBException {
    this.blob_hash = blobHash;
    this.name = name;
    this.commit_hash = commitHash;
    this.value_type = hyperparameterValuesConfigBlob.getValueCase().getNumber();
    switch (hyperparameterValuesConfigBlob.getValueCase()) {
      case INT_VALUE:
        this.int_value = hyperparameterValuesConfigBlob.getIntValue();
        break;
      case FLOAT_VALUE:
        this.float_value = (double) hyperparameterValuesConfigBlob.getFloatValue();
        break;
      case STRING_VALUE:
        this.string_value = hyperparameterValuesConfigBlob.getStringValue();
        break;
      case VALUE_NOT_SET:
      default:
        throw new ModelDBException(
            "Invalid value found in HyperparameterValuesConfigBlob", Status.Code.INVALID_ARGUMENT);
    }
  }

  @Id
  @Column(name = "blob_hash", columnDefinition = "varchar", length = 64, nullable = false)
  private String blob_hash;

  @Column(name = "name", columnDefinition = "varchar")
  private String name;

  @Column(name = "commit_hash", columnDefinition = "varchar", length = 64)
  private String commit_hash;

  @Column(name = "value_type")
  private Integer value_type;

  @Column(name = "int_value")
  private Long int_value;

  @Column(name = "float_value")
  private Double float_value;

  @Column(name = "string_value", columnDefinition = "varchar")
  private String string_value;

  public String getName() {
    return name;
  }

  public HyperparameterValuesConfigBlob toProto() {
    HyperparameterValuesConfigBlob.Builder builder = HyperparameterValuesConfigBlob.newBuilder();
    if (this.int_value != null) {
      builder.setIntValue(this.int_value);
    }
    if (this.float_value != null) {
      builder.setFloatValue(this.float_value.floatValue());
    }
    if (this.string_value != null && !this.string_value.isEmpty()) {
      builder.setStringValue(this.string_value);
    }
    return builder.build();
  }

  public String getBlobHash() {
    return blob_hash;
  }
}
