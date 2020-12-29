package ai.verta.modeldb.dto;

import ai.verta.modeldb.Project;
import java.util.List;

public class ProjectPaginationDTO {

  private List<Project> projects;
  private Long totalRecords;

  public List<Project> getProjects() {
    return projects;
  }

  public void setProjects(List<Project> projects) {
    this.projects = projects;
  }

  public Long getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(Long totalRecords) {
    this.totalRecords = totalRecords;
  }
}
