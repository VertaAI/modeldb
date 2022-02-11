package ai.verta.modeldb.project;

import ai.verta.common.*;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.handlers.TagsHandlerBase;
import ai.verta.modeldb.dto.ProjectPaginationDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.reconcilers.ReconcilerInitializer;
import ai.verta.modeldb.telemetry.TelemetryUtils;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.criteria.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.query.Query;

public class ProjectDAORdbImpl implements ProjectDAO {

  private static final Logger LOGGER = LogManager.getLogger(ProjectDAORdbImpl.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private ExperimentDAO experimentDAO;
  private ExperimentRunDAO experimentRunDAO;
  private String starterProjectID;
  private final AuthService authService;
  private final MDBRoleService mdbRoleService;

  private static final String GET_PROJECT_ATTR_BY_KEYS_HQL =
      "From AttributeEntity kv where kv.key in (:keys) AND kv.projectEntity.id = :projectId AND kv.field_type = :fieldType";
  private static final String DELETE_ALL_PROJECT_TAGS_HQL =
      "delete from TagsMapping tm WHERE tm.projectEntity.id = :projectId";
  private static final String DELETE_SELECTED_PROJECT_TAGS_HQL =
      "delete from TagsMapping tm WHERE tm.tags in (:tags) AND tm.projectEntity.id = :projectId";
  private static final String DELETE_ALL_PROJECT_ATTRIBUTES_HQL =
      "delete from AttributeEntity attr WHERE attr.projectEntity.id = :projectId";
  private static final String DELETE_SELECTED_PROJECT_ATTRIBUTES_HQL =
      "delete from AttributeEntity attr WHERE attr.key in (:keys) AND attr.projectEntity.id = :projectId";
  private static final String GET_PROJECT_EXPERIMENTS_COUNT_HQL =
      "SELECT COUNT(*) FROM ExperimentEntity ee WHERE ee.project_id IN (:project_ids)";
  private static final String GET_PROJECT_EXPERIMENT_RUNS_COUNT_HQL =
      "SELECT COUNT(*) FROM ExperimentRunEntity ere WHERE ere.project_id IN (:project_ids)";
  private static final String GET_PROJECT_BY_ID_HQL =
      "From ProjectEntity p where p.id = :id AND p.deleted = false AND p.created = true";
  private static final String COUNT_PROJECT_BY_ID_HQL = "Select Count(id) " + GET_PROJECT_BY_ID_HQL;
  private static final String NON_DELETED_PROJECT_IDS =
      "select p.id  From ProjectEntity p where p.deleted = false AND p.created = true";
  private static final String NON_DELETED_PROJECT_IDS_BY_IDS =
      NON_DELETED_PROJECT_IDS + " AND p.id in (:project_ids)";
  private static final String GET_PROJECT_BY_SHORT_NAME_HQL =
      "From ProjectEntity p where p.deleted = false AND p.short_name = :projectShortName AND p.id IN (:projectIds)";
  private static final String DELETE_ALL_ARTIFACTS_HQL =
      "delete from ArtifactEntity ar WHERE ar.projectEntity.id = :projectId";
  private static final String DELETE_SELECTED_ARTIFACT_BY_KEYS_HQL =
      "delete from ArtifactEntity ar WHERE ar.key in (:keys) AND ar.projectEntity.id = :projectId";
  private static final String DELETED_STATUS_PROJECT_QUERY_STRING =
      "UPDATE ProjectEntity pr SET pr.deleted = :deleted WHERE pr.id IN (:projectIds)";
  private static final String GET_DELETED_PROJECTS_IDS_BY_NAME_HQL =
      "SELECT p.id From ProjectEntity p where p.name = :projectName AND p.deleted = true";

  public ProjectDAORdbImpl(
      AuthService authService,
      MDBRoleService mdbRoleService,
      ExperimentDAO experimentDAO,
      ExperimentRunDAO experimentRunDAO) {
    this.authService = authService;
    this.mdbRoleService = mdbRoleService;
    this.experimentDAO = experimentDAO;
    this.experimentRunDAO = experimentRunDAO;
    var app = App.getInstance();
    this.starterProjectID = app.mdbConfig.starterProject;
  }

  /**
   * Method to convert createProject request to Project object. This method generates the project Id
   * using UUID and puts it in Project object.
   *
   * @param request  : CreateProject
   * @param userInfo : UserInfo
   * @return Project
   */
  private Project getProjectFromRequest(CreateProject request, UserInfo userInfo) {

    if (request.getName().isEmpty()) {
      request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
    }

    String projectShortName = ModelDBUtils.convertToProjectShortName(request.getName());

    /*
     * Create Project entity from given CreateProject request. generate UUID and put as id in
     * project for uniqueness. set above created List<KeyValue> attributes in project entity.
     */
    var projectBuilder =
        Project.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setShortName(projectShortName)
            .setDescription(request.getDescription())
            .addAllAttributes(request.getAttributesList())
            .addAllTags(TagsHandlerBase.checkEntityTagsLength(request.getTagsList()))
            .setProjectVisibility(request.getProjectVisibility())
            .setVisibility(request.getVisibility())
            .addAllArtifacts(request.getArtifactsList())
            .setReadmeText(request.getReadmeText())
            .setCustomPermission(request.getCustomPermission())
            .setVersionNumber(1L);

    if (request.getDateCreated() != 0L) {
      projectBuilder
          .setDateCreated(request.getDateCreated())
          .setDateUpdated(request.getDateCreated());
    } else {
      projectBuilder
          .setDateCreated(Calendar.getInstance().getTimeInMillis())
          .setDateUpdated(Calendar.getInstance().getTimeInMillis());
    }

    return projectBuilder.build();
  }

  @Override
  public Project insertProject(CreateProject createProjectRequest, UserInfo userInfo) {
    var project = getProjectFromRequest(createProjectRequest, userInfo);
    return insertProject(project, createProjectRequest.getWorkspaceName(), userInfo);
  }

  private Project insertProject(Project project, String workspaceName, UserInfo userInfo) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var workspace = mdbRoleService.getWorkspaceByWorkspaceName(userInfo, workspaceName);

      var deletedEntitiesQuery = session.createQuery(GET_DELETED_PROJECTS_IDS_BY_NAME_HQL);
      deletedEntitiesQuery.setParameter("projectName", project.getName());
      List<String> deletedEntityIds = deletedEntitiesQuery.list();
      if (!deletedEntityIds.isEmpty()) {
        mdbRoleService.deleteEntityResourcesWithServiceUser(
            deletedEntityIds, ModelDBServiceResourceTypes.PROJECT);
      }

      var transaction = session.beginTransaction();
      var projectEntity = RdbmsUtils.generateProjectEntity(project);
      session.save(projectEntity);
      transaction.commit();
      LOGGER.debug("ProjectEntity created successfully");

      var resourceVisibility = project.getVisibility();
      if (project.getVisibility().equals(ResourceVisibility.UNKNOWN)) {
        resourceVisibility =
            ModelDBUtils.getResourceVisibility(
                Optional.of(workspace), project.getProjectVisibility());
      }

      mdbRoleService.createWorkspacePermissions(
          Optional.of(workspace.getId()),
          Optional.empty(),
          project.getId(),
          project.getName(),
          Optional.empty(),
          ModelDBServiceResourceTypes.PROJECT,
          project.getCustomPermission(),
          resourceVisibility,
          false);
      LOGGER.debug("Project role bindings created successfully");
      transaction = session.beginTransaction();
      projectEntity.setCreated(true);
      projectEntity.setVisibility_migration(true);
      transaction.commit();
      LOGGER.debug("Project created successfully");
      TelemetryUtils.insertModelDBDeploymentInfo();
      return projectEntity.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return insertProject(project, workspaceName, userInfo);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project updateProjectDescription(String projectId, String projectDescription) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var projectEntity = session.load(ProjectEntity.class, projectId, LockMode.PESSIMISTIC_WRITE);
      projectEntity.setDescription(projectDescription);
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      projectEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug("Project description updated successfully");
      return projectEntity.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateProjectDescription(projectId, projectDescription);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project updateProjectReadme(String projectId, String projectReadme) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var projectEntity = session.load(ProjectEntity.class, projectId, LockMode.PESSIMISTIC_WRITE);
      projectEntity.setReadme_text(projectReadme);
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      projectEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug("Project readme updated successfully");
      return projectEntity.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateProjectReadme(projectId, projectReadme);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project logProjectCodeVersion(String projectId, CodeVersion updatedCodeVersion) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var projectEntity = session.get(ProjectEntity.class, projectId, LockMode.PESSIMISTIC_WRITE);

      var existingCodeVersionEntity = projectEntity.getCode_version_snapshot();
      if (existingCodeVersionEntity == null) {
        projectEntity.setCode_version_snapshot(
            RdbmsUtils.generateCodeVersionEntity(
                ModelDBConstants.CODE_VERSION, updatedCodeVersion));
      } else {
        existingCodeVersionEntity.setDate_logged(updatedCodeVersion.getDateLogged());
        if (updatedCodeVersion.hasGitSnapshot()) {
          existingCodeVersionEntity.setGit_snapshot(
              RdbmsUtils.generateGitSnapshotEntity(
                  ModelDBConstants.GIT_SNAPSHOT, updatedCodeVersion.getGitSnapshot()));
          existingCodeVersionEntity.setCode_archive(null);
        } else if (updatedCodeVersion.hasCodeArchive()) {
          existingCodeVersionEntity.setCode_archive(
              RdbmsUtils.generateArtifactEntity(
                  projectEntity,
                  ModelDBConstants.CODE_ARCHIVE,
                  updatedCodeVersion.getCodeArchive()));
          existingCodeVersionEntity.setGit_snapshot(null);
        }
      }
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      projectEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug("Project code version snapshot updated successfully");
      return projectEntity.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return logProjectCodeVersion(projectId, updatedCodeVersion);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project updateProjectAttributes(String projectId, KeyValue attribute) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectObj =
          session.get(ProjectEntity.class, projectId, LockMode.PESSIMISTIC_WRITE);
      if (projectObj == null) {
        var errorMessage = "Project not found for given ID";
        throw new NotFoundException(errorMessage);
      }

      var updatedAttributeObj =
          RdbmsUtils.generateAttributeEntity(projectObj, ModelDBConstants.ATTRIBUTES, attribute);

      List<AttributeEntity> existingAttributes = projectObj.getAttributeMapping();
      if (!existingAttributes.isEmpty()) {
        var doesExist = false;
        for (AttributeEntity existingAttribute : existingAttributes) {
          if (existingAttribute.getKey().equals(attribute.getKey())) {
            existingAttribute.setKey(updatedAttributeObj.getKey());
            existingAttribute.setValue(updatedAttributeObj.getValue());
            existingAttribute.setValue_type(updatedAttributeObj.getValue_type());
            doesExist = true;
            break;
          }
        }
        if (!doesExist) {
          projectObj.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
        }
      } else {
        projectObj.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
      }
      projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      projectObj.increaseVersionNumber();
      var transaction = session.beginTransaction();
      session.saveOrUpdate(projectObj);
      transaction.commit();
      return projectObj.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateProjectAttributes(projectId, attribute);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<KeyValue> getProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean getAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      if (getAll) {
        ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
        if (projectObj == null) {
          String errorMessage = ModelDBMessages.PROJECT_NOT_FOUND_FOR_GIVEN_ID_ERROR + projectId;
          throw new NotFoundException(errorMessage);
        }
        return projectObj
            .getProtoObject(mdbRoleService, authService, new HashMap<>(), new HashMap<>())
            .getAttributesList();
      } else {
        var query = session.createQuery(GET_PROJECT_ATTR_BY_KEYS_HQL);
        query.setParameterList("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.setParameter("fieldType", ModelDBConstants.ATTRIBUTES);

        @SuppressWarnings("unchecked")
        List<AttributeEntity> attributeEntities = query.list();
        return RdbmsUtils.convertAttributeEntityListFromAttributes(attributeEntities);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getProjectAttributes(projectId, attributeKeyList, getAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project addProjectTags(String projectId, List<String> tagsList) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectObj =
          session.get(ProjectEntity.class, projectId, LockMode.PESSIMISTIC_WRITE);
      if (projectObj == null) {
        var errorMessage = "Project not found for given ID";
        throw new NotFoundException(errorMessage);
      }
      List<String> newTags = new ArrayList<>();
      Map<Long, Workspace> cacheWorkspaceMap = new HashMap<>();
      Map<String, GetResourcesResponseItem> getResourcesMap = new HashMap<>();
      var existingProtoProjectObj =
          projectObj.getProtoObject(
              mdbRoleService, authService, cacheWorkspaceMap, getResourcesMap);
      for (String tag : tagsList) {
        if (!existingProtoProjectObj.getTagsList().contains(tag)) {
          newTags.add(tag);
        }
      }
      if (!newTags.isEmpty()) {
        List<TagsMapping> newTagMappings =
            RdbmsUtils.convertTagListFromTagMappingList(projectObj, newTags);
        projectObj.getTags().addAll(newTagMappings);
        projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
        projectObj.increaseVersionNumber();
        var transaction = session.beginTransaction();
        session.saveOrUpdate(projectObj);
        transaction.commit();
      }
      LOGGER.debug("Project tags added successfully");
      return projectObj.getProtoObject(
          mdbRoleService, authService, cacheWorkspaceMap, getResourcesMap);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addProjectTags(projectId, tagsList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project deleteProjectTags(
      String projectId, List<String> projectTagList, Boolean deleteAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();

      if (deleteAll) {
        var query =
            session
                .createQuery(DELETE_ALL_PROJECT_TAGS_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      } else {
        var query =
            session
                .createQuery(DELETE_SELECTED_PROJECT_TAGS_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("tags", projectTagList);
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      }

      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      projectObj.increaseVersionNumber();
      session.update(projectObj);
      transaction.commit();
      LOGGER.debug("Project tags deleted successfully");
      return projectObj.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteProjectTags(projectId, projectTagList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ProjectPaginationDTO getProjects(
      UserInfo userInfo,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey,
      ResourceVisibility projectVisibility) {

    var findProjects =
        FindProjects.newBuilder()
            .setPageNumber(pageNumber)
            .setPageLimit(pageLimit)
            .setAscending(order)
            .setSortKey(sortKey)
            .build();
    return findProjects(findProjects, null, userInfo, projectVisibility);
  }

  @Override
  public List<Project> getProjects(String key, String value, UserInfo userInfo) {
    var findProjects =
        FindProjects.newBuilder()
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setKey(key)
                    .setValue(Value.newBuilder().setStringValue(value).build())
                    .setOperator(OperatorEnum.Operator.EQ)
                    .setValueType(ValueTypeEnum.ValueType.STRING)
                    .build())
            .build();
    var projectPaginationDTO =
        findProjects(findProjects, null, userInfo, ResourceVisibility.PRIVATE);
    LOGGER.debug("Projects size is {}", projectPaginationDTO.getProjects().size());
    return projectPaginationDTO.getProjects();
  }

  @Override
  public Project addProjectAttributes(String projectId, List<KeyValue> attributesList) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectObj =
          session.get(ProjectEntity.class, projectId, LockMode.PESSIMISTIC_WRITE);
      projectObj.setAttributeMapping(
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              projectObj, ModelDBConstants.ATTRIBUTES, attributesList));
      projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      projectObj.increaseVersionNumber();
      var transaction = session.beginTransaction();
      session.saveOrUpdate(projectObj);
      transaction.commit();
      LOGGER.debug("Project attributes added successfully");
      return projectObj.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addProjectAttributes(projectId, attributesList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project deleteProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean deleteAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();

      if (deleteAll) {
        var query =
            session
                .createQuery(DELETE_ALL_PROJECT_ATTRIBUTES_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      } else {
        var query =
            session
                .createQuery(DELETE_SELECTED_PROJECT_ATTRIBUTES_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      }
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      projectObj.increaseVersionNumber();
      session.update(projectObj);
      transaction.commit();
      return projectObj.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteProjectAttributes(projectId, attributeKeyList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getProjectTags(String projectId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      return projectObj
          .getProtoObject(mdbRoleService, authService, new HashMap<>(), new HashMap<>())
          .getTagsList();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getProjectTags(projectId);
      } else {
        throw ex;
      }
    }
  }

  /**
   * Copies details of a source project and updates the id, created and updated Timestamp
   *
   * @param srcProject : source project
   * @return updatedProject
   */
  private Project copyProjectAndUpdateDetails(Project srcProject, UserInfo userInfo) {
    var projectBuilder = Project.newBuilder(srcProject).setId(UUID.randomUUID().toString());

    if (userInfo != null) {
      projectBuilder.setOwner(authService.getVertaIdFromUserInfo(userInfo));
    }
    return projectBuilder.build();
  }

  @Override
  public Project deepCopyProjectForUser(String srcProjectID, UserInfo newOwner)
      throws ModelDBException {
    // if no project id specified , default to the one captured from config.yaml
    // TODO: extend the starter project to be set of projects, so parameterizing this function makes
    // sense
    if (srcProjectID == null || srcProjectID.isEmpty()) {
      srcProjectID = starterProjectID;
    }

    // if this is not a starter project, then cloning is not supported
    if (!srcProjectID.equals(starterProjectID)) {
      throw new InvalidArgumentException("Cloning project supported only for starter project");
    }

    // source project
    var srcProject = getProjectByID(srcProjectID);
    if (newOwner == null) {
      throw new InvalidArgumentException("New owner not passed for cloning Project");
    }

    // cloned project
    var newProject = copyProjectAndUpdateDetails(srcProject, newOwner);
    insertProject(newProject, authService.getUsernameFromUserInfo(newOwner), newOwner);
    newProject = getProjectByID(newProject.getId());

    // Deep Copy Experiments
    List<KeyValue> projectLevelFilter = new ArrayList<>();
    var projectIdValue = Value.newBuilder().setStringValue(srcProject.getId()).build();
    projectLevelFilter.add(
        KeyValue.newBuilder().setKey(ModelDBConstants.PROJECT_ID).setValue(projectIdValue).build());
    // get  Experiments from the source Project, copy their clone and associate them to new project
    List<Experiment> experiments = this.experimentDAO.getExperiments(projectLevelFilter);
    for (Experiment srcExperiment : experiments) {
      var newExperiment =
          this.experimentDAO.deepCopyExperimentForUser(srcExperiment, newProject, newOwner);
      var experimentIDValue = Value.newBuilder().setStringValue(srcExperiment.getId()).build();
      List<KeyValue> experimentLevelFilter = new ArrayList<>(projectLevelFilter);
      experimentLevelFilter.add(
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.EXPERIMENT_ID)
              .setValue(experimentIDValue)
              .build());

      List<ExperimentRun> experimentRuns =
          this.experimentRunDAO.getExperimentRuns(experimentLevelFilter);
      for (ExperimentRun srcExperimentRun : experimentRuns) {
        var cloneExperimentRun =
            CloneExperimentRun.newBuilder()
                .setSrcExperimentRunId(srcExperimentRun.getId())
                .setDestExperimentId(newExperiment.getId())
                .build();
        this.experimentRunDAO.cloneExperimentRun(this, cloneExperimentRun, newOwner);
      }
    }

    return newProject;
  }

  @Override
  public List<String> deleteProjects(List<String> projectIds) {
    // Get self allowed resources id where user has delete permission
    List<String> allowedProjectIds =
        mdbRoleService.getAccessibleResourceIdsByActions(
            ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.DELETE, projectIds);
    if (allowedProjectIds.isEmpty()) {
      throw new PermissionDeniedException(
          "Delete Access Denied for given project Ids : " + projectIds);
    }

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      var deletedProjectQuery =
          session
              .createQuery(DELETED_STATUS_PROJECT_QUERY_STRING)
              .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
      deletedProjectQuery.setParameter("deleted", true);
      deletedProjectQuery.setParameter("projectIds", allowedProjectIds);
      int updatedCount = deletedProjectQuery.executeUpdate();
      LOGGER.debug("Mark Projects as deleted : {}, count : {}", allowedProjectIds, updatedCount);
      transaction.commit();
      LOGGER.debug("Project deleted successfully");
      allowedProjectIds.forEach(ReconcilerInitializer.softDeleteProjects::insert);
      return allowedProjectIds;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteProjects(projectIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Long getExperimentCount(List<String> projectIds) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Query<?> query = session.createQuery(GET_PROJECT_EXPERIMENTS_COUNT_HQL);
      query.setParameterList(ModelDBConstants.PROJECT_IDS, projectIds);
      Long count = (Long) query.uniqueResult();
      LOGGER.debug("Experiment Count : {}", count);
      return count;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentCount(projectIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Long getExperimentRunCount(List<String> projectIds) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Query<?> query = session.createQuery(GET_PROJECT_EXPERIMENT_RUNS_COUNT_HQL);
      query.setParameterList(ModelDBConstants.PROJECT_IDS, projectIds);
      Long count = (Long) query.uniqueResult();
      LOGGER.debug("ExperimentRun Count : {}", count);
      return count;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRunCount(projectIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project getProjectByID(String id) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", id);
      var projectEntity = (ProjectEntity) query.uniqueResult();
      if (projectEntity == null) {
        String errorMessage = ModelDBMessages.PROJECT_NOT_FOUND_FOR_ID;
        throw new NotFoundException(errorMessage);
      }
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);

      return projectEntity.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getProjectByID(id);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project setProjectShortName(String projectId, String projectShortName, UserInfo userInfo)
      throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      List<String> accessibleProjectIds =
          mdbRoleService.getSelfDirectlyAllowedResources(
              ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.READ);

      var query = session.createQuery(GET_PROJECT_BY_SHORT_NAME_HQL);
      query.setParameter("projectShortName", projectShortName);
      query.setParameterList("projectIds", accessibleProjectIds);
      List<ProjectEntity> projectEntities = query.list();
      if (!projectEntities.isEmpty()) {
        throw new AlreadyExistsException("Project already exist with given short name");
      }

      query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", projectId);
      var projectEntity = (ProjectEntity) query.uniqueResult();
      projectEntity.setShort_name(projectShortName);
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      projectEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      return projectEntity.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setProjectShortName(projectId, projectShortName, userInfo);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Project> getPublicProjects(
      UserInfo hostUserInfo, UserInfo currentLoginUserInfo, String workspaceName) {
    CollaboratorUser hostCollaboratorBase = null;
    if (hostUserInfo != null) {
      hostCollaboratorBase = new CollaboratorUser(authService, hostUserInfo);
    }

    var findProjects = FindProjects.newBuilder();
    findProjects.setWorkspaceName(workspaceName);

    var projectPaginationDTO =
        findProjects(
            findProjects.build(),
            hostCollaboratorBase,
            currentLoginUserInfo,
            ResourceVisibility.PRIVATE);
    List<Project> projects = projectPaginationDTO.getProjects();
    if (projects != null) {
      LOGGER.debug("Projects size is {}", projects.size());
      return projects;
    } else {
      LOGGER.debug("Projects size is empty");
      return Collections.emptyList();
    }
  }

  @Override
  public ProjectPaginationDTO findProjects(
      FindProjects queryParameters,
      CollaboratorBase host,
      UserInfo currentLoginUserInfo,
      ResourceVisibility visibility) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {

      var builder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<ProjectEntity> criteriaQuery = builder.createQuery(ProjectEntity.class);
      Root<ProjectEntity> projectRoot = criteriaQuery.from(ProjectEntity.class);
      projectRoot.alias("pr");
      List<Predicate> finalPredicatesList = new ArrayList<>();

      Set<String> accessibleProjectIds = new HashSet<>();
      Map<String, GetResourcesResponseItem> getResourcesMap = new HashMap<>();
      String workspaceName = queryParameters.getWorkspaceName();
      if (!workspaceName.isEmpty()
          && workspaceName.equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
        List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
            mdbRoleService.getResourceItems(
                null,
                !queryParameters.getProjectIdsList().isEmpty()
                    ? new HashSet<>(queryParameters.getProjectIdsList())
                    : Collections.emptySet(),
                ModelDBServiceResourceTypes.PROJECT,
                false);
        accessibleProjectIds =
            accessibleAllWorkspaceItems.stream()
                .peek(
                    responseItem -> getResourcesMap.put(responseItem.getResourceId(), responseItem))
                .map(GetResourcesResponseItem::getResourceId)
                .collect(Collectors.toSet());

        List<String> orgWorkspaceIds =
            mdbRoleService.listMyOrganizations().stream()
                .map(Organization::getWorkspaceId)
                .collect(Collectors.toList());
        for (GetResourcesResponseItem item : accessibleAllWorkspaceItems) {
          if (orgWorkspaceIds.contains(String.valueOf(item.getWorkspaceId()))) {
            accessibleProjectIds.remove(item.getResourceId());
          }
        }
      } else {
        UserInfo userInfo =
            host != null && host.isUser()
                ? (UserInfo) host.getCollaboratorMessage()
                : currentLoginUserInfo;
        if (userInfo != null) {
          var workspace = mdbRoleService.getWorkspaceByWorkspaceName(userInfo, workspaceName);
          List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
              mdbRoleService.getResourceItems(
                  workspace,
                  !queryParameters.getProjectIdsList().isEmpty()
                      ? new HashSet<>(queryParameters.getProjectIdsList())
                      : Collections.emptySet(),
                  ModelDBServiceResourceTypes.PROJECT,
                  false);
          accessibleProjectIds =
              accessibleAllWorkspaceItems.stream()
                  .peek(
                      responseItem ->
                          getResourcesMap.put(responseItem.getResourceId(), responseItem))
                  .map(GetResourcesResponseItem::getResourceId)
                  .collect(Collectors.toSet());
        }
      }

      if (accessibleProjectIds.isEmpty() && mdbRoleService.IsImplemented()) {
        LOGGER.debug("Accessible Project Ids not found, size 0");
        var projectPaginationDTO = new ProjectPaginationDTO();
        projectPaginationDTO.setProjects(Collections.emptyList());
        projectPaginationDTO.setTotalRecords(0L);
        return projectPaginationDTO;
      }

      List<KeyValueQuery> predicates = new ArrayList<>(queryParameters.getPredicatesList());
      for (KeyValueQuery predicate : predicates) {
        // Validate if current user has access to the entity or not where predicate key has an id
        RdbmsUtils.validatePredicates(
            ModelDBConstants.PROJECTS,
            new ArrayList<>(accessibleProjectIds),
            predicate,
            mdbRoleService.IsImplemented());
      }

      if (!accessibleProjectIds.isEmpty()) {
        Expression<String> exp = projectRoot.get(ModelDBConstants.ID);
        var predicate2 = exp.in(accessibleProjectIds);
        finalPredicatesList.add(predicate2);
      }

      var entityName = "projectEntity";
      try {
        List<Predicate> queryPredicatesList =
            RdbmsUtils.getQueryPredicatesFromPredicateList(
                entityName,
                predicates,
                builder,
                criteriaQuery,
                projectRoot,
                authService,
                mdbRoleService,
                ModelDBServiceResourceTypes.PROJECT);
        if (!queryPredicatesList.isEmpty()) {
          finalPredicatesList.addAll(queryPredicatesList);
        }
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          var projectPaginationDTO = new ProjectPaginationDTO();
          projectPaginationDTO.setProjects(Collections.emptyList());
          projectPaginationDTO.setTotalRecords(0L);
          return projectPaginationDTO;
        }
        throw ex;
      }

      finalPredicatesList.add(builder.equal(projectRoot.get(ModelDBConstants.DELETED), false));
      finalPredicatesList.add(builder.equal(projectRoot.get(ModelDBConstants.CREATED), true));

      var orderBy =
          RdbmsUtils.getOrderBasedOnSortKey(
              queryParameters.getSortKey(),
              queryParameters.getAscending(),
              builder,
              projectRoot,
              entityName);

      var predicateArr = new Predicate[finalPredicatesList.size()];
      for (var index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      var predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(projectRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      var query = session.createQuery(criteriaQuery);
      LOGGER.debug("Projects final query : {}", query.getQueryString());
      if (queryParameters.getPageNumber() != 0 && queryParameters.getPageLimit() != 0) {
        // Calculate number of documents to skip
        int skips = queryParameters.getPageLimit() * (queryParameters.getPageNumber() - 1);
        query.setFirstResult(skips);
        query.setMaxResults(queryParameters.getPageLimit());
      }

      List<ProjectEntity> projectEntities = query.list();

      Set<String> projectIdsSet = new HashSet<>();
      List<Project> finalProjects = new ArrayList<>();
      Map<Long, Workspace> cacheWorkspaceMap = new HashMap<>();
      for (ProjectEntity projectEntity : projectEntities) {
        if (!projectIdsSet.contains(projectEntity.getId())) {
          projectIdsSet.add(projectEntity.getId());
          if (queryParameters.getIdsOnly()) {
            finalProjects.add(Project.newBuilder().setId(projectEntity.getId()).build());
          } else {
            finalProjects.add(
                projectEntity.getProtoObject(
                    mdbRoleService, authService, cacheWorkspaceMap, getResourcesMap));
          }
        }
      }

      long totalRecords = RdbmsUtils.count(session, projectRoot, criteriaQuery);

      var projectPaginationDTO = new ProjectPaginationDTO();
      projectPaginationDTO.setProjects(finalProjects);
      projectPaginationDTO.setTotalRecords(totalRecords);
      return projectPaginationDTO;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findProjects(queryParameters, host, currentLoginUserInfo, visibility);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project logArtifacts(String projectId, List<Artifact> newArtifacts) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", projectId);
      var projectEntity = (ProjectEntity) query.uniqueResult();
      if (projectEntity == null) {
        String errorMessage = ModelDBMessages.PROJECT_NOT_FOUND_FOR_GIVEN_ID_ERROR + projectId;
        throw new NotFoundException(errorMessage);
      }

      Map<Long, Workspace> cacheWorkspaceMap = new HashMap<>();
      Map<String, GetResourcesResponseItem> getResourcesMap = new HashMap<>();
      List<Artifact> existingArtifacts =
          projectEntity
              .getProtoObject(mdbRoleService, authService, cacheWorkspaceMap, getResourcesMap)
              .getArtifactsList();
      for (Artifact existingArtifact : existingArtifacts) {
        for (Artifact newArtifact : newArtifacts) {
          if (existingArtifact.getKey().equals(newArtifact.getKey())) {
            throw new AlreadyExistsException(
                "Artifact being logged already exists. existing artifact key : "
                    + newArtifact.getKey());
          }
        }
      }

      projectEntity.setArtifactMapping(
          RdbmsUtils.convertArtifactsFromArtifactEntityList(
              projectEntity, ModelDBConstants.ARTIFACTS, newArtifacts));
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      projectEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      return projectEntity.getProtoObject(
          mdbRoleService, authService, cacheWorkspaceMap, getResourcesMap);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return logArtifacts(projectId, newArtifacts);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Artifact> getProjectArtifacts(String projectId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", projectId);
      var projectEntity = (ProjectEntity) query.uniqueResult();
      if (projectEntity == null) {
        String errorMessage = ModelDBMessages.PROJECT_NOT_FOUND_FOR_GIVEN_ID_ERROR + projectId;
        throw new NotFoundException(errorMessage);
      }
      var project =
          projectEntity.getProtoObject(
              mdbRoleService, authService, new HashMap<>(), new HashMap<>());
      if (project.getArtifactsList() != null && !project.getArtifactsList().isEmpty()) {
        LOGGER.debug("Project Artifacts getting successfully");
        return project.getArtifactsList();
      } else {
        var errorMessage = "Artifacts not found in the Project";
        throw new NotFoundException(errorMessage);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getProjectArtifacts(projectId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project deleteArtifacts(String projectId, String artifactKey) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();

      if (false) { // Change it with parameter for support to delete all artifacts
        var query = session.createQuery(DELETE_ALL_ARTIFACTS_HQL);
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      } else {
        var query = session.createQuery(DELETE_SELECTED_ARTIFACT_BY_KEYS_HQL);
        query.setParameter("keys", Collections.singletonList(artifactKey));
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      }
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      projectObj.increaseVersionNumber();
      session.update(projectObj);
      transaction.commit();
      return projectObj.getProtoObject(
          mdbRoleService, authService, new HashMap<>(), new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteArtifacts(projectId, artifactKey);
      } else {
        throw ex;
      }
    }
  }

  /**
   * returns a list of projectIds accessible to the user passed as an argument within the workspace
   * passed as an argument. For no auth returns the list of non deleted projects
   */
  @Override
  public List<String> getWorkspaceProjectIDs(String workspaceName, UserInfo currentLoginUserInfo) {
    if (!mdbRoleService.IsImplemented()) {
      try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        return session.createQuery(NON_DELETED_PROJECT_IDS).list();
      }
    } else {

      // get list of accessible projects
      @SuppressWarnings("unchecked")
      List<String> accessibleProjectIds =
          mdbRoleService.getAccessibleResourceIds(
              null,
              new CollaboratorUser(authService, currentLoginUserInfo),
              ModelDBServiceResourceTypes.PROJECT,
              Collections.EMPTY_LIST);

      Set<String> accessibleResourceIds = new HashSet<>(accessibleProjectIds);
      // in personal workspace show projects directly shared
      if (workspaceName != null
          && !workspaceName.isEmpty()
          && workspaceName.equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
        LOGGER.debug("Workspace and current login user match");
        List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
            mdbRoleService.getResourceItems(
                null, Collections.emptySet(), ModelDBServiceResourceTypes.PROJECT, false);
        accessibleResourceIds.addAll(
            accessibleAllWorkspaceItems.stream()
                .map(GetResourcesResponseItem::getResourceId)
                .collect(Collectors.toSet()));
      } else if (workspaceName != null && !workspaceName.isEmpty()) {
        // get list of accessible projects
        accessibleResourceIds =
            ModelDBUtils.filterWorkspaceOnlyAccessibleIds(
                mdbRoleService,
                accessibleResourceIds,
                workspaceName,
                currentLoginUserInfo,
                ModelDBServiceResourceTypes.PROJECT);
      }

      LOGGER.debug("accessibleAllWorkspaceProjectIds : {}", accessibleResourceIds);

      try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        @SuppressWarnings("unchecked")
        Query<String> query = session.createQuery(NON_DELETED_PROJECT_IDS_BY_IDS);
        query.setParameterList(ModelDBConstants.PROJECT_IDS, accessibleResourceIds);
        List<String> resultProjects = query.list();
        LOGGER.debug(
            "Total accessible project Ids in function getWorkspaceProjectIDs : {}", resultProjects);
        return resultProjects;
      }
    }
  }

  @Override
  public boolean projectExistsInDB(String projectId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(COUNT_PROJECT_BY_ID_HQL);
      query.setParameter("id", projectId);
      Long projectCount = (Long) query.getSingleResult();
      return projectCount == 1L;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return projectExistsInDB(projectId);
      } else {
        throw ex;
      }
    }
  }
}
