package ai.verta.modeldb.versioning;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.versioning.RepositoryEnums;
import ai.verta.modeldb.experimentRun.FutureExperimentRunDAO;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.ListRepositoriesRequest.Response;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceImplBase;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.AutogenBlob;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.AutogenBlobDiff;
import ai.verta.modeldb.versioning.blob.container.BlobContainer;
import ai.verta.modeldb.versioning.blob.visitors.Validator;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.grpc.Status.Code;
import io.grpc.stub.StreamObserver;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VersioningServiceImpl extends VersioningServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(VersioningServiceImpl.class);
  private static final String UPDATE_REPOSITORY_EVENT_TYPE =
      "update.resource.repository.update_repository_succeeded";
  private final AuthService authService;
  private final MDBRoleService mdbRoleService;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final BlobDAO blobDAO;
  private final FutureExperimentRunDAO futureExperimentRunDAO;
  private final FileHasher fileHasher;
  private final Validator validator = new Validator();
  private final ArtifactStoreDAO artifactStoreDAO;
  private final FutureEventDAO futureEventDAO;
  private final boolean isEventSystemEnabled;

  public VersioningServiceImpl(ServiceSet serviceSet, DAOSet daoSet, FileHasher fileHasher) {
    this.authService = serviceSet.getAuthService();
    this.mdbRoleService = serviceSet.getMdbRoleService();
    this.repositoryDAO = daoSet.getRepositoryDAO();
    this.commitDAO = daoSet.getCommitDAO();
    this.blobDAO = daoSet.getBlobDAO();
    this.futureExperimentRunDAO = daoSet.getFutureExperimentRunDAO();
    this.artifactStoreDAO = daoSet.getArtifactStoreDAO();
    this.fileHasher = fileHasher;
    this.futureEventDAO = daoSet.getFutureEventDAO();
    this.isEventSystemEnabled = serviceSet.getApp().mdbConfig.isEvent_system_enabled();
  }

  private void addEvent(
      long entityId,
      long workspaceId,
      String eventType,
      Optional<String> updatedField,
      Map<String, Object> extraFieldsMap,
      String eventMessage) {
    // Add succeeded event in local DB
    JsonObject eventMetadata = new JsonObject();
    eventMetadata.addProperty("entity_id", entityId);
    if (updatedField.isPresent() && !updatedField.get().isEmpty()) {
      eventMetadata.addProperty("updated_field", updatedField.get());
    }
    if (extraFieldsMap != null && !extraFieldsMap.isEmpty()) {
      JsonObject updatedFieldValue = new JsonObject();
      extraFieldsMap.forEach(
          (key, value) -> {
            if (value instanceof JsonElement) {
              updatedFieldValue.add(key, (JsonElement) value);
            } else {
              updatedFieldValue.addProperty(key, String.valueOf(value));
            }
          });
      eventMetadata.add("updated_field_value", updatedFieldValue);
    }
    eventMetadata.addProperty("message", eventMessage);
    futureEventDAO.addLocalEventWithBlocking(
        ModelDBServiceResourceTypes.REPOSITORY.name(), eventType, workspaceId, eventMetadata);
  }

  @Override
  public void listRepositories(
      ListRepositoriesRequest request, StreamObserver<Response> responseObserver) {
    try {
      if (request.hasPagination()) {

        if (request.getPagination().getPageLimit() < 1
            || request.getPagination().getPageLimit() > 100) {
          throw new ModelDBException("Page limit is invalid", Code.INVALID_ARGUMENT);
        }
      }
      var userInfo = authService.getCurrentLoginUserInfo();
      var response = repositoryDAO.listRepositories(request, userInfo);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, ListRepositoriesRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getRepository(
      GetRepositoryRequest request,
      StreamObserver<GetRepositoryRequest.Response> responseObserver) {
    try {
      var response = repositoryDAO.getRepository(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetRepositoryRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void createRepository(
      SetRepository request, StreamObserver<SetRepository.Response> responseObserver) {
    try {
      if (request.getRepository().getName().isEmpty()) {
        request =
            request
                .toBuilder()
                .setRepository(
                    request
                        .getRepository()
                        .toBuilder()
                        .setName(MetadataServiceImpl.createRandomName()))
                .build();
      }

      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY, null, ModelDBServiceActions.CREATE);
      var userInfo = authService.getCurrentLoginUserInfo();
      var requestBuilder = request.toBuilder();
      requestBuilder.setRepository(request.getRepository().toBuilder().setVersionNumber(1L));
      if (userInfo != null) {
        String vertaId = authService.getVertaIdFromUserInfo(userInfo);
        requestBuilder.setRepository(request.getRepository().toBuilder().setOwner(vertaId));
      }

      ModelDBUtils.validateEntityNameWithColonAndSlash(requestBuilder.getRepository().getName());

      var response = repositoryDAO.setRepository(requestBuilder.build(), userInfo, true);

      // Add succeeded event in local DB
      if (isEventSystemEnabled) {
        addEvent(
            response.getRepository().getId(),
            response.getRepository().getWorkspaceServiceId(),
            "add.resource.repository.add_repository_succeeded",
            Optional.empty(),
            Collections.emptyMap(),
            "repository logged successfully");
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, SetRepository.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateRepository(
      SetRepository request, StreamObserver<SetRepository.Response> responseObserver) {
    try {
      if (request.getRepository().getDescription().isEmpty()) {
        // FIXME: allow empty description
        throw new ModelDBException("Description is empty", Code.INVALID_ARGUMENT);
      } else if (request.getRepository().getName().isEmpty()) {
        request =
            request
                .toBuilder()
                .setRepository(
                    request
                        .getRepository()
                        .toBuilder()
                        .setName(MetadataServiceImpl.createRandomName()))
                .build();
      }

      ModelDBUtils.validateEntityNameWithColonAndSlash(request.getRepository().getName());
      var response =
          repositoryDAO.setRepository(request, authService.getCurrentLoginUserInfo(), false);

      // Add succeeded event in local DB
      if (isEventSystemEnabled) {
        addEvent(
            response.getRepository().getId(),
            response.getRepository().getWorkspaceServiceId(),
            UPDATE_REPOSITORY_EVENT_TYPE,
            Optional.empty(),
            Collections.emptyMap(),
            "repository updated successfully");
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, SetRepository.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteRepository(
      DeleteRepositoryRequest request,
      StreamObserver<DeleteRepositoryRequest.Response> responseObserver) {
    try {
      GetRepositoryRequest.Response repositoryResponse =
          repositoryDAO.getRepository(
              GetRepositoryRequest.newBuilder().setId(request.getRepositoryId()).build());
      var response =
          repositoryDAO.deleteRepository(
              request,
              commitDAO,
              futureExperimentRunDAO,
              true,
              RepositoryEnums.RepositoryTypeEnum.REGULAR);

      // Add succeeded event in local DB
      if (isEventSystemEnabled) {
        addEvent(
            repositoryResponse.getRepository().getId(),
            repositoryResponse.getRepository().getWorkspaceServiceId(),
            "delete.resource.repository.delete_repository_succeeded",
            Optional.empty(),
            Collections.emptyMap(),
            "repository deleted successfully");
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteRepositoryRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listCommits(
      ListCommitsRequest request, StreamObserver<ListCommitsRequest.Response> responseObserver) {
    try {
      var response =
          commitDAO.listCommits(
              request,
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              false);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, ListCommitsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getCommit(
      GetCommitRequest request, StreamObserver<GetCommitRequest.Response> responseObserver) {
    try {
      var commit =
          commitDAO.getCommit(
              request.getCommitSha(),
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()));
      var response = GetCommitRequest.Response.newBuilder().setCommit(commit).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetCommitRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void createCommit(
      CreateCommitRequest request, StreamObserver<CreateCommitRequest.Response> responseObserver) {
    try {
      if (request.getCommit().getParentShasList().isEmpty()) {
        throw new ModelDBException(
            "Parent commits not found in the CreateCommitRequest", Code.INVALID_ARGUMENT);
      } else if (request.getBlobsCount() > 0
          && (!request.getCommitBase().isEmpty() || request.getDiffsCount() > 0)) {
        throw new ModelDBException(
            "Blob list and commit base with diffs should not be allowed together",
            Code.INVALID_ARGUMENT);
      }

      if (request.getCommit().getMessage().isEmpty()) {
        throw new ModelDBException("Commit message should not be empty", Code.INVALID_ARGUMENT);
      }

      List<BlobContainer> blobContainers;
      final RepositoryFunction repositoryFunction =
          (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId(), true);
      if (request.getBlobsCount() != 0) {
        blobContainers = validateBlobs(request);
      } else if (request.getDiffsCount() != 0) {
        List<AutogenBlobDiff> diffs = validateBlobDiffs(request);
        blobContainers =
            blobDAO.convertBlobDiffsToBlobs(
                diffs,
                repositoryFunction,
                (session, repository) ->
                    commitDAO.getCommitEntity(session, request.getCommitBase(), repository));
      } else {
        blobContainers = Collections.emptyList();
      }
      var currentLoginUserInfo = authService.getCurrentLoginUserInfo();

      var response =
          commitDAO.setCommit(
              authService.getVertaIdFromUserInfo(currentLoginUserInfo),
              request.getCommit(),
              (session) -> blobDAO.setBlobs(session, blobContainers, fileHasher),
              (session, repoId, commitHash) ->
                  blobDAO.setBlobsAttributes(session, repoId, commitHash, blobContainers, true),
              repositoryFunction);

      // Add succeeded event in local DB
      if (isEventSystemEnabled) {
        GetRepositoryRequest.Response repositoryResponse =
            repositoryDAO.getRepository(
                GetRepositoryRequest.newBuilder().setId(request.getRepositoryId()).build());
        addEvent(
            repositoryResponse.getRepository().getId(),
            repositoryResponse.getRepository().getWorkspaceServiceId(),
            UPDATE_REPOSITORY_EVENT_TYPE,
            Optional.of("commit"),
            Collections.singletonMap("commit_hash", response.getCommit().getCommitSha()),
            "Commit added successfully");
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, CreateCommitRequest.Response.getDefaultInstance());
    }
  }

  private List<AutogenBlobDiff> validateBlobDiffs(CreateCommitRequest request)
      throws ModelDBException {
    List<AutogenBlobDiff> diffs = new LinkedList<>();
    for (BlobDiff blobDiff : request.getDiffsList()) {
      var autogenBlobDiff = AutogenBlobDiff.fromProto(blobDiff);
      validator.validate(autogenBlobDiff);
      diffs.add(autogenBlobDiff);
    }
    return diffs;
  }

  private List<BlobContainer> validateBlobs(CreateCommitRequest request) throws ModelDBException {
    List<BlobContainer> blobContainers = new LinkedList<>();
    for (BlobExpanded blobExpanded : request.getBlobsList()) {
      if (blobExpanded.getLocationList().isEmpty()) {
        throw new ModelDBException("Blob path should not be empty", Code.INVALID_ARGUMENT);
      }
      validator.validate(AutogenBlob.fromProto(blobExpanded.getBlob()));
      final var blobContainer = BlobContainer.create(blobExpanded);
      blobContainers.add(blobContainer);
    }
    return blobContainers;
  }

  @Override
  public void deleteCommit(
      DeleteCommitRequest request, StreamObserver<DeleteCommitRequest.Response> responseObserver) {
    try {
      commitDAO.deleteCommits(
          request.getRepositoryId(),
          Collections.singletonList(request.getCommitSha()),
          repositoryDAO);
      var response = DeleteCommitRequest.Response.newBuilder().build();

      // Add succeeded event in local DB
      if (isEventSystemEnabled) {
        GetRepositoryRequest.Response repositoryResponse =
            repositoryDAO.getRepository(
                GetRepositoryRequest.newBuilder().setId(request.getRepositoryId()).build());
        addEvent(
            repositoryResponse.getRepository().getId(),
            repositoryResponse.getRepository().getWorkspaceServiceId(),
            UPDATE_REPOSITORY_EVENT_TYPE,
            Optional.of("commit"),
            Collections.singletonMap("commit_hash", request.getCommitSha()),
            "Commit deleted successfully");
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteCommitRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listCommitBlobs(
      ListCommitBlobsRequest request,
      StreamObserver<ListCommitBlobsRequest.Response> responseObserver) {
    try {
      if (request.getCommitSha().isEmpty()) {
        throw new ModelDBException("Commit SHA should not be empty", Code.INVALID_ARGUMENT);
      }

      var response =
          blobDAO.getCommitBlobsList(
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              request.getCommitSha(),
              request.getLocationPrefixList());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, ListCommitBlobsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getCommitComponent(
      GetCommitComponentRequest request,
      StreamObserver<GetCommitComponentRequest.Response> responseObserver) {
    try {
      if (request.getCommitSha().isEmpty()) {
        throw new ModelDBException("Commit SHA should not be empty", Code.INVALID_ARGUMENT);
      }

      var response =
          blobDAO.getCommitComponent(
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              request.getCommitSha(),
              request.getLocationList());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetCommitComponentRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void computeRepositoryDiff(
      ComputeRepositoryDiffRequest request,
      StreamObserver<ComputeRepositoryDiffRequest.Response> responseObserver) {
    try {
      var response = blobDAO.computeRepositoryDiff(repositoryDAO, request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, ComputeRepositoryDiffRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void mergeRepositoryCommits(
      MergeRepositoryCommitsRequest request,
      StreamObserver<ai.verta.modeldb.versioning.MergeRepositoryCommitsRequest.Response>
          responseObserver) {
    try {
      var mergeResponse = blobDAO.mergeCommit(repositoryDAO, request);

      // Add succeeded event in local DB
      if (!mergeResponse.hasCommonBase() && isEventSystemEnabled) {
        GetRepositoryRequest.Response repositoryResponse =
            repositoryDAO.getRepository(
                GetRepositoryRequest.newBuilder().setId(request.getRepositoryId()).build());
        addEvent(
            repositoryResponse.getRepository().getId(),
            repositoryResponse.getRepository().getWorkspaceServiceId(),
            UPDATE_REPOSITORY_EVENT_TYPE,
            Optional.of("commit"),
            Collections.singletonMap("commit_hash", mergeResponse.getCommit().getCommitSha()),
            mergeResponse.getCommit().getMessage());
      }

      responseObserver.onNext(mergeResponse);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, MergeRepositoryCommitsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void revertRepositoryCommits(
      RevertRepositoryCommitsRequest request,
      StreamObserver<RevertRepositoryCommitsRequest.Response> responseObserver) {
    try {
      var mergeResponse = blobDAO.revertCommit(repositoryDAO, request);

      if (isEventSystemEnabled) {
        GetRepositoryRequest.Response repositoryResponse =
            repositoryDAO.getRepository(
                GetRepositoryRequest.newBuilder().setId(request.getRepositoryId()).build());
        addEvent(
            repositoryResponse.getRepository().getId(),
            repositoryResponse.getRepository().getWorkspaceServiceId(),
            UPDATE_REPOSITORY_EVENT_TYPE,
            Optional.of("commit"),
            Collections.singletonMap("commit_hash", mergeResponse.getCommit().getCommitSha()),
            mergeResponse.getCommit().getMessage());
      }

      responseObserver.onNext(mergeResponse);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, RevertRepositoryCommitsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listBranches(
      ListBranchesRequest request, StreamObserver<ListBranchesRequest.Response> responseObserver) {
    try {
      var response = repositoryDAO.listBranches(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, ListBranchesRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getBranch(
      GetBranchRequest request, StreamObserver<GetBranchRequest.Response> responseObserver) {
    try {
      var response =
          repositoryDAO.getBranch(request, true, RepositoryEnums.RepositoryTypeEnum.REGULAR);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetBranchRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void setBranch(
      SetBranchRequest request, StreamObserver<SetBranchRequest.Response> responseObserver) {
    try {
      var response =
          repositoryDAO.setBranch(request, true, RepositoryEnums.RepositoryTypeEnum.REGULAR);

      // Add succeeded event in local DB
      if (isEventSystemEnabled) {
        Map<String, Object> updatedFieldValueMap = new HashMap<>();
        updatedFieldValueMap.put("branch", request.getBranch());
        updatedFieldValueMap.put("commit_hash", request.getCommitSha());
        GetRepositoryRequest.Response repositoryResponse =
            repositoryDAO.getRepository(
                GetRepositoryRequest.newBuilder().setId(request.getRepositoryId()).build());
        addEvent(
            repositoryResponse.getRepository().getId(),
            repositoryResponse.getRepository().getWorkspaceServiceId(),
            UPDATE_REPOSITORY_EVENT_TYPE,
            Optional.of("branch"),
            updatedFieldValueMap,
            String.format("Set branch '%s' successfully", request.getBranch()));
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, SetBranchRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteBranch(
      DeleteBranchRequest request, StreamObserver<DeleteBranchRequest.Response> responseObserver) {
    try {
      if (request.getBranch().isEmpty()) {
        throw new ModelDBException(
            "Branch not found in the DeleteBranchRequest", Code.INVALID_ARGUMENT);
      }
      var response = repositoryDAO.deleteBranch(request);

      // Add succeeded event in local DB
      if (isEventSystemEnabled) {
        GetRepositoryRequest.Response repositoryResponse =
            repositoryDAO.getRepository(
                GetRepositoryRequest.newBuilder().setId(request.getRepositoryId()).build());
        addEvent(
            repositoryResponse.getRepository().getId(),
            repositoryResponse.getRepository().getWorkspaceServiceId(),
            UPDATE_REPOSITORY_EVENT_TYPE,
            Optional.of("branch"),
            Collections.singletonMap("branch", request.getBranch()),
            String.format("Branch '%s' deleted successfully", request.getBranch()));
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteBranchRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listCommitsLog(
      ListCommitsLogRequest request,
      StreamObserver<ListCommitsLogRequest.Response> responseObserver) {
    try {
      var response = repositoryDAO.listCommitsLog(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, ListCommitsLogRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listTags(
      ListTagsRequest request, StreamObserver<ListTagsRequest.Response> responseObserver) {
    try {
      var response = repositoryDAO.listTags(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, ListTagsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getTag(
      GetTagRequest request, StreamObserver<GetTagRequest.Response> responseObserver) {
    try {
      var response = repositoryDAO.getTag(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetTagRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void setTag(
      SetTagRequest request, StreamObserver<SetTagRequest.Response> responseObserver) {
    try {
      var response = repositoryDAO.setTag(request);

      // Add succeeded event in local DB
      if (isEventSystemEnabled) {
        Map<String, Object> updatedFieldValueMap = new HashMap<>();
        updatedFieldValueMap.put("tag", request.getTag());
        updatedFieldValueMap.put("commit_hash", request.getCommitSha());
        GetRepositoryRequest.Response repositoryResponse =
            repositoryDAO.getRepository(
                GetRepositoryRequest.newBuilder().setId(request.getRepositoryId()).build());
        addEvent(
            repositoryResponse.getRepository().getId(),
            repositoryResponse.getRepository().getWorkspaceServiceId(),
            UPDATE_REPOSITORY_EVENT_TYPE,
            Optional.of("tag"),
            updatedFieldValueMap,
            String.format("Set tag '%s' successfully", request.getTag()));
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, SetTagRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteTag(
      DeleteTagRequest request, StreamObserver<DeleteTagRequest.Response> responseObserver) {
    try {
      if (request.getTag().isEmpty()) {
        throw new ModelDBException("Tag not found in the DeleteTagRequest", Code.INVALID_ARGUMENT);
      }
      var response = repositoryDAO.deleteTag(request);

      // Add succeeded event in local DB
      if (isEventSystemEnabled) {
        GetRepositoryRequest.Response repositoryResponse =
            repositoryDAO.getRepository(
                GetRepositoryRequest.newBuilder().setId(request.getRepositoryId()).build());
        addEvent(
            repositoryResponse.getRepository().getId(),
            repositoryResponse.getRepository().getWorkspaceServiceId(),
            UPDATE_REPOSITORY_EVENT_TYPE,
            Optional.of("tag"),
            Collections.singletonMap("tag", request.getTag()),
            String.format("Tag '%s' deleted successfully", request.getTag()));
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, DeleteTagRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void findRepositories(
      FindRepositories request, StreamObserver<FindRepositories.Response> responseObserver) {
    try {
      var response = repositoryDAO.findRepositories(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, FindRepositories.Response.getDefaultInstance());
    }
  }

  @Override
  public void findRepositoriesBlobs(
      FindRepositoriesBlobs request,
      StreamObserver<FindRepositoriesBlobs.Response> responseObserver) {
    try {
      List<Repository> repositories = new LinkedList<>();
      var response = blobDAO.findRepositoriesBlobs(commitDAO, request, repositories);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, FindRepositoriesBlobs.Response.getDefaultInstance());
    }
  }

  @Override
  public void getUrlForBlobVersioned(
      GetUrlForBlobVersioned request,
      StreamObserver<GetUrlForBlobVersioned.Response> responseObserver) {
    try {
      // Validate request parameters
      validateGetUrlForVersionedBlobRequest(request);

      var response =
          blobDAO.getUrlForVersionedBlob(
              artifactStoreDAO,
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              (session, repository) ->
                  commitDAO.getCommitEntity(session, request.getCommitSha(), repository),
              request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetUrlForBlobVersioned.Response.getDefaultInstance());
    }
  }

  private void validateGetUrlForVersionedBlobRequest(GetUrlForBlobVersioned request)
      throws ModelDBException {
    String errorMessage = null;
    if (request.getCommitSha().isEmpty()
        && request.getLocationList().isEmpty()
        && request.getMethod().isEmpty()
        && request.getPathDatasetComponentBlobPath().isEmpty()) {
      errorMessage =
          "Commit hash and Blob location and Method type AND Blob path not found in GetUrlForBlobVersioned request";
    } else if (request.getCommitSha().isEmpty()) {
      errorMessage = "Commit hash not found in GetUrlForBlobVersioned request";
    } else if (request.getLocationList().isEmpty()) {
      errorMessage = "Blob location not found in GetUrlForBlobVersioned request";
    } else if (request.getPathDatasetComponentBlobPath().isEmpty()) {
      errorMessage = "Blob path not found in GetUrlForBlobVersioned request";
    } else if (request.getMethod().isEmpty()) {
      errorMessage = "Method is not found in GetUrlForBlobVersioned request";
    }
    if (errorMessage != null) {
      LOGGER.warn(errorMessage);
      throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
    }
  }

  @Override
  public void commitVersionedBlobArtifactPart(
      CommitVersionedBlobArtifactPart request,
      StreamObserver<CommitVersionedBlobArtifactPart.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getCommitSha().isEmpty()
          && request.getLocationList().isEmpty()
          && !request.hasArtifactPart()) {
        errorMessage =
            "Commit hash and Location and Artifact Part not found in CommitVersionedBlobArtifactPart request";
      } else if (request.getCommitSha().isEmpty()) {
        errorMessage = "Commit hash not found in CommitVersionedBlobArtifactPart request";
      } else if (request.getLocationList().isEmpty()) {
        errorMessage = "Location not found in CommitVersionedBlobArtifactPart request";
      } else if (!request.hasArtifactPart()) {
        errorMessage = "Artifact Part not found in CommitVersionedBlobArtifactPart request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      var response =
          blobDAO.commitVersionedBlobArtifactPart(
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              (session, repository) ->
                  commitDAO.getCommitEntity(session, request.getCommitSha(), repository),
              request.getLocationList(),
              request.getPathDatasetComponentBlobPath(),
              request.getArtifactPart());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, CommitVersionedBlobArtifactPart.Response.getDefaultInstance());
    }
  }

  @Override
  public void getCommittedVersionedBlobArtifactParts(
      GetCommittedVersionedBlobArtifactParts request,
      StreamObserver<GetCommittedVersionedBlobArtifactParts.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getCommitSha().isEmpty() && request.getLocationList().isEmpty()) {
        errorMessage =
            "Commit hash and Location not found in GetCommittedVersionedBlobArtifactParts request";
      } else if (request.getCommitSha().isEmpty()) {
        errorMessage = "Commit hash not found in GetCommittedVersionedBlobArtifactParts request";
      } else if (request.getLocationList().isEmpty()) {
        errorMessage = "Location not found in GetCommittedVersionedBlobArtifactParts request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      var response =
          blobDAO.getCommittedVersionedBlobArtifactParts(
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              (session, repository) ->
                  commitDAO.getCommitEntity(session, request.getCommitSha(), repository),
              request.getLocationList(),
              request.getPathDatasetComponentBlobPath());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver,
          e,
          GetCommittedVersionedBlobArtifactParts.Response.getDefaultInstance());
    }
  }

  @Override
  public void commitMultipartVersionedBlobArtifact(
      CommitMultipartVersionedBlobArtifact request,
      StreamObserver<CommitMultipartVersionedBlobArtifact.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getCommitSha().isEmpty()
          && request.getLocationList().isEmpty()
          && request.getPathDatasetComponentBlobPath().isEmpty()) {
        errorMessage =
            "Commit hash and Location and path not found in CommitMultipartVersionedBlobArtifact request";
      } else if (request.getCommitSha().isEmpty()) {
        errorMessage = "Commit hash not found in CommitMultipartVersionedBlobArtifact request";
      } else if (request.getLocationList().isEmpty()) {
        errorMessage = "Location not found in CommitMultipartVersionedBlobArtifact request";
      } else if (request.getPathDatasetComponentBlobPath().isEmpty()) {
        errorMessage = "Path not found in CommitMultipartVersionedBlobArtifact request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      var response =
          blobDAO.commitMultipartVersionedBlobArtifact(
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              (session, repository) ->
                  commitDAO.getCommitEntity(session, request.getCommitSha(), repository),
              request.getLocationList(),
              request.getPathDatasetComponentBlobPath(),
              artifactStoreDAO::commitMultipart);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, CommitMultipartVersionedBlobArtifact.Response.getDefaultInstance());
    }
  }
}
