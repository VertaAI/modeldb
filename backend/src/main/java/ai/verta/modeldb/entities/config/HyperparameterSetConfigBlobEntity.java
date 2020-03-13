package ai.verta.modeldb.entities.config;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.ValueCase;
import ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob;
import io.grpc.Status.Code;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "hyperparameter_set_config_blob")
public class HyperparameterSetConfigBlobEntity {
  private HyperparameterSetConfigBlobEntity() {}

  public HyperparameterSetConfigBlobEntity(
      String blobHash, HyperparameterSetConfigBlob hyperparameterSetConfigBlob)
      throws ModelDBException {
    this.blob_hash = blobHash;
    this.name = hyperparameterSetConfigBlob.getName();
    this.value_type = hyperparameterSetConfigBlob.getValueCase().getNumber();
  }

  @Id
  @Column(name = "blob_hash", columnDefinition = "varchar", length = 64, nullable = false)
  private String blob_hash;

  @Column(name = "name", columnDefinition = "varchar")
  private String name;

  @Column(name = "value_type")
  private Integer value_type;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "interval_begin_hash")
  private HyperparameterElementConfigBlobEntity interval_begin_hash;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "interval_end_hash")
  private HyperparameterElementConfigBlobEntity interval_end_hash;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "interval_step_hash")
  private HyperparameterElementConfigBlobEntity interval_step_hash;

  @ManyToMany
  @JoinTable(
      name = "hyperparameter_discrete_set_element_mapping",
      joinColumns = {@JoinColumn(name = "set_hash")},
      inverseJoinColumns = {@JoinColumn(name = "element_hash")})
  private Set<HyperparameterElementConfigBlobEntity> hyperparameterSetConfigElementMapping =
      new HashSet<>();

  public void setInterval_begin_hash(HyperparameterElementConfigBlobEntity interval_begin_hash) {
    this.interval_begin_hash = interval_begin_hash;
  }

  public void setInterval_end_hash(HyperparameterElementConfigBlobEntity interval_end_hash) {
    this.interval_end_hash = interval_end_hash;
  }

  public void setInterval_step_hash(HyperparameterElementConfigBlobEntity interval_step_hash) {
    this.interval_step_hash = interval_step_hash;
  }

  public void setHyperparameterSetConfigElementMapping(
      Collection<HyperparameterElementConfigBlobEntity> hyperparameterSetConfigElementMapping) {
    this.hyperparameterSetConfigElementMapping.addAll(hyperparameterSetConfigElementMapping);
  }

  public HyperparameterSetConfigBlob toProto() throws ModelDBException {
    HyperparameterSetConfigBlob.Builder builder =
        HyperparameterSetConfigBlob.newBuilder().setName(this.name);
    ValueCase valueCase = ValueCase.forNumber(this.value_type);
    if (valueCase == null) {
      throw new ModelDBException(
          "Invalid value found for HyperparameterSetConfigBlob", Code.INVALID_ARGUMENT);
    }

    switch (valueCase) {
      case CONTINUOUS:
        builder.setContinuous(
            ContinuousHyperparameterSetConfigBlob.newBuilder()
                .setIntervalBegin(this.interval_begin_hash.toProto())
                .setIntervalEnd(this.interval_end_hash.toProto())
                .setIntervalStep(this.interval_step_hash.toProto())
                .build());
        break;
      case DISCRETE:
        List<HyperparameterValuesConfigBlob> valueSetConfigBlob =
            this.hyperparameterSetConfigElementMapping.stream()
                .map(HyperparameterElementConfigBlobEntity::toProto)
                .collect(Collectors.toList());

        DiscreteHyperparameterSetConfigBlob hyperparameterSetConfigBlob =
            DiscreteHyperparameterSetConfigBlob.newBuilder()
                .addAllValues(valueSetConfigBlob)
                .build();
        builder.setDiscrete(hyperparameterSetConfigBlob);
        break;
      case VALUE_NOT_SET:
      default:
        throw new ModelDBException(
            "Invalid value found for HyperparameterSetConfigBlob", Code.INVALID_ARGUMENT);
    }
    return builder.build();
  }

  public String getBlobHash() {
    return blob_hash;
  }
}
