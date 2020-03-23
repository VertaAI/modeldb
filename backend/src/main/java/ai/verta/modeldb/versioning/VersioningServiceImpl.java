package ai.verta.modeldb.versioning;

import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.DiffStatusEnum.DiffStatus;
import ai.verta.modeldb.versioning.ListRepositoriesRequest.Response;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob.Builder;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceImplBase;
import ai.verta.modeldb.versioning.blob.container.BlobContainer;
import ai.verta.modeldb.versioning.blob.container.CodeContainer;
import ai.verta.modeldb.versioning.blob.container.ConfigContainer;
import ai.verta.modeldb.versioning.blob.container.EnvironmentContainer;
import ai.verta.uac.UserInfo;
import io.grpc.Status.Code;
import io.grpc.stub.StreamObserver;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VersioningServiceImpl extends VersioningServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(VersioningServiceImpl.class);
  private final AuthService authService;
  private final RoleService roleService;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final BlobDAO blobDAO;
  private final ExperimentDAO experimentDAO;
  private final ExperimentRunDAO experimentRunDAO;
  private final ModelDBAuthInterceptor modelDBAuthInterceptor;
  private final FileHasher fileHasher;

  public VersioningServiceImpl(
      AuthService authService,
      RoleService roleService,
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      BlobDAO blobDAO,
      ExperimentDAO experimentDAO,
      ExperimentRunDAO experimentRunDAO,
      ModelDBAuthInterceptor modelDBAuthInterceptor,
      FileHasher fileHasher) {
    this.authService = authService;
    this.roleService = roleService;
    this.repositoryDAO = repositoryDAO;
    this.commitDAO = commitDAO;
    this.blobDAO = blobDAO;
    this.experimentDAO = experimentDAO;
    this.experimentRunDAO = experimentRunDAO;
    this.modelDBAuthInterceptor = modelDBAuthInterceptor;
    this.fileHasher = fileHasher;
  }

  @Override
  public void listRepositories(
      ListRepositoriesRequest request, StreamObserver<Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        if (request.hasPagination()) {

          if (request.getPagination().getPageLimit() < 1
              || request.getPagination().getPageLimit() > 100) {
            throw new ModelDBException("Page limit is invalid", Code.INVALID_ARGUMENT);
          }
        }
        Response response = repositoryDAO.listRepositories(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, ListRepositoriesRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getRepository(
      GetRepositoryRequest request,
      StreamObserver<GetRepositoryRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        GetRepositoryRequest.Response response = repositoryDAO.getRepository(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetRepositoryRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void createRepository(
      SetRepository request, StreamObserver<SetRepository.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        if (request.getRepository().getName().isEmpty()) {
          throw new ModelDBException("Repository name is empty", Code.INVALID_ARGUMENT);
        }

        UserInfo userInfo = authService.getCurrentLoginUserInfo();
        SetRepository.Builder requestBuilder = request.toBuilder();
        if (userInfo != null) {
          String vertaId = authService.getVertaIdFromUserInfo(userInfo);
          requestBuilder.setRepository(request.getRepository().toBuilder().setOwner(vertaId));
        }
        SetRepository.Response response = repositoryDAO.setRepository(requestBuilder.build(), true);

        RepositoryIdentification repositoryId =
            RepositoryIdentification.newBuilder()
                .setRepoId(response.getRepository().getId())
                .build();
        CreateCommitRequest.Response commitResponse =
            commitDAO.setCommit(
                authService.getVertaIdFromUserInfo(userInfo),
                Commit.newBuilder().setMessage(ModelDBConstants.INITIAL_COMMIT_MESSAGE).build(),
                () -> FileHasher.getSha(new String()),
                (session) -> repositoryDAO.getRepositoryById(session, repositoryId));

        repositoryDAO.setBranch(
            SetBranchRequest.newBuilder()
                .setCommitSha(commitResponse.getCommit().getCommitSha())
                .setBranch(ModelDBConstants.MASTER_BRANCH)
                .setRepositoryId(repositoryId)
                .build());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, SetRepository.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateRepository(
      SetRepository request, StreamObserver<SetRepository.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        if (request.getRepository().getName().isEmpty()) {
          throw new ModelDBException("Repository name is empty", Code.INVALID_ARGUMENT);
        }

        SetRepository.Response response = repositoryDAO.setRepository(request, false);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, SetRepository.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteRepository(
      DeleteRepositoryRequest request,
      StreamObserver<DeleteRepositoryRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        DeleteRepositoryRequest.Response response = repositoryDAO.deleteRepository(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteRepositoryRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listCommits(
      ListCommitsRequest request, StreamObserver<ListCommitsRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
      ListCommitsRequest.Response response =
          commitDAO.listCommits(
              request,
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()));
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, ListCommitsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getCommit(
      GetCommitRequest request, StreamObserver<GetCommitRequest.Response> responseObserver) {

    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        Commit commit =
            commitDAO.getCommit(
                request.getCommitSha(),
                (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()));
        responseObserver.onNext(GetCommitRequest.Response.newBuilder().setCommit(commit).build());
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetCommitRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void createCommit(
      CreateCommitRequest request, StreamObserver<CreateCommitRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
      if (request.getBlobsCount() == 0) {
        if (request.getCommitBase().isEmpty() || request.getDiffsCount() == 0) {
          throw new ModelDBException(
              "Blob list should not be empty or commit base and diffs should be specified",
              Code.INVALID_ARGUMENT);
        }
      } else if (request.getCommit().getParentShasList().isEmpty()) {
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
          (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId());
      if (request.getBlobsCount() != 0) {
        blobContainers = validateBlobs(request);
      } else {
        //validateBlobDiffs(request);
        blobContainers =
            blobDAO.convertBlobDiffsToBlobs(
                request,
                repositoryFunction,
                (session, repository) ->
                    commitDAO.getCommitEntity(session, request.getCommitBase(), repository));
        for (BlobContainer blobContainer: blobContainers) {
          blobContainer.validate();
        }
      }
      UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();

      CreateCommitRequest.Response response =
          commitDAO.setCommit(
              authService.getVertaIdFromUserInfo(currentLoginUserInfo),
              request.getCommit(),
              () -> blobDAO.setBlobs(blobContainers, fileHasher),
              repositoryFunction);

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, CreateCommitRequest.Response.getDefaultInstance());
    }
  }

  private void validateBlobDiffs(CreateCommitRequest request) throws ModelDBException {
    for (BlobDiff blobDiff : request.getDiffsList()) {
      if (blobDiff.getLocationList().isEmpty()) {
        throw new ModelDBException("Blob diff location is empty", Code.INVALID_ARGUMENT);
      }
      switch (blobDiff.getContentCase()) {
        case CODE:
          CodeDiff code = blobDiff.getCode();
          switch (code.getContentCase()) {
            case GIT:
              validate(code.getGit());
              break;
            case NOTEBOOK:
              NotebookCodeDiff notebook = code.getNotebook();
              validate(notebook.getGitRepo());
              if (notebook.hasPath()) {
                validate(notebook.getPath());
              }
              break;
            default:
              throw new ModelDBException("Unknown diff type", Code.INVALID_ARGUMENT);
          }
          break;
        case CONFIG:
          ConfigDiff configDIff = blobDiff.getConfig();
          List<HyperparameterConfigDiff> hyperparametersList = configDIff.getHyperparametersList();
          for (HyperparameterConfigDiff hyperparameterConfigDiff : hyperparametersList) {
            validate(hyperparameterConfigDiff);
          }
          List<HyperparameterSetConfigDiff> hyperparameterSetList =
              configDIff.getHyperparameterSetList();
          for (HyperparameterSetConfigDiff hyperparameterSetConfigDiff : hyperparameterSetList) {
            validate(hyperparameterSetConfigDiff);
          }
          break;
        case DATASET:
          DatasetDiff datasetDiff = blobDiff.getDataset();
          switch (datasetDiff.getContentCase()) {
            case PATH:
              for (PathDatasetComponentDiff pathDatasetComponentDiff :
                  datasetDiff.getPath().getComponentsList()) {
                validate(pathDatasetComponentDiff);
              }
              break;
            case S3:
              for (S3DatasetComponentDiff s3DatasetComponentDiff :
                  datasetDiff.getS3().getComponentsList()) {
                validate(s3DatasetComponentDiff.getPath());
              }
              break;
            default:
              throw new ModelDBException("Unknown diff type", Code.INVALID_ARGUMENT);
          }
          break;
        case ENVIRONMENT:
          EnvironmentDiff environmentDiff = blobDiff.getEnvironment();
          if (environmentDiff.hasCommandLine()) {
            validate(environmentDiff.getCommandLine());
          }
          List<EnvironmentVariablesDiff> environmentVariablesList =
              environmentDiff.getEnvironmentVariablesList();
          List<EnvironmentVariablesBlob> environmentVariablesBlobList = new LinkedList<>();
          Set<EnvironmentVariablesBlob> environmentVariablesBlobSet = new HashSet<>();
          for (EnvironmentVariablesDiff environmentVariablesDiff : environmentVariablesList) {
            validate(environmentVariablesDiff);
            if (environmentVariablesDiff.hasB()) {
              environmentVariablesBlobList.add(environmentVariablesDiff.getB());
              environmentVariablesBlobSet.add(environmentVariablesDiff.getB());
            }
          }
          if (environmentVariablesBlobList.size() != environmentVariablesBlobSet.size()) {
            throw new ModelDBException("There are recurring variables", Code.INVALID_ARGUMENT);
          }
          switch (environmentDiff.getContentCase()) {
            case DOCKER:
              DockerEnvironmentDiff docker = environmentDiff.getDocker();
              validate(docker);
              break;
            case PYTHON:
              PythonEnvironmentDiff pythonEnvironmentDiff = environmentDiff.getPython();
              List<PythonRequirementEnvironmentDiff> pythonEnvironmentDiffRequirementsList =
                  pythonEnvironmentDiff.getRequirementsList();
              List<PythonRequirementEnvironmentBlob> requirements = new LinkedList<>();
              Set<PythonRequirementEnvironmentBlob> requirementsSet = new HashSet<>();
              for (PythonRequirementEnvironmentDiff pythonRequirementEnvironmentDiff :
                  pythonEnvironmentDiffRequirementsList) {
                validate(pythonRequirementEnvironmentDiff, "requirement");
                if (pythonRequirementEnvironmentDiff.hasB()) {
                  requirements.add(pythonRequirementEnvironmentDiff.getB());
                  requirementsSet.add(pythonRequirementEnvironmentDiff.getB());
                }
              }
              List<PythonRequirementEnvironmentDiff> pythonEnvironmentDiffConstraintsList =
                  pythonEnvironmentDiff.getConstraintsList();
              for (PythonRequirementEnvironmentDiff pythonConstraintEnvironmentDiff :
                  pythonEnvironmentDiffConstraintsList) {
                validate(pythonConstraintEnvironmentDiff, "constraint");
                if (pythonConstraintEnvironmentDiff.hasB()) {
                  requirements.add(pythonConstraintEnvironmentDiff.getB());
                  requirementsSet.add(pythonConstraintEnvironmentDiff.getB());
                }
              }
              if (requirementsSet.size() != requirements.size()) {
                throw new ModelDBException(
                    "There are recurring requirements or constraints", Code.INVALID_ARGUMENT);
              }
              break;
          }
          break;
        default:
          throw new ModelDBException("Unknown diff type", Code.INVALID_ARGUMENT);
      }
      checkStatus(blobDiff.getStatus());
    }
  }

  private void validate(
      PythonRequirementEnvironmentDiff pythonRequirementEnvironmentDiff, String name)
      throws ModelDBException {
    Supplier<Optional<String>> supplierA =
        () ->
            EnvironmentContainer.validateReturnMessage(
                pythonRequirementEnvironmentDiff.getA(), name);
    Supplier<Optional<String>> supplierB =
        () ->
            EnvironmentContainer.validateReturnMessage(
                pythonRequirementEnvironmentDiff.getB(), name);
    Supplier<Optional<String>> supplierAB = () -> getSupplierAB(supplierA, supplierB);
    checkStatus(pythonRequirementEnvironmentDiff.getStatus(), supplierA, supplierB, supplierAB);
  }

  private void validate(DockerEnvironmentDiff dockerEnvironmentDiff) throws ModelDBException {
    Supplier<Optional<String>> supplierA =
        () -> EnvironmentContainer.validateReturnMessage(dockerEnvironmentDiff.getA());
    Supplier<Optional<String>> supplierB =
        () -> EnvironmentContainer.validateReturnMessage(dockerEnvironmentDiff.getB());
    Supplier<Optional<String>> supplierAB = () -> getSupplierAB(supplierA, supplierB);
    checkStatus(dockerEnvironmentDiff.getStatus(), supplierA, supplierB, supplierAB);
  }

  private void validate(EnvironmentVariablesDiff environmentVariablesDiff) throws ModelDBException {
    Supplier<Optional<String>> supplierA =
        () -> EnvironmentContainer.validateReturnMessage(environmentVariablesDiff.getA());
    Supplier<Optional<String>> supplierB =
        () -> EnvironmentContainer.validateReturnMessage(environmentVariablesDiff.getB());
    Supplier<Optional<String>> supplierAB = () -> getSupplierAB(supplierA, supplierB);
    checkStatus(environmentVariablesDiff.getStatus(), supplierA, supplierB, supplierAB);
  }

  private void validate(CommandLineEnvironmentDiff commandLine) throws ModelDBException {
    Supplier<Optional<String>> supplierA =
        () -> EnvironmentContainer.validateReturnMessage(commandLine.getAList());
    Supplier<Optional<String>> supplierB =
        () -> EnvironmentContainer.validateReturnMessage(commandLine.getBList());
    Supplier<Optional<String>> supplierAB = () -> getSupplierAB(supplierA, supplierB);
    checkStatus(commandLine.getStatus(), supplierA, supplierB, supplierAB);
  }

  private void validate(PathDatasetComponentDiff path) throws ModelDBException {
    Supplier<Optional<String>> supplierA = () -> BlobContainer.validateReturnMessage(path.getA());
    Supplier<Optional<String>> supplierB = () -> BlobContainer.validateReturnMessage(path.getB());
    Supplier<Optional<String>> supplierAB = () -> getSupplierAB(supplierA, supplierB);
    checkStatus(path.getStatus(), supplierA, supplierB, supplierAB);
  }

  private Optional<String> getSupplierAB(
      Supplier<Optional<String>> supplierA, Supplier<Optional<String>> supplierB) {
    Optional<String> result = supplierA.get();
    if (result.isPresent()) {
      return result;
    }
    return supplierB.get();
  }

  private void validate(HyperparameterConfigDiff hyperparameterConfigDiff) throws ModelDBException {
    Supplier<Optional<String>> supplierA =
        () -> ConfigContainer.validateReturnMessage(hyperparameterConfigDiff.getA());
    Supplier<Optional<String>> supplierB =
        () -> ConfigContainer.validateReturnMessage(hyperparameterConfigDiff.getB());
    Supplier<Optional<String>> supplierAB = () -> getSupplierAB(supplierA, supplierB);
    checkStatus(hyperparameterConfigDiff.getStatus(), supplierA, supplierB, supplierAB);
  }

  private void validate(HyperparameterSetConfigDiff hyperparameterSetConfigDiff)
      throws ModelDBException {
    Supplier<Optional<String>> supplierA =
        () -> ConfigContainer.validateReturnMessage(hyperparameterSetConfigDiff.getA());
    Supplier<Optional<String>> supplierB =
        () -> ConfigContainer.validateReturnMessage(hyperparameterSetConfigDiff.getB());
    Supplier<Optional<String>> supplierAB = () -> getSupplierAB(supplierA, supplierB);
    checkStatus(hyperparameterSetConfigDiff.getStatus(), supplierA, supplierB, supplierAB);
  }

  private void checkStatus(DiffStatus status) throws ModelDBException {
    checkStatus(status, Optional::empty, Optional::empty, Optional::empty);
  }

  private void validate(GitCodeDiff git) throws ModelDBException {
    Supplier<Optional<String>> supplierA = () -> CodeContainer.validateReturnMessage(git.getA());
    Supplier<Optional<String>> supplierB = () -> CodeContainer.validateReturnMessage(git.getB());
    Supplier<Optional<String>> supplierAB = () -> getSupplierAB(supplierA, supplierB);
    checkStatus(git.getStatus(), supplierA, supplierB, supplierAB);
  }

  private void checkStatus(
      DiffStatus diffStatus,
      Supplier<Optional<String>> deleted,
      Supplier<Optional<String>> added,
      Supplier<Optional<String>> modified)
      throws ModelDBException {
    final Optional<String> message;
    switch (diffStatus) {
      case DELETED:
        message = deleted.get();
        break;
      case ADDED:
        message = added.get();
        break;
      case MODIFIED:
        message = modified.get();
        break;
      default:
        message = Optional.of("Unknown diff status specified");
    }
    if (message.isPresent()) {
      throw new ModelDBException(message.get(), Code.INVALID_ARGUMENT);
    }
  }

  private List<BlobContainer> validateBlobs(CreateCommitRequest request) throws ModelDBException {
    List<BlobContainer> blobContainers = new LinkedList<>();
    for (BlobExpanded blobExpanded : request.getBlobsList()) {
      if (blobExpanded.getLocationList().isEmpty()) {
        throw new ModelDBException("Blob path should not be empty", Code.INVALID_ARGUMENT);
      }
      final BlobContainer blobContainer = BlobContainer.create(blobExpanded);
      blobContainer.validate();
      blobContainers.add(blobContainer);
    }
    return blobContainers;
  }

  @Override
  public void deleteCommit(
      DeleteCommitRequest request, StreamObserver<DeleteCommitRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        DeleteCommitRequest.Response response =
            commitDAO.deleteCommit(
                request.getCommitSha(),
                (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()));
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteCommitRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listCommitBlobs(
      ListCommitBlobsRequest request,
      StreamObserver<ListCommitBlobsRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
      if (request.getCommitSha().isEmpty()) {
        throw new ModelDBException("Commit SHA should not be empty", Code.INVALID_ARGUMENT);
      }

      ListCommitBlobsRequest.Response response =
          blobDAO.getCommitBlobsList(
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              request.getCommitSha(),
              request.getLocationPrefixList());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, ListCommitBlobsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getCommitComponent(
      GetCommitComponentRequest request,
      StreamObserver<GetCommitComponentRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
      if (request.getCommitSha().isEmpty()) {
        throw new ModelDBException("Commit SHA should not be empty", Code.INVALID_ARGUMENT);
      }

      GetCommitComponentRequest.Response response =
          blobDAO.getCommitComponent(
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              request.getCommitSha(),
              request.getLocationList());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetCommitComponentRequest.Response.getDefaultInstance());
    }
  }

  private Builder getPathInfo(PathDatasetComponentBlob path)
      throws ModelDBException, NoSuchAlgorithmException {
    // TODO: md5
    return path.toBuilder().setSha256(generateAndValidateSha(path));
  }

  String generateAndValidateSha(PathDatasetComponentBlob path)
      throws ModelDBException, NoSuchAlgorithmException {
    String sha = path.getSha256();
    String generatedSha = fileHasher.getSha(path);
    if (!sha.isEmpty() && !sha.equals(generatedSha)) {
      throw new ModelDBException("Checksum is wrong", Code.INVALID_ARGUMENT);
    }
    return generatedSha;
  }

  @Override
  public void computeRepositoryDiff(
      ComputeRepositoryDiffRequest request,
      StreamObserver<ComputeRepositoryDiffRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
      ComputeRepositoryDiffRequest.Response response =
          blobDAO.computeRepositoryDiff(
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, ComputeRepositoryDiffRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listBranches(
      ListBranchesRequest request, StreamObserver<ListBranchesRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        ListBranchesRequest.Response response = repositoryDAO.listBranches(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, ListBranchesRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getBranch(
      GetBranchRequest request, StreamObserver<GetBranchRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        GetBranchRequest.Response response = repositoryDAO.getBranch(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetBranchRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void setBranch(
      SetBranchRequest request, StreamObserver<SetBranchRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        SetBranchRequest.Response response = repositoryDAO.setBranch(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, SetBranchRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteBranch(
      DeleteBranchRequest request, StreamObserver<DeleteBranchRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        if (request.getBranch().isEmpty()) {
          throw new ModelDBException(
              "Branch not found in the DeleteBranchRequest", Code.INVALID_ARGUMENT);
        }
        DeleteBranchRequest.Response response = repositoryDAO.deleteBranch(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteBranchRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listBranchCommits(
      ListBranchCommitsRequest request,
      StreamObserver<ListBranchCommitsRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        ListBranchCommitsRequest.Response response = repositoryDAO.listBranchCommits(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, ListBranchCommitsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listTags(
      ListTagsRequest request, StreamObserver<ListTagsRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        ListTagsRequest.Response response = repositoryDAO.listTags(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, ListTagsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getTag(
      GetTagRequest request, StreamObserver<GetTagRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        GetTagRequest.Response response = repositoryDAO.getTag(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, GetTagRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void setTag(
      SetTagRequest request, StreamObserver<SetTagRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        SetTagRequest.Response response = repositoryDAO.setTag(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, SetTagRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteTag(
      DeleteTagRequest request, StreamObserver<DeleteTagRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(modelDBAuthInterceptor.getMethodName())) {
        if (request.getTag().isEmpty()) {
          throw new ModelDBException(
              "Tag not found in the DeleteTagRequest", Code.INVALID_ARGUMENT);
        }
        DeleteTagRequest.Response response = repositoryDAO.deleteTag(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteTagRequest.Response.getDefaultInstance());
    }
  }
}
