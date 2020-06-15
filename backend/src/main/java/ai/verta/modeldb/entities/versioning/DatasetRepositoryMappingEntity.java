package ai.verta.modeldb.entities.versioning;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "dataset_repository_mapping")
public class DatasetRepositoryMappingEntity implements Serializable {
  public DatasetRepositoryMappingEntity() {}

  public DatasetRepositoryMappingEntity(
      RepositoryEntity repositoryEntity, boolean isDatasetRepository) {
    this.repositoryEntity = repositoryEntity;
    this.isDatasetRepository = isDatasetRepository;
  }

  @Id
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "repository_id")
  private RepositoryEntity repositoryEntity;

  @Column(name = "is_dataset_repository")
  private Boolean isDatasetRepository = false;

  public Boolean getIsDatasetRepository() {
    return isDatasetRepository;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DatasetRepositoryMappingEntity that = (DatasetRepositoryMappingEntity) o;
    return repositoryEntity.equals(that.repositoryEntity)
        && isDatasetRepository.equals(that.isDatasetRepository);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repositoryEntity, isDatasetRepository);
  }
}
