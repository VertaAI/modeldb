package ai.verta.modeldb.versioning;

import static ai.verta.modeldb.metadata.IDTypeEnum.IDType.VERSIONING_REPOSITORY;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.dto.DatasetPaginationDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.versioning.*;
import ai.verta.modeldb.entities.versioning.RepositoryEnums.RepositoryTypeEnum;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.metadata.IdentificationType;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.persistence.criteria.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class RepositoryDAORdbImpl implements RepositoryDAO {

  private static final Logger LOGGER = LogManager.getLogger(RepositoryDAORdbImpl.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private static final String GLOBAL_SHARING = "_REPO_GLOBAL_SHARING";
  public static final String UNEXPECTED_ERROR_ON_REPOSITORY_ENTITY_CONVERSION_TO_PROTO =
      "Unexpected error on repository entity conversion to proto";
  private final AuthService authService;
  private final RoleService roleService;
  private final CommitDAO commitDAO;
  private final MetadataDAO metadataDAO;

  private static final String SHORT_NAME = "repo";

  private static final String GET_REPOSITORY_COUNT_BY_NAME_PREFIX_HQL =
      new StringBuilder("Select count(*) From ")
          .append(RepositoryEntity.class.getSimpleName())
          .append(" ")
          .append(SHORT_NAME)
          .append(" where ")
          .append(" ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.NAME)
          .append(" = :repositoryName ")
          .toString();

  private static final String GET_REPOSITORY_IDS_BY_NAME_HQL =
      new StringBuilder("SELECT ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.ID)
          .append(" FROM ")
          .append(RepositoryEntity.class.getSimpleName())
          .append(" ")
          .append(SHORT_NAME)
          .append(" where ")
          .append(" ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.NAME)
          .append(" = :repositoryName ")
          .append(" AND ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.DELETED)
          .append(" = false ")
          .append(" AND ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.CREATED)
          .append(" = true")
          .toString();

  private static final String GET_TAG_HQL =
      new StringBuilder("From ")
          .append(TagsEntity.class.getSimpleName())
          .append(" t ")
          .append(" where ")
          .append(" t.id.")
          .append(ModelDBConstants.REPOSITORY_ID)
          .append(" = :repositoryId ")
          .append(" AND t.id.")
          .append(ModelDBConstants.TAG)
          .append(" = :tag ")
          .toString();
  private static final String GET_TAGS_HQL =
      new StringBuilder("From TagsEntity te where te.id.")
          .append(ModelDBConstants.REPOSITORY_ID)
          .append(" = :repoId ")
          .toString();
  public static final String CHECK_BRANCH_IN_REPOSITORY_HQL =
      new StringBuilder("From ")
          .append(BranchEntity.class.getSimpleName())
          .append(" br ")
          .append(" where ")
          .append(" br.id.")
          .append(ModelDBConstants.REPOSITORY_ID)
          .append(" = :repositoryId ")
          .append(" AND br.id.")
          .append(ModelDBConstants.BRANCH)
          .append(" = :branch ")
          .toString();
  private static final String GET_REPOSITORY_BRANCHES_HQL =
      new StringBuilder("From ")
          .append(BranchEntity.class.getSimpleName())
          .append(" br where br.id.")
          .append(ModelDBConstants.REPOSITORY_ID)
          .append(" = :repoId ")
          .toString();
  private static final String DELETED_STATUS_REPOSITORY_QUERY_STRING =
      new StringBuilder("UPDATE ")
          .append(RepositoryEntity.class.getSimpleName())
          .append(" rp ")
          .append("SET rp.")
          .append(ModelDBConstants.DELETED)
          .append(" = :deleted ")
          .append(" WHERE rp.")
          .append(ModelDBConstants.ID)
          .append(" IN (:repoIds)")
          .toString();
  private static final String GET_REPOSITORY_BY_ID_HQL =
      new StringBuilder("From ")
          .append(RepositoryEntity.class.getSimpleName())
          .append(" ")
          .append(SHORT_NAME)
          .append(" where ")
          .append(" ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.ID)
          .append(" = :repoId ")
          .append(" AND ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.DELETED)
          .append(" = false ")
          .append(" AND ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.CREATED)
          .append(" = true ")
          .toString();
  private static final String GET_REPOSITORY_ATTRIBUTES_QUERY =
      new StringBuilder("From " + AttributeEntity.class.getSimpleName() + " attr where attr.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND attr.repositoryEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :repoId AND attr.field_type = :fieldType")
          .toString();
  private static final String DELETE_ALL_REPOSITORY_ATTRIBUTES_HQL =
      new StringBuilder("delete from AttributeEntity attr WHERE attr.repositoryEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :repoId")
          .toString();
  private static final String DELETE_SELECTED_REPOSITORY_ATTRIBUTES_HQL =
      new StringBuilder("delete from AttributeEntity attr WHERE attr.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND attr.repositoryEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :repoId")
          .toString();
  private static final String GET_DELETED_REPOSITORY_IDS_BY_NAME_HQL =
      new StringBuilder("SELECT ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.ID)
          .append(" FROM ")
          .append(RepositoryEntity.class.getSimpleName())
          .append(" ")
          .append(SHORT_NAME)
          .append(" where ")
          .append(" ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.NAME)
          .append(" = :name ")
          .append(" AND ")
          .append(SHORT_NAME)
          .append(".")
          .append(ModelDBConstants.DELETED)
          .append(" = true ")
          .toString();

  public RepositoryDAORdbImpl(
      AuthService authService,
      RoleService roleService,
      CommitDAO commitDAO,
      MetadataDAO metadataDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.commitDAO = commitDAO;
    this.metadataDAO = metadataDAO;
  }

  private void checkIfEntityAlreadyExists(
      Session session, Workspace workspace, String name, RepositoryTypeEnum repositoryType) {
    List<Long> repositoryEntityIds = getRepositoryEntityIdsByName(session, name, repositoryType);
    ModelDBServiceResourceTypes modelDBServiceResourceTypes =
        ModelDBServiceResourceTypes.REPOSITORY;
    if (repositoryType.equals(RepositoryTypeEnum.DATASET)) {
      modelDBServiceResourceTypes = ModelDBServiceResourceTypes.DATASET;
    }

    if (repositoryEntityIds != null && !repositoryEntityIds.isEmpty()) {
      ModelDBUtils.checkIfEntityAlreadyExists(
          roleService,
          workspace,
          name,
          repositoryEntityIds.stream().map(String::valueOf).collect(Collectors.toList()),
          modelDBServiceResourceTypes);
    }
  }

  private List<Long> getRepositoryEntityIdsByName(
      Session session, String name, RepositoryTypeEnum repositoryType) {
    StringBuilder getRepoCountByNamePrefixHQL = new StringBuilder(GET_REPOSITORY_IDS_BY_NAME_HQL);
    setRepositoryTypeInQueryBuilder(repositoryType, getRepoCountByNamePrefixHQL);
    Query query = session.createQuery(getRepoCountByNamePrefixHQL.toString());
    query.setParameter("repositoryName", name);
    List<Long> repositoryEntityIds = query.list();
    return repositoryEntityIds;
  }

  @Override
  public GetRepositoryRequest.Response getRepository(GetRepositoryRequest request)
      throws Exception {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getId());
      return GetRepositoryRequest.Response.newBuilder()
          .setRepository(
              repository.toProto(roleService, authService, new HashMap<>(), new HashMap<>()))
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getRepository(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public RepositoryEntity getRepositoryById(
      Session session, RepositoryIdentification id, boolean checkWrite)
      throws ModelDBException, ExecutionException, InterruptedException {
    return getRepositoryById(session, id, checkWrite, true, RepositoryTypeEnum.REGULAR);
  }

  /**
   * canNotOperateOnProtected if true forces the repo to be protected, throws not_found otherwise.
   */
  @Override
  public RepositoryEntity getRepositoryById(
      Session session,
      RepositoryIdentification id,
      boolean checkWrite,
      boolean canNotOperateOnProtected,
      RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException {
    return getRepositoryEntity(
        session, null, id, checkWrite, canNotOperateOnProtected, repositoryType);
  }

  private RepositoryEntity getRepositoryEntity(
      Session session,
      Workspace workspace,
      RepositoryIdentification id,
      boolean checkWrite,
      boolean canNotOperateOnProtected,
      RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException {
    RepositoryEntity repository;
    if (id.hasNamedId()) {
      if (workspace == null) {
        workspace =
            roleService.getWorkspaceByWorkspaceName(
                authService.getCurrentLoginUserInfo(), id.getNamedId().getWorkspaceName());
      }
      repository =
          getRepositoryByName(session, id.getNamedId().getName(), workspace, repositoryType)
              .orElseThrow(
                  () ->
                      new ModelDBException(
                          "Couldn't find repository by name " + id.getNamedId().getName(),
                          Code.NOT_FOUND));
    } else {
      repository =
          getRepositoryById(session, id.getRepoId())
              .orElseThrow(
                  () ->
                      new ModelDBException(
                          "Couldn't find repository by id " + id.getRepoId(), Code.NOT_FOUND));
    }
    try {
      if (canNotOperateOnProtected && repository.isProtected()) {
        throw new ModelDBException(
            "Can't access repository because it's protected", Code.PERMISSION_DENIED);
      }

      ModelDBServiceResourceTypes modelDBServiceResourceTypes =
          ModelDBUtils.getModelDBServiceResourceTypesFromRepository(repository);
      if (checkWrite) {
        roleService.validateEntityUserWithUserInfo(
            modelDBServiceResourceTypes,
            repository.getId().toString(),
            ModelDBServiceActions.UPDATE);
      } else {
        roleService.validateEntityUserWithUserInfo(
            modelDBServiceResourceTypes, repository.getId().toString(), ModelDBServiceActions.READ);
      }
    } catch (InvalidProtocolBufferException e) {
      LOGGER.info(e.getMessage());
      throw new ModelDBException("Unexpected error", e);
    }
    return repository;
  }

  @Override
  public RepositoryEntity getRepositoryById(Session session, RepositoryIdentification id)
      throws ModelDBException, ExecutionException, InterruptedException {
    return getRepositoryById(session, id, false);
  }

  @Override
  public RepositoryEntity getProtectedRepositoryById(
      RepositoryIdentification id, boolean checkWrite)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      return getRepositoryById(session, id, checkWrite, false, RepositoryTypeEnum.DATASET);
    }
  }

  private Optional<RepositoryEntity> getRepositoryById(Session session, long id) {
    Query query = session.createQuery(GET_REPOSITORY_BY_ID_HQL);
    query.setParameter("repoId", id);
    return Optional.ofNullable((RepositoryEntity) query.uniqueResult());
  }

  private Optional<RepositoryEntity> getRepositoryByName(
      Session session, String name, Workspace workspace, RepositoryTypeEnum repositoryType) {
    List<Long> repositoryIds = getRepositoryEntityIdsByName(session, name, repositoryType);
    // TODO: replace with the helper function
    ModelDBServiceResourceTypes modelDBServiceResourceTypes =
        ModelDBServiceResourceTypes.REPOSITORY;
    if (repositoryType.equals(RepositoryTypeEnum.DATASET)) {
      modelDBServiceResourceTypes = ModelDBServiceResourceTypes.DATASET;
    }
    List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
        roleService.getResourceItems(
            workspace,
            !repositoryIds.isEmpty()
                ? repositoryIds.stream().map(String::valueOf).collect(Collectors.toSet())
                : Collections.emptySet(),
            modelDBServiceResourceTypes);
    Set<Long> repoIds =
        accessibleAllWorkspaceItems.stream()
            .filter(
                getResourcesResponseItem -> getResourcesResponseItem.getResourceName().equals(name))
            .map(
                getResourcesResponseItem ->
                    Long.parseLong(getResourcesResponseItem.getResourceId()))
            .collect(Collectors.toSet());
    if (!repoIds.isEmpty()) {
      if (repoIds.size() > 1) {
        throw new InternalErrorException(
            "Multiple "
                + modelDBServiceResourceTypes
                + " found for the name '"
                + name
                + "' in the workspace");
      }
      return getRepositoryById(session, new ArrayList<>(repoIds).get(0));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public SetRepository.Response setRepository(
      SetRepository request, UserInfo userInfo, boolean create)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException,
          ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository =
          setRepository(
              session,
              null,
              request.getRepository(),
              request.getId(),
              null,
              userInfo,
              create,
              RepositoryTypeEnum.REGULAR);
      return SetRepository.Response.newBuilder()
          .setRepository(
              repository.toProto(roleService, authService, new HashMap<>(), new HashMap<>()))
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setRepository(request, userInfo, create);
      } else {
        throw ex;
      }
    }
  }

  private RepositoryEntity setRepository(
      Session session,
      String workspaceName,
      Repository repository,
      RepositoryIdentification repoId,
      List<String> tagList,
      UserInfo userInfo,
      boolean create,
      RepositoryTypeEnum repositoryType)
      throws ModelDBException, NoSuchAlgorithmException, InvalidProtocolBufferException,
          ExecutionException, InterruptedException {

    if (workspaceName == null && repoId.hasNamedId()) {
      workspaceName = repoId.getNamedId().getWorkspaceName();
    }

    RepositoryEntity repositoryEntity;
    boolean nameChanged = false;
    Workspace workspace = roleService.getWorkspaceByWorkspaceName(userInfo, workspaceName);
    if (create) {
      String name = repository.getName();
      if (name.isEmpty()) {
        throw new ModelDBException("Repository name should not be empty", Code.INVALID_ARGUMENT);
      }

      StringBuilder deletedQueryStringBuilder =
          new StringBuilder(GET_DELETED_REPOSITORY_IDS_BY_NAME_HQL);
      setRepositoryTypeInQueryBuilder(repositoryType, deletedQueryStringBuilder);
      Query deletedEntitiesQuery = session.createQuery(deletedQueryStringBuilder.toString());
      deletedEntitiesQuery.setParameter("name", name);
      List<Long> deletedEntityIds = deletedEntitiesQuery.list();
      if (!deletedEntityIds.isEmpty()) {
        roleService.deleteEntityResourcesWithServiceUser(
            deletedEntityIds.stream().map(String::valueOf).collect(Collectors.toList()),
            repositoryType.equals(RepositoryTypeEnum.DATASET)
                ? ModelDBServiceResourceTypes.DATASET
                : ModelDBServiceResourceTypes.REPOSITORY);
      }

      repositoryEntity = new RepositoryEntity(repository, repositoryType);
    } else {
      repositoryEntity =
          getRepositoryEntity(session, workspace, repoId, true, false, repositoryType);
      session.lock(repositoryEntity, LockMode.PESSIMISTIC_WRITE);
      if (!repository.getName().isEmpty()
          && !repositoryEntity.getName().equals(repository.getName())) {
        // TODO: Remove this after UAC support update entity name using SetResource
        checkIfEntityAlreadyExists(session, workspace, repository.getName(), repositoryType);
        nameChanged = true;
      }
      repositoryEntity.update(repository);
    }
    session.beginTransaction();
    session.saveOrUpdate(repositoryEntity);
    if (create) {
      Commit initCommit =
          Commit.newBuilder().setMessage(ModelDBConstants.INITIAL_COMMIT_MESSAGE).build();
      CommitEntity commitEntity =
          commitDAO.saveCommitEntity(
              session,
              initCommit,
              FileHasher.getSha(new String()),
              authService.getVertaIdFromUserInfo(userInfo),
              repositoryEntity);

      saveBranch(
          session, commitEntity.getCommit_hash(), ModelDBConstants.MASTER_BRANCH, repositoryEntity);

      if (tagList != null && !tagList.isEmpty()) {
        metadataDAO.addLabels(
            session,
            IdentificationType.newBuilder()
                .setIdType(VERSIONING_REPOSITORY)
                .setIntId(repositoryEntity.getId())
                .build(),
            tagList);
      }
    }
    session.getTransaction().commit();
    if (create || nameChanged) {
      try {
        ResourceVisibility resourceVisibility = repository.getVisibility();
        if (repository.getVisibility().equals(ResourceVisibility.UNKNOWN)) {
          resourceVisibility =
              ModelDBUtils.getResourceVisibility(
                  Optional.of(workspace), repository.getRepositoryVisibility());
        }
        ModelDBServiceResourceTypes modelDBServiceResourceTypes =
            ModelDBUtils.getModelDBServiceResourceTypesFromRepository(repositoryEntity);
        roleService.createWorkspacePermissions(
            Optional.of(workspace.getId()),
            Optional.empty(),
            String.valueOf(repositoryEntity.getId()),
            repositoryEntity.getName(),
            Optional.empty(),
            modelDBServiceResourceTypes,
            repository.getCustomPermission(),
            resourceVisibility);
        LOGGER.debug("Repository role bindings created successfully");
        Transaction transaction = session.beginTransaction();
        repositoryEntity.setCreated(true);
        repositoryEntity.setVisibility_migration(true);
        transaction.commit();
      } catch (Exception e) {
        LOGGER.info("Exception from UAC during Repo role binding creation : {}", e.getMessage());
        LOGGER.info("Deleting the created repository {}", repository.getId());
        // delete the repo created
        session.beginTransaction();
        repositoryEntity.setDeleted(true);
        session.update(repositoryEntity);
        session.getTransaction().commit();
        throw e;
      }
    }

    return repositoryEntity;
  }

  private void setRepositoryTypeInQueryBuilder(
      RepositoryTypeEnum repositoryType, StringBuilder getRepoCountByNamePrefixHQL) {
    getRepoCountByNamePrefixHQL.append(" AND ").append(SHORT_NAME).append(".");
    if (repositoryType.equals(RepositoryTypeEnum.DATASET)) {
      getRepoCountByNamePrefixHQL.append("datasetRepositoryMappingEntity IS NOT EMPTY ");
    } else {
      getRepoCountByNamePrefixHQL.append("datasetRepositoryMappingEntity IS EMPTY ");
    }
  }

  @Override
  public DeleteRepositoryRequest.Response deleteRepository(
      DeleteRepositoryRequest request,
      CommitDAO commitDAO,
      ExperimentRunDAO experimentRunDAO,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository =
          getRepositoryById(
              session, request.getRepositoryId(), true, canNotOperateOnProtected, repositoryType);
      // Get self allowed resources id where user has delete permission
      ModelDBServiceResourceTypes modelDBServiceResourceTypes =
          ModelDBUtils.getModelDBServiceResourceTypesFromRepository(repository);
      List<String> allowedRepositoryIds =
          roleService.getAccessibleResourceIdsByActions(
              modelDBServiceResourceTypes,
              ModelDBServiceActions.DELETE,
              Collections.singletonList(String.valueOf(repository.getId())));
      if (allowedRepositoryIds.isEmpty()) {
        throw new ModelDBException(
            "Delete Access Denied for given repository Id : " + request.getRepositoryId(),
            Code.PERMISSION_DENIED);
      }

      deleteRepositories(session, experimentRunDAO, allowedRepositoryIds);
      return DeleteRepositoryRequest.Response.newBuilder().setStatus(true).build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteRepository(
            request, commitDAO, experimentRunDAO, canNotOperateOnProtected, repositoryType);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void deleteRepositories(
      Session session, ExperimentRunDAO experimentRunDAO, List<String> allowedRepositoryIds) {
    Query deletedRepositoriesQuery =
        session
            .createQuery(DELETED_STATUS_REPOSITORY_QUERY_STRING)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    deletedRepositoriesQuery.setParameter("deleted", true);
    final List<Long> repositoriesIdsLong =
        allowedRepositoryIds.stream().map(Long::valueOf).collect(Collectors.toList());
    deletedRepositoriesQuery.setParameter("repoIds", repositoriesIdsLong);
    Transaction transaction = session.beginTransaction();
    int updatedCount = deletedRepositoriesQuery.executeUpdate();
    LOGGER.debug(
        "Mark Repositories as deleted : {}, count : {}", allowedRepositoryIds, updatedCount);
    // Delete all VersionedInputs for repository ID
    experimentRunDAO.deleteLogVersionedInputs(session, repositoriesIdsLong);
    transaction.commit();
  }

  @Override
  public Boolean deleteRepositories(List<String> repositoryIds, ExperimentRunDAO experimentRunDAO)
      throws ModelDBException {
    List<String> allowedRepositoryIds =
        roleService.getAccessibleResourceIdsByActions(
            ModelDBServiceResourceTypes.REPOSITORY,
            ModelDBServiceActions.DELETE,
            Collections.singletonList(String.valueOf(repositoryIds)));
    if (allowedRepositoryIds.isEmpty()) {
      throw new ModelDBException(
          "Delete Access Denied for given repository Ids : " + repositoryIds,
          Code.PERMISSION_DENIED);
    }
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      deleteRepositories(session, experimentRunDAO, allowedRepositoryIds);
    }
    return true;
  }

  @Override
  public Dataset createOrUpdateDataset(
      Dataset dataset, String workspaceName, boolean create, UserInfo userInfo)
      throws ModelDBException, NoSuchAlgorithmException, InvalidProtocolBufferException,
          ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryIdentification.Builder repositoryIdBuilder = RepositoryIdentification.newBuilder();
      if (dataset.getId().isEmpty()) {
        repositoryIdBuilder.setNamedId(
            RepositoryNamedIdentification.newBuilder()
                .setName(dataset.getName())
                .setWorkspaceName(workspaceName)
                .build());
      } else {
        repositoryIdBuilder.setRepoId(Long.parseLong(dataset.getId()));
      }
      Repository repository =
          createDatasetRepository(
              session, dataset, repositoryIdBuilder.build(), workspaceName, create, userInfo);
      return repositoryToDataset(session, metadataDAO, repository);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return createOrUpdateDataset(dataset, workspaceName, create, userInfo);
      } else {
        throw ex;
      }
    }
  }

  private Repository createDatasetRepository(
      Session session,
      Dataset dataset,
      RepositoryIdentification repositoryId,
      String workspaceName,
      boolean create,
      UserInfo userInfo)
      throws NoSuchAlgorithmException, ModelDBException, InvalidProtocolBufferException,
          ExecutionException, InterruptedException {
    Repository.Builder datasetRepositoryBuilder =
        Repository.newBuilder()
            .setRepositoryVisibility(
                RepositoryVisibilityEnum.RepositoryVisibility.forNumber(
                    dataset.getDatasetVisibilityValue()))
            .setVisibility(dataset.getVisibility())
            .setDateCreated(dataset.getTimeCreated())
            .setDateUpdated(dataset.getTimeUpdated())
            .setName(dataset.getName())
            .setDescription(dataset.getDescription())
            .setOwner(dataset.getOwner())
            .addAllAttributes(dataset.getAttributesList())
            .setCustomPermission(dataset.getCustomPermission());

    if (!dataset.getId().isEmpty()) {
      datasetRepositoryBuilder.setId(Long.parseLong(dataset.getId()));
    }

    RepositoryEntity repositoryEntity =
        setRepository(
            session,
            workspaceName,
            datasetRepositoryBuilder.build(),
            repositoryId,
            dataset.getTagsList(),
            userInfo,
            create,
            RepositoryTypeEnum.DATASET);

    return repositoryEntity.toProto(roleService, authService, new HashMap<>(), new HashMap<>());
  }

  Dataset convertToDataset(
      Session session,
      MetadataDAO metadataDAO,
      RepositoryEntity repositoryEntity,
      Map<Long, Workspace> cacheWorkspaceMap,
      Map<String, GetResourcesResponseItem> getResourcesMap)
      throws ModelDBException, InvalidProtocolBufferException {

    Repository repository =
        repositoryEntity.toProto(roleService, authService, cacheWorkspaceMap, getResourcesMap);
    return repositoryToDataset(session, metadataDAO, repository);
  }

  private Dataset repositoryToDataset(
      Session session, MetadataDAO metadataDAO, Repository repository) throws ModelDBException {
    Dataset.Builder dataset = Dataset.newBuilder();
    dataset.setId(String.valueOf(repository.getId()));

    dataset
        .setVisibility(repository.getVisibility())
        .setWorkspaceType(repository.getWorkspaceType())
        .setWorkspaceId(repository.getWorkspaceId())
        .setWorkspaceServiceId(repository.getWorkspaceServiceId())
        .setTimeCreated(repository.getDateCreated())
        .setTimeUpdated(repository.getDateUpdated())
        .setName(repository.getName())
        .setDescription(repository.getDescription())
        .setOwner(repository.getOwner())
        .setCustomPermission(repository.getCustomPermission())
        .setDatasetVisibility(
            DatasetVisibilityEnum.DatasetVisibility.forNumber(
                repository.getRepositoryVisibilityValue()));
    dataset.addAllAttributes(repository.getAttributesList());
    try (Session session1 = modelDBHibernateUtil.getSessionFactory().openSession()) {
      List<String> tags =
          metadataDAO.getLabels(
              session1,
              IdentificationType.newBuilder()
                  .setIdType(VERSIONING_REPOSITORY)
                  .setIntId(repository.getId())
                  .build());
      dataset.addAllTags(tags);
      return dataset.build();
    }
  }

  @Override
  public ListRepositoriesRequest.Response listRepositories(
      ListRepositoriesRequest request, UserInfo currentLoginUserInfo)
      throws ModelDBException, InvalidProtocolBufferException, ExecutionException,
          InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<RepositoryEntity> criteriaQuery =
          criteriaBuilder.createQuery(RepositoryEntity.class);
      Root<RepositoryEntity> repositoryEntityRoot = criteriaQuery.from(RepositoryEntity.class);
      repositoryEntityRoot.alias(SHORT_NAME);
      List<Predicate> finalPredicatesList = new ArrayList<>();

      Set<String> accessibleResourceIds;
      Map<String, GetResourcesResponseItem> getResourcesMap = new HashMap<>();
      String workspaceName = request.getWorkspaceName();
      if (!workspaceName.isEmpty()
          && workspaceName.equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
        List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
            roleService.getResourceItems(
                null, Collections.emptySet(), ModelDBServiceResourceTypes.REPOSITORY);
        accessibleResourceIds =
            accessibleAllWorkspaceItems.stream()
                .peek(
                    responseItem -> getResourcesMap.put(responseItem.getResourceId(), responseItem))
                .map(GetResourcesResponseItem::getResourceId)
                .collect(Collectors.toSet());

        List<String> orgWorkspaceIds =
            roleService.listMyOrganizations().stream()
                .map(Organization::getWorkspaceId)
                .collect(Collectors.toList());
        for (GetResourcesResponseItem item : accessibleAllWorkspaceItems) {
          if (orgWorkspaceIds.contains(String.valueOf(item.getWorkspaceId()))) {
            accessibleResourceIds.remove(item.getResourceId());
          }
        }
      } else {
        Workspace workspace =
            roleService.getWorkspaceByWorkspaceName(currentLoginUserInfo, workspaceName);
        List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
            roleService.getResourceItems(
                workspace, Collections.emptySet(), ModelDBServiceResourceTypes.REPOSITORY);
        accessibleResourceIds =
            accessibleAllWorkspaceItems.stream()
                .peek(
                    responseItem -> getResourcesMap.put(responseItem.getResourceId(), responseItem))
                .map(GetResourcesResponseItem::getResourceId)
                .collect(Collectors.toSet());
      }

      if (accessibleResourceIds.isEmpty() && roleService.IsImplemented()) {
        LOGGER.debug("Accessible Repository Ids not found, size 0");
        return ListRepositoriesRequest.Response.newBuilder().setTotalRecords(0).build();
      }

      if (!accessibleResourceIds.isEmpty()) {
        Expression<String> exp = repositoryEntityRoot.get(ModelDBConstants.ID);
        Predicate predicate2 = exp.in(accessibleResourceIds);
        finalPredicatesList.add(predicate2);
      }

      finalPredicatesList.add(
          criteriaBuilder.equal(repositoryEntityRoot.get(ModelDBConstants.DELETED), false));
      finalPredicatesList.add(
          criteriaBuilder.equal(
              repositoryEntityRoot.get(ModelDBConstants.REPOSITORY_ACCESS_MODIFIER),
              RepositoryEnums.RepositoryModifierEnum.REGULAR.ordinal()));

      Order orderBy = criteriaBuilder.desc(repositoryEntityRoot.get(ModelDBConstants.DATE_UPDATED));

      Predicate[] predicateArr = new Predicate[finalPredicatesList.size()];
      for (int index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      Predicate predicateWhereCause = criteriaBuilder.and(predicateArr);
      criteriaQuery.select(repositoryEntityRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      Query query = session.createQuery(criteriaQuery);
      LOGGER.debug("Repository final query : {}", query.getQueryString());

      if (request.hasPagination()) {
        // Calculate number of documents to skip
        int pageLimit = request.getPagination().getPageLimit();
        query.setFirstResult((request.getPagination().getPageNumber() - 1) * pageLimit);
        query.setMaxResults(pageLimit);
      }

      List<RepositoryEntity> repositoryEntities = query.list();
      ListRepositoriesRequest.Response.Builder builder =
          ListRepositoriesRequest.Response.newBuilder();

      List<Repository> repositories = new ArrayList<>(repositoryEntities.size());
      Map<Long, Workspace> cacheWorkspaceMap = new HashMap<>();
      for (RepositoryEntity repositoryEntity : repositoryEntities) {
        repositories.add(
            repositoryEntity.toProto(roleService, authService, cacheWorkspaceMap, getResourcesMap));
      }
      builder.addAllRepositories(repositories);

      long totalRecords = RdbmsUtils.count(session, repositoryEntityRoot, criteriaQuery);
      builder.setTotalRecords(totalRecords);
      return builder.build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return listRepositories(request, currentLoginUserInfo);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public SetTagRequest.Response setTag(SetTagRequest request)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId(), true);
      session.lock(repository, LockMode.PESSIMISTIC_WRITE);

      boolean exists =
          VersioningUtils.commitRepositoryMappingExists(
              session, request.getCommitSha(), repository.getId());
      if (!exists) {
        throw new ModelDBException(
            "Commit_hash and repository_id mapping not found for repository "
                + repository.getId()
                + " commit "
                + " request.getCommitSha()",
            Code.NOT_FOUND);
      }

      Query query = session.createQuery(GET_TAG_HQL);
      query.setParameter("repositoryId", repository.getId());
      query.setParameter("tag", request.getTag());
      TagsEntity tagsEntity = (TagsEntity) query.uniqueResult();
      if (tagsEntity != null) {
        throw new ModelDBException("Tag '" + request.getTag() + "' already exists", Code.NOT_FOUND);
      }

      tagsEntity = new TagsEntity(repository.getId(), request.getCommitSha(), request.getTag());
      session.beginTransaction();
      session.save(tagsEntity);
      session.getTransaction().commit();
      return SetTagRequest.Response.newBuilder().build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setTag(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public GetTagRequest.Response getTag(GetTagRequest request)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      Query query = session.createQuery(GET_TAG_HQL);
      query.setParameter("repositoryId", repository.getId());
      query.setParameter("tag", request.getTag());
      TagsEntity tagsEntity = (TagsEntity) query.uniqueResult();
      if (tagsEntity == null) {
        throw new ModelDBException("Tag not found " + request.getTag(), Code.NOT_FOUND);
      }

      CommitEntity commitEntity = session.get(CommitEntity.class, tagsEntity.getCommit_hash());
      return GetTagRequest.Response.newBuilder().setCommit(commitEntity.toCommitProto()).build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getTag(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DeleteTagRequest.Response deleteTag(DeleteTagRequest request)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId(), true);
      TagsEntity tagsEntity =
          session.get(
              TagsEntity.class,
              new TagsEntity.TagId(request.getTag(), repository.getId()),
              LockMode.PESSIMISTIC_WRITE);
      if (tagsEntity == null) {
        throw new ModelDBException("Tag not found " + request.getTag(), Code.NOT_FOUND);
      }
      session.beginTransaction();
      session.delete(tagsEntity);
      session.getTransaction().commit();
      return DeleteTagRequest.Response.newBuilder().build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteTag(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ListTagsRequest.Response listTags(ListTagsRequest request)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      Query query = session.createQuery(GET_TAGS_HQL);
      query.setParameter("repoId", repository.getId());
      List<TagsEntity> tagsEntities = query.list();

      if (tagsEntities == null || tagsEntities.isEmpty()) {
        return ListTagsRequest.Response.newBuilder().setTotalRecords(0).build();
      }

      session.getTransaction().commit();
      List<String> tags =
          tagsEntities.stream()
              .map(tagsEntity -> tagsEntity.getId().getTag())
              .collect(Collectors.toList());
      return ListTagsRequest.Response.newBuilder()
          .addAllTags(tags)
          .setTotalRecords(tags.size())
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return listTags(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public SetBranchRequest.Response setBranch(
      SetBranchRequest request,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository =
          getRepositoryById(
              session, request.getRepositoryId(), true, canNotOperateOnProtected, repositoryType);

      session.beginTransaction();
      session.lock(repository, LockMode.PESSIMISTIC_READ);
      saveBranch(session, request.getCommitSha(), request.getBranch(), repository);
      session.getTransaction().commit();
      return SetBranchRequest.Response.newBuilder().build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setBranch(request, canNotOperateOnProtected, repositoryType);
      } else {
        throw ex;
      }
    }
  }

  private void saveBranch(
      Session session, String commitSHA, String branch, RepositoryEntity repository)
      throws ModelDBException {
    ModelDBUtils.validateEntityNameWithColonAndSlash(branch);
    boolean exists =
        VersioningUtils.commitRepositoryMappingExists(session, commitSHA, repository.getId());
    if (!exists) {
      throw new ModelDBException(
          "Commit_hash and repository_id mapping not found for repository "
              + repository.getId()
              + " and commit "
              + commitSHA,
          Code.NOT_FOUND);
    }

    Query query =
        session
            .createQuery(CHECK_BRANCH_IN_REPOSITORY_HQL)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    query.setParameter("repositoryId", repository.getId());
    query.setParameter("branch", branch);
    BranchEntity branchEntity = (BranchEntity) query.uniqueResult();
    if (branchEntity != null) {
      if (branchEntity.getCommit_hash().equals(commitSHA)) return;
      session.delete(branchEntity);
    }

    branchEntity = new BranchEntity(repository.getId(), commitSHA, branch);
    session.save(branchEntity);
  }

  @Override
  public BranchEntity getBranchEntity(Session session, Long repoId, String branchName)
      throws ModelDBException {
    Query query = session.createQuery(CHECK_BRANCH_IN_REPOSITORY_HQL);
    query.setParameter("repositoryId", repoId);
    query.setParameter("branch", branchName);
    BranchEntity branchEntity = (BranchEntity) query.uniqueResult();
    if (branchEntity == null) {
      throw new ModelDBException(ModelDBConstants.BRANCH_NOT_FOUND, Code.NOT_FOUND);
    }
    return branchEntity;
  }

  @Override
  public GetBranchRequest.Response getBranch(
      GetBranchRequest request,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository =
          getRepositoryById(
              session, request.getRepositoryId(), false, canNotOperateOnProtected, repositoryType);

      BranchEntity branchEntity = getBranchEntity(session, repository.getId(), request.getBranch());
      CommitEntity commitEntity = session.get(CommitEntity.class, branchEntity.getCommit_hash());
      return GetBranchRequest.Response.newBuilder().setCommit(commitEntity.toCommitProto()).build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getBranch(request, canNotOperateOnProtected, repositoryType);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DeleteBranchRequest.Response deleteBranch(DeleteBranchRequest request)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId(), true);
      BranchEntity branchEntity =
          session.get(
              BranchEntity.class,
              new BranchEntity.BranchId(request.getBranch(), repository.getId()),
              LockMode.PESSIMISTIC_WRITE);
      if (branchEntity == null) {
        throw new ModelDBException(
            ModelDBConstants.BRANCH_NOT_FOUND + request.getBranch(), Code.NOT_FOUND);
      }
      session.beginTransaction();
      session.delete(branchEntity);
      session.getTransaction().commit();
      return DeleteBranchRequest.Response.newBuilder().build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteBranch(request);
      } else {
        throw ex;
      }
    }
  }

  public void deleteBranchByCommit(Session session, Long repoId, String commitHash) {
    StringBuilder deleteBranchesHQLBuilder =
        new StringBuilder("DELETE FROM ")
            .append(BranchEntity.class.getSimpleName())
            .append(" br where br.id.repository_id = :repositoryId ")
            .append(" AND br.commit_hash = :commitHash ");
    Query deleteBranchQuery =
        session
            .createQuery(deleteBranchesHQLBuilder.toString())
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    deleteBranchQuery.setParameter("repositoryId", repoId);
    deleteBranchQuery.setParameter("commitHash", commitHash);
    deleteBranchQuery.executeUpdate();
  }

  @Override
  public ListBranchesRequest.Response listBranches(ListBranchesRequest request)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      Query query = session.createQuery(GET_REPOSITORY_BRANCHES_HQL);
      query.setParameter("repoId", repository.getId());
      List<BranchEntity> branchEntities = query.list();

      if (branchEntities == null || branchEntities.isEmpty()) {
        return ListBranchesRequest.Response.newBuilder().setTotalRecords(0).build();
      }

      List<String> branches =
          branchEntities.stream()
              .map(branchEntity -> branchEntity.getId().getBranch())
              .collect(Collectors.toList());
      return ListBranchesRequest.Response.newBuilder()
          .addAllBranches(branches)
          .setTotalRecords(branches.size())
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return listBranches(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ListCommitsLogRequest.Response listCommitsLog(ListCommitsLogRequest request)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      String referenceCommit;

      if (!request.getBranch().isEmpty()) {
        Query query = session.createQuery(CHECK_BRANCH_IN_REPOSITORY_HQL);
        query.setParameter("repositoryId", repository.getId());
        query.setParameter("branch", request.getBranch());
        BranchEntity branchEntity = (BranchEntity) query.uniqueResult();
        if (branchEntity == null) {
          throw new ModelDBException(
              ModelDBConstants.BRANCH_NOT_FOUND + request.getBranch(), Code.NOT_FOUND);
        }
        referenceCommit = branchEntity.getCommit_hash();
      } else {
        CommitEntity commit = session.get(CommitEntity.class, request.getCommitSha());
        if (commit == null) {
          throw new ModelDBException(
              ModelDBConstants.COMMIT_NOT_FOUND + request.getCommitSha(), Code.NOT_FOUND);
        }
        referenceCommit = commit.getCommit_hash();
      }
      // list of commits to be used in the in clause in the final query
      Set<String> commitSHAs = new HashSet<>();
      // List of commits to be traversed
      List<String> childCommitSHAs = new LinkedList<>();
      childCommitSHAs.add(referenceCommit);
      String getParentCommitsQuery = "SELECT parent_hash FROM commit_parent WHERE child_hash = \'";

      while (!childCommitSHAs.isEmpty()) {
        String childCommit = childCommitSHAs.remove(0);
        commitSHAs.add(childCommit);
        Query sqlQuery = session.createSQLQuery(getParentCommitsQuery + childCommit + "\'");
        List<String> parentCommitSHAs = sqlQuery.list();
        childCommitSHAs.addAll(parentCommitSHAs);
      }

      String getChildCommits =
          "FROM "
              + CommitEntity.class.getSimpleName()
              + " c WHERE c.commit_hash IN (:childCommitSHAs)  ORDER BY c.date_created DESC";
      Query query = session.createQuery(getChildCommits);
      query.setParameterList("childCommitSHAs", commitSHAs);
      List<CommitEntity> commits = query.list();

      return ListCommitsLogRequest.Response.newBuilder()
          .addAllCommits(
              commits.stream().map(CommitEntity::toCommitProto).collect(Collectors.toList()))
          .setTotalRecords(commits.size())
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return listCommitsLog(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public FindRepositories.Response findRepositories(FindRepositories request)
      throws ModelDBException, InvalidProtocolBufferException, ExecutionException,
          InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();
      try {
        Set<String> accessibleResourceIdsWithCollaborator =
            new HashSet<>(
                roleService.getAccessibleResourceIds(
                    null,
                    new CollaboratorUser(authService, currentLoginUserInfo),
                    ModelDBServiceResourceTypes.REPOSITORY,
                    request.getRepoIdsList().stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList())));

        Map<String, GetResourcesResponseItem> getResourcesMap = new HashMap<>();
        String workspaceName = request.getWorkspaceName();
        if (!workspaceName.isEmpty()
            && workspaceName.equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
          List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
              roleService.getResourceItems(
                  null,
                  accessibleResourceIdsWithCollaborator,
                  ModelDBServiceResourceTypes.REPOSITORY);
          accessibleResourceIdsWithCollaborator =
              accessibleAllWorkspaceItems.stream()
                  .peek(
                      responseItem ->
                          getResourcesMap.put(responseItem.getResourceId(), responseItem))
                  .map(GetResourcesResponseItem::getResourceId)
                  .collect(Collectors.toSet());

          List<String> orgWorkspaceIds =
              roleService.listMyOrganizations().stream()
                  .map(Organization::getWorkspaceId)
                  .collect(Collectors.toList());
          for (GetResourcesResponseItem item : accessibleAllWorkspaceItems) {
            if (orgWorkspaceIds.contains(String.valueOf(item.getWorkspaceId()))) {
              accessibleResourceIdsWithCollaborator.remove(item.getResourceId());
            }
          }
        } else {
          Workspace workspace =
              roleService.getWorkspaceByWorkspaceName(currentLoginUserInfo, workspaceName);
          List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
              roleService.getResourceItems(
                  workspace,
                  accessibleResourceIdsWithCollaborator,
                  ModelDBServiceResourceTypes.REPOSITORY);
          accessibleResourceIdsWithCollaborator =
              accessibleAllWorkspaceItems.stream()
                  .peek(
                      responseItem ->
                          getResourcesMap.put(responseItem.getResourceId(), responseItem))
                  .map(GetResourcesResponseItem::getResourceId)
                  .collect(Collectors.toSet());
        }

        if (accessibleResourceIdsWithCollaborator.isEmpty() && roleService.IsImplemented()) {
          LOGGER.debug("Accessible Repository Ids not found, size 0");
          return FindRepositories.Response.newBuilder()
              .addAllRepositories(Collections.emptyList())
              .setTotalRecords(0L)
              .build();
        }

        for (KeyValueQuery predicate : request.getPredicatesList()) {
          // Validate if current user has access to the entity or not where predicate key has an id
          RdbmsUtils.validatePredicates(
              ModelDBConstants.REPOSITORY,
              new ArrayList<>(accessibleResourceIdsWithCollaborator),
              predicate,
              roleService);
        }

        FindRepositoriesQuery findRepositoriesQuery =
            new FindRepositoriesQuery.FindRepositoriesHQLQueryBuilder(
                    session, authService, roleService)
                .setRepoIds(
                    accessibleResourceIdsWithCollaborator.stream()
                        .map(Long::valueOf)
                        .collect(Collectors.toList()))
                .setPredicates(request.getPredicatesList())
                .setPageLimit(request.getPageLimit())
                .setPageNumber(request.getPageNumber())
                .build();
        List<RepositoryEntity> repositoryEntities =
            findRepositoriesQuery.getFindRepositoriesHQLQuery().list();
        Long totalRecords =
            (Long) findRepositoriesQuery.getFindRepositoriesCountHQLQuery().uniqueResult();
        LOGGER.debug("Final return Repositories, size {}", repositoryEntities.size());
        LOGGER.debug("Final return Total Records: {}", totalRecords);

        List<Repository> repositories = new ArrayList<>();
        Map<Long, Workspace> cacheWorkspaceMap = new HashMap<>();
        for (RepositoryEntity repositoryEntity : repositoryEntities) {
          repositories.add(
              repositoryEntity.toProto(
                  roleService, authService, cacheWorkspaceMap, getResourcesMap));
        }

        return FindRepositories.Response.newBuilder()
            .addAllRepositories(repositories)
            .setTotalRecords(totalRecords)
            .build();
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == com.google.rpc.Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          return FindRepositories.Response.newBuilder()
              .addAllRepositories(Collections.emptyList())
              .setTotalRecords(0L)
              .build();
        }
        throw ex;
      }
    } catch (IllegalArgumentException ex) {
      throw ModelDBUtils.getInvalidFieldException(ex);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findRepositories(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public AddDatasetTags.Response addDatasetTags(
      MetadataDAO metadataDAO, String id, List<String> tags)
      throws ModelDBException, InvalidProtocolBufferException, ExecutionException,
          InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder().setRepoId(Long.parseLong(id)).build();
      addRepositoryTags(
          metadataDAO, repositoryIdentification, tags, false, RepositoryTypeEnum.DATASET);
      return AddDatasetTags.Response.newBuilder()
          .setDataset(
              convertToDataset(
                  session,
                  metadataDAO,
                  getRepositoryById(
                      session, repositoryIdentification, true, false, RepositoryTypeEnum.DATASET),
                  new HashMap<>(),
                  new HashMap<>()))
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addDatasetTags(metadataDAO, id, tags);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void addRepositoryTags(
      MetadataDAO metadataDAO,
      RepositoryIdentification repositoryIdentification,
      List<String> tags,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      RepositoryEntity repositoryEntity =
          getRepositoryById(
              session, repositoryIdentification, true, canNotOperateOnProtected, repositoryType);
      session.lock(repositoryEntity, LockMode.PESSIMISTIC_WRITE);
      repositoryEntity.update();
      List<String> tagsOld =
          metadataDAO.getLabels(
              session,
              IdentificationType.newBuilder()
                  .setIdType(VERSIONING_REPOSITORY)
                  .setIntId(repositoryIdentification.getRepoId())
                  .build());
      List<String> uniqueTags = new ArrayList<>(tags);
      uniqueTags.removeAll(tagsOld);
      metadataDAO.addLabels(
          session,
          IdentificationType.newBuilder()
              .setIdType(VERSIONING_REPOSITORY)
              .setIntId(repositoryIdentification.getRepoId())
              .build(),
          uniqueTags);
      transaction.commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        addRepositoryTags(
            metadataDAO, repositoryIdentification, tags, canNotOperateOnProtected, repositoryType);
      } else {
        throw ex;
      }
    }
  }

  public DatasetPaginationDTO findDatasets(
      MetadataDAO metadataDAO,
      FindDatasets queryParameters,
      UserInfo currentLoginUserInfo,
      ResourceVisibility resourceVisibility)
      throws InvalidProtocolBufferException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      CriteriaBuilder builder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<RepositoryEntity> criteriaQuery = builder.createQuery(RepositoryEntity.class);
      Root<RepositoryEntity> repositoryRoot = criteriaQuery.from(RepositoryEntity.class);
      repositoryRoot.alias("ds");

      Set<String> accessibleDatasetIds;
      String workspaceName = queryParameters.getWorkspaceName();
      Map<String, GetResourcesResponseItem> getResourcesMap = new HashMap<>();
      if (!workspaceName.isEmpty()
          && workspaceName.equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
        List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
            roleService.getResourceItems(
                null,
                !queryParameters.getDatasetIdsList().isEmpty()
                    ? new HashSet<>(queryParameters.getDatasetIdsList())
                    : Collections.emptySet(),
                ModelDBServiceResourceTypes.DATASET);
        accessibleDatasetIds =
            accessibleAllWorkspaceItems.stream()
                .peek(
                    responseItem -> getResourcesMap.put(responseItem.getResourceId(), responseItem))
                .map(GetResourcesResponseItem::getResourceId)
                .collect(Collectors.toSet());

        List<String> orgWorkspaceIds =
            roleService.listMyOrganizations().stream()
                .map(Organization::getWorkspaceId)
                .collect(Collectors.toList());
        /*TODO: Remove organization resource filtering after UAC provide the endpoint which just
        returns the accessible ids with collaborators entities not include the organization
        entities*/
        for (GetResourcesResponseItem item : accessibleAllWorkspaceItems) {
          if (orgWorkspaceIds.contains(String.valueOf(item.getWorkspaceId()))) {
            accessibleDatasetIds.remove(item.getResourceId());
          }
        }
      } else {
        Workspace workspace =
            roleService.getWorkspaceByWorkspaceName(currentLoginUserInfo, workspaceName);
        List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
            roleService.getResourceItems(
                workspace,
                !queryParameters.getDatasetIdsList().isEmpty()
                    ? new HashSet<>(queryParameters.getDatasetIdsList())
                    : Collections.emptySet(),
                ModelDBServiceResourceTypes.DATASET);
        accessibleDatasetIds =
            accessibleAllWorkspaceItems.stream()
                .peek(
                    responseItem -> getResourcesMap.put(responseItem.getResourceId(), responseItem))
                .map(GetResourcesResponseItem::getResourceId)
                .collect(Collectors.toSet());
      }

      if (accessibleDatasetIds.isEmpty() && roleService.IsImplemented()) {
        LOGGER.debug("Accessible Dataset Ids not found, size 0");
        return getEmptyDatasetPaginationDTO();
      }

      List<Predicate> finalPredicatesList = new ArrayList<>();

      List<KeyValueQuery> predicates = new ArrayList<>(queryParameters.getPredicatesList());
      for (KeyValueQuery predicate : predicates) {
        // Validate if current user has access to the entity or not where predicate key has an id
        RdbmsUtils.validatePredicates(
            ModelDBConstants.DATASETS,
            new ArrayList<>(accessibleDatasetIds),
            predicate,
            roleService);
      }

      if (!accessibleDatasetIds.isEmpty()) {
        Expression<String> exp = repositoryRoot.get(ModelDBConstants.ID);
        Predicate predicate2 =
            exp.in(accessibleDatasetIds.stream().map(Long::parseLong).collect(Collectors.toList()));
        finalPredicatesList.add(predicate2);
      }

      String entityName = ModelDBConstants.REPOSITORY_ENTITY;
      try {
        List<Predicate> queryPredicatesList =
            RdbmsUtils.getQueryPredicatesFromPredicateList(
                entityName,
                predicates,
                builder,
                criteriaQuery,
                repositoryRoot,
                authService,
                roleService,
                ModelDBServiceResourceTypes.DATASET);
        if (!queryPredicatesList.isEmpty()) {
          finalPredicatesList.addAll(queryPredicatesList);
        }
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == com.google.rpc.Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          return getEmptyDatasetPaginationDTO();
        }
        throw ex;
      }

      finalPredicatesList.add(builder.equal(repositoryRoot.get(ModelDBConstants.DELETED), false));
      finalPredicatesList.add(builder.equal(repositoryRoot.get(ModelDBConstants.CREATED), true));
      finalPredicatesList.add(
          builder.equal(
              repositoryRoot.get(ModelDBConstants.REPOSITORY_ACCESS_MODIFIER),
              RepositoryEnums.RepositoryModifierEnum.PROTECTED.ordinal()));

      String sortBy = queryParameters.getSortKey();
      if (sortBy.isEmpty()) {
        sortBy = ModelDBConstants.DATE_UPDATED;
      } else if (sortBy.equals(ModelDBConstants.TIME_CREATED)) {
        sortBy = ModelDBConstants.DATE_CREATED;
      }

      Order orderBy =
          RdbmsUtils.getOrderBasedOnSortKey(
              sortBy, queryParameters.getAscending(), builder, repositoryRoot, entityName);

      Predicate[] predicateArr = new Predicate[finalPredicatesList.size()];
      for (int index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      Predicate predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(repositoryRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      Query<RepositoryEntity> query = session.createQuery(criteriaQuery);
      LOGGER.debug("Repositories final query : {}", query.getQueryString());
      if (queryParameters.getPageNumber() != 0 && queryParameters.getPageLimit() != 0) {
        // Calculate number of documents to skip
        int skips = queryParameters.getPageLimit() * (queryParameters.getPageNumber() - 1);
        query.setFirstResult(skips);
        query.setMaxResults(queryParameters.getPageLimit());
      }

      List<RepositoryEntity> repositoryEntities = query.list();
      LOGGER.debug("Repositorys result count : {}", repositoryEntities.size());

      Map<Long, Workspace> cacheWorkspaceMap = new HashMap<>();
      Map<Long, SimpleEntry<Dataset, Repository>> repositoriesAndDatasetsMap =
          convertRepositoriesFromRepositoryEntityList(
              session,
              metadataDAO,
              repositoryEntities,
              queryParameters.getIdsOnly(),
              cacheWorkspaceMap,
              getResourcesMap);

      LinkedHashMap<Dataset, Repository> repositoriesAndDatasets =
          repositoriesAndDatasetsMap.values().stream()
              .collect(
                  Collectors.toMap(
                      SimpleEntry::getKey, SimpleEntry::getValue, (a, b) -> a, LinkedHashMap::new));

      long totalRecords = RdbmsUtils.count(session, repositoryRoot, criteriaQuery);
      LOGGER.debug("Repositorys total records count : {}", totalRecords);

      DatasetPaginationDTO repositoryDatasetPaginationDTO = new DatasetPaginationDTO();
      repositoryDatasetPaginationDTO.setDatasets(new ArrayList<>(repositoriesAndDatasets.keySet()));
      repositoryDatasetPaginationDTO.setRepositories(
          new ArrayList<>(repositoriesAndDatasets.values()));
      repositoryDatasetPaginationDTO.setTotalRecords(totalRecords);
      return repositoryDatasetPaginationDTO;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findDatasets(metadataDAO, queryParameters, currentLoginUserInfo, resourceVisibility);
      } else {
        throw ex;
      }
    }
  }

  private DatasetPaginationDTO getEmptyDatasetPaginationDTO() {
    DatasetPaginationDTO emptyPaginationDTO = new DatasetPaginationDTO();
    emptyPaginationDTO.setDatasets(Collections.emptyList());
    emptyPaginationDTO.setRepositories(Collections.emptyList());
    emptyPaginationDTO.setTotalRecords(0L);
    return emptyPaginationDTO;
  }

  @Override
  public Dataset deleteDatasetTags(
      MetadataDAO metadataDAO, String id, List<String> tagsList, boolean deleteAll)
      throws ModelDBException, InvalidProtocolBufferException, ExecutionException,
          InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder().setRepoId(Long.parseLong(id)).build();
      deleteRepositoryTags(
          metadataDAO,
          repositoryIdentification,
          tagsList,
          deleteAll,
          false,
          RepositoryTypeEnum.DATASET);
      return convertToDataset(
          session,
          metadataDAO,
          getRepositoryById(
              session, repositoryIdentification, true, false, RepositoryTypeEnum.DATASET),
          new HashMap<>(),
          new HashMap<>());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteDatasetTags(metadataDAO, id, tagsList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void deleteRepositoryTags(
      MetadataDAO metadataDAO,
      RepositoryIdentification repositoryIdentification,
      List<String> tagsList,
      boolean deleteAll,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repositoryEntity =
          getRepositoryById(
              session, repositoryIdentification, true, canNotOperateOnProtected, repositoryType);
      session.lock(repositoryEntity, LockMode.PESSIMISTIC_WRITE);
      repositoryEntity.update();
      metadataDAO.deleteLabels(
          IdentificationType.newBuilder()
              .setIdType(VERSIONING_REPOSITORY)
              .setIntId(repositoryIdentification.getRepoId())
              .build(),
          tagsList,
          deleteAll);
      session.update(repositoryEntity);
      session.getTransaction().commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        deleteRepositoryTags(
            metadataDAO,
            repositoryIdentification,
            tagsList,
            deleteAll,
            canNotOperateOnProtected,
            repositoryType);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public GetDatasetById.Response getDatasetById(MetadataDAO metadataDAO, String id)
      throws ModelDBException, InvalidProtocolBufferException, ExecutionException,
          InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repositoryEntity =
          getRepositoryById(
              session,
              RepositoryIdentification.newBuilder().setRepoId(Long.parseLong(id)).build(),
              false,
              false,
              RepositoryTypeEnum.DATASET);
      return GetDatasetById.Response.newBuilder()
          .setDataset(
              convertToDataset(
                  session, metadataDAO, repositoryEntity, new HashMap<>(), new HashMap<>()))
          .build();
    } catch (NumberFormatException e) {
      String message = "Can't find repository, wrong id format: " + id;
      throw new ModelDBException(message, Code.INVALID_ARGUMENT);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getDatasetById(metadataDAO, id);
      } else {
        throw ex;
      }
    }
  }

  private Map<Long, SimpleEntry<Dataset, Repository>> convertRepositoriesFromRepositoryEntityList(
      Session session,
      MetadataDAO metadataDAO,
      List<RepositoryEntity> repositoryEntityList,
      boolean idsOnly,
      Map<Long, Workspace> cacheWorkspaceMap,
      Map<String, GetResourcesResponseItem> getResourcesMap) {
    return repositoryEntityList.stream()
        .collect(
            Collectors.toMap(
                RepositoryEntity::getId,
                repositoryEntity ->
                    getDatasetRepositorySimpleEntry(
                        session,
                        metadataDAO,
                        idsOnly,
                        repositoryEntity,
                        cacheWorkspaceMap,
                        getResourcesMap),
                (a, b) -> a,
                LinkedHashMap::new));
  }

  private SimpleEntry<Dataset, Repository> getDatasetRepositorySimpleEntry(
      Session session,
      MetadataDAO metadataDAO,
      boolean idsOnly,
      RepositoryEntity repositoryEntity,
      Map<Long, Workspace> cacheWorkspaceMap,
      Map<String, GetResourcesResponseItem> getResourcesMap) {
    try {
      return new SimpleEntry<>(
          convertToDataset(
              session, metadataDAO, repositoryEntity, cacheWorkspaceMap, getResourcesMap),
          repositoryEntity.toProto(roleService, authService, cacheWorkspaceMap, getResourcesMap));
    } catch (InvalidProtocolBufferException | ModelDBException e) {
      LOGGER.warn(UNEXPECTED_ERROR_ON_REPOSITORY_ENTITY_CONVERSION_TO_PROTO);
      throw new InternalErrorException(UNEXPECTED_ERROR_ON_REPOSITORY_ENTITY_CONVERSION_TO_PROTO);
    }
  }

  @Override
  public void deleteRepositoryAttributes(
      Long repositoryId,
      List<String> attributesKeys,
      boolean deleteAll,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repositoryEntity =
          getRepositoryById(
              session,
              RepositoryIdentification.newBuilder().setRepoId(repositoryId).build(),
              true,
              canNotOperateOnProtected,
              repositoryType);
      session.lock(repositoryEntity, LockMode.PESSIMISTIC_WRITE);
      if (deleteAll) {
        Query query =
            session
                .createQuery(DELETE_ALL_REPOSITORY_ATTRIBUTES_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("repoId", repositoryEntity.getId());
        query.executeUpdate();
      } else {
        Query query =
            session
                .createQuery(DELETE_SELECTED_REPOSITORY_ATTRIBUTES_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("keys", attributesKeys);
        query.setParameter("repoId", repositoryEntity.getId());
        query.executeUpdate();
      }

      StringBuilder updateRepoTimeQuery =
          new StringBuilder(
              "UPDATE RepositoryEntity rp SET rp.date_updated = :updatedTime where rp.id = :repoId ");
      Query updateRepoQuery = session.createQuery(updateRepoTimeQuery.toString());
      updateRepoQuery.setParameter("updatedTime", new Date().getTime());
      updateRepoQuery.setParameter("repoId", repositoryId);
      updateRepoQuery.executeUpdate();
      session.getTransaction().commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        deleteRepositoryAttributes(
            repositoryId, attributesKeys, deleteAll, canNotOperateOnProtected, repositoryType);
      } else {
        throw ex;
      }
    }
  }
}
