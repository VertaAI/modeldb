package ai.verta.modeldb.versioning;

import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.CommitPaginationDTO;
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
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
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
import org.hibernate.query.Query;

public class RepositoryDAORdbImpl implements RepositoryDAO {

  private static final String GET_REPOSITORY_BY_IDS_QUERY =
      "From RepositoryEntity ent where ent.id IN (:ids)";
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

  private static final String GET_REPOSITORY_COUNT_PREFIX_HQL =
      new StringBuilder("Select count(*) From ")
          .append(RepositoryEntity.class.getSimpleName())
          .append(" ")
          .append(SHORT_NAME)
          .append(" where ")
          .toString();
  private static final String GET_REPOSITORY_COUNT_PREFIX_HQL_OSS =
      new StringBuilder("Select count(*) From ")
          .append(RepositoryEntity.class.getSimpleName())
          .append(" ")
          .append(SHORT_NAME)
          .toString();

  private static final String GET_REPOSITORY_PREFIX_HQL =
      new StringBuilder("From ")
          .append(RepositoryEntity.class.getSimpleName())
          .append(" ")
          .append(SHORT_NAME)
          .append(" where ")
          .toString();

  private static final String GET_REPOSITORY_PREFIX_HQL_OSS =
      new StringBuilder("From ")
          .append(RepositoryEntity.class.getSimpleName())
          .append(" ")
          .append(SHORT_NAME)
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
  private static final String CHECK_COMMIT_IN_REPOSITORY_HQL =
      new StringBuilder("From ")
          .append(CommitEntity.class.getSimpleName())
          .append(" br ")
          .append(" where ")
          .append(" br.id.")
          .append(ModelDBConstants.REPOSITORY_ID)
          .append(" = :repositoryId ")
          .append(" AND br.id.")
          .append(ModelDBConstants.COMMIT)
          .append(" = :commit ")
          .toString();
  private static final String GET_REPOSITORY_BRANCHES_HQL =
      new StringBuilder("From ")
          .append(BranchEntity.class.getSimpleName())
          .append(" br where br.id.")
          .append(ModelDBConstants.REPOSITORY_ID)
          .append(" = :repoId ")
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
      }
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY,
          repository.getId().toString(),
          ModelDBServiceActions.READ);
    } catch (InvalidProtocolBufferException e) {
      LOGGER.error(e);
      throw new ModelDBException("Unexpected error", e);
    }
    try {
      if (checkWrite) {
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.REPOSITORY,
            repository.getId().toString(),
            ModelDBServiceActions.UPDATE);
      }
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY,
          repository.getId().toString(),
          ModelDBServiceActions.READ);
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
    return Optional.ofNullable(session.get(RepositoryEntity.class, id));
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
      session.beginTransaction();
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
      session.saveOrUpdate(repository);
      if (create) {
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
      return SetRepository.Response.newBuilder().setRepository(repository.toProto()).build();
    }
  }

  private void deleteRoleBindingsOfAccessibleResources(List<RepositoryEntity> allowedResources) {
    final List<String> roleBindingNames = Collections.synchronizedList(new ArrayList<>());
    for (RepositoryEntity repositoryEntity : allowedResources) {
      String repositoryId = String.valueOf(repositoryEntity.getId());
      String ownerRoleBindingName =
          roleService.buildRoleBindingName(
              ModelDBConstants.ROLE_REPOSITORY_OWNER,
              repositoryId,
              repositoryEntity.getOwner(),
              ModelDBServiceResourceTypes.REPOSITORY.name());
      if (ownerRoleBindingName != null && !ownerRoleBindingName.isEmpty()) {
        roleBindingNames.add(ownerRoleBindingName);
      }

      // Delete workspace based roleBindings
      List<String> repoOrgWorkspaceRoleBindings =
          roleService.getWorkspaceRoleBindings(
              repositoryEntity.getWorkspace_id(),
              WorkspaceType.forNumber(repositoryEntity.getWorkspace_type()),
              String.valueOf(repositoryEntity.getId()),
              ModelDBConstants.ROLE_REPOSITORY_ADMIN,
              ModelDBServiceResourceTypes.REPOSITORY,
              repositoryEntity
                  .getRepository_visibility()
                  .equals(DatasetVisibility.ORG_SCOPED_PUBLIC_VALUE),
              GLOBAL_SHARING);
      if (!repoOrgWorkspaceRoleBindings.isEmpty()) {
        roleBindingNames.addAll(repoOrgWorkspaceRoleBindings);
      }
    }
    // Remove all repositoryEntity collaborators
    roleService.deleteAllResourceCollaborators(
        allowedResources.stream()
            .map(repositoryEntity -> String.valueOf(repositoryEntity.getId()))
            .collect(Collectors.toList()),
        ModelDBServiceResourceTypes.REPOSITORY);

    // Remove all role bindings
    roleService.deleteRoleBindings(roleBindingNames);
  }

  @Override
  public DeleteRepositoryRequest.Response deleteRepository(
      DeleteRepositoryRequest request, CommitDAO commitDAO, ExperimentRunDAO experimentRunDAO)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
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

      ListBranchesRequest.Response listBranchesResponse =
          listBranches(
              ListBranchesRequest.newBuilder().setRepositoryId(request.getRepositoryId()).build());
      if (!listBranchesResponse.getBranchesList().isEmpty()) {
        String deleteBranchesHQL =
            "DELETE FROM "
                + BranchEntity.class.getSimpleName()
                + " br where br.id.repository_id = :repositoryId AND br.id.branch IN (:branches)";
        Query deleteBranchQuery = session.createQuery(deleteBranchesHQL);
        deleteBranchQuery.setParameter("repositoryId", repository.getId());
        deleteBranchQuery.setParameterList("branches", listBranchesResponse.getBranchesList());
        deleteBranchQuery.executeUpdate();
      }

      CommitPaginationDTO commitPaginationDTO =
          commitDAO.fetchCommitEntityList(
              session, ListCommitsRequest.newBuilder().build(), repository.getId());
      commitPaginationDTO
          .getCommitEntities()
          .forEach(
              commitEntity -> {
                if (commitEntity.getRepository().contains(repository)) {
                  commitEntity.getRepository().remove(repository);
                  if (commitEntity.getRepository().isEmpty()) {
                    session.delete(commitEntity);
                  } else {
                    session.update(commitEntity);
                  }
                }
              });
      // Delete all VersionedInputs for repository ID
      experimentRunDAO.deleteLogVersionedInputs(session, repository.getId(), null);

      deleteRoleBindingsOfAccessibleResources(Collections.singletonList(repository));
      session.delete(repository);
      session.getTransaction().commit();
      return DeleteRepositoryRequest.Response.newBuilder().setStatus(true).build();
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
              ProjectVisibility.PRIVATE,
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
    } catch (ModelDBException e) {
      LOGGER.warn(e.getMessage(), e);
      throw e;
    }
  }

  @Override
  public SetTagRequest.Response setTag(SetTagRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
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
      session.save(tagsEntity);
      session.getTransaction().commit();
      return SetTagRequest.Response.newBuilder().build();
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
    }
  }

  @Override
  public DeleteTagRequest.Response deleteTag(DeleteTagRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId(), true);
      TagsEntity tagsEntity =
          session.get(TagsEntity.class, new TagsEntity.TagId(request.getTag(), repository.getId()));
      if (tagsEntity == null) {
        throw new ModelDBException("Tag not found " + request.getTag(), Code.NOT_FOUND);
      }
      session.delete(tagsEntity);
      session.getTransaction().commit();
      return DeleteTagRequest.Response.newBuilder().build();
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
    }
  }

  @Override
  public SetBranchRequest.Response setBranch(SetBranchRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId(), true);

      if (saveBranch(session, request.getCommitSha(), request.getBranch(), repository))
        return SetBranchRequest.Response.newBuilder().build();
      session.getTransaction().commit();
      return SetBranchRequest.Response.newBuilder().build();
    }
  }

  private boolean saveBranch(
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
      if (branchEntity.getCommit_hash().equals(commitSHA)) return true;
      session.delete(branchEntity);
    }

    branchEntity = new BranchEntity(repository.getId(), commitSHA, branch);
    session.save(branchEntity);
    return false;
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
    }
  }

  @Override
  public DeleteBranchRequest.Response deleteBranch(DeleteBranchRequest request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId(), true);
      BranchEntity branchEntity =
          session.get(
              BranchEntity.class,
              new BranchEntity.BranchId(request.getBranch(), repository.getId()));
      if (branchEntity == null) {
        throw new ModelDBException(
            ModelDBConstants.BRANCH_NOT_FOUND + request.getBranch(), Code.NOT_FOUND);
      }
      session.delete(branchEntity);
      session.getTransaction().commit();
      return DeleteBranchRequest.Response.newBuilder().build();
    }
  }

  @Override
  public void deleteBranchByCommit(Long repoId, String commitHash) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();

      StringBuilder deleteBranchesHQLBuilder =
          new StringBuilder("DELETE FROM ")
              .append(BranchEntity.class.getSimpleName())
              .append(" br where br.id.repository_id = :repositoryId ")
              .append(" AND br.commit_hash = :commitHash ");
      Query deleteBranchQuery = session.createQuery(deleteBranchesHQLBuilder.toString());
      deleteBranchQuery.setParameter("repositoryId", repoId);
      deleteBranchQuery.setParameter("commitHash", commitHash);
      deleteBranchQuery.executeUpdate();
      session.getTransaction().commit();
    }
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
    }
  }

  @Override
  public FindRepositories.Response findRepositories(FindRepositories request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      WorkspaceDTO workspaceDTO =
          roleService.getWorkspaceDTOByWorkspaceName(userInfo, request.getWorkspaceName());
      FindRepositoriesQuery findRepositoriesQuery =
          new FindRepositoriesQuery.FindRepositoriesHQLQueryBuilder(session, workspaceDTO)
              .setRepoIds(request.getRepoIdsList())
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
    }
  }
}
