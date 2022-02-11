package ai.verta.modeldb.project;

import ai.verta.common.Artifact;
import ai.verta.common.CodeVersion;
import ai.verta.common.KeyValue;
import ai.verta.modeldb.CreateProject;
import ai.verta.modeldb.FindProjects;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.dto.ProjectPaginationDTO;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.UserInfo;
import java.util.List;

public interface ProjectDAO {

  /**
   * Insert Project entity in database.
   *
   * @param createProjectRequest : create project request
   * @param userInfo             : current login userInfo
   * @return Project
   */
  Project insertProject(CreateProject createProjectRequest, UserInfo userInfo);

  /**
   * @param projectId          : project.id
   * @param projectDescription : updated project description from client request
   * @return {@link Project} : updated project
   */
  Project updateProjectDescription(String projectId, String projectDescription);

  /**
   * @param projectId     : project.id
   * @param projectReadme : updated project readme text from client request
   * @return {@link Project} : updated project
   */
  Project updateProjectReadme(String projectId, String projectReadme);

  /**
   * @param projectId          : project.id
   * @param updatedCodeVersion : updated project codeVersion snapshot from client request
   * @return {@link Project} : updated project
   */
  Project logProjectCodeVersion(String projectId, CodeVersion updatedCodeVersion);

  /**
   * Update Project Attributes in database using projectId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param String projectId, KeyValue attribute
   * @return Project updated Project entity
   */
  Project updateProjectAttributes(String projectId, KeyValue attribute);

  /**
   * Fetch Project Attributes from database using projectId.
   *
   * @param getAll           flag
   * @param attributeKeyList
   * @param String           projectId
   * @return List<KeyValue> projectAttributes.
   */
  List<KeyValue> getProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean getAll);

  /**
   * Delete the Projects in database using projectIds.
   *
   * <p>TODO : Add logic of Deleting Experiment & ExperimentRun associated with project.
   *
   * @param projectIds : list of Project.id
   * @return {@link List} : deleted projectIds
   */
  List<String> deleteProjects(List<String> projectIds);

  /**
   * Fetch All the Project from database bases on user details.
   *
   * @param userInfo          : userInfo
   * @param pageNumber        : page number use for pagination.
   * @param pageLimit         : page limit is per page record count.
   * @param order             : this parameter has order like asc OR desc.
   * @param sortKey           : Use this field for filter data.
   * @param projectVisibility : ProjectVisibility.PUBLIC, ProjectVisibility.PRIVATE
   * @return {@link ProjectPaginationDTO} : projectPaginationDTO contains the projectList &
   * total_pages count
   */
  ProjectPaginationDTO getProjects(
      UserInfo userInfo,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey,
      ResourceVisibility projectVisibility);

  /**
   * Update Project Tags in database using projectId.
   *
   * @param String projectId, List<String> tagsList
   * @return Project updated Project entity
   */
  Project addProjectTags(String projectId, List<String> tagsList);

  /**
   * Delete Project Tags in database using projectId.
   *
   * @param projectTagList
   * @param deleteAll
   * @param String         projectId
   * @return Project project
   */
  Project deleteProjectTags(String projectId, List<String> projectTagList, Boolean deleteAll);

  /**
   * Fetch the Projects based on key and value from database.
   *
   * @param key      : key like ModelDBConstants.ID,ModelDBConstants.NAME etc.
   * @param value    : value is project.Id, project.name etc.
   * @param userInfo : current login user
   * @return {@link Project} : project based on search return project entity.
   */
  List<Project> getProjects(String key, String value, UserInfo userInfo);

  /**
   * Add attributes in database using projectId.
   *
   * @param String         projectId
   * @param List<KeyValue> attributesList
   * @return Project --> updated Project entity
   */
  Project addProjectAttributes(String projectId, List<KeyValue> attributesList);

  /**
   * Delete Project Attributes in database using projectId.
   *
   * @param deleteAll
   * @param attributeKeyList
   * @param String           projectId
   * @return Project project
   */
  Project deleteProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean deleteAll);

  /**
   * Fetch Project Tags from database using projectId.
   *
   * @param String projectId
   * @return List<String> projectTags.
   */
  List<String> getProjectTags(String projectId);

  /**
   * Deep copy project in database. In current scope we deep copy associated Experiments and
   * ExperimentRuns
   *
   * @param Project project
   * @return Project
   */
  Project deepCopyProjectForUser(String srcProjectID, UserInfo userInfo) throws ModelDBException;

  /**
   * Fetch the Projects corresponding to the id
   *
   * @param id
   * @return Project
   */
  Project getProjectByID(String id);

  /**
   * Fetch count of experiment of project
   *
   * @param projectIds : projectIds
   * @return count of Experiment entity
   */
  Long getExperimentCount(List<String> projectIds);

  /**
   * Fetch count of experimentRun of project
   *
   * @param projectIds : projectIds
   * @return count of ExperimentRun entity
   */
  Long getExperimentRunCount(List<String> projectIds);

  /**
   * Set project short name
   *
   * @param projectId
   * @param projectShortName
   * @param userInfo
   * @return updated project
   */
  Project setProjectShortName(String projectId, String projectShortName, UserInfo userInfo)
      throws ModelDBException;

  /**
   * Return public projects
   *
   * @param hostUserInfo
   * @param currentLoginUserInfo
   * @return List<Project> public project list
   */
  List<Project> getPublicProjects(
      UserInfo hostUserInfo, UserInfo currentLoginUserInfo, String workspaceName);

  /**
   * Return list of projects based on FindProjects queryParameters
   *
   * @param queryParameters      : queryParameters --> query parameters for filtering projects
   * @param host                 : host userInfo based on shared URL (ex: xyz.abcPlatform.ai/xyz_user_email_id)
   * @param currentLoginUserInfo : Current login userInfo
   * @param projectVisibility    : projectVisibility.PUBLIC, projectVisibility.PRIVATE
   * @return {@link ProjectPaginationDTO} : projectPaginationDTO contains the list of projects based
   * on filter queryParameters & total_pages count
   */
  ProjectPaginationDTO findProjects(
      FindProjects queryParameters,
      CollaboratorBase host,
      UserInfo currentLoginUserInfo,
      ResourceVisibility projectVisibility);

  /**
   * Project has artifacts field. Add new Artifact in that artifacts List.
   *
   * @param projectId : project.id
   * @param artifacts : project.artifacts
   * @return {@link Project} : updated project
   */
  Project logArtifacts(String projectId, List<Artifact> artifacts);

  /**
   * Return List<Artifact> artifacts from Project entity.
   *
   * @param projectId : project.id
   * @return {@link List<Artifact>} : artifact list from project
   */
  List<Artifact> getProjectArtifacts(String projectId);

  /**
   * Deletes the artifact key associated with the experiment run
   *
   * @param projectId   : project.id
   * @param artifactKey : artifact.key
   * @return {@link Project} : updated project
   */
  Project deleteArtifacts(String projectId, String artifactKey);

  List<String> getWorkspaceProjectIDs(String workspaceName, UserInfo currentLoginUserInfo);

  /**
   * Checks if project with the id exists with delete flag false
   *
   * @param projectId
   * @return
   */
  boolean projectExistsInDB(String projectId);
}
