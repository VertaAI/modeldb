package ai.verta.modeldb.lineage;

import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.entities.lineage.ConnectionEntity;
import java.util.Objects;

public class VersioningBlobElement extends LineageElement {

  private final String location;
  private final String commitSha;
  private final Long repositoryId;

  public VersioningBlobElement(Long repositoryId, String commitSha, String location) {
    this.repositoryId = repositoryId;
    this.commitSha = commitSha;
    this.location = location;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VersioningBlobElement that = (VersioningBlobElement) o;
    return Objects.equals(location, that.location) &&
        Objects.equals(commitSha, that.commitSha) &&
        Objects.equals(repositoryId, that.repositoryId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, commitSha, repositoryId);
  }

  @Override
  String getInputExperimentId(ConnectionEntity value) {
    return null;
  }

  @Override
  VersioningLineageEntry getInputBlob(ConnectionEntity value) {
    return null;
  }
}
