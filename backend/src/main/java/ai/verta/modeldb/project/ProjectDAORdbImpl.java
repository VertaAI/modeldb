package ai.verta.modeldb.project;

import ai.verta.common.KeyValue;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.App;
import ai.verta.modeldb.Artifact;
import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.FindProjects;
import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorBase;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.ProjectPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.CodeVersionEntity;
import ai.verta.modeldb.entities.CommentEntity;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.telemetry.TelemetryUtils;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.Organization;
import ai.verta.uac.Role;
import ai.verta.uac.RoleBinding;
import ai.verta.uac.RoleScope;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
  private static final String FIND_EXPERIMENT_BY_PROJECT_IDS_HQL =
      new StringBuilder("From ExperimentEntity ee where ee.")
          .append(ModelDBConstants.PROJECT_ID)
          .append(" IN (:")
          .append(ModelDBConstants.PROJECT_IDS)
          .append(") ")
          .toString();
  private static final String FIND_EXPERIMENT_RUN_BY_PROJECT_IDS_HQL =
      new StringBuilder("From ExperimentRunEntity ee where ee.")
          .append(ModelDBConstants.PROJECT_ID)
          .append(" IN (:")
          .append(ModelDBConstants.PROJECT_IDS)
          .append(") ")
          .toString();
  private static final String FIND_COMMENTS_HQL =
      new StringBuilder("From CommentEntity ce where ce.")
          .append(ModelDBConstants.ENTITY_ID)
          .append(" IN (:entityIds) AND ce.")
          .append(ModelDBConstants.ENTITY_NAME)
          .append(" =:entityName")
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
  private static final String GET_PROJECT_BY_ID_HQL = "From ProjectEntity p where p.id = :id";
  private static final String GET_PROJECT_BY_IDS_HQL = "From ProjectEntity p where p.id IN (:ids)";
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
      Transaction transaction = session.beginTransaction();
      checkIfEntityAlreadyExists(session, project);
      ProjectEntity projectEntity = RdbmsUtils.generateProjectEntity(project);
      session.save(projectEntity);

      Role ownerRole = roleService.getRoleByName(ModelDBConstants.ROLE_PROJECT_OWNER, null);
      roleService.createRoleBinding(
          ownerRole,
          new CollaboratorUser(authService, userInfo),
          project.getId(),
          ModelDBServiceResourceTypes.PROJECT);
      if (project.getProjectVisibility().equals(ProjectVisibility.PUBLIC)) {
        Role publicReadRole =
            roleService.getRoleByName(ModelDBConstants.ROLE_PROJECT_PUBLIC_READ, null);
        UserInfo unsignedUser = authService.getUnsignedUser();
        roleService.createRoleBinding(
            publicReadRole,
            new CollaboratorUser(authService, unsignedUser),
            project.getId(),
            ModelDBServiceResourceTypes.PROJECT);
      }

      createWorkspaceRoleBinding(
          project.getWorkspaceId(),
          project.getWorkspaceType(),
          project.getId(),
          project.getProjectVisibility());

      transaction.commit();
      LOGGER.debug("Project created successfully");
      TelemetryUtils.insertModelDBDeploymentInfo();
      return projectEntity.getProtoObject();
    }
  }

  private void createWorkspaceRoleBinding(
      String workspaceId,
      WorkspaceType workspaceType,
      String projectId,
      ProjectVisibility projectVisibility) {
    if (workspaceId != null && !workspaceId.isEmpty()) {
      Role projAdmin = roleService.getRoleByName(ModelDBConstants.ROLE_PROJECT_ADMIN, null);
      Role projRead = roleService.getRoleByName(ModelDBConstants.ROLE_PROJECT_READ_ONLY, null);
      switch (workspaceType) {
        case ORGANIZATION:
          Organization org = (Organization) roleService.getOrgById(workspaceId);
          roleService.createRoleBinding(
              projAdmin,
              new CollaboratorUser(authService, org.getOwnerId()),
              projectId,
              ModelDBServiceResourceTypes.PROJECT);
          if (projectVisibility.equals(ProjectVisibility.ORG_SCOPED_PUBLIC)) {
            String globalSharingRoleName =
                new StringBuilder()
                    .append("O_")
                    .append(workspaceId)
                    .append("_GLOBAL_SHARING")
                    .toString();
            try {
              Role globalSharingRole =
                  roleService.getRoleByName(
                      globalSharingRoleName, RoleScope.newBuilder().setOrgId(workspaceId).build());
              roleService.createRoleBinding(
                  globalSharingRole,
                  new CollaboratorOrg(workspaceId),
                  projectId,
                  ModelDBServiceResourceTypes.PROJECT);
            } catch (StatusRuntimeException ex) {
              if (ex.getStatus().getCode().value() == Code.NOT_FOUND_VALUE) {
                // DO NOTHING if the role does not exist
                LOGGER.warn(ex.getMessage());
              } else {
                throw ex;
              }
            }
          }
          break;
        case USER:
          roleService.createRoleBinding(
              projAdmin,
              new CollaboratorUser(authService, workspaceId),
              projectId,
              ModelDBServiceResourceTypes.PROJECT);
          break;
        default:
          break;
      }
    }
  }

  @Override
  public Project updateProjectName(String projectId, String projectName)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
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
      session.update(projectEntity);
      LOGGER.debug("Project name updated successfully");
      transaction.commit();
      return projectEntity.getProtoObject();
    }
  }

  @Override
  public Project updateProjectDescription(String projectId, String projectDescription)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectEntity = session.load(ProjectEntity.class, projectId);
      projectEntity.setDescription(projectDescription);
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      session.update(projectEntity);
      LOGGER.debug("Project description updated successfully");
      transaction.commit();
      return projectEntity.getProtoObject();
    }
  }

  @Override
  public Project updateProjectReadme(String projectId, String projectReadme)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectEntity = session.load(ProjectEntity.class, projectId);
      projectEntity.setReadme_text(projectReadme);
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      session.update(projectEntity);
      LOGGER.debug("Project readme updated successfully");
      transaction.commit();
      return projectEntity.getProtoObject();
    }
  }

  @Override
  public Project logProjectCodeVersion(String projectId, CodeVersion updatedCodeVersion)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
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
      session.update(projectEntity);
      LOGGER.debug("Project code version snapshot updated successfully");
      transaction.commit();
      return projectEntity.getProtoObject();
    }
  }

  @Override
  public Project updateProjectAttributes(String projectId, KeyValue attribute)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      if (projectObj == null) {
        String errorMessage = "Project not found for given ID";
        LOGGER.warn(errorMessage);
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
      session.saveOrUpdate(projectObj);
      transaction.commit();
      return projectObj.getProtoObject();
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
          LOGGER.warn(errorMessage);
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
    }
  }

  @Override
  public Project addProjectTags(String projectId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      if (projectObj == null) {
        String errorMessage = "Project not found for given ID";
        LOGGER.warn(errorMessage);
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
        session.saveOrUpdate(projectObj);
      }
      transaction.commit();
      LOGGER.debug("Project tags added successfully");
      return projectObj.getProtoObject();
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
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      projectObj.setAttributeMapping(
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              projectObj, ModelDBConstants.ATTRIBUTES, attributesList));
      projectObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      session.saveOrUpdate(projectObj);
      transaction.commit();
      LOGGER.debug("Project attributes added successfully");
      return projectObj.getProtoObject();
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
    }
  }

  @Override
  public List<String> getProjectTags(String projectId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      return projectObj.getProtoObject().getTagsList();
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

  private void deleteExperimentsWithPagination(Session session, List<String> projectIds) {
    int lowerBound = 0;
    final int pagesize = 100;
    Long count = getExperimentCount(projectIds);
    LOGGER.debug("Total experimentEntities {}", count);

    while (lowerBound < count) {
      Transaction transaction = session.beginTransaction();
      // Delete the ExperimentEntity object
      Query experimentDeleteQuery = session.createQuery(FIND_EXPERIMENT_BY_PROJECT_IDS_HQL);
      experimentDeleteQuery.setParameterList(ModelDBConstants.PROJECT_IDS, projectIds);
      experimentDeleteQuery.setFirstResult(lowerBound);
      experimentDeleteQuery.setMaxResults(pagesize);
      List<ExperimentEntity> experimentEntities = experimentDeleteQuery.list();
      for (ExperimentEntity experimentEntity : experimentEntities) {
        session.delete(experimentEntity);
      }
      transaction.commit();
      lowerBound += pagesize;
    }
  }

  private void deleteExperimentRunsWithPagination(Session session, List<String> projectIds) {
    int lowerBound = 0;
    final int pagesize = 100;
    Long count = getExperimentRunCount(projectIds);
    LOGGER.debug("Total experimentRunEntities {}", count);

    while (lowerBound < count) {
      Transaction transaction = session.beginTransaction();

      Query experimentRunDeleteQuery = session.createQuery(FIND_EXPERIMENT_RUN_BY_PROJECT_IDS_HQL);
      experimentRunDeleteQuery.setParameterList(ModelDBConstants.PROJECT_IDS, projectIds);
      experimentRunDeleteQuery.setFirstResult(lowerBound);
      experimentRunDeleteQuery.setMaxResults(pagesize);
      List<ExperimentRunEntity> experimentRunEntities = experimentRunDeleteQuery.list();
      List<String> experimentRunIds = new ArrayList<>();
      for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
        experimentRunIds.add(experimentRunEntity.getId());
        session.delete(experimentRunEntity);
      }
      // Delete the ExperimentRUn comments
      if (!experimentRunIds.isEmpty()) {
        removeEntityComments(session, experimentRunIds, ExperimentRunEntity.class.getSimpleName());
      }

      transaction.commit();
      lowerBound += pagesize;
    }
  }

  private void deleteRoleBindingsOfAccessibleProjects(List<String> allowedProjectIds)
      throws InvalidProtocolBufferException {
    List<Project> allowedProjects = getProjectsByBatchIds(allowedProjectIds);
    UserInfo unsignedUser = authService.getUnsignedUser();
    for (Project project : allowedProjects) {
      String projectId = project.getId();
      String ownerRoleBindingName =
          roleService.buildRoleBindingName(
              ModelDBConstants.ROLE_PROJECT_OWNER,
              projectId,
              project.getOwner(),
              ModelDBServiceResourceTypes.PROJECT.name());
      RoleBinding roleBinding = roleService.getRoleBindingByName(ownerRoleBindingName);
      if (roleBinding != null && !roleBinding.getId().isEmpty()) {
        roleService.deleteRoleBinding(roleBinding.getId());
      }

      if (project.getProjectVisibility().equals(ProjectVisibility.PUBLIC)) {
        String publicReadRoleBindingName =
            roleService.buildRoleBindingName(
                ModelDBConstants.ROLE_PROJECT_PUBLIC_READ,
                projectId,
                authService.getVertaIdFromUserInfo(unsignedUser),
                ModelDBServiceResourceTypes.PROJECT.name());
        RoleBinding publicReadRoleBinding =
            roleService.getRoleBindingByName(publicReadRoleBindingName);
        if (publicReadRoleBinding != null && !publicReadRoleBinding.getId().isEmpty()) {
          roleService.deleteRoleBinding(publicReadRoleBinding.getId());
        }
      }

      // Remove all project collaborators
      roleService.removeResourceRoleBindings(
          projectId, project.getOwner(), ModelDBServiceResourceTypes.PROJECT);

      // Delete workspace based roleBindings
      deleteWorkspaceRoleBindings(
          project.getWorkspaceId(),
          project.getWorkspaceType(),
          project.getId(),
          project.getProjectVisibility());
    }
  }

  private void deleteWorkspaceRoleBindings(
      String workspaceId,
      WorkspaceType workspaceType,
      String projectId,
      ProjectVisibility projectVisibility) {
    if (workspaceId != null && !workspaceId.isEmpty()) {
      switch (workspaceType) {
        case ORGANIZATION:
          Organization org = (Organization) roleService.getOrgById(workspaceId);
          String projectAdminRoleBindingName =
              roleService.buildRoleBindingName(
                  ModelDBConstants.ROLE_PROJECT_ADMIN,
                  projectId,
                  new CollaboratorUser(authService, org.getOwnerId()),
                  ModelDBServiceResourceTypes.PROJECT.name());
          RoleBinding projectAdminRoleBinding =
              roleService.getRoleBindingByName(projectAdminRoleBindingName);
          if (projectAdminRoleBinding != null && !projectAdminRoleBinding.getId().isEmpty()) {
            roleService.deleteRoleBinding(projectAdminRoleBinding.getId());
          }
          if (projectVisibility.equals(ProjectVisibility.ORG_SCOPED_PUBLIC)) {
            String globalSharingRoleName =
                new StringBuilder()
                    .append("O_")
                    .append(workspaceId)
                    .append("_GLOBAL_SHARING")
                    .toString();

            String globalSharingRoleBindingName =
                roleService.buildRoleBindingName(
                    globalSharingRoleName,
                    projectId,
                    new CollaboratorOrg(workspaceId),
                    ModelDBServiceResourceTypes.PROJECT.name());
            RoleBinding globalSharingRoleBinding =
                roleService.getRoleBindingByName(globalSharingRoleBindingName);
            if (globalSharingRoleBinding != null && !globalSharingRoleBinding.getId().isEmpty()) {
              roleService.deleteRoleBinding(globalSharingRoleBinding.getId());
            }
          }
          break;
        case USER:
          String projectRoleBindingName =
              roleService.buildRoleBindingName(
                  ModelDBConstants.ROLE_PROJECT_ADMIN,
                  projectId,
                  new CollaboratorUser(authService, workspaceId),
                  ModelDBServiceResourceTypes.PROJECT.name());
          RoleBinding projectRoleBinding = roleService.getRoleBindingByName(projectRoleBindingName);
          if (projectRoleBinding != null && !projectRoleBinding.getId().isEmpty()) {
            roleService.deleteRoleBinding(projectRoleBinding.getId());
          }
          break;
        default:
          break;
      }
    }
  }

  @Override
  public Boolean deleteProjects(List<String> projectIds) throws InvalidProtocolBufferException {

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

    // Remove roleBindings by accessible projects
    deleteRoleBindingsOfAccessibleProjects(allowedProjectIds);

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {

      deleteExperimentsWithPagination(session, allowedProjectIds);

      // Delete the ExperimentRunEntity object
      deleteExperimentRunsWithPagination(session, allowedProjectIds);

      Transaction transaction = session.beginTransaction();
      for (String projectId : allowedProjectIds) {
        ProjectEntity projectObj = session.load(ProjectEntity.class, projectId);
        session.delete(projectObj);
      }
      transaction.commit();
      LOGGER.debug("Project deleted successfully");
      return true;
    }
  }

  private void removeEntityComments(Session session, List<String> entityIds, String entityName) {
    Query commentDeleteQuery = session.createQuery(FIND_COMMENTS_HQL);
    commentDeleteQuery.setParameterList("entityIds", entityIds);
    commentDeleteQuery.setParameter("entityName", entityName);
    LOGGER.debug("Comments delete query : {}", commentDeleteQuery.getQueryString());
    List<CommentEntity> commentEntities = commentDeleteQuery.list();
    for (CommentEntity commentEntity : commentEntities) {
      session.delete(commentEntity);
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
    }
  }

  @Override
  public Project getProjectByID(String id) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", id);
      ProjectEntity projectEntity = (ProjectEntity) query.uniqueResult();
      if (projectEntity == null) {
        String errorMessage = "Project not found for given ID";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      return projectEntity.getProtoObject();
    }
  }

  @Override
  public Project setProjectShortName(String projectId, String projectShortName, UserInfo userInfo)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
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
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      return projectEntity.getProtoObject();
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
      Transaction transaction = session.beginTransaction();
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
        session.update(projectEntity);
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
      transaction.commit();
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      return projectEntity.getProtoObject();
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
        Role publicReadRole =
            roleService.getRoleByName(ModelDBConstants.ROLE_PROJECT_PUBLIC_READ, null);
        roleService.createRoleBinding(
            publicReadRole,
            new CollaboratorUser(authService, authService.getUnsignedUser()),
            projectId,
            ModelDBServiceResourceTypes.PROJECT);

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
            roleService.buildRoleBindingName(
                ModelDBConstants.ROLE_PROJECT_PUBLIC_READ,
                projectId,
                authService.getVertaIdFromUserInfo(authService.getUnsignedUser()),
                ModelDBServiceResourceTypes.PROJECT.name());
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
      Query query = session.createQuery(GET_PROJECT_BY_IDS_HQL);
      query.setParameterList("ids", projectIds);

      @SuppressWarnings("unchecked")
      List<ProjectEntity> projectEntities = query.list();
      LOGGER.debug("Project by Ids getting successfully");
      return RdbmsUtils.convertProjectsFromProjectEntityList(projectEntities);
    }
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
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          if (!predicate.getOperator().equals(OperatorEnum.Operator.EQ)) {
            Status statusMessage =
                Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT_VALUE)
                    .setMessage(ModelDBConstants.NON_EQ_ID_PRED_ERROR_MESSAGE)
                    .build();
            throw StatusProto.toStatusRuntimeException(statusMessage);
          }
          String projectId = predicate.getValue().getStringValue();
          if ((accessibleProjectIds.isEmpty() || !accessibleProjectIds.contains(projectId))
              && roleService.IsImplemented()) {
            Status statusMessage =
                Status.newBuilder()
                    .setCode(Code.PERMISSION_DENIED_VALUE)
                    .setMessage(
                        "Access is denied. User is unauthorized for given Project entity ID : "
                            + projectId)
                    .build();
            throw StatusProto.toStatusRuntimeException(statusMessage);
          }
        }

        if (predicate.getKey().equalsIgnoreCase(ModelDBConstants.WORKSPACE)
            || predicate.getKey().equalsIgnoreCase(ModelDBConstants.WORKSPACE_NAME)
            || predicate.getKey().equalsIgnoreCase(ModelDBConstants.WORKSPACE_TYPE)) {
          Status statusMessage =
              Status.newBuilder()
                  .setCode(Code.INVALID_ARGUMENT_VALUE)
                  .setMessage("Workspace name OR type not supported as predicate")
                  .build();
          throw StatusProto.toStatusRuntimeException(statusMessage);
        }
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
          UserInfo userInfo =
              host != null && host.isUser()
                  ? (UserInfo) host.getCollaboratorMessage()
                  : currentLoginUserInfo;
          if (userInfo != null) {
            List<KeyValueQuery> workspacePredicates =
                ModelDBUtils.getKeyValueQueriesByWorkspace(roleService, userInfo, workspaceName);
            if (workspacePredicates.size() > 0) {
              Predicate privateWorkspacePredicate =
                  builder.equal(
                      projectRoot.get(ModelDBConstants.WORKSPACE),
                      workspacePredicates.get(0).getValue().getStringValue());
              Predicate privateWorkspaceTypePredicate =
                  builder.equal(
                      projectRoot.get(ModelDBConstants.WORKSPACE_TYPE),
                      workspacePredicates.get(1).getValue().getNumberValue());
              Predicate privatePredicate =
                  builder.and(privateWorkspacePredicate, privateWorkspaceTypePredicate);

              finalPredicatesList.add(privatePredicate);
            }
          }
        }
      }

      if (!accessibleProjectIds.isEmpty()) {
        Expression<String> exp = projectRoot.get(ModelDBConstants.ID);
        Predicate predicate2 = exp.in(accessibleProjectIds);
        finalPredicatesList.add(predicate2);
      }

      String entityName = "projectEntity";
      List<Predicate> queryPredicatesList =
          RdbmsUtils.getQueryPredicatesFromPredicateList(
              entityName, predicates, builder, criteriaQuery, projectRoot);
      if (!queryPredicatesList.isEmpty()) {
        finalPredicatesList.addAll(queryPredicatesList);
      }

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
    }
  }

  @Override
  public Project logArtifacts(String projectId, List<Artifact> newArtifacts)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      Query query = session.createQuery(GET_PROJECT_BY_ID_HQL);
      query.setParameter("id", projectId);
      ProjectEntity projectEntity = (ProjectEntity) query.uniqueResult();
      if (projectEntity == null) {
        String errorMessage = "Project not found for given ID: " + projectId;
        LOGGER.warn(errorMessage);
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
      session.update(projectEntity);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.GETTING_PROJECT_BY_ID_MSG_STR);
      return projectEntity.getProtoObject();
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
        LOGGER.warn(errorMessage);
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
  }

  @Override
  public Project deleteArtifacts(String projectId, String artifactKey)
      throws InvalidProtocolBufferException {
    Transaction transaction = null;
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      transaction = session.beginTransaction();

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
    } catch (StatusRuntimeException ex) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw ex;
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
    }
  }

  @Override
  public Project setProjectWorkspace(String projectId, WorkspaceDTO workspaceDTO)
      throws InvalidProtocolBufferException {

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {

      Transaction transaction = session.beginTransaction();
      ProjectEntity projectEntity = session.load(ProjectEntity.class, projectId);
      deleteWorkspaceRoleBindings(
          projectEntity.getWorkspace(),
          WorkspaceType.forNumber(projectEntity.getWorkspace_type()),
          projectId,
          ProjectVisibility.forNumber(projectEntity.getProject_visibility()));
      createWorkspaceRoleBinding(
          workspaceDTO.getWorkspaceId(),
          workspaceDTO.getWorkspaceType(),
          projectId,
          ProjectVisibility.forNumber(projectEntity.getProject_visibility()));
      projectEntity.setWorkspace(workspaceDTO.getWorkspaceId());
      projectEntity.setWorkspace_type(workspaceDTO.getWorkspaceType().getNumber());
      projectEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      session.update(projectEntity);
      LOGGER.debug("Project workspace updated successfully");
      transaction.commit();
      return projectEntity.getProtoObject();
    }
  }
}
