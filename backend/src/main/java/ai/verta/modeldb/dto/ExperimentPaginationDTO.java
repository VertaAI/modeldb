package ai.verta.modeldb.dto;

import ai.verta.modeldb.Experiment;
import java.util.List;

public class ExperimentPaginationDTO {

  private List<Experiment> experiments;
  private Long totalRecords;

  public List<Experiment> getExperiments() {
    return experiments;
  }

  public void setExperiments(List<Experiment> experiments) {
    this.experiments = experiments;
  }

  public Long getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(Long totalRecords) {
    this.totalRecords = totalRecords;
  }
}
