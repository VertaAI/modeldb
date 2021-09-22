package ai.verta.modeldb.entities.environment;

import ai.verta.modeldb.versioning.VersionEnvironmentBlob;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "python_environment_blob")
public class PythonEnvironmentBlobEntity implements Serializable {

  @Id
  @Column(name = "blob_hash", nullable = false, columnDefinition = "varchar", length = 64)
  private String blob_hash;

  @Column(name = "major")
  private Integer major;

  @Column(name = "minor")
  private Integer minor;

  @Column(name = "patch")
  private Integer patch;

  @Column(name = "suffix", columnDefinition = "varchar", length = 50)
  private String suffix;

  @Column(name = "raw_requirements", columnDefinition = "TEXT")
  private String raw_requirements;

  @Column(name = "raw_constraints", columnDefinition = "TEXT")
  private String raw_constraints;

  public String getBlob_hash() {
    return blob_hash;
  }

  public void setBlob_hash(String blob_hash) {
    this.blob_hash = blob_hash;
  }

  public Integer getMajor() {
    return major;
  }

  public void setMajor(Integer major) {
    this.major = major;
  }

  public Integer getMinor() {
    return minor;
  }

  public void setMinor(Integer minor) {
    this.minor = minor;
  }

  public Integer getPatch() {
    return patch;
  }

  public void setPatch(Integer patch) {
    this.patch = patch;
  }

  public String getRaw_requirements() {
    return raw_requirements;
  }

  public void setRaw_requirements(String raw_requirements) {
    this.raw_requirements = raw_requirements;
  }

  public String getRaw_constraints() {
    return raw_constraints;
  }

  public void setRaw_constraints(String raw_constraints) {
    this.raw_constraints = raw_constraints;
  }

  @OneToMany(mappedBy = "pythonEnvironmentBlobEntity")
  private Set<PythonEnvironmentRequirementBlobEntity> pythonEnvironmentRequirementBlobEntity =
      new HashSet<>();

  public VersionEnvironmentBlob getVersion() {
    return VersionEnvironmentBlob.newBuilder()
        .setMajor(major)
        .setMinor(minor)
        .setPatch(patch)
        .build();
  }

  public Set<PythonEnvironmentRequirementBlobEntity> getPythonEnvironmentRequirementBlobEntity() {
    return pythonEnvironmentRequirementBlobEntity;
  }
}
