package ai.verta.modeldb.entities.environment;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "environment_blob")
public class EnvironmentBlobEntity {

  public EnvironmentBlobEntity() {}

  @Id
  @Column(name = "blob_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String blob_hash;

  @Column(name = "environment_type")
  private Integer environment_type;

  @OneToOne(targetEntity = PythonEnvironmentBlobEntity.class, cascade = CascadeType.ALL)
  @JoinColumn(name = "python_environment_blob_hash")
  private PythonEnvironmentBlobEntity pythonEnvironmentBlobEntity;

  @OneToOne(targetEntity = DockerEnvironmentBlobEntity.class, cascade = CascadeType.ALL)
  @JoinColumn(name = "docker_environment_blob_hash")
  private DockerEnvironmentBlobEntity dockerEnvironmentBlobEntity;

  @OneToMany(mappedBy = "environmentBlobEntity")
  @OrderBy("command_seq_number")
  private Set<EnvironmentCommandLineEntity> environmentCommandLineEntities = new HashSet<>();

  @OneToMany(mappedBy = "environmentBlobEntity")
  private Set<EnvironmentVariablesEntity> environmentVariablesEntities = new HashSet<>();

  public String getBlob_hash() {
    return blob_hash;
  }

  public void setBlob_hash(String blob_hash) {
    this.blob_hash = blob_hash;
  }

  public Integer getEnvironment_type() {
    return environment_type;
  }

  public void setEnvironment_type(Integer environment_type) {
    this.environment_type = environment_type;
  }

  public PythonEnvironmentBlobEntity getPythonEnvironmentBlobEntity() {
    return pythonEnvironmentBlobEntity;
  }

  public void setPythonEnvironmentBlobEntity(
      PythonEnvironmentBlobEntity pythonEnvironmentBlobEntity) {
    this.pythonEnvironmentBlobEntity = pythonEnvironmentBlobEntity;
  }

  public DockerEnvironmentBlobEntity getDockerEnvironmentBlobEntity() {
    return dockerEnvironmentBlobEntity;
  }

  public void setDockerEnvironmentBlobEntity(
      DockerEnvironmentBlobEntity dockerEnvironmentBlobEntity) {
    this.dockerEnvironmentBlobEntity = dockerEnvironmentBlobEntity;
  }

  public Set<EnvironmentCommandLineEntity> getEnvironmentCommandLineEntities() {
    return environmentCommandLineEntities;
  }

  public Set<EnvironmentVariablesEntity> getEnvironmentVariablesEntities() {
    return environmentVariablesEntities;
  }
}
