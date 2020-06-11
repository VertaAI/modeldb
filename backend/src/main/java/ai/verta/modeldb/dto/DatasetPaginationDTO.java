package ai.verta.modeldb.dto;

import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.versioning.Repository;
import java.util.List;

public class DatasetPaginationDTO {
  private List<Dataset> datasets;
  private Long totalRecords;
  private List<Repository> repositories;

  /** @return the datasets */
  public List<Dataset> getDatasets() {
    return datasets;
  }

  /** @param datasets the datasets to set */
  public void setDatasets(List<Dataset> datasets) {
    this.datasets = datasets;
  }

  /** @return the totalRecords */
  public Long getTotalRecords() {
    return totalRecords;
  }

  /** @param totalRecords the totalRecords to set */
  public void setTotalRecords(Long totalRecords) {
    this.totalRecords = totalRecords;
  }

  public List<Repository> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<Repository> repositories) {
    this.repositories = repositories;
  }
}
