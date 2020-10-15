package ai.verta.modeldb.lineage;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.AddLineage;
import ai.verta.modeldb.DeleteLineage;
import ai.verta.modeldb.FindAllInputs;
import ai.verta.modeldb.FindAllInputsOutputs;
import ai.verta.modeldb.FindAllInputsOutputs.Response;
import ai.verta.modeldb.FindAllInputsOutputs.Response.Builder;
import ai.verta.modeldb.FindAllOutputs;
import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.LineageEntryBatchResponse;
import ai.verta.modeldb.LineageEntryBatchResponseSingle;
import ai.verta.modeldb.LineageEntryEnum.LineageEntryType;
import ai.verta.modeldb.LineageServiceGrpc.LineageServiceImplBase;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

public class LineageServiceImpl extends LineageServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(LineageServiceImpl.class);
  private final ExperimentRunDAO experimentDAO;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final LineageDAO lineageDAO;
  private final BlobDAO blobDAO;
  private final RoleService roleService;

  public LineageServiceImpl(
      LineageDAO lineageDAO,
      ExperimentRunDAO experimentRunDAO,
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      BlobDAO blobDAO,
      RoleService roleService) {
    this.lineageDAO = lineageDAO;
    this.experimentDAO = experimentRunDAO;
    this.repositoryDAO = repositoryDAO;
    this.commitDAO = commitDAO;
    this.blobDAO = blobDAO;
    this.roleService = roleService;
  }

  @Override
  public void addLineage(AddLineage request, StreamObserver<AddLineage.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      if (request.getInputCount() == 0 && request.getOutputCount() == 0) {
        throw new ModelDBException("Input and output not specified", Code.INVALID_ARGUMENT);
      }
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
        AddLineage.Response response =
            lineageDAO.addLineage(request, this::checkResourcesExistsAndAccessible);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, AddLineage.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteLineage(
      DeleteLineage request, StreamObserver<DeleteLineage.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
        DeleteLineage.Response response =
            lineageDAO.deleteLineage(request, this::checkResourcesExistsAndAccessible);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, DeleteLineage.Response.getDefaultInstance());
    }
  }

  @Override
  public void findAllInputs(
      FindAllInputs request, StreamObserver<FindAllInputs.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      if (request.getItemsCount() == 0) {
        throw new ModelDBException("Items not specified", Code.INVALID_ARGUMENT);
      }
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
        FindAllInputs.Response response =
            lineageDAO.findAllInputs(
                request, this::checkResourcesExistsAndAccessible, this::filterInput);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, FindAllInputs.Response.getDefaultInstance());
    }
  }

  @Override
  public void findAllOutputs(
      FindAllOutputs request, StreamObserver<FindAllOutputs.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      if (request.getItemsCount() == 0) {
        throw new ModelDBException("Items not specified", Code.INVALID_ARGUMENT);
      }
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
        FindAllOutputs.Response response =
            lineageDAO.findAllOutputs(
                request, this::checkResourcesExistsAndAccessible, this::filterOutput);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, FindAllOutputs.Response.getDefaultInstance());
    }
  }

  @Override
  public void findAllInputsOutputs(
      FindAllInputsOutputs request,
      StreamObserver<FindAllInputsOutputs.Response> responseObserver) {
    QPSCountResource.inc();
    try {
      if (request.getItemsCount() == 0) {
        throw new ModelDBException("Items not specified", Code.INVALID_ARGUMENT);
      }
      try (RequestLatencyResource latencyResource =
          new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
        FindAllInputsOutputs.Response response =
            lineageDAO.findAllInputsOutputs(
                request, this::checkResourcesExistsAndAccessible, this::filterInputOutput);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, FindAllInputsOutputs.Response.getDefaultInstance());
    }
  }

  private static class CommitSet {
    Set<String> commits = new HashSet<>();

    public boolean containsKey(String commitSha) {
      return commits.contains(commitSha);
    }

    public void add(String commitSha) {
      commits.add(commitSha);
    }
  }

  private static class RepositoryContainer {
    Map.Entry<RepositoryEntity, CommitSet> repository;

    RepositoryContainer(RepositoryEntity repo) {
      repository = new AbstractMap.SimpleEntry<>(repo, new CommitSet());
    }

    public CommitSet getValue() {
      return repository.getValue();
    }

    public RepositoryEntity getKey() {
      return repository.getKey();
    }
  }

  private FindAllInputs.Response filterInput(Session session, FindAllInputs.Response response) {
    FindAllInputs.Response.Builder builder = FindAllInputs.Response.newBuilder();
    builder.addAllInputs(filter(session, response.getInputsList()));
    return builder.build();
  }

  private FindAllOutputs.Response filterOutput(Session session, FindAllOutputs.Response response) {
    FindAllOutputs.Response.Builder builder = FindAllOutputs.Response.newBuilder();
    builder.addAllOutputs(filter(session, response.getOutputsList()));
    return builder.build();
  }

  private Response filterInputOutput(Session session, Response response) {
    Builder builder = Response.newBuilder();
    builder.addAllInputs(filter(session, response.getInputsList()));
    builder.addAllOutputs(filter(session, response.getOutputsList()));
    return builder.build();
  }

  private Iterable<LineageEntryBatchResponse> filter(
      Session session, List<LineageEntryBatchResponse> lineageEntryBatchResponses) {
    final Set<String> experimentRuns = new HashSet<>();
    final Map<Long, RepositoryContainer> repositories = new HashMap<>();
    return lineageEntryBatchResponses.stream()
        .map(
            lineageEntryBatchResponse ->
                filterLineageEntryBatchResponse(
                    session, experimentRuns, repositories, lineageEntryBatchResponse))
        .collect(Collectors.toList());
  }

  private LineageEntryBatchResponse filterLineageEntryBatchResponse(
      Session session,
      Set<String> experimentRuns,
      Map<Long, RepositoryContainer> repositories,
      LineageEntryBatchResponse lineageEntryBatchResponse) {
    List<LineageEntryBatchResponseSingle> lineageEntryBatchResponseItemsList =
        lineageEntryBatchResponse.getItemsList();
    List<LineageEntryBatchResponseSingle> result =
        lineageEntryBatchResponseItemsList.stream()
            .flatMap(
                lineageEntryBatchResponseSingle ->
                    filterLineageEntryBatchResponseSingle(
                        session, experimentRuns, repositories, lineageEntryBatchResponseSingle))
            .collect(Collectors.toList());
    return LineageEntryBatchResponse.newBuilder().addAllItems(result).build();
  }

  private Stream<? extends LineageEntryBatchResponseSingle> filterLineageEntryBatchResponseSingle(
      Session session,
      Set<String> experimentRuns,
      Map<Long, RepositoryContainer> repositories,
      LineageEntryBatchResponseSingle lineageEntryBatchResponseSingle) {
    List<LineageEntryBatchResponseSingle> newLineageEntryBatchResponseSingleList =
        new LinkedList<>();
    List<LineageEntry> itemList = lineageEntryBatchResponseSingle.getItemsList();
    List<LineageEntry> filterResult =
        itemList.stream()
            .filter(
                lineageEntry ->
                    filterLineageEntry(session, experimentRuns, repositories, lineageEntry))
            .collect(Collectors.toList());
    if (filterResult.size() != 0) {
      newLineageEntryBatchResponseSingleList.add(
          LineageEntryBatchResponseSingle.newBuilder()
              .setId(lineageEntryBatchResponseSingle.getId())
              .addAllItems(filterResult)
              .build());
    }
    return newLineageEntryBatchResponseSingleList.stream();
  }

  private boolean filterLineageEntry(
      Session session,
      Set<String> experimentRuns,
      Map<Long, RepositoryContainer> repositories,
      LineageEntry lineageEntry) {
    try {
      validate(session, experimentRuns, repositories, lineageEntry);
      return true;
    } catch (StatusRuntimeException | ModelDBException e) {
      LOGGER.warn("Can't access entity {}", e.getMessage());
      return false;
    } catch (NoSuchAlgorithmException | InvalidProtocolBufferException e) {
      LOGGER.error("Unexpected error {}", e.getMessage());
      Status status =
          Status.newBuilder()
              .setCode(com.google.rpc.Code.INTERNAL_VALUE)
              .setMessage(e.getMessage())
              .addDetails(Any.pack(LineageEntryBatchResponse.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  private void checkResourcesExistsAndAccessible(Session session, List<LineageEntry> lineageEntries)
      throws ModelDBException, NoSuchAlgorithmException, InvalidProtocolBufferException {
    Set<String> experimentRuns = new HashSet<>();
    Map<Long, RepositoryContainer> blobs = new HashMap<>();
    for (LineageEntry lineageEntry : lineageEntries) {
      validate(session, experimentRuns, blobs, lineageEntry);
    }
  }

  private void validate(
      Session session,
      Set<String> experimentRuns,
      Map<Long, RepositoryContainer> repositories,
      LineageEntry lineageEntry)
      throws InvalidProtocolBufferException, ModelDBException, NoSuchAlgorithmException {
    LineageEntryType type = lineageEntry.getType();
    switch (type) {
      case EXPERIMENT_RUN:
        String experimentRun = lineageEntry.getExternalId();
        if (!experimentRuns.contains(experimentRun)) {
          ExperimentRunEntity experimentRunEntity =
              experimentDAO.getExperimentRun(session, experimentRun);
          experimentRuns.add(experimentRun);
          // Validate if current user has access to the entity or not
          roleService.validateEntityUserWithUserInfo(
              ModelDBServiceResourceTypes.PROJECT,
              experimentRunEntity.getProject_id(),
              ModelDBServiceActions.READ);
        }
        break;
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        long repositoryId = blob.getRepositoryId();
        RepositoryEntity repo;
        CommitSet result;
        if (!repositories.containsKey(repositoryId)) {
          // checks permissions and gets a repository
          repo =
              repositoryDAO.getRepositoryById(
                  session, RepositoryIdentification.newBuilder().setRepoId(repositoryId).build());
          repositories.put(repositoryId, new RepositoryContainer(repo));
          result = repositories.get(repositoryId).getValue();
        } else {
          RepositoryContainer entityMapEntry = repositories.get(repositoryId);
          repo = entityMapEntry.getKey();
          result = entityMapEntry.getValue();
        }
        String commitSha = blob.getCommitSha();
        if (!result.containsKey(commitSha)) {
          commitDAO.getCommitEntity(session, commitSha, session2 -> repo);
          result.add(commitSha);
        }
        blobDAO.getCommitComponent(session2 -> repo, commitSha, blob.getLocationList());
        break;
        // TODO: add method to check
        // return experimentDAO.isExperimentRunExists(session, id);
      case DATASET_VERSION:
        break;
        // TODO: add method to check
        // return commitDAO.isCommitExists(session, id);
      default:
        throw new ModelDBException(
            "Unexpected LineageEntryType '" + type + "' found", Code.INTERNAL);
    }
  }
}
