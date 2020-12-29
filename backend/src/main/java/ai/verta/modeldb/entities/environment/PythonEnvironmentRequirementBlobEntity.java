package ai.verta.modeldb.entities.environment;

import ai.verta.modeldb.versioning.PythonEnvironmentBlob.Builder;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import ai.verta.modeldb.versioning.VersionEnvironmentBlob;
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
@Table(name = "python_environment_requirements_blob")
public class PythonEnvironmentRequirementBlobEntity implements Serializable {

  public PythonEnvironmentRequirementBlobEntity() {}

  public PythonEnvironmentRequirementBlobEntity(
      PythonRequirementEnvironmentBlob pythonRequirementEnvironmentBlob,
      PythonEnvironmentBlobEntity pythonEnvironmentBlobEntity,
      boolean isRequirement) {
    library = pythonRequirementEnvironmentBlob.getLibrary();
    constraint = pythonRequirementEnvironmentBlob.getConstraint();
    final VersionEnvironmentBlob version = pythonRequirementEnvironmentBlob.getVersion();
    major = version.getMajor();
    minor = version.getMinor();
    patch = version.getPatch();
    this.pythonEnvironmentBlobEntity = pythonEnvironmentBlobEntity;
    this.isRequirement = isRequirement;
  }

  @Id
  @ManyToOne(targetEntity = PythonEnvironmentBlobEntity.class, cascade = CascadeType.ALL)
  @JoinColumn(name = "python_environment_blob_hash")
  private PythonEnvironmentBlobEntity pythonEnvironmentBlobEntity;

  @Id
  @Column(name = "library")
  private String library;

  @Id
  @Column(name = "python_constraint")
  private String constraint;

  @Column(name = "major")
  private Integer major;

  @Column(name = "minor")
  private Integer minor;

  @Column(name = "patch")
  private Integer patch;

  @Column(name = "suffix", columnDefinition = "varchar", length = 50)
  private String suffix;

  @Id
  @Column(name = "req_or_constraint", nullable = false)
  private Boolean isRequirement;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PythonEnvironmentRequirementBlobEntity that = (PythonEnvironmentRequirementBlobEntity) o;
    return Objects.equals(pythonEnvironmentBlobEntity, that.pythonEnvironmentBlobEntity)
        && Objects.equals(library, that.library)
        && Objects.equals(constraint, that.constraint)
        && Objects.equals(major, that.major)
        && Objects.equals(minor, that.minor)
        && Objects.equals(patch, that.patch)
        && Objects.equals(isRequirement, that.isRequirement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        pythonEnvironmentBlobEntity, library, constraint, major, minor, patch, isRequirement);
  }

  public String getLibrary() {
    return library;
  }

  public String getConstraint() {
    return constraint;
  }

  public String getSuffix() {
    return suffix;
  }

  public VersionEnvironmentBlob getVersion() {
    return VersionEnvironmentBlob.newBuilder()
        .setMajor(major)
        .setMinor(minor)
        .setPatch(patch)
        .build();
  }

  public Boolean isRequirement() {
    return isRequirement;
  }

  public void toProto(Builder pythonEnvironmentBlobBuilder) {
    final PythonRequirementEnvironmentBlob.Builder builderForValue =
        PythonRequirementEnvironmentBlob.newBuilder().setVersion(getVersion());
    builderForValue.setLibrary(library).setConstraint(constraint);
    if (isRequirement()) {
      pythonEnvironmentBlobBuilder.addRequirements(builderForValue);
    } else {
      pythonEnvironmentBlobBuilder.addConstraints(builderForValue);
    }
  }
}
