package ai.verta.modeldb.entities.lineage;

import ai.verta.modeldb.lineage.ExperimentRunEntryContainer;
import ai.verta.modeldb.lineage.LineageEntryContainer;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "lineage_experiment_run")
public class LineageExperimentRunEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "experiment_run_id")
  private String experimentRunId;

  public LineageExperimentRunEntity() {}

  public LineageExperimentRunEntity(String experimentRunId) {
    this.experimentRunId = experimentRunId;
  }

  public Long getId() {
    return id;
  }

  public String getExperimentRunId() {
    return experimentRunId;
  }

  public LineageEntryContainer getEntry() {
    return new ExperimentRunEntryContainer(experimentRunId);
  }
}
