package ai.verta.modeldb.dto;

import ai.verta.modeldb.Dataset;
import java.util.List;

public class DatasetPaginationDTO {
  private List<Dataset> datasets;
  private Long totalRecords;

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
}
