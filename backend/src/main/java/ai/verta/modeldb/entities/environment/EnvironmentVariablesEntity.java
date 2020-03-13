package ai.verta.modeldb.entities.environment;

import ai.verta.modeldb.versioning.EnvironmentVariablesBlob;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "environment_variables")
public class EnvironmentVariablesEntity implements Serializable {
  public EnvironmentVariablesEntity() {}

  public EnvironmentVariablesEntity(EnvironmentVariablesBlob environmentVariablesBlob) {
    variable_name = environmentVariablesBlob.getName();
    variable_value = environmentVariablesBlob.getValue();
  }

  @Id
  @ManyToOne(targetEntity = EnvironmentBlobEntity.class, cascade = CascadeType.ALL)
  @JoinColumn(name = "environment_blob_hash")
  private EnvironmentBlobEntity environmentBlobEntity;

  @Id
  @Column(name = "variable_name")
  private String variable_name;

  @Column(name = "variable_value")
  private String variable_value;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnvironmentVariablesEntity that = (EnvironmentVariablesEntity) o;
    return Objects.equals(environmentBlobEntity, that.environmentBlobEntity)
        && Objects.equals(variable_name, that.variable_name)
        && Objects.equals(variable_value, that.variable_value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environmentBlobEntity, variable_name, variable_value);
  }

  public void setEnvironmentBlobEntity(EnvironmentBlobEntity environmentBlobEntity) {
    this.environmentBlobEntity = environmentBlobEntity;
  }

  public EnvironmentVariablesBlob toProto() {
    return EnvironmentVariablesBlob.newBuilder()
        .setName(variable_name)
        .setValue(variable_value)
        .build();
  }
}
