package ai.verta.modeldb.versioning;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.TagsEntity;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.GetRepositoryRequest.Response;
import ai.verta.modeldb.versioning.RepositoryVisibilityEnum.RepositoryVisibility;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.Role;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

public class RepositoryDAORdbImpl implements RepositoryDAO {

  private static final Logger LOGGER = LogManager.getLogger(RepositoryDAORdbImpl.class);
  private static final String GLOBAL_SHARING = "_REPO_GLOBAL_SHARING";
  private final AuthService authService;
  private final RoleService roleService;

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

  private static final String GET_REPOSITORY_BY_NAME_PREFIX_HQL =
      new StringBuilder("From ")
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
  private static final String CHECK_BRANCH_IN_REPOSITORY_HQL =
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
          .toString();

  public RepositoryDAORdbImpl(AuthService authService, RoleService roleService) {
    this.authService = authService;
    this.roleService = roleService;
  }

  @Override
  public Response getRepository(GetRepositoryRequest request) throws Exception {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getId());
      return Response.newBuilder().setRepository(repository.toProto()).build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getRepository(request);
      } else {
        throw ex;
      }
    }
  }

  private WorkspaceDTO verifyAndGetWorkspaceDTO(
      RepositoryIdentification id, boolean shouldCheckNamed, boolean create)
      throws ModelDBException {
    WorkspaceDTO workspaceDTO = null;
    String message = null;
    if (id.hasNamedId()) {
      UserInfo userInfo;
      try {
        userInfo = authService.getCurrentLoginUserInfo();
      } catch (StatusRuntimeException e) {
        throw new ModelDBException("Authorization error", e.getStatus().getCode());
      }
      RepositoryNamedIdentification named = id.getNamedId();
      try {
        workspaceDTO =
            roleService.getWorkspaceDTOByWorkspaceName(userInfo, named.getWorkspaceName());
        if (create) {
          ModelDBUtils.checkPersonalWorkspace(
              userInfo,
              workspaceDTO.getWorkspaceType(),
              workspaceDTO.getWorkspaceId(),
              "repository");
        }
      } catch (StatusRuntimeException e) {
        LOGGER.warn(e);
        throw new ModelDBException(
            "Error getting workspace: " + e.getStatus().getDescription(), e.getStatus().getCode());
      }
      if (named.getName().isEmpty() && shouldCheckNamed) {
        message = "Repository name should not be empty";
      }
    }

    if (message != null) {
      throw new ModelDBException(message, Code.INVALID_ARGUMENT);
    }
    return workspaceDTO;
  }

  private WorkspaceDTO verifyAndGetWorkspaceDTO(
      RepositoryIdentification id, boolean shouldCheckNamed) throws ModelDBException {
    return verifyAndGetWorkspaceDTO(id, shouldCheckNamed, false);
  }

  @Override
  public RepositoryEntity getRepositoryById(
      Session session, RepositoryIdentification id, boolean checkWrite) throws ModelDBException {
    RepositoryEntity repository;
    if (id.hasNamedId()) {
      WorkspaceDTO workspaceDTO = verifyAndGetWorkspaceDTO(id, true);
      repository =
          getRepositoryByName(session, id.getNamedId().getName(), workspaceDTO)
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
      if (checkWrite) {
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.REPOSITORY,
            repository.getId().toString(),
            ModelDBServiceActions.UPDATE);
      } else {
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.REPOSITORY,
            repository.getId().toString(),
            ModelDBServiceActions.READ);
      }
    } catch (InvalidProtocolBufferException e) {
      LOGGER.error(e);
      throw new ModelDBException("Unexpected error", e);
    }
    return repository;
  }

  @Override
  public RepositoryEntity getRepositoryById(Session session, RepositoryIdentification id)
      throws ModelDBException {
    return getRepositoryById(session, id, false);
  }

  private Optional<RepositoryEntity> getRepositoryById(Session session, long id) {
    Query query = session.createQuery(GET_REPOSITORY_BY_ID_HQL);
    query.setParameter("repoId", id);
    return Optional.ofNullable((RepositoryEntity) query.uniqueResult());
  }

  private Optional<RepositoryEntity> getRepositoryByName(
      Session session, String name, WorkspaceDTO workspaceDTO) {
    Query query =
        ModelDBHibernateUtil.getWorkspaceEntityQuery(
            session,
            SHORT_NAME,
            GET_REPOSITORY_BY_NAME_PREFIX_HQL,
            "repositoryName",
            name,
            ModelDBConstants.WORKSPACE_ID,
            workspaceDTO.getWorkspaceId(),
            workspaceDTO.getWorkspaceType(),
            true,
            null);
    return Optional.ofNullable((RepositoryEntity) query.uniqueResult());
  }

  @Override
  public SetRepository.Response setRepository(
      CommitDAO commitDAO, SetRepository request, UserInfo userInfo, boolean create)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository;
      if (create) {
        WorkspaceDTO workspaceDTO = verifyAndGetWorkspaceDTO(request.getId(), false, true);
        ModelDBHibernateUtil.checkIfEntityAlreadyExists(
            session,
            SHORT_NAME,
            GET_REPOSITORY_COUNT_BY_NAME_PREFIX_HQL,
            "Repository",
            "repositoryName",
            request.getRepository().getName(),
            ModelDBConstants.WORKSPACE_ID,
            workspaceDTO.getWorkspaceId(),
            workspaceDTO.getWorkspaceType(),
            LOGGER);
        repository =
            new RepositoryEntity(
                request.getRepository().getName(),
                workspaceDTO,
                request.getRepository().getOwner(),
                request.getRepository().getRepositoryVisibility());
      } else {
        repository = getRepositoryById(session, request.getId(), true);
        ModelDBHibernateUtil.checkIfEntityAlreadyExists(
            session,
            SHORT_NAME,
            GET_REPOSITORY_COUNT_BY_NAME_PREFIX_HQL,
            "Repository",
            "repositoryName",
            request.getRepository().getName(),
            ModelDBConstants.WORKSPACE_ID,
            repository.getWorkspace_id(),
            WorkspaceType.forNumber(repository.getWorkspace_type()),
            LOGGER);
        repository.update(request);
      }
      session.beginTransaction();
      session.saveOrUpdate(repository);
      if (create) {
        Commit initCommit =
            Commit.newBuilder().setMessage(ModelDBConstants.INITIAL_COMMIT_MESSAGE).build();
        CommitEntity commitEntity =
            commitDAO.saveCommitEntity(
                session,
                initCommit,
                FileHasher.getSha(new String()),
                authService.getVertaIdFromUserInfo(userInfo),
                repository);

        saveBranch(
            session, commitEntity.getCommit_hash(), ModelDBConstants.MASTER_BRANCH, repository);
      }
      session.getTransaction().commit();
      if (create) {
        try {
          createRoleBindingsForRepository(request, userInfo, repository);
        } catch (Exception e) {
          LOGGER.info("Exception from UAC during Repo role binding creation : {}", e.getMessage());
          LOGGER.info("Deleting the created repository {}", repository.getId());
          // delete the repo created
          session.beginTransaction();
          session.delete(repository);
          session.getTransaction().commit();
          throw e;
        }
      }
      return SetRepository.Response.newBuilder().setRepository(repository.toProto()).build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setRepository(commitDAO, request, userInfo, create);
      } else {

        throw ex;
      }
    }
  }

  private void createRoleBindingsForRepository(
      SetRepository request, UserInfo userInfo, RepositoryEntity repository) {
    Role ownerRole = roleService.getRoleByName(ModelDBConstants.ROLE_REPOSITORY_OWNER, null);
    roleService.createRoleBinding(
        ownerRole,
        new CollaboratorUser(authService, userInfo),
        String.valueOf(repository.getId()),
        ModelDBServiceResourceTypes.REPOSITORY);
    roleService.createWorkspaceRoleBinding(
        repository.getWorkspace_id(),
        WorkspaceType.forNumber(repository.getWorkspace_type()),
        String.valueOf(repository.getId()),
        ModelDBConstants.ROLE_REPOSITORY_ADMIN,
        ModelDBServiceResourceTypes.REPOSITORY,
        request.getRepository().getRepositoryVisibility() != null
            && request
                .getRepository()
                .getRepositoryVisibility()
                .equals(RepositoryVisibility.ORG_SCOPED_PUBLIC),
        GLOBAL_SHARING);
  }

  @Override
  public DeleteRepositoryRequest.Response deleteRepository(
      DeleteRepositoryRequest request, CommitDAO commitDAO, ExperimentRunDAO experimentRunDAO)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());
      // Get self allowed resources id where user has delete permission
      List<String> allowedRepositoryIds =
          roleService.getAccessibleResourceIdsByActions(
              ModelDBServiceResourceTypes.REPOSITORY,
              ModelDBServiceActions.DELETE,
              Collections.singletonList(String.valueOf(repository.getId())));
      if (allowedRepositoryIds.isEmpty()) {
        throw new ModelDBException(
            "Delete Access Denied for given repository Id : " + request.getRepositoryId(),
            Code.PERMISSION_DENIED);
      }

      Query deletedRepositoriesQuery = session.createQuery(DELETED_STATUS_REPOSITORY_QUERY_STRING);
      deletedRepositoriesQuery.setParameter("deleted", true);
      deletedRepositoriesQuery.setParameter(
          "repoIds", allowedRepositoryIds.stream().map(Long::valueOf).collect(Collectors.toList()));
      Transaction transaction = session.beginTransaction();
      int updatedCount = deletedRepositoriesQuery.executeUpdate();
      LOGGER.debug(
          "Mark Repositories as deleted : {}, count : {}", allowedRepositoryIds, updatedCount);
      // Delete all VersionedInputs for repository ID
      experimentRunDAO.deleteLogVersionedInputs(session, repository.getId(), null);
      transaction.commit();
      return DeleteRepositoryRequest.Response.newBuilder().setStatus(true).build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteRepository(request, commitDAO, experimentRunDAO);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ListRepositoriesRequest.Response listRepositories(
      ListRepositoriesRequest request, UserInfo currentLoginUserInfo) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<String> accessibleResourceIds =
          roleService.getAccessibleResourceIds(
              null,
              new CollaboratorUser(authService, currentLoginUserInfo),
              RepositoryVisibility.PRIVATE,
              ModelDBServiceResourceTypes.REPOSITORY,
              Collections.emptyList());

      if (accessibleResourceIds.isEmpty() && roleService.IsImplemented()) {
        LOGGER.debug("Accessible Repository Ids not found, size 0");
        return ListRepositoriesRequest.Response.newBuilder().setTotalRecords(0).build();
      }

      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<RepositoryEntity> criteriaQuery =
          criteriaBuilder.createQuery(RepositoryEntity.class);
      Root<RepositoryEntity> repositoryEntityRoot = criteriaQuery.from(RepositoryEntity.class);
      repositoryEntityRoot.alias(SHORT_NAME);
      List<Predicate> finalPredicatesList = new ArrayList<>();

      if (!request.getWorkspaceName().isEmpty()) {
        WorkspaceDTO workspaceDTO =
            verifyAndGetWorkspaceDTO(
                RepositoryIdentification.newBuilder()
                    .setNamedId(
                        RepositoryNamedIdentification.newBuilder()
                            .setWorkspaceName(request.getWorkspaceName()))
                    .build(),
                false);
        List<KeyValueQuery> workspacePredicates =
            ModelDBUtils.getKeyValueQueriesByWorkspaceDTO(workspaceDTO);
        if (workspacePredicates.size() > 0) {
          Predicate privateWorkspacePredicate =
              criteriaBuilder.equal(
                  repositoryEntityRoot.get(ModelDBConstants.WORKSPACE_ID),
                  workspacePredicates.get(0).getValue().getStringValue());
          Predicate privateWorkspaceTypePredicate =
              criteriaBuilder.equal(
                  repositoryEntityRoot.get(ModelDBConstants.WORKSPACE_TYPE),
                  workspacePredicates.get(1).getValue().getNumberValue());
          Predicate privatePredicate =
              criteriaBuilder.and(privateWorkspacePredicate, privateWorkspaceTypePredicate);

          finalPredicatesList.add(privatePredicate);
        }
      }

      if (!accessibleResourceIds.isEmpty()) {
        Expression<String> exp = repositoryEntityRoot.get(ModelDBConstants.ID);
        Predicate predicate2 = exp.in(accessibleResourceIds);
        finalPredicatesList.add(predicate2);
      }

      finalPredicatesList.add(
          criteriaBuilder.equal(repositoryEntityRoot.get(ModelDBConstants.DELETED), false));

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

      repositoryEntities.forEach(
          repositoryEntity -> builder.addRepositories(repositoryEntity.toProto()));

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
  public SetTagRequest.Response setTag(SetTagRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId(), true);

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
  public GetTagRequest.Response getTag(GetTagRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
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
  public DeleteTagRequest.Response deleteTag(DeleteTagRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId(), true);
      TagsEntity tagsEntity =
          session.get(TagsEntity.class, new TagsEntity.TagId(request.getTag(), repository.getId()));
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
  public ListTagsRequest.Response listTags(ListTagsRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
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
  public SetBranchRequest.Response setBranch(SetBranchRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId(), true);

      session.beginTransaction();
      saveBranch(session, request.getCommitSha(), request.getBranch(), repository);
      session.getTransaction().commit();
      return SetBranchRequest.Response.newBuilder().build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setBranch(request);
      } else {
        throw ex;
      }
    }
  }

  private void saveBranch(
      Session session, String commitSHA, String branch, RepositoryEntity repository)
      throws ModelDBException {
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

    Query query = session.createQuery(CHECK_BRANCH_IN_REPOSITORY_HQL);
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
  public GetBranchRequest.Response getBranch(GetBranchRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      BranchEntity branchEntity = getBranchEntity(session, repository.getId(), request.getBranch());
      CommitEntity commitEntity = session.get(CommitEntity.class, branchEntity.getCommit_hash());
      return GetBranchRequest.Response.newBuilder().setCommit(commitEntity.toCommitProto()).build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getBranch(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DeleteBranchRequest.Response deleteBranch(DeleteBranchRequest request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId(), true);
      BranchEntity branchEntity =
          session.get(
              BranchEntity.class,
              new BranchEntity.BranchId(request.getBranch(), repository.getId()));
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
    Query deleteBranchQuery = session.createQuery(deleteBranchesHQLBuilder.toString());
    deleteBranchQuery.setParameter("repositoryId", repoId);
    deleteBranchQuery.setParameter("commitHash", commitHash);
    deleteBranchQuery.executeUpdate();
  }

  @Override
  public ListBranchesRequest.Response listBranches(ListBranchesRequest request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
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
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
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
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();
      WorkspaceDTO workspaceDTO =
          roleService.getWorkspaceDTOByWorkspaceName(
              currentLoginUserInfo, request.getWorkspaceName());
      try {
        List<String> accessibleResourceIds =
            roleService.getAccessibleResourceIds(
                null,
                new CollaboratorUser(authService, currentLoginUserInfo),
                RepositoryVisibility.PRIVATE,
                ModelDBServiceResourceTypes.REPOSITORY,
                request.getRepoIdsList().stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList()));

        if (accessibleResourceIds.isEmpty() && roleService.IsImplemented()) {
          LOGGER.debug("Accessible Repository Ids not found, size 0");
          return FindRepositories.Response.newBuilder()
              .addAllRepositories(Collections.emptyList())
              .setTotalRecords(0L)
              .build();
        }

        for (KeyValueQuery predicate : request.getPredicatesList()) {
          // Validate if current user has access to the entity or not where predicate key has an id
          RdbmsUtils.validatePredicates(
              ModelDBConstants.REPOSITORY, accessibleResourceIds, predicate, roleService);
        }

        FindRepositoriesQuery findRepositoriesQuery =
            new FindRepositoriesQuery.FindRepositoriesHQLQueryBuilder(
                    session, authService, workspaceDTO)
                .setRepoIds(
                    accessibleResourceIds.stream().map(Long::valueOf).collect(Collectors.toList()))
                .setPredicates(request.getPredicatesList())
                .setPageLimit(request.getPageLimit())
                .setPageNumber(request.getPageNumber())
                .build();
        List<RepositoryEntity> repositoryEntities =
            findRepositoriesQuery.getFindRepositoriesHQLQuery().list();
        Long totalRecords =
            (Long) findRepositoriesQuery.getFindRepositoriesCountHQLQuery().uniqueResult();

        return FindRepositories.Response.newBuilder()
            .addAllRepositories(
                repositoryEntities.stream()
                    .map(RepositoryEntity::toProto)
                    .collect(Collectors.toList()))
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
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findRepositories(request);
      } else {
        throw ex;
      }
    }
  }
}
