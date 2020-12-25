package ai.verta.modeldb.dto;

import ai.verta.modeldb.DatasetVersion;
import java.util.List;

public class DatasetVersionDTO {
  private List<DatasetVersion> datasetVersions;
  private Long totalRecords;

  /** @return the datasetVersions */
  public List<DatasetVersion> getDatasetVersions() {
    return datasetVersions;
  }

  /** @param datasetVersions the datasetVersions to set */
  public void setDatasetVersions(List<DatasetVersion> datasetVersions) {
    this.datasetVersions = datasetVersions;
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
