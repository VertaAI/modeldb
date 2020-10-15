package ai.verta.modeldb.lineage;

import ai.verta.modeldb.LineageEntry;
import java.util.Objects;

public class ExperimentRunEntryContainer extends LineageEntryContainer {

  private final String experimentRunId;

  public ExperimentRunEntryContainer(String experimentRunId) {
    this.experimentRunId = experimentRunId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExperimentRunEntryContainer that = (ExperimentRunEntryContainer) o;
    return Objects.equals(experimentRunId, that.experimentRunId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(experimentRunId);
  }

  @Override
  public LineageEntry toProto() {
    return LineageEntry.newBuilder().setExternalId(experimentRunId).build();
  }
}
