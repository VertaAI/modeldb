package ai.verta.modeldb.entities.lineage;

import ai.verta.modeldb.lineage.ExperimentRunElement;
import ai.verta.modeldb.lineage.LineageElement;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "lineage_experiment_run")
public class ExperimentRunEntity {
  // Auto increment
  @Id
  @Column(name = "id")
  private Long id;

  @Column(name = "experiment_run_id")
  private String experimentRunId;

  public ExperimentRunEntity(String experimentRunId) {
    this.experimentRunId = experimentRunId;
  }

  public Long getId() {
    return id;
  }

  public String getExperimentRunId() {
    return experimentRunId;
  }

  public LineageElement getElement() {
    return new ExperimentRunElement(experimentRunId);
  }
}
