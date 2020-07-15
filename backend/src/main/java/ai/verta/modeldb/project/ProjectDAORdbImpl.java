package ai.verta.modeldb.project;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum;
import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.App;
import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.FindProjects;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorBase;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.ProjectPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.CodeVersionEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.telemetry.TelemetryUtils;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.Organization;
import ai.verta.uac.Role;
import ai.verta.uac.RoleBinding;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class ProjectDAORdbImpl implements ProjectDAO {

  private static final Logger LOGGER = LogManager.getLogger(ProjectDAORdbImpl.class);
  private ExperimentDAO experimentDAO;
  private ExperimentRunDAO experimentRunDAO;
  private String starterProjectID;
  private final AuthService authService;
  private final RoleService roleService;

  private static final String GET_PROJECT_COUNT_BY_NAME_PREFIX_HQL =
      new StringBuilder("Select count(*) From ProjectEntity p where p.")
          .append(ModelDBConstants.NAME)
          .append(" = :projectName ")
          .toString();
  private static final String GET_PROJECT_ATTR_BY_KEYS_HQL =
      new StringBuilder("From AttributeEntity kv where kv.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND kv.projectEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :projectId AND kv.field_type = :fieldType")
          .toString();
  private static final String DELETE_ALL_PROJECT_TAGS_HQL =
      new StringBuilder("delete from TagsMapping tm WHERE tm.projectEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :projectId")
          .toString();
  private static final String DELETE_SELECTED_PROJECT_TAGS_HQL =
      new StringBuilder("delete from TagsMapping tm WHERE tm.")
          .append(ModelDBConstants.TAGS)
          .append(" in (:tags) AND tm.projectEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :projectId")
          .toString();
  private static final String DELETE_ALL_PROJECT_ATTRIBUTES_HQL =
      new StringBuilder("delete from AttributeEntity attr WHERE attr.projectEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :projectId")
          .toString();
  private static final String DELETE_SELECTED_PROJECT_ATTRIBUTES_HQL =
      new StringBuilder("delete from AttributeEntity attr WHERE attr.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND attr.projectEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :projectId")
          .toString();
  private static final String GET_PROJECT_EXPERIMENTS_COUNT_HQL =
      new StringBuilder("SELECT COUNT(*) FROM ExperimentEntity ee WHERE ee.")
          .append(ModelDBConstants.PROJECT_ID)
          .append(" IN (:")
          .append(ModelDBConstants.PROJECT_IDS)
          .append(")")
          .toString();
  private static final String GET_PROJECT_EXPERIMENT_RUNS_COUNT_HQL =
      new StringBuilder("SELECT COUNT(*) FROM ExperimentRunEntity ere WHERE ere.")
          .append(ModelDBConstants.PROJECT_ID)
          .append(" IN (:")
          .append(ModelDBConstants.PROJECT_IDS)
          .append(")")
          .toString();
  private static final String GET_PROJECT_BY_ID_HQL =
      "From ProjectEntity p where p.id = :id AND p." + ModelDBConstants.DELETED + " = false";
  private static final String COUNT_PROJECT_BY_ID_HQL =
      "Select Count(id) From ProjectEntity p where p.deleted = false AND p.id = :projectId";
  private static final String NON_DELETED_PROJECT_IDS =
      "select id  From ProjectEntity p where p.deleted = false";
  private static final String NON_DELETED_PROJECT_IDS_BY_IDS =
      NON_DELETED_PROJECT_IDS + " AND p.id in (:" + ModelDBConstants.PROJECT_IDS + ")";
  private static final String IDS_FILTERED_BY_WORKSPACE =
      NON_DELETED_PROJECT_IDS_BY_IDS
          + " AND p."
          + ModelDBConstants.WORKSPACE
          + " = :"
          + ModelDBConstants.WORKSPACE
          + " AND p."
          + ModelDBConstants.WORKSPACE_TYPE
          + " = :"
          + ModelDBConstants.WORKSPACE_TYPE;
  private static final String GET_PROJECT_BY_IDS_HQL =
      "From ProjectEntity p where p.id IN (:ids) AND p." + ModelDBConstants.DELETED + " = false";
  private static final String GET_PROJECT_BY_SHORT_NAME_AND_OWNER_HQL =
      new StringBuilder("From ProjectEntity p where p.")
          .append(ModelDBConstants.SHORT_NAME)
          .append(" = :projectShortName AND ")
          .append(ModelDBConstants.OWNER)
          .append(" =:vertaId")
          .toString();
  private static final String DELETE_ALL_ARTIFACTS_HQL =
      new StringBuilder("delete from ArtifactEntity ar WHERE ar.projectEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :projectId")
          .toString();
  private static final String DELETE_SELECTED_ARTIFACT_BY_KEYS_HQL =
      new StringBuilder("delete from ArtifactEntity ar WHERE ar.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND ar.projectEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :projectId")
          .toString();
  private static final String DELETED_STATUS_PROJECT_QUERY_STRING =
      new StringBuilder("UPDATE ")
          .append(ProjectEntity.class.getSimpleName())
          .append(" pr ")
          .append("SET pr.")
          .append(ModelDBConstants.DELETED)
          .append(" = :deleted ")
          .append(" WHERE pr.")
          .append(ModelDBConstants.ID)
          .append(" IN (:projectIds)")
          .toString();

  public ProjectDAORdbImpl(
      AuthService authService,
      RoleService roleService,
      ExperimentDAO experimentDAO,
      ExperimentRunDAO experimentRunDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.experimentDAO = experimentDAO;
    this.experimentRunDAO = experimentRunDAO;
    App app = App.getInstance();
    this.starterProjectID = app.getStarterProjectID();
  }

  private void checkIfEntityAlreadyExists(Session session, Project project) {
    ModelDBHibernateUtil.checkIfEntityAlreadyExists(
        session,
        "p",
        GET_PROJECT_COUNT_BY_NAME_PREFIX_HQL,
        "Project",
        "projectName",
        project.getName(),
        ModelDBConstants.WORKSPACE,
        project.getWorkspaceId(),
        project.getWorkspaceType(),
        LOGGER);
  }

  @Override
  public Project insertProject(Project project, UserInfo userInfo)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      checkIfEntityAlreadyExists(session, project);
      createRoleBindingsForProject(project, userInfo);

      Transaction transaction = session.beginTransaction();
      ProjectEntity projectEntity = RdbmsUtils.generateProjectEntity(project);
      session.save(projectEntity);
      transaction.commit();
      LOGGER.debug("Project created successfully");
      TelemetryUtils.insertModelDBDeploymentInfo();
      return projectEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return insertProject(project, userInfo);
      } else {
        throw ex;
      }
    }
  }

  private void createRoleBindingsForProject(Project project, UserInfo userInfo) {
    Role ownerRole = roleService.getRoleByName(ModelDBConstants.ROLE_PROJECT_OWNER, null);
    roleService.createRoleBinding(
        ownerRole,
        new CollaboratorUser(authService, userInfo),
        project.getId(),
        ModelDBServiceResourceTypes.PROJECT);

    if (project.getProjectVisibility().equals(ProjectVisibility.PUBLIC)) {
      roleService.createPublicRoleBinding(project.getId(), ModelDBServiceResourceTypes.PROJECT);
    }

    createWorkspaceRoleBinding(
        project.getWorkspaceId(),
        project.getWorkspaceType(),
        project.getId(),
        project.getProjectVisibility());
  }

  private void createWorkspaceRoleBinding(
      String workspaceId,
      WorkspaceType workspaceType,
      String projectId,
      ProjectVisibility projectVisibility) {
    if (workspaceId != null && !workspaceId.isEmpty()) {
      roleService.createWorkspaceRoleBinding(
          workspaceId,
          workspaceType,
          projectId,
          ModelDBConstants.ROLE_PROJECT_ADMIN,
          ModelDBServiceResourceTypes.PROJECT,
          projectVisibility.equals(ProjectVisibility.ORG_SCOPED_PUBLIC),
          "_GLOBAL_SHARING");
    }
  }

  @Override
  public Project updateProjectName(String projectId, String projectName)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectEntity = session.load(ProjectEntity.class, projectId);

      Project project =
          Project.newBuilder()
              .setName(projectName)
              .setWorkspaceId(projectEntity.getWorkspace())
              .setWorkspaceTypeValue(projectEntity.getWorkspace_type())
              .build();
      checkIfEntityAlreadyExists(session, project);

      projectEntity.setName(projectName);
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug("Project name updated successfully");
      return projectEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateProjectName(projectId, projectName);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project updateProjectDescription(String projectId, String projectDescription)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectEntity = session.load(ProjectEntity.class, projectId);
      projectEntity.setDescription(projectDescription);
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug("Project description updated successfully");
      return projectEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateProjectDescription(projectId, projectDescription);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project updateProjectReadme(String projectId, String projectReadme)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectEntity = session.load(ProjectEntity.class, projectId);
      projectEntity.setReadme_text(projectReadme);
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug("Project readme updated successfully");
      return projectEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateProjectReadme(projectId, projectReadme);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project logProjectCodeVersion(String projectId, CodeVersion updatedCodeVersion)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectEntity = session.get(ProjectEntity.class, projectId);

      CodeVersionEntity existingCodeVersionEntity = projectEntity.getCode_version_snapshot();
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
      Transaction transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug("Project code version snapshot updated successfully");
      return projectEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return logProjectCodeVersion(projectId, updatedCodeVersion);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project updateProjectAttributes(String projectId, KeyValue attribute)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      if (projectObj == null) {
        String errorMessage = "Project not found for given ID";
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      AttributeEntity updatedAttributeObj =
          RdbmsUtils.generateAttributeEntity(projectObj, ModelDBConstants.ATTRIBUTES, attribute);

      List<AttributeEntity> existingAttributes = projectObj.getAttributeMapping();
      if (!existingAttributes.isEmpty()) {
        boolean doesExist = false;
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
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(projectObj);
      transaction.commit();
      return projectObj.getProtoObject();
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
      String projectId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      if (getAll) {
        ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
        if (projectObj == null) {
          String errorMessage = "Project not found for given ID: " + projectId;
          LOGGER.info(errorMessage);
          Status status =
              Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
          throw StatusProto.toStatusRuntimeException(status);
        }
        return projectObj.getProtoObject().getAttributesList();
      } else {
        Query query = session.createQuery(GET_PROJECT_ATTR_BY_KEYS_HQL);
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
  public Project addProjectTags(String projectId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      if (projectObj == null) {
        String errorMessage = "Project not found for given ID";
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      List<String> newTags = new ArrayList<>();
      Project existingProtoProjectObj = projectObj.getProtoObject();
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
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(projectObj);
        transaction.commit();
      }
      LOGGER.debug("Project tags added successfully");
      return projectObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addProjectTags(projectId, tagsList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project deleteProjectTags(String projectId, List<String> projectTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      if (deleteAll) {
        Query query = session.createQuery(DELETE_ALL_PROJECT_TAGS_HQL);
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      } else {
        Query query = session.createQuery(DELETE_SELECTED_PROJECT_TAGS_HQL);
        query.setParameter("tags", projectTagList);
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      }

      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      session.update(projectObj);
      transaction.commit();
      LOGGER.debug("Project tags deleted successfully");
      return projectObj.getProtoObject();
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
      ProjectVisibility projectVisibility)
      throws InvalidProtocolBufferException {

    FindProjects findProjects =
        FindProjects.newBuilder()
            .setPageNumber(pageNumber)
            .setPageLimit(pageLimit)
            .setAscending(order)
            .setSortKey(sortKey)
            .build();
    return findProjects(findProjects, null, userInfo, projectVisibility);
  }

  @Override
  public List<Project> getProjects(String key, String value, UserInfo userInfo)
      throws InvalidProtocolBufferException {
    FindProjects findProjects =
        FindProjects.newBuilder()
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setKey(key)
                    .setValue(Value.newBuilder().setStringValue(value).build())
                    .setOperator(OperatorEnum.Operator.EQ)
                    .setValueType(ValueTypeEnum.ValueType.STRING)
                    .build())
            .build();
    ProjectPaginationDTO projectPaginationDTO =
        findProjects(findProjects, null, userInfo, ProjectVisibility.PRIVATE);
    LOGGER.debug("Projects size is {}", projectPaginationDTO.getProjects().size());
    return projectPaginationDTO.getProjects();
  }

  @Override
  public Project addProjectAttributes(String projectId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      projectObj.setAttributeMapping(
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              projectObj, ModelDBConstants.ATTRIBUTES, attributesList));
      projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(projectObj);
      transaction.commit();
      LOGGER.debug("Project attributes added successfully");
      return projectObj.getProtoObject();
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
      String projectId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      if (deleteAll) {
        Query query = session.createQuery(DELETE_ALL_PROJECT_ATTRIBUTES_HQL);
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      } else {
        Query query = session.createQuery(DELETE_SELECTED_PROJECT_ATTRIBUTES_HQL);
        query.setParameter("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      }
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      session.update(projectObj);
      transaction.commit();
      return projectObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteProjectAttributes(projectId, attributeKeyList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getProjectTags(String projectId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      return projectObj.getProtoObject().getTagsList();
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
    Project.Builder projectBuilder =
        Project.newBuilder(srcProject).setId(UUID.randomUUID().toString());

    if (userInfo != null) {
      projectBuilder.setOwner(authService.getVertaIdFromUserInfo(userInfo));
    }
    return projectBuilder.build();
  }

  @Override
  public Project deepCopyProjectForUser(String srcProjectID, UserInfo newOwner)
      throws InvalidProtocolBufferException {
    // if no project id specified , default to the one captured from config.yaml
    // TODO: extend the starter project to be set of projects, so parameterizing this function makes
    // sense
    if (srcProjectID == null || srcProjectID.isEmpty()) srcProjectID = starterProjectID;

    // if this is not a starter project, then cloning is not supported
    if (!srcProjectID.equals(starterProjectID)) {
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage("Cloning project supported only for starter project")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    // source project
    Project srcProject = getProjectByID(srcProjectID);
    if (newOwner == null) {
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage("New owner not passed for cloning Project ")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    // cloned project
    Project newProject = copyProjectAndUpdateDetails(srcProject, newOwner);
    insertProject(newProject, newOwner);
    newProject = getProjectByID(newProject.getId());

    // Deep Copy Experiments
    List<KeyValue> projectLevelFilter = new ArrayList<>();
    Value projectIdValue = Value.newBuilder().setStringValue(srcProject.getId()).build();
    projectLevelFilter.add(
        KeyValue.newBuilder().setKey(ModelDBConstants.PROJECT_ID).setValue(projectIdValue).build());
    // get  Experiments from the source Project, copy their clone and associate them to new project
    List<Experiment> experiments = this.experimentDAO.getExperiments(projectLevelFilter);
    for (Experiment srcExperiment : experiments) {
      Experiment newExperiment =
          this.experimentDAO.deepCopyExperimentForUser(srcExperiment, newProject, newOwner);
      Value experimentIDValue = Value.newBuilder().setStringValue(srcExperiment.getId()).build();
      List<KeyValue> experimentLevelFilter = new ArrayList<>(projectLevelFilter);
      experimentLevelFilter.add(
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.EXPERIMENT_ID)
              .setValue(experimentIDValue)
              .build());

      List<ExperimentRun> experimentRuns =
          this.experimentRunDAO.getExperimentRuns(experimentLevelFilter);
      for (ExperimentRun srcExperimentRun : experimentRuns) {
        this.experimentRunDAO.deepCopyExperimentRunForUser(
            srcExperimentRun, newExperiment, newProject, newOwner);
      }
    }

    return newProject;
  }

  private List<String> getWorkspaceRoleBindings(
      String workspaceId,
      WorkspaceType workspaceType,
      String projectId,
      ProjectVisibility projectVisibility) {
    return roleService.getWorkspaceRoleBindings(
        workspaceId,
        workspaceType,
        projectId,
        ModelDBConstants.ROLE_PROJECT_ADMIN,
        ModelDBServiceResourceTypes.PROJECT,
        projectVisibility.equals(ProjectVisibility.ORG_SCOPED_PUBLIC),
        "_GLOBAL_SHARING");
  }

  @Override
  public Boolean deleteProjects(List<String> projectIds) {

    // Get self allowed resources id where user has delete permission
    List<String> allowedProjectIds =
        roleService.getAccessibleResourceIdsByActions(
            ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.DELETE, projectIds);
    if (allowedProjectIds.isEmpty()) {
      Status status =
          Status.newBuilder()
              .setCode(Code.PERMISSION_DENIED_VALUE)
              .setMessage("Delete Access Denied for given project Ids : " + projectIds)
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      Query deletedProjectQuery = session.createQuery(DELETED_STATUS_PROJECT_QUERY_STRING);
      deletedProjectQuery.setParameter("deleted", true);
      deletedProjectQuery.setParameter("projectIds", allowedProjectIds);
      int updatedCount = deletedProjectQuery.executeUpdate();
      LOGGER.debug("Mark Projects as deleted : {}, count : {}", allowedProjectIds, updatedCount);
      transaction.commit();
      LOGGER.debug("Project deleted successfully");
      return true;
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
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
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
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
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
  public Project getProjectByID(String id) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", id);
      ProjectEntity projectEntity = (ProjectEntity) query.uniqueResult();
      if (projectEntity == null) {
        String errorMessage = ModelDBMessages.PROJECT_NOT_FOUND_FOR_ID;
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      return projectEntity.getProtoObject();
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
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_PROJECT_BY_SHORT_NAME_AND_OWNER_HQL);
      query.setParameter("projectShortName", projectShortName);
      query.setParameter("vertaId", authService.getVertaIdFromUserInfo(userInfo));
      ProjectEntity projectEntity = (ProjectEntity) query.uniqueResult();
      if (projectEntity != null) {
        Status status =
            Status.newBuilder()
                .setCode(Code.ALREADY_EXISTS_VALUE)
                .setMessage("Project already exist with given short name")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", projectId);
      projectEntity = (ProjectEntity) query.uniqueResult();
      projectEntity.setShort_name(projectShortName);
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      return projectEntity.getProtoObject();
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
      UserInfo hostUserInfo, UserInfo currentLoginUserInfo, String workspaceName)
      throws InvalidProtocolBufferException {
    CollaboratorUser hostCollaboratorBase = null;
    if (hostUserInfo != null) {
      hostCollaboratorBase = new CollaboratorUser(authService, hostUserInfo);
    }

    FindProjects.Builder findProjects = FindProjects.newBuilder();
    findProjects.setWorkspaceName(workspaceName);

    ProjectPaginationDTO projectPaginationDTO =
        findProjects(
            findProjects.build(),
            hostCollaboratorBase,
            currentLoginUserInfo,
            ProjectVisibility.PUBLIC);
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
  public Project setProjectVisibility(String projectId, ProjectVisibility projectVisibility)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", projectId);
      ProjectEntity projectEntity = (ProjectEntity) query.uniqueResult();

      Integer oldVisibilityInt = projectEntity.getProject_visibility();
      ProjectVisibility oldVisibility = ProjectVisibility.PRIVATE;
      if (oldVisibilityInt != null) {
        oldVisibility = ProjectVisibility.forNumber(oldVisibilityInt);
      }
      if (!oldVisibility.equals(projectVisibility)) {
        projectEntity.setProject_visibility(projectVisibility.ordinal());
        projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
        Transaction transaction = session.beginTransaction();
        session.update(projectEntity);
        transaction.commit();
        deleteOldVisibilityBasedBinding(
            oldVisibility,
            projectId,
            projectEntity.getWorkspace_type(),
            projectEntity.getWorkspace());
        createNewVisibilityBasedBinding(
            projectVisibility,
            projectId,
            projectEntity.getWorkspace_type(),
            projectEntity.getWorkspace());
      }
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      return projectEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setProjectVisibility(projectId, projectVisibility);
      } else {
        throw ex;
      }
    }
  }

  private void createNewVisibilityBasedBinding(
      ProjectVisibility newVisibility,
      String projectId,
      Integer projectWorkspaceType,
      String workspaceId) {
    switch (newVisibility) {
      case ORG_SCOPED_PUBLIC:
        if (projectWorkspaceType == WorkspaceType.ORGANIZATION_VALUE) {
          Role projRead = roleService.getRoleByName(ModelDBConstants.ROLE_PROJECT_READ_ONLY, null);
          roleService.createRoleBinding(
              projRead,
              new CollaboratorOrg(workspaceId),
              projectId,
              ModelDBServiceResourceTypes.PROJECT);
        }
        break;
      case PUBLIC:
        roleService.createPublicRoleBinding(projectId, ModelDBServiceResourceTypes.PROJECT);

        break;
      case PRIVATE:
      case UNRECOGNIZED:
        break;
    }
  }

  private void deleteOldVisibilityBasedBinding(
      ProjectVisibility oldVisibility,
      String projectId,
      int projectWorkspaceType,
      String workspaceId) {
    switch (oldVisibility) {
      case ORG_SCOPED_PUBLIC:
        if (projectWorkspaceType == WorkspaceType.ORGANIZATION_VALUE) {
          String roleBindingName =
              roleService.buildReadOnlyRoleBindingName(
                  projectId, new CollaboratorOrg(workspaceId), ModelDBServiceResourceTypes.PROJECT);
          RoleBinding roleBinding = roleService.getRoleBindingByName(roleBindingName);
          if (roleBinding != null && !roleBinding.getId().isEmpty()) {
            roleService.deleteRoleBinding(roleBinding.getId());
          }
        }
        break;
      case PUBLIC:
        String roleBindingName =
            roleService.buildPublicRoleBindingName(projectId, ModelDBServiceResourceTypes.PROJECT);
        RoleBinding publicReadRoleBinding = roleService.getRoleBindingByName(roleBindingName);
        if (publicReadRoleBinding != null && !publicReadRoleBinding.getId().isEmpty()) {
          roleService.deleteRoleBinding(publicReadRoleBinding.getId());
        }
        break;
      case PRIVATE:
      case UNRECOGNIZED:
        break;
    }
  }

  @Override
  public List<Project> getProjectsByBatchIds(List<String> projectIds)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<ProjectEntity> projectEntities = getProjectEntityByBatchIds(session, projectIds);
      LOGGER.debug("Project by Ids getting successfully");
      return RdbmsUtils.convertProjectsFromProjectEntityList(projectEntities);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getProjectsByBatchIds(projectIds);
      } else {
        throw ex;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<ProjectEntity> getProjectEntityByBatchIds(Session session, List<String> projectIds) {
    Query query = session.createQuery(GET_PROJECT_BY_IDS_HQL);
    query.setParameterList("ids", projectIds);
    return query.list();
  }

  @Override
  public ProjectPaginationDTO findProjects(
      FindProjects queryParameters,
      CollaboratorBase host,
      UserInfo currentLoginUserInfo,
      ProjectVisibility projectVisibility)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {

      List<String> accessibleProjectIds =
          roleService.getAccessibleResourceIds(
              host,
              new CollaboratorUser(authService, currentLoginUserInfo),
              projectVisibility,
              ModelDBServiceResourceTypes.PROJECT,
              queryParameters.getProjectIdsList());

      if (accessibleProjectIds.isEmpty() && roleService.IsImplemented()) {
        LOGGER.debug("Accessible Project Ids not found, size 0");
        ProjectPaginationDTO projectPaginationDTO = new ProjectPaginationDTO();
        projectPaginationDTO.setProjects(Collections.emptyList());
        projectPaginationDTO.setTotalRecords(0L);
        return projectPaginationDTO;
      }

      CriteriaBuilder builder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<ProjectEntity> criteriaQuery = builder.createQuery(ProjectEntity.class);
      Root<ProjectEntity> projectRoot = criteriaQuery.from(ProjectEntity.class);
      projectRoot.alias("pr");
      List<Predicate> finalPredicatesList = new ArrayList<>();

      List<KeyValueQuery> predicates = new ArrayList<>(queryParameters.getPredicatesList());
      for (KeyValueQuery predicate : predicates) {
        // Validate if current user has access to the entity or not where predicate key has an id
        RdbmsUtils.validatePredicates(
            ModelDBConstants.PROJECTS, accessibleProjectIds, predicate, roleService);
      }

      String workspaceName = queryParameters.getWorkspaceName();
      if (workspaceName != null
          && !workspaceName.isEmpty()
          && workspaceName.equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
        accessibleProjectIds =
            roleService.getSelfDirectlyAllowedResources(
                ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.READ);
        if (queryParameters.getProjectIdsList() != null
            && !queryParameters.getProjectIdsList().isEmpty()) {
          accessibleProjectIds.retainAll(queryParameters.getProjectIdsList());
        }
        // user is in his workspace and has no projects, return empty
        if (accessibleProjectIds.isEmpty()) {
          ProjectPaginationDTO projectPaginationDTO = new ProjectPaginationDTO();
          projectPaginationDTO.setProjects(Collections.emptyList());
          projectPaginationDTO.setTotalRecords(0L);
          return projectPaginationDTO;
        }
        List<String> orgIds =
            roleService.listMyOrganizations().stream()
                .map(Organization::getId)
                .collect(Collectors.toList());
        if (!orgIds.isEmpty()) {
          finalPredicatesList.add(
              builder.not(
                  builder.and(
                      projectRoot.get(ModelDBConstants.WORKSPACE).in(orgIds),
                      builder.equal(
                          projectRoot.get(ModelDBConstants.WORKSPACE_TYPE),
                          WorkspaceType.ORGANIZATION_VALUE))));
        }
      } else {
        if (projectVisibility.equals(ProjectVisibility.PRIVATE)) {
          RdbmsUtils.getWorkspacePredicates(
              host,
              currentLoginUserInfo,
              builder,
              projectRoot,
              finalPredicatesList,
              workspaceName,
              roleService);
        }
      }

      if (!accessibleProjectIds.isEmpty()) {
        Expression<String> exp = projectRoot.get(ModelDBConstants.ID);
        Predicate predicate2 = exp.in(accessibleProjectIds);
        finalPredicatesList.add(predicate2);
      }

      String entityName = "projectEntity";
      try {
        List<Predicate> queryPredicatesList =
            RdbmsUtils.getQueryPredicatesFromPredicateList(
                entityName, predicates, builder, criteriaQuery, projectRoot, authService);
        if (!queryPredicatesList.isEmpty()) {
          finalPredicatesList.addAll(queryPredicatesList);
        }
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          ProjectPaginationDTO projectPaginationDTO = new ProjectPaginationDTO();
          projectPaginationDTO.setProjects(Collections.emptyList());
          projectPaginationDTO.setTotalRecords(0L);
          return projectPaginationDTO;
        }
      }

      finalPredicatesList.add(builder.equal(projectRoot.get(ModelDBConstants.DELETED), false));

      Order orderBy =
          RdbmsUtils.getOrderBasedOnSortKey(
              queryParameters.getSortKey(),
              queryParameters.getAscending(),
              builder,
              projectRoot,
              entityName);

      Predicate[] predicateArr = new Predicate[finalPredicatesList.size()];
      for (int index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      Predicate predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(projectRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      Query query = session.createQuery(criteriaQuery);
      LOGGER.debug("Projects final query : {}", query.getQueryString());
      if (queryParameters.getPageNumber() != 0 && queryParameters.getPageLimit() != 0) {
        // Calculate number of documents to skip
        int skips = queryParameters.getPageLimit() * (queryParameters.getPageNumber() - 1);
        query.setFirstResult(skips);
        query.setMaxResults(queryParameters.getPageLimit());
      }

      List<Project> projectList = new ArrayList<>();
      List<ProjectEntity> projectEntities = query.list();
      if (!projectEntities.isEmpty()) {
        projectList = RdbmsUtils.convertProjectsFromProjectEntityList(projectEntities);
      }

      Set<String> projectIdsSet = new HashSet<>();
      List<Project> projects = new ArrayList<>();
      for (Project project : projectList) {
        if (!projectIdsSet.contains(project.getId())) {
          projectIdsSet.add(project.getId());
          if (queryParameters.getIdsOnly()) {
            project = Project.newBuilder().setId(project.getId()).build();
            projects.add(project);
          } else {
            projects.add(project);
          }
        }
      }

      long totalRecords = RdbmsUtils.count(session, projectRoot, criteriaQuery);

      ProjectPaginationDTO projectPaginationDTO = new ProjectPaginationDTO();
      projectPaginationDTO.setProjects(projects);
      projectPaginationDTO.setTotalRecords(totalRecords);
      return projectPaginationDTO;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findProjects(queryParameters, host, currentLoginUserInfo, projectVisibility);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project logArtifacts(String projectId, List<Artifact> newArtifacts)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", projectId);
      ProjectEntity projectEntity = (ProjectEntity) query.uniqueResult();
      if (projectEntity == null) {
        String errorMessage = "Project not found for given ID: " + projectId;
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<Artifact> existingArtifacts = projectEntity.getProtoObject().getArtifactsList();
      for (Artifact existingArtifact : existingArtifacts) {
        for (Artifact newArtifact : newArtifacts) {
          if (existingArtifact.getKey().equals(newArtifact.getKey())) {
            Status status =
                Status.newBuilder()
                    .setCode(Code.ALREADY_EXISTS_VALUE)
                    .setMessage(
                        "Artifact being logged already exists. existing artifact key : "
                            + newArtifact.getKey())
                    .build();
            throw StatusProto.toStatusRuntimeException(status);
          }
        }
      }

      projectEntity.setArtifactMapping(
          RdbmsUtils.convertArtifactsFromArtifactEntityList(
              projectEntity, ModelDBConstants.ARTIFACTS, newArtifacts));
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      return projectEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return logArtifacts(projectId, newArtifacts);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Artifact> getProjectArtifacts(String projectId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", projectId);
      ProjectEntity projectEntity = (ProjectEntity) query.uniqueResult();
      if (projectEntity == null) {
        String errorMessage = "Project not found for given ID: " + projectId;
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      Project project = projectEntity.getProtoObject();
      if (project.getArtifactsList() != null && !project.getArtifactsList().isEmpty()) {
        LOGGER.debug("Project Artifacts getting successfully");
        return project.getArtifactsList();
      } else {
        String errorMessage = "Artifacts not found in the Project";
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
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
  public Project deleteArtifacts(String projectId, String artifactKey)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      if (false) { // Change it with parameter for support to delete all artifacts
        Query query = session.createQuery(DELETE_ALL_ARTIFACTS_HQL);
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      } else {
        Query query = session.createQuery(DELETE_SELECTED_ARTIFACT_BY_KEYS_HQL);
        query.setParameter("keys", Collections.singletonList(artifactKey));
        query.setParameter(ModelDBConstants.PROJECT_ID_STR, projectId);
        query.executeUpdate();
      }
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      session.update(projectObj);
      transaction.commit();
      return projectObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteArtifacts(projectId, artifactKey);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Map<String, String> getOwnersByProjectIds(List<String> projectIds) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_PROJECT_BY_IDS_HQL);
      query.setParameterList("ids", projectIds);

      @SuppressWarnings("unchecked")
      List<ProjectEntity> projectEntities = query.list();
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      Map<String, String> projectOwnersMap = new HashMap<>();
      for (ProjectEntity projectEntity : projectEntities) {
        projectOwnersMap.put(projectEntity.getId(), projectEntity.getOwner());
      }
      return projectOwnersMap;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getOwnersByProjectIds(projectIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Project setProjectWorkspace(String projectId, WorkspaceDTO workspaceDTO)
      throws InvalidProtocolBufferException {

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectEntity = session.load(ProjectEntity.class, projectId);
      List<String> roleBindingNames =
          getWorkspaceRoleBindings(
              projectEntity.getWorkspace(),
              WorkspaceType.forNumber(projectEntity.getWorkspace_type()),
              projectId,
              ProjectVisibility.forNumber(projectEntity.getProject_visibility()));
      roleService.deleteRoleBindings(roleBindingNames);
      createWorkspaceRoleBinding(
          workspaceDTO.getWorkspaceId(),
          workspaceDTO.getWorkspaceType(),
          projectId,
          ProjectVisibility.forNumber(projectEntity.getProject_visibility()));
      projectEntity.setWorkspace(workspaceDTO.getWorkspaceId());
      projectEntity.setWorkspace_type(workspaceDTO.getWorkspaceType().getNumber());
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug("Project workspace updated successfully");
      return projectEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setProjectWorkspace(projectId, workspaceDTO);
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
  public List<String> getWorkspaceProjectIDs(String workspaceName, UserInfo currentLoginUserInfo)
      throws InvalidProtocolBufferException {
    if (!roleService.IsImplemented()) {
      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        return session.createQuery(NON_DELETED_PROJECT_IDS).list();
      }
    } else {

      // get list of accessible projects
      @SuppressWarnings("unchecked")
      List<String> accessibleProjectIds =
          roleService.getAccessibleResourceIds(
              null,
              new CollaboratorUser(authService, currentLoginUserInfo),
              ProjectVisibility.PRIVATE,
              ModelDBServiceResourceTypes.PROJECT,
              Collections.EMPTY_LIST);
      LOGGER.debug(
          "accessible Project Ids in function getWorkspaceProjectIDs : {}", accessibleProjectIds);

      // resolve workspace
      WorkspaceDTO workspaceDTO =
          roleService.getWorkspaceDTOByWorkspaceName(currentLoginUserInfo, workspaceName);

      List<String> resultProjects = new LinkedList<String>();
      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        @SuppressWarnings("unchecked")
        Query<String> query = session.createQuery(IDS_FILTERED_BY_WORKSPACE);
        query.setParameterList(ModelDBConstants.PROJECT_IDS, accessibleProjectIds);
        query.setParameter(ModelDBConstants.WORKSPACE, workspaceDTO.getWorkspaceId());
        query.setParameter(
            ModelDBConstants.WORKSPACE_TYPE, workspaceDTO.getWorkspaceType().getNumber());
        resultProjects = query.list();

        // in personal workspace show projects directly shared
        if (workspaceDTO
            .getWorkspaceName()
            .equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
          LOGGER.debug("Workspace and current login user match");
          List<String> directlySharedProjects =
              roleService.getSelfDirectlyAllowedResources(
                  ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.READ);
          query = session.createQuery(NON_DELETED_PROJECT_IDS_BY_IDS);
          query.setParameterList(ModelDBConstants.PROJECT_IDS, directlySharedProjects);
          resultProjects.addAll(query.list());
          LOGGER.debug(
              "accessible directlySharedProjects Ids in function getWorkspaceProjectIDs : {}",
              directlySharedProjects);
        }
      }
      LOGGER.debug(
          "Total accessible project Ids in function getWorkspaceProjectIDs : {}", resultProjects);
      return resultProjects;
    }
  }

  @Override
  public boolean projectExistsInDB(String projectId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(COUNT_PROJECT_BY_ID_HQL);
      query.setParameter("projectId", projectId);
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
