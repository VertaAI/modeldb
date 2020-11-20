package ai.verta.modeldb.project;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.FindProjects;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.collaborator.CollaboratorBase;
import ai.verta.modeldb.dto.ProjectPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;

public interface ProjectDAO {

  /**
   * Insert Project entity in database.
   *
   * @param Project project
   * @param UserInfo userInfo
   * @return void
   */
  Project insertProject(Project project, UserInfo userInfo) throws InvalidProtocolBufferException;

  /**
   * @param projectId : project.id
   * @param projectName : updated project name from client request
   * @return {@link Project} : updated project
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Project updateProjectName(String projectId, String projectName)
      throws InvalidProtocolBufferException;

  /**
   * @param projectId : project.id
   * @param projectDescription : updated project description from client request
   * @return {@link Project} : updated project
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Project updateProjectDescription(String projectId, String projectDescription)
      throws InvalidProtocolBufferException;

  /**
   * @param projectId : project.id
   * @param projectReadme : updated project readme text from client request
   * @return {@link Project} : updated project
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Project updateProjectReadme(String projectId, String projectReadme)
      throws InvalidProtocolBufferException;

  /**
   * @param projectId : project.id
   * @param updatedCodeVersion : updated project codeVersion snapshot from client request
   * @return {@link Project} : updated project
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Project logProjectCodeVersion(String projectId, CodeVersion updatedCodeVersion)
      throws InvalidProtocolBufferException;

  /**
   * Update Project Attributes in database using projectId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param String projectId, KeyValue attribute
   * @return Project updated Project entity
   */
  Project updateProjectAttributes(String projectId, KeyValue attribute)
      throws InvalidProtocolBufferException;

  /**
   * Fetch Project Attributes from database using projectId.
   *
   * @param getAll flag
   * @param attributeKeyList
   * @param String projectId
   * @return List<KeyValue> projectAttributes.
   * @throws InvalidProtocolBufferException
   */
  List<KeyValue> getProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException;

  /**
   * Delete the Projects in database using projectIds.
   *
   * <p>TODO : Add logic of Deleting Experiment & ExperimentRun associated with project.
   *
   * @param projectIds : list of Project.id
   * @return {@link Boolean} : Boolean updated status
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Boolean deleteProjects(List<String> projectIds) throws InvalidProtocolBufferException;

  /**
   * Fetch All the Project from database bases on user details.
   *
   * @param userInfo : userInfo
   * @param pageNumber : page number use for pagination.
   * @param pageLimit : page limit is per page record count.
   * @param order : this parameter has order like asc OR desc.
   * @param sortKey : Use this field for filter data.
   * @param projectVisibility : ProjectVisibility.PUBLIC, ProjectVisibility.PRIVATE
   * @return {@link ProjectPaginationDTO} : projectPaginationDTO contains the projectList &
   *     total_pages count
   * @throws InvalidProtocolBufferException : InvalidProtocolBufferException
   */
  ProjectPaginationDTO getProjects(
      UserInfo userInfo,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey,
      ProjectVisibility projectVisibility)
      throws InvalidProtocolBufferException;

  /**
   * Update Project Tags in database using projectId.
   *
   * @param String projectId, List<String> tagsList
   * @return Project updated Project entity
   * @throws InvalidProtocolBufferException
   */
  Project addProjectTags(String projectId, List<String> tagsList)
      throws InvalidProtocolBufferException;

  /**
   * Delete Project Tags in database using projectId.
   *
   * @param projectTagList
   * @param deleteAll
   * @param String projectId
   * @return Project project
   * @throws InvalidProtocolBufferException
   */
  Project deleteProjectTags(String projectId, List<String> projectTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Fetch the Projects based on key and value from database.
   *
   * @param key : key like ModelDBConstants.ID,ModelDBConstants.NAME etc.
   * @param value : value is project.Id, project.name etc.
   * @param userInfo : current login user
   * @return {@link Project} : project based on search return project entity.
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  List<Project> getProjects(String key, String value, UserInfo userInfo)
      throws InvalidProtocolBufferException;

  /**
   * Add attributes in database using projectId.
   *
   * @param String projectId
   * @param List<KeyValue> attributesList
   * @return Project --> updated Project entity
   */
  Project addProjectAttributes(String projectId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException;

  /**
   * Delete Project Attributes in database using projectId.
   *
   * @param deleteAll
   * @param attributeKeyList
   * @param String projectId
   * @return Project project
   * @throws InvalidProtocolBufferException
   */
  Project deleteProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Fetch Project Tags from database using projectId.
   *
   * @param String projectId
   * @return List<String> projectTags.
   * @throws InvalidProtocolBufferException
   */
  List<String> getProjectTags(String projectId) throws InvalidProtocolBufferException;

  /**
   * Deep copy project in database. In current scope we deep copy associated Experiments and
   * ExperimentRuns
   *
   * @param Project project
   * @return Project
   */
  Project deepCopyProjectForUser(String srcProjectID, UserInfo userInfo)
      throws InvalidProtocolBufferException, ModelDBException;

  /**
   * Fetch the Projects corresponding to the id
   *
   * @param id
   * @return Project
   */
  Project getProjectByID(String id) throws InvalidProtocolBufferException;

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
   * @throws InvalidProtocolBufferException
   */
  Project setProjectShortName(String projectId, String projectShortName, UserInfo userInfo)
      throws InvalidProtocolBufferException;

  /**
   * Return public projects
   *
   * @param hostUserInfo
   * @param currentLoginUserInfo
   * @return List<Project> public project list
   * @throws InvalidProtocolBufferException
   */
  List<Project> getPublicProjects(
      UserInfo hostUserInfo, UserInfo currentLoginUserInfo, String workspaceName)
      throws InvalidProtocolBufferException;

  /**
   * Set/Update project visibility
   *
   * @param projectId
   * @param projectVisibility
   * @return updated project
   * @throws InvalidProtocolBufferException
   */
  Project setProjectVisibility(String projectId, ProjectVisibility projectVisibility)
      throws InvalidProtocolBufferException;

  /**
   * @param projectIds : list of project ids
   * @return : project list
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  List<Project> getProjectsByBatchIds(List<String> projectIds)
      throws InvalidProtocolBufferException;

  /**
   * Return list of projects based on FindProjects queryParameters
   *
   * @param queryParameters : queryParameters --> query parameters for filtering projects
   * @param host : host userInfo based on shared URL (ex: xyz.abcPlatform.ai/xyz_user_email_id)
   * @param currentLoginUserInfo : Current login userInfo
   * @param projectVisibility : projectVisibility.PUBLIC, projectVisibility.PRIVATE
   * @return {@link ProjectPaginationDTO} : projectPaginationDTO contains the list of projects based
   *     on filter queryParameters & total_pages count
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  ProjectPaginationDTO findProjects(
      FindProjects queryParameters,
      CollaboratorBase host,
      UserInfo currentLoginUserInfo,
      ProjectVisibility projectVisibility)
      throws InvalidProtocolBufferException;

  /**
   * Project has artifacts field. Add new Artifact in that artifacts List.
   *
   * @param projectId : project.id
   * @param artifacts : project.artifacts
   * @return {@link Project} : updated project
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Project logArtifacts(String projectId, List<Artifact> artifacts)
      throws InvalidProtocolBufferException;

  /**
   * Return List<Artifact> artifacts from Project entity.
   *
   * @param projectId : project.id
   * @return {@link List<Artifact>} : artifact list from project
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  List<Artifact> getProjectArtifacts(String projectId) throws InvalidProtocolBufferException;

  /**
   * Deletes the artifact key associated with the experiment run
   *
   * @param projectId : project.id
   * @param artifactKey : artifact.key
   * @return {@link Project} : updated project
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Project deleteArtifacts(String projectId, String artifactKey)
      throws InvalidProtocolBufferException;

  /**
   * Getting all the owners with respected to project ids and returned by this method.
   *
   * @param projectIds : List<String> list of accessible Project Id
   * @return {@link Map} : Map<String,String> where key= projectId, value= project owner Id
   */
  Map<String, String> getOwnersByProjectIds(List<String> projectIds);

  /**
   * Sets the workspace of an existing project
   *
   * @param projectId : projectId
   * @param workspaceDTO : workspaceDTO
   * @return {@link Project} : updated project
   * @throws InvalidProtocolBufferException : InvalidProtocolBufferException
   */
  Project setProjectWorkspace(String projectId, WorkspaceDTO workspaceDTO)
      throws InvalidProtocolBufferException;

  public List<String> getWorkspaceProjectIDs(String workspaceName, UserInfo currentLoginUserInfo)
      throws InvalidProtocolBufferException;
  /**
   * Checks if project with the id exists with delete flag false
   *
   * @param projectId
   * @return
   */
  boolean projectExistsInDB(String projectId);
}
