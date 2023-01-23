package ai.verta.modeldb.entities.config;

import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob;
import com.google.rpc.Code;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Entity
@Table(name = "hyperparameter_element_mapping")
public class HyperparameterElementMappingEntity implements Serializable {
  private HyperparameterElementMappingEntity() {}

  private static final Logger LOGGER =
      LogManager.getLogger(HyperparameterElementMappingEntity.class);

  public HyperparameterElementMappingEntity(
      Object entity, String name, HyperparameterValuesConfigBlob hyperparameterValuesConfigBlob)
      throws ModelDBException {
    this.name = name;
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
            "Invalid value found in HyperparameterValuesConfigBlob", Code.INVALID_ARGUMENT);
    }

    this.entity_type = entity.getClass().getSimpleName();
    if (entity instanceof ExperimentRunEntity) {
      this.experimentRunEntity = (ExperimentRunEntity) entity;
    } else {
      LOGGER.warn("ExperimentRunEntity expected : found {}", entity.getClass());
      throw new InternalErrorException("Invalid ModelDB entity found");
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "name", columnDefinition = "varchar")
  private String name;

  @Column(name = "int_value")
  private Long int_value;

  @Column(name = "float_value")
  private Double float_value;

  @Column(name = "string_value", columnDefinition = "varchar")
  private String string_value;

  @Column(name = "entity_type", length = 50)
  private String entity_type;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_run_id")
  private ExperimentRunEntity experimentRunEntity;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getInt_value() {
    return int_value;
  }

  public void setInt_value(Long int_value) {
    this.int_value = int_value;
  }

  public Double getFloat_value() {
    return float_value;
  }

  public void setFloat_value(Double float_value) {
    this.float_value = float_value;
  }

  public String getString_value() {
    return string_value;
  }

  public void setString_value(String string_value) {
    this.string_value = string_value;
  }

  public String getEntity_type() {
    return entity_type;
  }

  public void setEntity_type(String entity_type) {
    this.entity_type = entity_type;
  }

  public ExperimentRunEntity getExperimentRunEntity() {
    return experimentRunEntity;
  }

  public void setExperimentRunEntity(ExperimentRunEntity experimentRunEntity) {
    this.experimentRunEntity = experimentRunEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HyperparameterElementMappingEntity that = (HyperparameterElementMappingEntity) o;
    return name.equals(that.name)
        && int_value.equals(that.int_value)
        && float_value.equals(that.float_value)
        && string_value.equals(that.string_value)
        && entity_type.equals(that.entity_type)
        && experimentRunEntity.equals(that.experimentRunEntity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        name, int_value, float_value, string_value, entity_type, experimentRunEntity);
  }
}
