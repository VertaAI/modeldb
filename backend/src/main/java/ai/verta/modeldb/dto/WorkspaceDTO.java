package ai.verta.modeldb.dto;

import ai.verta.modeldb.WorkspaceTypeEnum.WorkspaceType;

public class WorkspaceDTO {
  private String workspaceName;
  private String workspaceId;
  private WorkspaceType workspaceType;

  public String getWorkspaceName() {
    return workspaceName;
  }

  public void setWorkspaceName(String workspaceName) {
    this.workspaceName = workspaceName;
  }

  public String getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(String workspaceId) {
    this.workspaceId = workspaceId;
  }

  public WorkspaceType getWorkspaceType() {
    return workspaceType;
  }

  public void setWorkspaceType(WorkspaceType workspaceType) {
    this.workspaceType = workspaceType;
  }
}
