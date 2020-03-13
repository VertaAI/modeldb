package ai.verta.modeldb.dto;

import ai.verta.modeldb.ExperimentRun;
import java.util.List;

public class ExperimentRunPaginationDTO {

  private List<ExperimentRun> experimentRuns;
  private Long totalRecords;

  public List<ExperimentRun> getExperimentRuns() {
    return experimentRuns;
  }

  public void setExperimentRuns(List<ExperimentRun> experimentRuns) {
    this.experimentRuns = experimentRuns;
  }

  public Long getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(Long totalRecords) {
    this.totalRecords = totalRecords;
  }
}
