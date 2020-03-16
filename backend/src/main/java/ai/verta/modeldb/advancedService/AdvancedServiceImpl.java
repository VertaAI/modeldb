package ai.verta.modeldb.advancedService;

import ai.verta.modeldb.AdvancedQueryDatasetVersionsResponse;
import ai.verta.modeldb.AdvancedQueryDatasetsResponse;
import ai.verta.modeldb.AdvancedQueryExperimentRunsResponse;
import ai.verta.modeldb.AdvancedQueryExperimentsResponse;
import ai.verta.modeldb.AdvancedQueryProjectsResponse;
import ai.verta.modeldb.Artifact;
import ai.verta.modeldb.CollaboratorUserInfo;
import ai.verta.modeldb.Comment;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.FindDatasetVersions;
import ai.verta.modeldb.FindDatasets;
import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.FindHydratedDatasetsByOrganization;
import ai.verta.modeldb.FindHydratedDatasetsByTeam;
import ai.verta.modeldb.FindHydratedProjectsByOrganization;
import ai.verta.modeldb.FindHydratedProjectsByTeam;
import ai.verta.modeldb.FindHydratedProjectsByUser;
import ai.verta.modeldb.FindProjects;
import ai.verta.modeldb.GetHydratedDatasetByName;
import ai.verta.modeldb.GetHydratedDatasetsByProjectId;
import ai.verta.modeldb.GetHydratedExperimentRunById;
import ai.verta.modeldb.GetHydratedExperimentRunsByProjectId;
import ai.verta.modeldb.GetHydratedExperimentsByProjectId;
import ai.verta.modeldb.GetHydratedProjectById;
import ai.verta.modeldb.GetHydratedProjects;
import ai.verta.modeldb.HydratedDataset;
import ai.verta.modeldb.HydratedDatasetVersion;
import ai.verta.modeldb.HydratedExperiment;
import ai.verta.modeldb.HydratedExperimentRun;
import ai.verta.modeldb.HydratedProject;
import ai.verta.modeldb.HydratedServiceGrpc.HydratedServiceImplBase;
import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBConstants.UserIdentifier;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.SortExperimentRuns;
import ai.verta.modeldb.TopExperimentRunsSelector;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorBase;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
import ai.verta.modeldb.collaborator.CollaboratorTeam;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.comment.CommentDAO;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.datasetVersion.DatasetVersionServiceImpl;
import ai.verta.modeldb.dto.DatasetPaginationDTO;
import ai.verta.modeldb.dto.DatasetVersionDTO;
import ai.verta.modeldb.dto.ExperimentPaginationDTO;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.dto.ProjectPaginationDTO;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experiment.ExperimentServiceImpl;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunServiceImpl;
import ai.verta.modeldb.monitoring.ErrorCountResource;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.Actions;
import ai.verta.uac.GetCollaboratorResponse;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancedServiceImpl extends HydratedServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(AdvancedServiceImpl.class);
  private AuthService authService;
  private RoleService roleService;
  private ProjectDAO projectDAO;
  private ExperimentRunDAO experimentRunDAO;
  private CommentDAO commentDAO;
  private ExperimentDAO experimentDAO;
  private ArtifactStoreDAO artifactStoreDAO;
  private DatasetDAO datasetDAO;
  private DatasetVersionDAO datasetVersionDAO;

  public AdvancedServiceImpl(
      AuthService authService,
      RoleService roleService,
      ProjectDAO projectDAO,
      ExperimentRunDAO experimentRunDAO,
      CommentDAO commentDAO,
      ExperimentDAO experimentDAO,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetDAO datasetDAO,
      DatasetVersionDAO datasetVersionDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.projectDAO = projectDAO;
    this.experimentRunDAO = experimentRunDAO;
    this.commentDAO = commentDAO;
    this.experimentDAO = experimentDAO;
    this.artifactStoreDAO = artifactStoreDAO;
    this.datasetDAO = datasetDAO;
    this.datasetVersionDAO = datasetVersionDAO;
  }

  private List<HydratedProject> getHydratedProjects(List<Project> projects)
      throws InvalidProtocolBufferException {

    LOGGER.trace("Hydrating {} projects.", projects.size());
    if (projects.isEmpty()) {
      return Collections.emptyList();
    }

    List<HydratedProject> hydratedProjects = new ArrayList<>();
    // Map from project id to list of users (owners + collaborators) which need to be resolved
    Map<String, List<GetCollaboratorResponse>> projectCollaboratorMap = new HashMap<>();
    List<String> vertaIds = new ArrayList<>();
    List<String> emailIds = new ArrayList<>();
    LOGGER.trace("projects {}", projects);
    List<String> resourceIds = new LinkedList<>();
    for (Project project : projects) {
      // Get list of collaborators
      List<GetCollaboratorResponse> projectCollaboratorList =
          roleService.getResourceCollaborators(
              ModelDBServiceResourceTypes.PROJECT, project.getId(), project.getOwner());
      projectCollaboratorMap.put(project.getId(), projectCollaboratorList);

      Map<String, List<String>> vertaIdAndEmailIdMap =
          ModelDBUtils.getVertaIdOrEmailIdMapFromCollaborator(projectCollaboratorList);

      vertaIds.addAll(vertaIdAndEmailIdMap.get(ModelDBConstants.VERTA_ID));
      emailIds.addAll(vertaIdAndEmailIdMap.get(ModelDBConstants.EMAILID));

      vertaIds.add(project.getOwner());
      resourceIds.add(project.getId());
    }
    LOGGER.trace("getHydratedProjects vertaIds : {}", vertaIds);
    LOGGER.trace("getHydratedProjects emailIds : {}", emailIds);

    Map<String, UserInfo> userInfoMap =
        authService.getUserInfoFromAuthServer(vertaIds, emailIds, null);

    Map<String, Actions> selfAllowedActions =
        roleService.getSelfAllowedActionsBatch(resourceIds, ModelDBServiceResourceTypes.PROJECT);
    for (Project project : projects) {
      // Use the map for vertaId  to UserInfo generated for this batch request to populate the
      // userInfo for individual projects.
      LOGGER.trace("Owner : {}", project.getOwner());
      List<CollaboratorUserInfo> collaboratorUserInfos =
          ModelDBUtils.getHydratedCollaboratorUserInfo(
              authService, roleService, projectCollaboratorMap.get(project.getId()), userInfoMap);

      HydratedProject.Builder hydratedProjectBuilder =
          HydratedProject.newBuilder()
              .setProject(project)
              .addAllCollaboratorUserInfos(collaboratorUserInfos);
      if (project.getOwner() != null && userInfoMap.get(project.getOwner()) != null) {
        hydratedProjectBuilder.setOwnerUserInfo(userInfoMap.get(project.getOwner()));

        if (selfAllowedActions != null && selfAllowedActions.size() > 0) {
          hydratedProjectBuilder.addAllAllowedActions(
              selfAllowedActions.get(project.getId()).getActionsList());
        }
      } else {
        LOGGER.error(ModelDBMessages.USER_NOT_FOUND_ERROR_MSG, project.getOwner());
      }
      hydratedProjects.add(hydratedProjectBuilder.build());
    }
    LOGGER.trace("Hydrated {} projects.", projects.size());
    return hydratedProjects;
  }

  @Override
  public void getHydratedProjects(
      GetHydratedProjects request, StreamObserver<GetHydratedProjects.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      ProjectPaginationDTO projectPaginationDTO;
      List<String> allowedProjectIds =
          roleService.getSelfAllowedResources(
              ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.READ);
      FindProjects findProjects =
          FindProjects.newBuilder()
              .addAllProjectIds(allowedProjectIds)
              .setPageNumber(request.getPageNumber())
              .setPageLimit(request.getPageLimit())
              .setAscending(request.getAscending())
              .setSortKey(request.getSortKey())
              .setWorkspaceName(request.getWorkspaceName())
              .build();
      projectPaginationDTO =
          projectDAO.findProjects(findProjects, null, userInfo, ProjectVisibility.PRIVATE);
      List<Project> projects = projectPaginationDTO.getProjects();

      List<HydratedProject> hydratedProjects = new ArrayList<>();
      if (projects != null && !projects.isEmpty()) {
        hydratedProjects = getHydratedProjects(projects);
      }

      responseObserver.onNext(
          GetHydratedProjects.Response.newBuilder()
              .addAllHydratedProjects(hydratedProjects)
              .setTotalRecords(projectPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetHydratedProjects.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getHydratedPublicProjects(
      GetHydratedProjects request, StreamObserver<GetHydratedProjects.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      ProjectPaginationDTO projectPaginationDTO =
          projectDAO.getProjects(
              null,
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending(),
              request.getSortKey(),
              ProjectVisibility.PUBLIC);
      List<Project> projects = projectPaginationDTO.getProjects();

      List<HydratedProject> hydratedProjects = new ArrayList<>();
      if (projects != null && !projects.isEmpty()) {
        hydratedProjects = getHydratedProjects(projects);
      }

      responseObserver.onNext(
          GetHydratedProjects.Response.newBuilder()
              .addAllHydratedProjects(hydratedProjects)
              .setTotalRecords(projectPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetHydratedProjects.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getHydratedProjectById(
      GetHydratedProjectById request,
      StreamObserver<GetHydratedProjectById.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId().isEmpty()) {
        String errorMessage = "Project ID not found in GetHydratedProjectById request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetHydratedProjectById.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.READ);

      Project project = projectDAO.getProjectByID(request.getId());
      responseObserver.onNext(
          GetHydratedProjectById.Response.newBuilder()
              .setHydratedProject(getHydratedProjects(Collections.singletonList(project)).get(0))
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetHydratedProjectById.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getHydratedExperimentsByProjectId(
      GetHydratedExperimentsByProjectId request,
      StreamObserver<GetHydratedExperimentsByProjectId.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getProjectId().isEmpty()) {
        String errorMessage = "Project ID not found in GetHydratedExperimentsByProjectId request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(
                    Any.pack(GetHydratedExperimentsByProjectId.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      ExperimentPaginationDTO experimentPaginationDTO =
          experimentDAO.getExperimentsInProject(
              request.getProjectId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending(),
              request.getSortKey());

      List<Experiment> experiments = experimentPaginationDTO.getExperiments();

      List<HydratedExperiment> hydratedExperiments = new ArrayList<>();
      if (!experiments.isEmpty()) {
        hydratedExperiments = getHydratedExperiments(request.getProjectId(), experiments);
      }

      responseObserver.onNext(
          GetHydratedExperimentsByProjectId.Response.newBuilder()
              .addAllHydratedExperiments(hydratedExperiments)
              .setTotalRecords(experimentPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetHydratedExperimentsByProjectId.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getHydratedExperimentRunsInProject(
      GetHydratedExperimentRunsByProjectId request,
      StreamObserver<GetHydratedExperimentRunsByProjectId.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getProjectId().isEmpty()) {
        String errorMessage =
            "Project ID not found in GetHydratedExperimentRunsByProjectId request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(
                    Any.pack(GetHydratedExperimentRunsByProjectId.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.getExperimentRunsFromEntity(
              ModelDBConstants.PROJECT_ID,
              request.getProjectId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending(),
              request.getSortKey());

      LOGGER.debug(
          ModelDBMessages.EXP_RUN_RECORD_COUNT_MSG, experimentRunPaginationDTO.getTotalRecords());
      List<ExperimentRun> experimentRuns = experimentRunPaginationDTO.getExperimentRuns();
      List<HydratedExperimentRun> hydratedExperimentRuns = new ArrayList<>();
      if (!experimentRuns.isEmpty()) {
        hydratedExperimentRuns = getHydratedExperimentRuns(experimentRuns);
      }

      responseObserver.onNext(
          GetHydratedExperimentRunsByProjectId.Response.newBuilder()
              .addAllHydratedExperimentRuns(hydratedExperimentRuns)
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(
                  Any.pack(GetHydratedExperimentRunsByProjectId.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private List<HydratedExperimentRun> getHydratedExperimentRuns(List<ExperimentRun> experimentRuns)
      throws InvalidProtocolBufferException {
    LOGGER.debug(
        "experimentRuns count in getHydratedExperimentRuns method : {}", experimentRuns.size());
    Set<String> experimentIdSet = new HashSet<>();
    List<String> vertaIdList = new ArrayList<>();
    Set<String> projectIdSet = new HashSet<>();
    for (ExperimentRun experimentRun : experimentRuns) {
      vertaIdList.add(experimentRun.getOwner());
      experimentIdSet.add(experimentRun.getExperimentId());
      projectIdSet.add(experimentRun.getProjectId());
    }

    Map<String, Actions> actions = new HashMap<>();
    if (projectIdSet.size() > 0) {
      actions =
          roleService.getSelfAllowedActionsBatch(
              new ArrayList<>(projectIdSet), ModelDBServiceResourceTypes.PROJECT);
    }

    LOGGER.trace("vertaIdList {}", vertaIdList);
    LOGGER.trace("experimentIdSet {}", experimentIdSet);
    // Fetch the experiment list
    List<String> experimentIds = new ArrayList<>(experimentIdSet);
    LOGGER.trace("experimentIds {}", experimentIds);
    List<Experiment> experimentList = experimentDAO.getExperimentsByBatchIds(experimentIds);
    LOGGER.trace("experimentList {}", experimentList);
    // key: experiment.id, value: experiment
    Map<String, Experiment> experimentMap = new HashMap<>();
    for (Experiment experiment : experimentList) {
      experimentMap.put(experiment.getId(), experiment);
    }

    // Fetch the experimentRun owners userInfo
    Map<String, UserInfo> userInfoMap =
        authService.getUserInfoFromAuthServer(vertaIdList, null, null);

    List<HydratedExperimentRun> hydratedExperimentRuns = new LinkedList<>();
    LOGGER.trace("hydrating experiments");
    for (ExperimentRun experimentRun : experimentRuns) {

      HydratedExperimentRun.Builder hydratedExperimentRunBuilder =
          HydratedExperimentRun.newBuilder();

      UserInfo userInfoValue = userInfoMap.get(experimentRun.getOwner());
      LOGGER.trace("owner {}", experimentRun.getOwner());
      if (userInfoValue != null) {
        hydratedExperimentRunBuilder.setOwnerUserInfo(userInfoValue);
        // Add Comments in hydrated data
        List<Comment> comments =
            commentDAO.getComments(
                ExperimentRunEntity.class.getSimpleName(), experimentRun.getId());
        LOGGER.trace("comments {}", comments);
        hydratedExperimentRunBuilder.addAllComments(comments);

        // Add user specific actions
        hydratedExperimentRunBuilder.addAllAllowedActions(
            ModelDBUtils.getActionsList(new ArrayList<>(projectIdSet), actions));
      } else {
        LOGGER.error(ModelDBMessages.USER_NOT_FOUND_ERROR_MSG, experimentRun.getOwner());
      }
      // Prepare experiment for hydratedExperimentRun
      Experiment hydratedExperiment =
          Experiment.newBuilder()
              .setName(experimentMap.get(experimentRun.getExperimentId()).getName())
              .build();
      LOGGER.trace("hydratedExperiment {}", hydratedExperiment);
      hydratedExperimentRunBuilder.setExperimentRun(experimentRun);
      hydratedExperimentRunBuilder.setExperiment(hydratedExperiment);
      HydratedExperimentRun hydratedExperimentRun = hydratedExperimentRunBuilder.build();
      LOGGER.trace("hydratedExperimentRun {}", hydratedExperimentRun);
      hydratedExperimentRuns.add(hydratedExperimentRun);
    }
    LOGGER.trace("done hydrating experiments");

    return hydratedExperimentRuns;
  }

  @Override
  public void getHydratedExperimentRunById(
      GetHydratedExperimentRunById request,
      StreamObserver<GetHydratedExperimentRunById.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetHydratedExperimentRunById request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetHydratedExperimentRunById.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun experimentRun = experimentRunDAO.getExperimentRun(request.getId());

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          experimentRun.getProjectId(),
          ModelDBServiceActions.READ);

      List<HydratedExperimentRun> hydratedExperimentRuns =
          getHydratedExperimentRuns(Collections.singletonList(experimentRun));

      responseObserver.onNext(
          GetHydratedExperimentRunById.Response.newBuilder()
              .setHydratedExperimentRun(hydratedExperimentRuns.get(0))
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetHydratedExperimentRunById.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void findHydratedExperimentRuns(
      FindExperimentRuns request,
      StreamObserver<AdvancedQueryExperimentRunsResponse> responseObserver) {
    LOGGER.trace("Starting findHydratedExperimentRuns");
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getProjectId().isEmpty()
          && request.getExperimentId().isEmpty()
          && request.getExperimentRunIdsList().isEmpty()) {
        String errorMessage =
            "Project ID and Experiment ID and ExperimentRun Id's not found in FindExperimentRuns request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(FindExperimentRuns.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      LOGGER.trace("parmeters checked, starting findHydratedExperimentRuns");

      LOGGER.trace("got current logged in user info");
      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);

        LOGGER.trace("Validated project accessibility");
      } else if (!request.getExperimentId().isEmpty()) {
        Experiment experiment = experimentDAO.getExperiment(request.getExperimentId());
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            experiment.getProjectId(),
            ModelDBServiceActions.READ);
        LOGGER.trace("Validated experiment accessibility");
      }

      ExperimentRunServiceImpl experimentRunService =
          new ExperimentRunServiceImpl(
              authService,
              roleService,
              experimentRunDAO,
              projectDAO,
              experimentDAO,
              artifactStoreDAO,
              datasetVersionDAO);
      if (!request.getExperimentRunIdsList().isEmpty()) {
        List<String> accessibleExperimentRunIds =
            experimentRunService.getAccessibleExperimentRunIDs(
                request.getExperimentRunIdsList(), ModelDBServiceActions.READ);
        if (accessibleExperimentRunIds.isEmpty()) {
          ModelDBUtils.logAndThrowError(
              ModelDBConstants.ACCESS_DENIED_EXPERIMENT_RUN,
              Code.PERMISSION_DENIED_VALUE,
              Any.pack(FindExperimentRuns.getDefaultInstance()));
        }
        request =
            request
                .toBuilder()
                .clearExperimentRunIds()
                .addAllExperimentRunIds(accessibleExperimentRunIds)
                .build();
      }
      LOGGER.trace("Updated experiment run ids");

      for (KeyValueQuery predicate : request.getPredicatesList()) {
        // ID predicates only supported for EQ
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          if (!predicate.getOperator().equals(OperatorEnum.Operator.EQ)) {
            ModelDBUtils.logAndThrowError(
                ModelDBConstants.NON_EQ_ID_PRED_ERROR_MESSAGE,
                Code.INVALID_ARGUMENT_VALUE,
                Any.pack(FindExperimentRuns.getDefaultInstance()));
          }

          List<String> accessibleExperimentRunIds =
              experimentRunService.getAccessibleExperimentRunIDs(
                  Collections.singletonList(predicate.getValue().getStringValue()),
                  ModelDBServiceActions.READ);
          if (accessibleExperimentRunIds.isEmpty()) {
            ModelDBUtils.logAndThrowError(
                ModelDBConstants.ACCESS_DENIED_EXPERIMENT_RUN,
                Code.PERMISSION_DENIED_VALUE,
                Any.pack(FindExperimentRuns.getDefaultInstance()));
          }
        }
      }

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.findExperimentRuns(request);
      LOGGER.debug(
          ModelDBMessages.EXP_RUN_RECORD_COUNT_MSG, experimentRunPaginationDTO.getTotalRecords());

      List<HydratedExperimentRun> hydratedExperimentRuns = new ArrayList<>();
      if (request.getIdsOnly()) {
        for (ExperimentRun experimentRun : experimentRunPaginationDTO.getExperimentRuns()) {
          hydratedExperimentRuns.add(
              HydratedExperimentRun.newBuilder().setExperimentRun(experimentRun).build());
        }
      } else if (!experimentRunPaginationDTO.getExperimentRuns().isEmpty()) {
        hydratedExperimentRuns =
            getHydratedExperimentRuns(experimentRunPaginationDTO.getExperimentRuns());
      }

      LOGGER.debug("hydratedExperimentRuns size {}", hydratedExperimentRuns.size());

      responseObserver.onNext(
          AdvancedQueryExperimentRunsResponse.newBuilder()
              .addAllHydratedExperimentRuns(hydratedExperimentRuns)
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AdvancedQueryExperimentRunsResponse.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void sortHydratedExperimentRuns(
      SortExperimentRuns request,
      StreamObserver<AdvancedQueryExperimentRunsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getExperimentRunIdsList().isEmpty() && request.getSortKey().isEmpty()) {
        errorMessage = "ExperimentRun Id's and sort key not found in SortExperimentRuns request";
      } else if (request.getExperimentRunIdsList().isEmpty()) {
        errorMessage = "ExperimentRun Id's not found in SortExperimentRuns request";
      } else if (request.getSortKey().isEmpty()) {
        errorMessage = "Sort key not found in SortExperimentRuns request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(SortExperimentRuns.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRunServiceImpl experimentRunService =
          new ExperimentRunServiceImpl(
              authService,
              roleService,
              experimentRunDAO,
              projectDAO,
              experimentDAO,
              artifactStoreDAO,
              datasetVersionDAO);
      List<String> accessibleExperimentRunIds =
          experimentRunService.getAccessibleExperimentRunIDs(
              request.getExperimentRunIdsList(), ModelDBServiceActions.READ);

      if (accessibleExperimentRunIds.isEmpty()) {
        ModelDBUtils.logAndThrowError(
            ModelDBConstants.ACCESS_DENIED_EXPERIMENT_RUN,
            Code.PERMISSION_DENIED_VALUE,
            Any.pack(FindExperimentRuns.getDefaultInstance()));
      }

      request =
          request
              .toBuilder()
              .clearExperimentRunIds()
              .addAllExperimentRunIds(accessibleExperimentRunIds)
              .build();

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.sortExperimentRuns(request);
      LOGGER.debug(
          ModelDBMessages.EXP_RUN_RECORD_COUNT_MSG, experimentRunPaginationDTO.getTotalRecords());

      List<HydratedExperimentRun> hydratedExperimentRuns = new ArrayList<>();
      if (!experimentRunPaginationDTO.getExperimentRuns().isEmpty()) {
        hydratedExperimentRuns =
            getHydratedExperimentRuns(experimentRunPaginationDTO.getExperimentRuns());
      }

      responseObserver.onNext(
          AdvancedQueryExperimentRunsResponse.newBuilder()
              .addAllHydratedExperimentRuns(hydratedExperimentRuns)
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AdvancedQueryExperimentRunsResponse.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getTopHydratedExperimentRuns(
      TopExperimentRunsSelector request,
      StreamObserver<AdvancedQueryExperimentRunsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if ((request.getProjectId().isEmpty()
              && request.getExperimentId().isEmpty()
              && request.getExperimentRunIdsList().isEmpty())
          || request.getSortKey().isEmpty()) {
        String errorMessage =
            "Project ID and Experiment ID and Experiment IDs and Sort key not found in TopExperimentRunsSelector request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(TopExperimentRunsSelector.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);
      } else if (!request.getExperimentId().isEmpty()) {
        Experiment experiment = experimentDAO.getExperiment(request.getExperimentId());
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            experiment.getProjectId(),
            ModelDBServiceActions.READ);
      }

      if (!request.getExperimentRunIdsList().isEmpty()) {
        ExperimentRunServiceImpl experimentRunService =
            new ExperimentRunServiceImpl(
                authService,
                roleService,
                experimentRunDAO,
                projectDAO,
                experimentDAO,
                artifactStoreDAO,
                datasetVersionDAO);
        List<String> accessibleExperimentRunIds =
            experimentRunService.getAccessibleExperimentRunIDs(
                request.getExperimentRunIdsList(), ModelDBServiceActions.READ);

        if (accessibleExperimentRunIds.isEmpty()) {
          ModelDBUtils.logAndThrowError(
              ModelDBConstants.ACCESS_DENIED_EXPERIMENT_RUN,
              Code.PERMISSION_DENIED_VALUE,
              Any.pack(FindExperimentRuns.getDefaultInstance()));
        }

        request =
            request
                .toBuilder()
                .clearExperimentRunIds()
                .addAllExperimentRunIds(accessibleExperimentRunIds)
                .build();
      }

      List<ExperimentRun> experimentRuns = experimentRunDAO.getTopExperimentRuns(request);
      List<HydratedExperimentRun> hydratedExperimentRuns = new ArrayList<>();
      if (!experimentRuns.isEmpty()) {
        hydratedExperimentRuns = getHydratedExperimentRuns(experimentRuns);
      }

      responseObserver.onNext(
          AdvancedQueryExperimentRunsResponse.newBuilder()
              .addAllHydratedExperimentRuns(hydratedExperimentRuns)
              // for get top experimentRun list, total_record count always 1. there is no
              // need to get count.
              .setTotalRecords(1)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(TopExperimentRunsSelector.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private List<HydratedExperiment> getHydratedExperiments(
      String projectId, List<Experiment> experiments) {
    Map<String, Actions> actions =
        roleService.getSelfAllowedActionsBatch(
            Collections.singletonList(projectId), ModelDBServiceResourceTypes.PROJECT);
    LOGGER.debug("experiments count in getHydratedExperiments method : {}", experiments.size());
    List<String> vertaIdList = new ArrayList<>();
    for (Experiment experiment : experiments) {
      vertaIdList.add(experiment.getOwner());
    }

    // Fetch the experiment owners userInfo
    Map<String, UserInfo> userInfoMap =
        authService.getUserInfoFromAuthServer(vertaIdList, null, null);

    List<HydratedExperiment> hydratedExperiments = new LinkedList<>();
    for (Experiment experiment : experiments) {
      HydratedExperiment.Builder hydratedExperimentBuilder =
          HydratedExperiment.newBuilder().setExperiment(experiment);

      UserInfo userInfoValue = userInfoMap.get(experiment.getOwner());
      if (userInfoValue != null) {
        hydratedExperimentBuilder.setOwnerUserInfo(userInfoValue);
        if (actions != null && actions.size() > 0) {
          hydratedExperimentBuilder.addAllAllowedActions(
              ModelDBUtils.getActionsList(Collections.singletonList(projectId), actions));
        }
      } else {
        LOGGER.error(ModelDBMessages.USER_NOT_FOUND_ERROR_MSG, experiment.getOwner());
      }
      hydratedExperiments.add(hydratedExperimentBuilder.build());
    }

    return hydratedExperiments;
  }

  @Override
  public void findHydratedExperiments(
      FindExperiments request, StreamObserver<AdvancedQueryExperimentsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getProjectId().isEmpty() && request.getExperimentIdsList().isEmpty()) {
        String errorMessage = "Project ID and Experiment Id's not found in FindExperiments request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(FindExperiments.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);
      }

      ExperimentServiceImpl experimentService =
          new ExperimentServiceImpl(
              authService, roleService, experimentDAO, projectDAO, artifactStoreDAO);
      if (!request.getExperimentIdsList().isEmpty()) {
        List<String> accessibleExperimentIds =
            experimentService.getAccessibleExperimentIDs(
                request.getExperimentIdsList(), ModelDBServiceActions.READ);
        if (accessibleExperimentIds.isEmpty()) {
          String errorMessage =
              "Access is denied. User is unauthorized for given Experiment IDs : "
                  + accessibleExperimentIds;
          ModelDBUtils.logAndThrowError(
              errorMessage,
              Code.PERMISSION_DENIED_VALUE,
              Any.pack(FindExperiments.getDefaultInstance()));
        }
        request =
            request
                .toBuilder()
                .clearExperimentIds()
                .addAllExperimentIds(accessibleExperimentIds)
                .build();
      }

      for (KeyValueQuery predicate : request.getPredicatesList()) {
        // ID predicates only supported for EQ
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          if (!predicate.getOperator().equals(OperatorEnum.Operator.EQ)) {
            Status statusMessage =
                Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT_VALUE)
                    .setMessage(ModelDBConstants.NON_EQ_ID_PRED_ERROR_MESSAGE)
                    .build();
            throw StatusProto.toStatusRuntimeException(statusMessage);
          }
          List<String> accessibleExperimentIds =
              experimentService.getAccessibleExperimentIDs(
                  Collections.singletonList(predicate.getValue().getStringValue()),
                  ModelDBServiceActions.READ);

          if (accessibleExperimentIds.isEmpty()) {
            String errorMessage =
                "Access is denied. User is unauthorized for given Experiment IDs : "
                    + accessibleExperimentIds;
            ModelDBUtils.logAndThrowError(
                errorMessage,
                Code.PERMISSION_DENIED_VALUE,
                Any.pack(FindExperiments.getDefaultInstance()));
          }
        }
      }

      ExperimentPaginationDTO experimentPaginationDTO = experimentDAO.findExperiments(request);
      LOGGER.debug(
          "ExperimentPaginationDTO record count : {}", experimentPaginationDTO.getTotalRecords());

      List<HydratedExperiment> hydratedExperiments = new ArrayList<>();
      if (request.getIdsOnly()) {
        for (Experiment experiment : experimentPaginationDTO.getExperiments()) {
          hydratedExperiments.add(
              HydratedExperiment.newBuilder().setExperiment(experiment).build());
        }
      } else if (!experimentPaginationDTO.getExperiments().isEmpty()) {
        hydratedExperiments =
            getHydratedExperiments(
                request.getProjectId(), experimentPaginationDTO.getExperiments());
      }

      responseObserver.onNext(
          AdvancedQueryExperimentsResponse.newBuilder()
              .addAllHydratedExperiments(hydratedExperiments)
              .setTotalRecords(experimentPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AdvancedQueryExperimentsResponse.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void findHydratedProjects(
      FindProjects request, StreamObserver<AdvancedQueryProjectsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      ProjectPaginationDTO projectPaginationDTO =
          projectDAO.findProjects(request, null, userInfo, ProjectVisibility.PRIVATE);
      LOGGER.debug(
          ModelDBMessages.PROJECT_RECORD_COUNT_MSG, projectPaginationDTO.getTotalRecords());

      List<HydratedProject> hydratedProjects = new ArrayList<>();
      if (request.getIdsOnly()) {
        for (Project project : projectPaginationDTO.getProjects()) {
          hydratedProjects.add(HydratedProject.newBuilder().setProject(project).build());
        }
      } else if (!projectPaginationDTO.getProjects().isEmpty()) {
        hydratedProjects = getHydratedProjects(projectPaginationDTO.getProjects());
      }

      responseObserver.onNext(
          AdvancedQueryProjectsResponse.newBuilder()
              .addAllHydratedProjects(hydratedProjects)
              .setTotalRecords(projectPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AdvancedQueryProjectsResponse.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private List<HydratedDataset> getHydratedDatasets(List<Dataset> datasets)
      throws InvalidProtocolBufferException {

    LOGGER.trace("Hydrating {} datasets.", datasets.size());
    List<HydratedDataset> hydratedDatasets = new ArrayList<>();

    // Map from dataset id to list of users (owners + collaborators) which need to be resolved
    Map<String, List<GetCollaboratorResponse>> datasetCollaboratorMap = new HashMap<>();
    List<String> vertaIds = new ArrayList<>();
    List<String> emailIds = new ArrayList<>();

    List<String> resourceIds = new LinkedList<>();
    for (Dataset dataset : datasets) {
      // Get list of collaborators
      List<GetCollaboratorResponse> datasetCollaboratorList =
          roleService.getResourceCollaborators(
              ModelDBServiceResourceTypes.DATASET, dataset.getId(), dataset.getOwner());
      datasetCollaboratorMap.put(dataset.getId(), datasetCollaboratorList);

      Map<String, List<String>> vertaIdAndEmailIdMap =
          ModelDBUtils.getVertaIdOrEmailIdMapFromCollaborator(datasetCollaboratorList);

      vertaIds.addAll(vertaIdAndEmailIdMap.get(ModelDBConstants.VERTA_ID));
      emailIds.addAll(vertaIdAndEmailIdMap.get(ModelDBConstants.EMAILID));

      vertaIds.add(dataset.getOwner());
      resourceIds.add(dataset.getId());
    }

    LOGGER.trace("getHydratedDatasets vertaIds : {}", vertaIds.size());
    LOGGER.trace("getHydratedDatasets emailIds : {}", emailIds.size());

    Map<String, UserInfo> userInfoMap =
        authService.getUserInfoFromAuthServer(vertaIds, emailIds, null);

    LOGGER.trace("Got results from UAC : {}", userInfoMap.size());

    Map<String, Actions> selfAllowedActions =
        roleService.getSelfAllowedActionsBatch(resourceIds, ModelDBServiceResourceTypes.DATASET);

    for (Dataset dataset : datasets) {
      // Use the map for vertaId  to UserInfo generated for this batch request to populate the
      // userInfo for individual datasets.
      List<CollaboratorUserInfo> collaboratorUserInfos =
          ModelDBUtils.getHydratedCollaboratorUserInfo(
              authService, roleService, datasetCollaboratorMap.get(dataset.getId()), userInfoMap);

      HydratedDataset.Builder hydratedDatasetBuilder =
          HydratedDataset.newBuilder()
              .setDataset(dataset)
              .addAllCollaboratorUserInfos(collaboratorUserInfos);
      if (dataset.getOwner() != null && userInfoMap.get(dataset.getOwner()) != null) {
        hydratedDatasetBuilder.setOwnerUserInfo(userInfoMap.get(dataset.getOwner()));
      } else {
        LOGGER.error(ModelDBMessages.USER_NOT_FOUND_ERROR_MSG, dataset.getOwner());
      }
      if (selfAllowedActions != null && selfAllowedActions.size() > 0) {
        hydratedDatasetBuilder.addAllAllowedActions(
            selfAllowedActions.get(dataset.getId()).getActionsList());
      }
      HydratedDataset hydratedDataset = hydratedDatasetBuilder.build();

      hydratedDatasets.add(hydratedDataset);
    }
    LOGGER.trace("Hydrated {} datasets.", datasets.size());
    return hydratedDatasets;
  }

  private List<HydratedDataset> findHydratedDatasets(
      DatasetPaginationDTO datasetPaginationDTO, Boolean isIdsOnly)
      throws InvalidProtocolBufferException {
    List<HydratedDataset> hydratedDatasets = new ArrayList<>();
    if (isIdsOnly) {
      for (Dataset dataset : datasetPaginationDTO.getDatasets()) {
        hydratedDatasets.add(HydratedDataset.newBuilder().setDataset(dataset).build());
      }
    } else if (!datasetPaginationDTO.getDatasets().isEmpty()) {
      hydratedDatasets = getHydratedDatasets(datasetPaginationDTO.getDatasets());
    }
    return hydratedDatasets;
  }

  @Override
  public void findHydratedDatasets(
      FindDatasets request, StreamObserver<AdvancedQueryDatasetsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      DatasetPaginationDTO datasetPaginationDTO =
          datasetDAO.findDatasets(request, userInfo, DatasetVisibility.PRIVATE);
      LOGGER.debug(
          ModelDBMessages.DATASET_RECORD_COUNT_MSG, datasetPaginationDTO.getTotalRecords());
      List<HydratedDataset> hydratedDatasets =
          findHydratedDatasets(datasetPaginationDTO, request.getIdsOnly());

      responseObserver.onNext(
          AdvancedQueryDatasetsResponse.newBuilder()
              .addAllHydratedDatasets(hydratedDatasets)
              .setTotalRecords(datasetPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(FindDatasets.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void findHydratedPublicDatasets(
      FindDatasets request, StreamObserver<AdvancedQueryDatasetsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      DatasetPaginationDTO datasetPaginationDTO =
          datasetDAO.findDatasets(request, userInfo, DatasetVisibility.PUBLIC);
      LOGGER.debug(
          ModelDBMessages.DATASET_RECORD_COUNT_MSG, datasetPaginationDTO.getTotalRecords());

      List<HydratedDataset> hydratedDatasets = new ArrayList<>();
      if (request.getIdsOnly()) {
        for (Dataset dataset : datasetPaginationDTO.getDatasets()) {
          hydratedDatasets.add(HydratedDataset.newBuilder().setDataset(dataset).build());
        }
      } else if (!datasetPaginationDTO.getDatasets().isEmpty()) {
        hydratedDatasets = getHydratedDatasets(datasetPaginationDTO.getDatasets());
      }

      responseObserver.onNext(
          AdvancedQueryDatasetsResponse.newBuilder()
              .addAllHydratedDatasets(hydratedDatasets)
              .setTotalRecords(datasetPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(FindDatasets.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private HydratedDatasetVersion getHydratedDatasetVersion(DatasetVersion datasetVersion) {
    UserInfo ownerUserInfo =
        authService.getUserInfo(datasetVersion.getOwner(), UserIdentifier.VERTA_ID);

    Map<String, Actions> selfAllowedActions =
        roleService.getSelfAllowedActionsBatch(
            Collections.singletonList(datasetVersion.getDatasetId()),
            ModelDBServiceResourceTypes.DATASET);

    HydratedDatasetVersion.Builder hydratedDatasetVersionBuilder =
        HydratedDatasetVersion.newBuilder().setDatasetVersion(datasetVersion);
    if (ownerUserInfo != null) {
      hydratedDatasetVersionBuilder.setOwnerUserInfo(ownerUserInfo);
    }
    if (selfAllowedActions != null && selfAllowedActions.size() > 0) {
      hydratedDatasetVersionBuilder.addAllAllowedActions(
          selfAllowedActions.get(datasetVersion.getDatasetId()).getActionsList());
    }
    return hydratedDatasetVersionBuilder.build();
  }

  @Override
  public void findHydratedDatasetVersions(
      FindDatasetVersions request,
      StreamObserver<AdvancedQueryDatasetVersionsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      if (!request.getDatasetId().isEmpty()) {
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.DATASET,
            request.getDatasetId(),
            ModelDBServiceActions.READ);
      }

      if (!request.getDatasetVersionIdsList().isEmpty()) {
        DatasetVersionServiceImpl datasetVersionService =
            new DatasetVersionServiceImpl(authService, roleService, datasetDAO, datasetVersionDAO);
        List<String> accessibleDatasetVersionIDs =
            datasetVersionService.getAccessibleDatasetVersionIDs(
                request.getDatasetVersionIdsList(), ModelDBServiceActions.READ);
        request =
            request
                .toBuilder()
                .clearDatasetVersionIds()
                .addAllDatasetVersionIds(accessibleDatasetVersionIDs)
                .build();
      }

      DatasetVersionDTO datasetVersionPaginationDTO =
          datasetVersionDAO.findDatasetVersions(request, userInfo);
      LOGGER.debug(
          "DatasetVersionPaginationDTO record count : "
              + datasetVersionPaginationDTO.getTotalRecords());

      List<HydratedDatasetVersion> hydratedDatasetVersions = new ArrayList<>();
      if (request.getIdsOnly()) {
        for (DatasetVersion datasetVersion : datasetVersionPaginationDTO.getDatasetVersions()) {
          hydratedDatasetVersions.add(
              HydratedDatasetVersion.newBuilder().setDatasetVersion(datasetVersion).build());
        }
      } else if (!datasetVersionPaginationDTO.getDatasetVersions().isEmpty()) {
        for (DatasetVersion datasetVersion : datasetVersionPaginationDTO.getDatasetVersions()) {
          hydratedDatasetVersions.add(getHydratedDatasetVersion(datasetVersion));
        }
      }

      responseObserver.onNext(
          AdvancedQueryDatasetVersionsResponse.newBuilder()
              .addAllHydratedDatasetVersions(hydratedDatasetVersions)
              .setTotalRecords(datasetVersionPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(FindDatasetVersions.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getHydratedDatasetByName(
      GetHydratedDatasetByName request,
      StreamObserver<GetHydratedDatasetByName.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getName().isEmpty()) {
        String errorMessage = "Dataset Name not found in GetHydratedDatasetByName request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetHydratedDatasetByName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      List<Dataset> datasets =
          datasetDAO.getDatasets(ModelDBConstants.NAME, request.getName(), userInfo);

      Dataset datasetByUser = null;
      List<Dataset> sharedDatasets = new ArrayList<>();
      boolean isDatasetFound = false;
      StatusRuntimeException statusRuntimeException = null;

      for (Dataset dataset : datasets) {
        try {
          // Validate if current user has access to the entity or not
          roleService.validateEntityUserWithUserInfo(
              ModelDBServiceResourceTypes.DATASET, dataset.getId(), ModelDBServiceActions.READ);
          if (userInfo == null
              || dataset.getOwner().equals(authService.getVertaIdFromUserInfo(userInfo))) {
            datasetByUser = dataset;
          } else {
            sharedDatasets.add(dataset);
          }
          isDatasetFound = true;
        } catch (StatusRuntimeException e) {
          statusRuntimeException = e;
        }
      }

      if (!isDatasetFound) {
        responseObserver.onError(statusRuntimeException);
        responseObserver.onCompleted();
      }

      List<HydratedDataset> sharedHydratedDatasets = new ArrayList<>();
      if (!sharedDatasets.isEmpty()) {
        sharedHydratedDatasets = getHydratedDatasets(sharedDatasets);
      }

      GetHydratedDatasetByName.Response.Builder getHydratedDatasetByNameResponse =
          GetHydratedDatasetByName.Response.newBuilder()
              .addAllSharedHydratedDatasets(sharedHydratedDatasets);

      if (datasetByUser != null) {
        getHydratedDatasetByNameResponse.setHydratedDatasetByUser(
            getHydratedDatasets(Collections.singletonList(datasetByUser)).get(0));
      }

      responseObserver.onNext(getHydratedDatasetByNameResponse.build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetHydratedDatasetByName.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void findHydratedPublicProjects(
      FindProjects request, StreamObserver<AdvancedQueryProjectsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      ProjectPaginationDTO projectPaginationDTO =
          projectDAO.findProjects(request, null, null, ProjectVisibility.PUBLIC);
      LOGGER.debug(
          ModelDBMessages.PROJECT_RECORD_COUNT_MSG, projectPaginationDTO.getTotalRecords());

      List<HydratedProject> hydratedProjects = new ArrayList<>();
      if (request.getIdsOnly()) {
        for (Project project : projectPaginationDTO.getProjects()) {
          hydratedProjects.add(HydratedProject.newBuilder().setProject(project).build());
        }
      } else if (!projectPaginationDTO.getProjects().isEmpty()) {
        hydratedProjects = getHydratedProjects(projectPaginationDTO.getProjects());
      }

      responseObserver.onNext(
          AdvancedQueryProjectsResponse.newBuilder()
              .addAllHydratedProjects(hydratedProjects)
              .setTotalRecords(projectPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AdvancedQueryProjectsResponse.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private AdvancedQueryProjectsResponse createQueryProjectsResponse(
      FindProjects findProjectsRequest,
      UserInfo currentLoginUserInfo,
      CollaboratorBase host,
      ProjectVisibility visibility)
      throws InvalidProtocolBufferException {

    ProjectPaginationDTO projectPaginationDTO =
        projectDAO.findProjects(findProjectsRequest, host, currentLoginUserInfo, visibility);
    LOGGER.debug(ModelDBMessages.PROJECT_RECORD_COUNT_MSG, projectPaginationDTO.getTotalRecords());

    List<HydratedProject> hydratedProjects = new ArrayList<>();
    if (findProjectsRequest.getIdsOnly()) {
      for (Project project : projectPaginationDTO.getProjects()) {
        hydratedProjects.add(HydratedProject.newBuilder().setProject(project).build());
      }
    } else if (!projectPaginationDTO.getProjects().isEmpty()) {
      hydratedProjects = getHydratedProjects(projectPaginationDTO.getProjects());
    }
    return AdvancedQueryProjectsResponse.newBuilder()
        .addAllHydratedProjects(hydratedProjects)
        .setTotalRecords(projectPaginationDTO.getTotalRecords())
        .build();
  }

  @Override
  public void findHydratedProjectsByUser(
      FindHydratedProjectsByUser request,
      StreamObserver<AdvancedQueryProjectsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      String errorMessage = null;
      if (request.getEmail().isEmpty() && request.getVertaId().isEmpty()) {
        errorMessage = "Email OR vertaId not found in the FindHydratedPublicProjects request";
      }

      CollaboratorUser hostCollaboratorBase = null;
      String userEmail = request.getEmail();
      if (!userEmail.isEmpty() && ModelDBUtils.isValidEmail(userEmail)) {
        UserInfo hostUserInfo = authService.getUserInfo(userEmail, UserIdentifier.EMAIL_ID);
        hostCollaboratorBase = new CollaboratorUser(authService, hostUserInfo);
      } else if (!userEmail.isEmpty()) {
        errorMessage = "Invalid email found in the FindHydratedPublicProjects request";
      } else if (!request.getVertaId().isEmpty()) {
        UserInfo hostUserInfo =
            authService.getUserInfo(request.getVertaId(), UserIdentifier.VERTA_ID);
        hostCollaboratorBase = new CollaboratorUser(authService, hostUserInfo);
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(FindHydratedProjectsByUser.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      responseObserver.onNext(
          createQueryProjectsResponse(
              request.getFindProjects(),
              authService.getCurrentLoginUserInfo(),
              hostCollaboratorBase,
              ProjectVisibility.PUBLIC));
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AdvancedQueryProjectsResponse.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void findHydratedProjectsByOrganization(
      FindHydratedProjectsByOrganization request,
      StreamObserver<AdvancedQueryProjectsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      String errorMessage = null;
      if (request.getId().isEmpty() && request.getName().isEmpty()) {
        errorMessage = "id OR name not found in the FindHydratedProjectsByOrganization request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(FindHydratedProjectsByOrganization.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      GeneratedMessageV3 hostOrgInfo;
      if (!request.getId().isEmpty()) {
        hostOrgInfo = roleService.getOrgById(request.getId());
      } else {
        hostOrgInfo = roleService.getOrgByName(request.getName());
      }

      responseObserver.onNext(
          createQueryProjectsResponse(
              request.getFindProjects(),
              null,
              new CollaboratorOrg(hostOrgInfo),
              ProjectVisibility.PRIVATE));
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AdvancedQueryProjectsResponse.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void findHydratedProjectsByTeam(
      FindHydratedProjectsByTeam request,
      StreamObserver<AdvancedQueryProjectsResponse> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      String errorMessage = null;
      if (request.getId().isEmpty()
          && (request.getName().isEmpty() || request.getOrgId().isEmpty())) {
        errorMessage = "id OR name not found in the FindHydratedProjectsByTeam request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(FindHydratedProjectsByTeam.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      GeneratedMessageV3 hostTeamInfo;
      if (!request.getId().isEmpty()) {
        hostTeamInfo = roleService.getTeamById(request.getId());
      } else {
        hostTeamInfo = roleService.getTeamByName(request.getOrgId(), request.getName());
      }

      responseObserver.onNext(
          createQueryProjectsResponse(
              request.getFindProjects(),
              null,
              new CollaboratorTeam(hostTeamInfo),
              ProjectVisibility.PRIVATE));
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AdvancedQueryProjectsResponse.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void findHydratedDatasetsByOrganization(
      FindHydratedDatasetsByOrganization request,
      StreamObserver<AdvancedQueryDatasetsResponse> responseObserver) {
    // TODO: Unimplemented method
    super.findHydratedDatasetsByOrganization(request, responseObserver);
  }

  @Override
  public void findHydratedDatasetsByTeam(
      FindHydratedDatasetsByTeam request,
      StreamObserver<AdvancedQueryDatasetsResponse> responseObserver) {
    // TODO: Unimplemented method
    super.findHydratedDatasetsByTeam(request, responseObserver);
  }

  @Override
  public void getHydratedDatasetsByProjectId(
      GetHydratedDatasetsByProjectId request,
      StreamObserver<GetHydratedDatasetsByProjectId.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getProjectId().isEmpty()) {
        String errorMessage = "Project ID not found in GetHydratedDatasetsByProjectId request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(
                    Any.pack(GetHydratedExperimentRunsByProjectId.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      List<ExperimentRun> experimentRuns =
          experimentRunDAO.getExperimentRuns(
              ModelDBConstants.PROJECT_ID, request.getProjectId(), null);

      LOGGER.debug("ExperimentRun list record count : {}", experimentRuns.size());
      List<HydratedDataset> hydratedDatasets = new ArrayList<>();
      long totalRecords = 0L;
      if (!experimentRuns.isEmpty()) {
        Set<String> datasetVersionIdSet = new HashSet<>();
        for (ExperimentRun experimentRun : experimentRuns) {
          for (Artifact dataset : experimentRun.getDatasetsList()) {
            if (!dataset.getLinkedArtifactId().isEmpty()) {
              datasetVersionIdSet.add(dataset.getLinkedArtifactId());
            }
          }
        }
        LOGGER.debug(
            "Dataset version ids from experimentRun count : {}", datasetVersionIdSet.size());
        Set<String> datasetIdSet = new HashSet<>();
        if (!datasetVersionIdSet.isEmpty()) {
          List<DatasetVersion> datasetVersionList =
              datasetVersionDAO.getDatasetVersionsByBatchIds(new ArrayList<>(datasetVersionIdSet));
          for (DatasetVersion datasetVersion : datasetVersionList) {
            if (!datasetVersion.getDatasetId().isEmpty()) {
              datasetIdSet.add(datasetVersion.getDatasetId());
            }
          }
        }
        LOGGER.debug(
            "Dataset ids count based on the dataset version founded from experimentRun : "
                + datasetIdSet.size());
        if (!datasetIdSet.isEmpty()) {
          FindDatasets findDatasetsRequest =
              FindDatasets.newBuilder()
                  .addAllDatasetIds(datasetIdSet)
                  .setPageNumber(request.getPageNumber())
                  .setPageLimit(request.getPageLimit())
                  .setAscending(request.getAscending())
                  .setSortKey(request.getSortKey())
                  .build();
          // Get the user info from the Context
          UserInfo userInfo = authService.getCurrentLoginUserInfo();
          DatasetPaginationDTO datasetPaginationDTO =
              datasetDAO.findDatasets(findDatasetsRequest, userInfo, DatasetVisibility.PRIVATE);
          LOGGER.debug(
              ModelDBMessages.DATASET_RECORD_COUNT_MSG, datasetPaginationDTO.getTotalRecords());
          hydratedDatasets = findHydratedDatasets(datasetPaginationDTO, false);
          totalRecords = datasetPaginationDTO.getTotalRecords();
        }
      }
      LOGGER.debug("Final return HydratedDataset count : {}", hydratedDatasets.size());
      LOGGER.debug("Final return total record count : {}", totalRecords);

      responseObserver.onNext(
          GetHydratedDatasetsByProjectId.Response.newBuilder()
              .addAllHydratedDatasets(hydratedDatasets)
              .setTotalRecords(totalRecords)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetHydratedDatasetsByProjectId.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }
}
