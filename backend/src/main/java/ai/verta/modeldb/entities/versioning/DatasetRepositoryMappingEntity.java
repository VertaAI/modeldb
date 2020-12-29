package ai.verta.modeldb.entities.versioning;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "dataset_repository_mapping")
public class DatasetRepositoryMappingEntity implements Serializable {
  public DatasetRepositoryMappingEntity() {}

  public DatasetRepositoryMappingEntity(RepositoryEntity repositoryEntity) {
    this.repositoryEntity = repositoryEntity;
  }

  @Id
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "repository_id")
  private RepositoryEntity repositoryEntity;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DatasetRepositoryMappingEntity that = (DatasetRepositoryMappingEntity) o;
    return repositoryEntity.equals(that.repositoryEntity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repositoryEntity);
  }
}
