package ai.verta.modeldb.versioning;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.TagsEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.GetRepositoryRequest.Response;
import ai.verta.uac.UserInfo;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class RepositoryDAORdbImpl implements RepositoryDAO {

  private static final Logger LOGGER = LogManager.getLogger(RepositoryDAORdbImpl.class);
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
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getId());
      session.getTransaction().commit();
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
  public RepositoryEntity getRepositoryById(Session session, RepositoryIdentification id)
      throws ModelDBException {
    RepositoryEntity repository;
    if (id.hasNamedId()) {
      WorkspaceDTO workspaceDTO = verifyAndGetWorkspaceDTO(id, true);
      repository =
          getRepositoryByName(session, id.getNamedId().getName(), workspaceDTO)
              .orElseThrow(
                  () -> new ModelDBException("Couldn't find repository by name", Code.NOT_FOUND));
    } else {
      repository =
          getRepositoryById(session, id.getRepoId())
              .orElseThrow(
                  () -> new ModelDBException("Couldn't find repository by id", Code.NOT_FOUND));
    }
    return repository;
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
  public SetRepository.Response setRepository(SetRepository request, boolean create)
      throws ModelDBException {
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
                request.getRepository().getOwner());
      } else {
        repository = getRepositoryById(session, request.getId());
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
      session.getTransaction().commit();
      return SetRepository.Response.newBuilder().setRepository(repository.toProto()).build();
    }
  }

  @Override
  public DeleteRepositoryRequest.Response deleteRepository(DeleteRepositoryRequest request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());
      session.delete(repository);
      session.getTransaction().commit();
      return DeleteRepositoryRequest.Response.newBuilder().setStatus(true).build();
    }
  }

  @Override
  public ListRepositoriesRequest.Response listRepositories(ListRepositoriesRequest request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      WorkspaceDTO workspaceDTO =
          verifyAndGetWorkspaceDTO(
              RepositoryIdentification.newBuilder()
                  .setNamedId(
                      RepositoryNamedIdentification.newBuilder()
                          .setWorkspaceName(request.getWorkspaceName()))
                  .build(),
              false);
      String query_prefix =
          workspaceDTO.getWorkspaceId() == null
              ? GET_REPOSITORY_PREFIX_HQL_OSS
              : GET_REPOSITORY_PREFIX_HQL;
      Query query =
          ModelDBHibernateUtil.getWorkspaceEntityQuery(
              session,
              SHORT_NAME,
              query_prefix,
              "repositoryName",
              null,
              ModelDBConstants.WORKSPACE_ID,
              workspaceDTO.getWorkspaceId(),
              workspaceDTO.getWorkspaceType(),
              false,
              Collections.singletonList(
                  ModelDBConstants.DATE_UPDATED + " " + ModelDBConstants.ORDER_DESC));
      int pageLimit = request.getPagination().getPageLimit();
      if (request.hasPagination()) {
        query.setFirstResult((request.getPagination().getPageNumber() - 1) * pageLimit);
        query.setMaxResults(pageLimit);
      }
      List list = query.list();
      ListRepositoriesRequest.Response.Builder builder =
          ListRepositoriesRequest.Response.newBuilder();
      query_prefix =
          workspaceDTO.getWorkspaceId() == null
              ? GET_REPOSITORY_COUNT_PREFIX_HQL_OSS
              : GET_REPOSITORY_COUNT_PREFIX_HQL;
      query =
          ModelDBHibernateUtil.getWorkspaceEntityQuery(
              session,
              SHORT_NAME,
              query_prefix,
              "repositoryName",
              null,
              ModelDBConstants.WORKSPACE_ID,
              workspaceDTO.getWorkspaceId(),
              workspaceDTO.getWorkspaceType(),
              false,
              null);
      list.forEach((o) -> builder.addRepositories(((RepositoryEntity) o).toProto()));
      final Long value = (Long) query.uniqueResult();
      builder.setTotalRecords(value);
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
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      boolean exists =
          VersioningUtils.commitRepositoryMappingExists(
              session, request.getCommitSha(), repository.getId());
      if (!exists) {
        throw new ModelDBException(
            "Commit_hash and repository_id mapping not found", Code.NOT_FOUND);
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
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      Query query = session.createQuery(GET_TAG_HQL);
      query.setParameter("repositoryId", repository.getId());
      query.setParameter("tag", request.getTag());
      TagsEntity tagsEntity = (TagsEntity) query.uniqueResult();
      if (tagsEntity == null) {
        throw new ModelDBException("Tag not found", Code.NOT_FOUND);
      }

      CommitEntity commitEntity = session.get(CommitEntity.class, tagsEntity.getCommit_hash());
      session.getTransaction().commit();
      return GetTagRequest.Response.newBuilder().setCommit(commitEntity.toCommitProto()).build();
    }
  }

  @Override
  public DeleteTagRequest.Response deleteTag(DeleteTagRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());
      TagsEntity tagsEntity =
          session.get(TagsEntity.class, new TagsEntity.TagId(request.getTag(), repository.getId()));
      if (tagsEntity == null) {
        throw new ModelDBException("Tag not found", Code.NOT_FOUND);
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
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      boolean exists =
          VersioningUtils.commitRepositoryMappingExists(
              session, request.getCommitSha(), repository.getId());
      if (!exists) {
        throw new ModelDBException(
            "Commit_hash and repository_id mapping not found", Code.NOT_FOUND);
      }

      Query query = session.createQuery(CHECK_BRANCH_IN_REPOSITORY_HQL);
      query.setParameter("repositoryId", repository.getId());
      query.setParameter("branch", request.getBranch());
      BranchEntity branchEntity = (BranchEntity) query.uniqueResult();
      if (branchEntity != null) {
        if (branchEntity.getCommit_hash().equals(request.getCommitSha()))
          return SetBranchRequest.Response.newBuilder().build();
        session.delete(branchEntity);
      }

      branchEntity =
          new BranchEntity(repository.getId(), request.getCommitSha(), request.getBranch());
      session.save(branchEntity);
      session.getTransaction().commit();
      return SetBranchRequest.Response.newBuilder().build();
    }
  }

  @Override
  public GetBranchRequest.Response getBranch(GetBranchRequest request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      Query query = session.createQuery(CHECK_BRANCH_IN_REPOSITORY_HQL);
      query.setParameter("repositoryId", repository.getId());
      query.setParameter("branch", request.getBranch());
      BranchEntity branchEntity = (BranchEntity) query.uniqueResult();
      if (branchEntity == null) {
        throw new ModelDBException(ModelDBConstants.BRANCH_NOT_FOUND, Code.NOT_FOUND);
      }

      CommitEntity commitEntity = session.get(CommitEntity.class, branchEntity.getCommit_hash());
      session.getTransaction().commit();
      return GetBranchRequest.Response.newBuilder().setCommit(commitEntity.toCommitProto()).build();
    }
  }

  @Override
  public DeleteBranchRequest.Response deleteBranch(DeleteBranchRequest request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());
      BranchEntity branchEntity =
          session.get(
              BranchEntity.class,
              new BranchEntity.BranchId(request.getBranch(), repository.getId()));
      if (branchEntity == null) {
        throw new ModelDBException(ModelDBConstants.BRANCH_NOT_FOUND, Code.NOT_FOUND);
      }
      session.delete(branchEntity);
      session.getTransaction().commit();
      return DeleteBranchRequest.Response.newBuilder().build();
    }
  }

  @Override
  public ListBranchesRequest.Response listBranches(ListBranchesRequest request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      Query query = session.createQuery(GET_REPOSITORY_BRANCHES_HQL);
      query.setParameter("repoId", repository.getId());
      List<BranchEntity> branchEntities = query.list();

      if (branchEntities == null || branchEntities.isEmpty()) {
        return ListBranchesRequest.Response.newBuilder().setTotalRecords(0).build();
      }

      session.getTransaction().commit();
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
  public ListBranchCommitsRequest.Response listBranchCommits(ListBranchCommitsRequest request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = getRepositoryById(session, request.getRepositoryId());

      Query query = session.createQuery(CHECK_BRANCH_IN_REPOSITORY_HQL);
      query.setParameter("repositoryId", repository.getId());
      query.setParameter("branch", request.getBranch());
      BranchEntity branchEntity = (BranchEntity) query.uniqueResult();
      if (branchEntity == null) {
        throw new ModelDBException(ModelDBConstants.BRANCH_NOT_FOUND, Code.NOT_FOUND);
      }
      // list of commits to be used in the in clause in the final query
      Set<String> commitSHAs = new HashSet<String>();
      // List of commits to be traversed
      List<String> childCommitSHAs = new LinkedList<String>();
      childCommitSHAs.add(branchEntity.getCommit_hash());
      String getParentCommitsQuery = "SELECT parent_hash FROM commit_parent WHERE child_hash = \"";

      while (!childCommitSHAs.isEmpty()) {
        String childCommit = childCommitSHAs.remove(0);
        commitSHAs.add(childCommit);
        Query sqlQuery = session.createSQLQuery(getParentCommitsQuery + childCommit + "\"");
        List<String> parentCommitSHAs = sqlQuery.list();
        childCommitSHAs.addAll(parentCommitSHAs);
      }

      String getChildCommits =
          "FROM "
              + CommitEntity.class.getSimpleName()
              + " c WHERE c.commit_hash IN (:childCommitSHAs)  ORDER BY c.date_created DESC";
      query = session.createQuery(getChildCommits);
      query.setParameterList("childCommitSHAs", commitSHAs);
      List<CommitEntity> commits = query.list();

      return ListBranchCommitsRequest.Response.newBuilder()
          .addAllCommits(
              commits.stream().map(CommitEntity::toCommitProto).collect(Collectors.toList()))
          .setTotalRecords(commits.size())
          .build();
    }
  }
}
