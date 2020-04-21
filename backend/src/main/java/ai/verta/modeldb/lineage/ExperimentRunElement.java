package ai.verta.modeldb.lineage;

import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.entities.lineage.ConnectionEntity;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;

public class ExperimentRunElement extends LineageElement {

  private final String experimentRunId;

  public ExperimentRunElement(String experimentRunId) {
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
    ExperimentRunElement that = (ExperimentRunElement) o;
    return Objects.equals(experimentRunId, that.experimentRunId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(experimentRunId);
  }

  @Override
  public LineageEntry toProto() {
    return LineageEntry.newBuilder().setExperimentRun(experimentRunId).build();
  }
}
