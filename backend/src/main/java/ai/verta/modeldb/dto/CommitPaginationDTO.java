package ai.verta.modeldb.dto;

import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.versioning.Commit;

import java.util.List;

public class CommitPaginationDTO {

  private List<CommitEntity> commitEntities;
  private Long totalRecords;

  public List<CommitEntity> getCommitEntities() {
    return commitEntities;
  }

  public void setCommitEntities(List<CommitEntity> commitEntities) {
    this.commitEntities = commitEntities;
  }

  public Long getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(Long totalRecords) {
    this.totalRecords = totalRecords;
  }
}
