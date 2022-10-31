package ai.verta.modeldb.advancedService;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.*;
import ai.verta.modeldb.FindProjects.Builder;
import ai.verta.modeldb.HydratedServiceGrpc.HydratedServiceImplBase;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.comment.CommentDAO;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.collaborator.CollaboratorOrg;
import ai.verta.modeldb.common.collaborator.CollaboratorTeam;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.exceptions.*;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.dto.*;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.experiment.FutureExperimentDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.experimentRun.FutureExperimentRunDAO;
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ServiceEnum.Service;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Value;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancedServiceImpl extends HydratedServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(AdvancedServiceImpl.class);
  private final AuthService authService;
  private final MDBRoleService mdbRoleService;
  private final FutureProjectDAO futureProjectDAO;
  private final ExperimentRunDAO experimentRunDAO;
  private final FutureExperimentRunDAO futureExperimentRunDAO;
  private final CommentDAO commentDAO;
  private final FutureExperimentDAO futureExperimentDAO;
  private final DatasetDAO datasetDAO;
  private final DatasetVersionDAO datasetVersionDAO;
  private final FutureExecutor executor;

  public AdvancedServiceImpl(ServiceSet serviceSet, DAOSet daoSet, FutureExecutor executor) {
    this.authService = serviceSet.getAuthService();
    this.mdbRoleService = serviceSet.getMdbRoleService();
    this.futureProjectDAO = daoSet.getFutureProjectDAO();
    this.experimentRunDAO = daoSet.getExperimentRunDAO();
    this.commentDAO = daoSet.getCommentDAO();
    this.futureExperimentDAO = daoSet.getFutureExperimentDAO();
    this.datasetDAO = daoSet.getDatasetDAO();
    this.datasetVersionDAO = daoSet.getDatasetVersionDAO();
    this.futureExperimentRunDAO = daoSet.getFutureExperimentRunDAO();
    this.executor = executor;
  }

  private List<HydratedProject> getHydratedProjects(List<Project> projects) {

    LOGGER.trace("Hydrating {} projects.", projects.size());
    if (projects.isEmpty()) {
      return Collections.emptyList();
    }

    List<HydratedProject> hydratedProjects = new ArrayList<>();
    // Map from project id to list of users (owners + collaborators) which need to be resolved
    Map<String, List<GetCollaboratorResponseItem>> projectCollaboratorMap =
        new ConcurrentHashMap<String, List<GetCollaboratorResponseItem>>();
    Set<String> vertaIds = new HashSet<>();
    Set<String> emailIds = new HashSet<>();
    LOGGER.trace("projects {}", projects);
    List<String> resourceIds = new LinkedList<>();
    Metadata requestHeaders = AuthInterceptor.METADATA_INFO.get();

    projects.forEach(
        project -> {
          vertaIds.add(project.getOwner());
          resourceIds.add(project.getId());
        });

    projects
        .parallelStream()
        .forEach(
            (project) -> {
              List<GetCollaboratorResponseItem> projectCollaboratorList =
                  mdbRoleService.getResourceCollaborators(
                      ModelDBServiceResourceTypes.PROJECT,
                      project.getId(),
                      project.getOwner(),
                      requestHeaders);
              projectCollaboratorMap.put(project.getId(), projectCollaboratorList);
            });

    projectCollaboratorMap.forEach(
        (k, projectCollaboratorList) -> {
          Map<String, List<String>> vertaIdAndEmailIdMap =
              ModelDBUtils.getVertaIdOrEmailIdMapFromCollaborator(projectCollaboratorList);
          vertaIds.addAll(vertaIdAndEmailIdMap.get(ModelDBConstants.VERTA_ID));
          emailIds.addAll(vertaIdAndEmailIdMap.get(ModelDBConstants.EMAILID));
        });

    LOGGER.trace("getHydratedProjects vertaIds : {}", vertaIds);
    LOGGER.trace("getHydratedProjects emailIds : {}", emailIds);

    Map<String, UserInfo> userInfoMap =
        authService.getUserInfoFromAuthServer(vertaIds, emailIds, null, false);

    Map<String, Actions> selfAllowedActions =
        mdbRoleService.getSelfAllowedActionsBatch(resourceIds, ModelDBServiceResourceTypes.PROJECT);
    for (Project project : projects) {
      // Use the map for vertaId  to UserInfo generated for this batch request to populate the
      // userInfo for individual projects.
      LOGGER.trace("Owner : {}", project.getOwner());
      List<CollaboratorUserInfo> collaboratorUserInfos =
          ModelDBUtils.getHydratedCollaboratorUserInfo(
              authService,
              mdbRoleService,
              projectCollaboratorMap.get(project.getId()),
              userInfoMap);

      var hydratedProjectBuilder =
          HydratedProject.newBuilder()
              .setProject(project)
              .addAllCollaboratorUserInfos(collaboratorUserInfos);
      if (project.getOwner() != null && userInfoMap.get(project.getOwner()) != null) {
        hydratedProjectBuilder.setOwnerUserInfo(userInfoMap.get(project.getOwner()));

        if (selfAllowedActions != null
            && selfAllowedActions.size() > 0
            && selfAllowedActions.containsKey(project.getId())
            && selfAllowedActions.get(project.getId()).getActionsList().size() > 0) {
          hydratedProjectBuilder.addAllAllowedActions(
              selfAllowedActions.get(project.getId()).getActionsList());
        }
      } else {
        LOGGER.info(ModelDBMessages.USER_NOT_FOUND_ERROR_MSG, project.getOwner());
      }
      hydratedProjects.add(hydratedProjectBuilder.build());
    }
    LOGGER.trace("Hydrated {} projects.", projects.size());
    return hydratedProjects;
  }

  @Override
  public void getHydratedProjects(
      GetHydratedProjects request, StreamObserver<GetHydratedProjects.Response> responseObserver) {
    try {
      // Get the user info from the Context
      var userInfo = authService.getCurrentLoginUserInfo();
      List<Resources> allowedProjects =
          mdbRoleService.getSelfAllowedResources(
              ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.READ);
      Builder builder = FindProjects.newBuilder();
      boolean allowedAllResources = RoleServiceUtils.checkAllResourceAllowed(allowedProjects);
      if (!allowedAllResources) {
        Set<String> allowedProjectIds = RoleServiceUtils.getResourceIds(allowedProjects);
        builder.addAllProjectIds(allowedProjectIds);
      }
      var findProjects =
          builder
              .setPageNumber(request.getPageNumber())
              .setPageLimit(request.getPageLimit())
              .setAscending(request.getAscending())
              .setSortKey(request.getSortKey())
              .setWorkspaceName(request.getWorkspaceName())
              .build();
      var response = futureProjectDAO.findProjects(findProjects).get();
      List<Project> projects = response.getProjectsList();

      List<HydratedProject> hydratedProjects = new ArrayList<>();
      if (!projects.isEmpty()) {
        hydratedProjects = getHydratedProjects(projects);
      }

      responseObserver.onNext(
          GetHydratedProjects.Response.newBuilder()
              .addAllHydratedProjects(hydratedProjects)
              .setTotalRecords(response.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetHydratedProjects.Response.getDefaultInstance());
    }
  }

  @Override
  public void getHydratedProjectById(
      GetHydratedProjectById request,
      StreamObserver<GetHydratedProjectById.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        var errorMessage = "Project ID not found in GetHydratedProjectById request";
        throw new InvalidArgumentException(errorMessage);
      }
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.READ);

      var project = futureProjectDAO.getProjectById(request.getId()).get();
      responseObserver.onNext(
          GetHydratedProjectById.Response.newBuilder()
              .setHydratedProject(getHydratedProjects(Collections.singletonList(project)).get(0))
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetHydratedProjectById.Response.getDefaultInstance());
    }
  }

  @Override
  public void getHydratedExperimentsByProjectId(
      GetHydratedExperimentsByProjectId request,
      StreamObserver<GetHydratedExperimentsByProjectId.Response> responseObserver) {
    try {
      if (request.getProjectId().isEmpty()) {
        var errorMessage = "Project ID not found in GetHydratedExperimentsByProjectId request";
        throw new InvalidArgumentException(errorMessage);
      }
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      FindExperiments.Response findExperimentResponse;
      try {
        findExperimentResponse =
            futureExperimentDAO
                .findExperiments(
                    FindExperiments.newBuilder()
                        .setProjectId(request.getProjectId())
                        .setPageLimit(request.getPageLimit())
                        .setPageNumber(request.getPageNumber())
                        .setAscending(request.getAscending())
                        .setSortKey(request.getSortKey())
                        .build())
                .get();
      } catch (Exception ex) {
        if (ex.getCause() != null && ex.getCause().getCause() != null) {
          if (ex.getCause().getCause() instanceof InvalidArgumentException) {
            throw (InvalidArgumentException) ex.getCause().getCause();
          } else if (ex.getCause().getCause() instanceof UnimplementedException) {
            throw (UnimplementedException) ex.getCause().getCause();
          }
        }
        throw ex;
      }

      List<Experiment> experiments = findExperimentResponse.getExperimentsList();

      List<HydratedExperiment> hydratedExperiments = new ArrayList<>();
      if (!experiments.isEmpty()) {
        hydratedExperiments = getHydratedExperiments(request.getProjectId(), experiments);
      }

      responseObserver.onNext(
          GetHydratedExperimentsByProjectId.Response.newBuilder()
              .addAllHydratedExperiments(hydratedExperiments)
              .setTotalRecords(findExperimentResponse.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetHydratedExperimentsByProjectId.Response.getDefaultInstance());
    }
  }

  @Override
  public void getHydratedExperimentRunsInProject(
      GetHydratedExperimentRunsByProjectId request,
      StreamObserver<GetHydratedExperimentRunsByProjectId.Response> responseObserver) {
    try {
      if (request.getProjectId().isEmpty()) {
        var errorMessage = "Project ID not found in GetHydratedExperimentRunsByProjectId request";
        throw new InvalidArgumentException(errorMessage);
      }
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      var experimentRunPaginationDTO =
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

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetHydratedExperimentRunsByProjectId.Response.getDefaultInstance());
    }
  }

  private List<HydratedExperimentRun> getHydratedExperimentRuns(
      List<ExperimentRun> experimentRuns) {
    LOGGER.debug(
        "experimentRuns count in getHydratedExperimentRuns method : {}", experimentRuns.size());
    Set<String> experimentIdSet = new HashSet<>();
    Set<String> vertaIdList = new HashSet<>();
    Set<String> projectIdSet = new HashSet<>();
    for (ExperimentRun experimentRun : experimentRuns) {
      vertaIdList.add(experimentRun.getOwner());
      experimentIdSet.add(experimentRun.getExperimentId());
      projectIdSet.add(experimentRun.getProjectId());
    }

    Map<String, Actions> actions = new HashMap<>();
    if (projectIdSet.size() > 0) {
      actions =
          mdbRoleService.getSelfAllowedActionsBatch(
              new ArrayList<>(projectIdSet), ModelDBServiceResourceTypes.PROJECT);
    }

    LOGGER.trace("vertaIdList {}", vertaIdList);
    LOGGER.trace("experimentIdSet {}", experimentIdSet);
    // Fetch the experiment list
    List<String> experimentIds = new ArrayList<>(experimentIdSet);
    LOGGER.trace("experimentIds {}", experimentIds);
    List<Experiment> experimentList = new ArrayList<>();
    if (!experimentIds.isEmpty()) {
      FindExperiments.Response findExperimentResponse = null;
      try {
        findExperimentResponse =
            futureExperimentDAO
                .findExperiments(
                    FindExperiments.newBuilder().addAllExperimentIds(experimentIds).build())
                .get();
      } catch (Exception e) {
        throw new ModelDBException(e);
      }
      experimentList = findExperimentResponse.getExperimentsList();
    }
    LOGGER.trace("experimentList {}", experimentList);
    // key: experiment.id, value: experiment
    Map<String, Experiment> experimentMap = new HashMap<>();
    for (Experiment experiment : experimentList) {
      experimentMap.put(experiment.getId(), experiment);
    }

    // Fetch the experimentRun owners userInfo
    Map<String, UserInfo> userInfoMap =
        authService.getUserInfoFromAuthServer(vertaIdList, null, null, false);

    List<HydratedExperimentRun> hydratedExperimentRuns = new LinkedList<>();
    LOGGER.trace("hydrating experiments");
    String currentUserVertaID =
        authService.getVertaIdFromUserInfo(authService.getCurrentLoginUserInfo());
    for (ExperimentRun experimentRun : experimentRuns) {

      var hydratedExperimentRunBuilder = HydratedExperimentRun.newBuilder();

      var userInfoValue = userInfoMap.get(experimentRun.getOwner());
      LOGGER.trace("owner {}", experimentRun.getOwner());
      if (userInfoValue != null) {
        hydratedExperimentRunBuilder.setOwnerUserInfo(userInfoValue);
        // Add Comments in hydrated data
        List<Comment> comments =
            commentDAO.getComments(
                ExperimentRunEntity.class.getSimpleName(), experimentRun.getId());
        LOGGER.trace("comments {}", comments);
        hydratedExperimentRunBuilder.addAllComments(comments);

        List<Action> actionList =
            ModelDBUtils.getActionsList(new ArrayList<>(projectIdSet), actions);
        LOGGER.info("actionList {}", actionList);
        var deleteAction =
            Action.newBuilder()
                .setModeldbServiceAction(ModelDBServiceActions.DELETE)
                .setService(Service.MODELDB_SERVICE)
                .build();
        var updateAction =
            Action.newBuilder()
                .setModeldbServiceAction(ModelDBServiceActions.UPDATE)
                .setService(Service.MODELDB_SERVICE)
                .build();
        LOGGER.info(
            "roleService.isCurrentUser(experimentRun.getOwner() {}",
            authService.isCurrentUser(experimentRun.getOwner()));
        if (currentUserVertaID.equalsIgnoreCase(experimentRun.getOwner())
            && !actionList.contains(deleteAction)
            && actionList.contains(updateAction)) {
          actionList.add(deleteAction);
        }
        LOGGER.info("actionList {}", actionList);
        // Add user specific actions
        hydratedExperimentRunBuilder.addAllAllowedActions(actionList);
      } else {
        LOGGER.info(ModelDBMessages.USER_NOT_FOUND_ERROR_MSG, experimentRun.getOwner());
      }
      // Prepare experiment for hydratedExperimentRun
      var hydratedExperiment =
          Experiment.newBuilder()
              .setName(experimentMap.get(experimentRun.getExperimentId()).getName())
              .build();
      LOGGER.trace("hydratedExperiment {}", hydratedExperiment);
      hydratedExperimentRunBuilder.setExperimentRun(experimentRun);
      hydratedExperimentRunBuilder.setExperiment(hydratedExperiment);
      var hydratedExperimentRun = hydratedExperimentRunBuilder.build();
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
    try {
      if (request.getId().isEmpty()) {
        var errorMessage = "ExperimentRun ID not found in GetHydratedExperimentRunById request";
        throw new InvalidArgumentException(errorMessage);
      }
      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      var currentLoginUserInfo = authService.getCurrentLoginUserInfo();

      var findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .addExperimentRunIds(request.getId())
              .setPageLimit(1)
              .setPageNumber(1)
              .build();
      final var experimentRunPaginationDTO =
          futureExperimentRunDAO.findExperimentRuns(findExperimentRuns).get();
      //      ExperimentRunPaginationDTO experimentRunPaginationDTO =
      //          experimentRunDAO.findExperimentRuns(projectDAO, currentLoginUserInfo,
      // findExperimentRuns);
      LOGGER.debug(
          ModelDBMessages.EXP_RUN_RECORD_COUNT_MSG, experimentRunPaginationDTO.getTotalRecords());

      List<HydratedExperimentRun> hydratedExperimentRuns = new ArrayList<>();
      if (!experimentRunPaginationDTO.getExperimentRunsList().isEmpty()) {
        hydratedExperimentRuns =
            getHydratedExperimentRuns(experimentRunPaginationDTO.getExperimentRunsList());
      }

      var response = GetHydratedExperimentRunById.Response.newBuilder();
      if (!hydratedExperimentRuns.isEmpty()) {
        if (hydratedExperimentRuns.size() > 1) {
          LOGGER.warn(
              "Multiple ({}) ExperimentRun found for given ID : {}",
              hydratedExperimentRuns.size(),
              request.getId());
        }
        response.setHydratedExperimentRun(hydratedExperimentRuns.get(0));
      }
      responseObserver.onNext(response.build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetHydratedExperimentRunById.Response.getDefaultInstance());
    }
  }

  @Override
  public void findHydratedExperimentRuns(
      FindExperimentRuns request,
      StreamObserver<AdvancedQueryExperimentRunsResponse> responseObserver) {
    try {
      LOGGER.trace("got current logged in user info");
      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        mdbRoleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);

        LOGGER.trace("Validated project accessibility");
      } else if (!request.getExperimentId().isEmpty()) {
        var experiment = futureExperimentDAO.getExperimentById(request.getExperimentId()).get();
        // Validate if current user has access to the entity or not
        mdbRoleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            experiment.getProjectId(),
            ModelDBServiceActions.READ);
        LOGGER.trace("Validated experiment accessibility");
      }

      final var experimentRunPaginationDTOFuture =
          futureExperimentRunDAO.findExperimentRuns(request);
      //      ExperimentRunPaginationDTO experimentRunPaginationDTO =
      //          experimentRunDAO.findExperimentRuns(projectDAO, currentLoginUserInfo, request);
      var futureResponse =
          experimentRunPaginationDTOFuture.thenCompose(
              experimentRunPaginationDTO -> {
                LOGGER.debug(
                    ModelDBMessages.EXP_RUN_RECORD_COUNT_MSG,
                    experimentRunPaginationDTO.getTotalRecords());

                List<HydratedExperimentRun> hydratedExperimentRuns = new ArrayList<>();
                if (request.getIdsOnly()) {
                  for (ExperimentRun experimentRun :
                      experimentRunPaginationDTO.getExperimentRunsList()) {
                    hydratedExperimentRuns.add(
                        HydratedExperimentRun.newBuilder().setExperimentRun(experimentRun).build());
                  }
                } else if (!experimentRunPaginationDTO.getExperimentRunsList().isEmpty()) {
                  hydratedExperimentRuns =
                      getHydratedExperimentRuns(experimentRunPaginationDTO.getExperimentRunsList());
                }

                LOGGER.debug("hydratedExperimentRuns size {}", hydratedExperimentRuns.size());
                return InternalFuture.completedInternalFuture(
                    AdvancedQueryExperimentRunsResponse.newBuilder()
                        .addAllHydratedExperimentRuns(hydratedExperimentRuns)
                        .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
                        .build());
              },
              executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AdvancedQueryExperimentRunsResponse.getDefaultInstance());
    }
  }

  @Override
  public void sortHydratedExperimentRuns(
      SortExperimentRuns request,
      StreamObserver<AdvancedQueryExperimentRunsResponse> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getExperimentRunIdsList().isEmpty() && request.getSortKey().isEmpty()) {
        errorMessage = "ExperimentRun Id's and sort key not found in SortExperimentRuns request";
      } else if (request.getExperimentRunIdsList().isEmpty()) {
        errorMessage = "ExperimentRun Id's not found in SortExperimentRuns request";
      } else if (request.getSortKey().isEmpty()) {
        errorMessage = "Sort key not found in SortExperimentRuns request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      var experimentRunPaginationDTO = experimentRunDAO.sortExperimentRuns(request);
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

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AdvancedQueryExperimentRunsResponse.getDefaultInstance());
    }
  }

  @Override
  public void getTopHydratedExperimentRuns(
      TopExperimentRunsSelector request,
      StreamObserver<AdvancedQueryExperimentRunsResponse> responseObserver) {
    try {
      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        mdbRoleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);
      } else if (!request.getExperimentId().isEmpty()) {
        var experiment = futureExperimentDAO.getExperimentById(request.getExperimentId()).get();
        // Validate if current user has access to the entity or not
        mdbRoleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            experiment.getProjectId(),
            ModelDBServiceActions.READ);
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

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AdvancedQueryExperimentRunsResponse.getDefaultInstance());
    }
  }

  private List<HydratedExperiment> getHydratedExperiments(
      String projectId, List<Experiment> experiments) {
    Map<String, Actions> actions =
        mdbRoleService.getSelfAllowedActionsBatch(
            Collections.singletonList(projectId), ModelDBServiceResourceTypes.PROJECT);
    LOGGER.debug("experiments count in getHydratedExperiments method : {}", experiments.size());
    Set<String> vertaIdList = new HashSet<>();
    for (Experiment experiment : experiments) {
      vertaIdList.add(experiment.getOwner());
    }

    // Fetch the experiment owners userInfo
    Map<String, UserInfo> userInfoMap =
        authService.getUserInfoFromAuthServer(vertaIdList, null, null, false);

    String currentUserVertaID =
        authService.getVertaIdFromUserInfo(authService.getCurrentLoginUserInfo());
    List<HydratedExperiment> hydratedExperiments = new LinkedList<>();
    for (Experiment experiment : experiments) {
      var hydratedExperimentBuilder = HydratedExperiment.newBuilder().setExperiment(experiment);

      var userInfoValue = userInfoMap.get(experiment.getOwner());
      if (userInfoValue != null) {
        hydratedExperimentBuilder.setOwnerUserInfo(userInfoValue);
        List<Action> actionList = new LinkedList<>();
        if (actions != null && actions.size() > 0) {
          actionList = ModelDBUtils.getActionsList(Collections.singletonList(projectId), actions);
        }
        var deleteAction =
            Action.newBuilder()
                .setModeldbServiceAction(ModelDBServiceActions.DELETE)
                .setService(Service.MODELDB_SERVICE)
                .build();
        var updateAction =
            Action.newBuilder()
                .setModeldbServiceAction(ModelDBServiceActions.UPDATE)
                .setService(Service.MODELDB_SERVICE)
                .build();
        if (currentUserVertaID.equalsIgnoreCase(experiment.getOwner())
            && !actionList.contains(deleteAction)
            && actionList.contains(updateAction)) {
          actionList.add(deleteAction);
        }
        hydratedExperimentBuilder.addAllAllowedActions(actionList);
      } else {
        LOGGER.info(ModelDBMessages.USER_NOT_FOUND_ERROR_MSG, experiment.getOwner());
      }
      hydratedExperiments.add(hydratedExperimentBuilder.build());
    }

    return hydratedExperiments;
  }

  @Override
  public void findHydratedExperiments(
      FindExperiments request, StreamObserver<AdvancedQueryExperimentsResponse> responseObserver) {
    try {
      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        mdbRoleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);
      }

      var userInfo = authService.getCurrentLoginUserInfo();
      FindExperiments.Response findExperimentResponse;
      try {
        findExperimentResponse =
            futureExperimentDAO
                .findExperiments(
                    FindExperiments.newBuilder()
                        .setProjectId(request.getProjectId())
                        .addAllPredicates(request.getPredicatesList())
                        .setPageLimit(request.getPageLimit())
                        .setPageNumber(request.getPageNumber())
                        .setAscending(request.getAscending())
                        .setSortKey(request.getSortKey())
                        .build())
                .get();
      } catch (Exception ex) {
        if (ex.getCause() != null && ex.getCause().getCause() != null) {
          if (ex.getCause().getCause() instanceof InvalidArgumentException) {
            throw (InvalidArgumentException) ex.getCause().getCause();
          } else if (ex.getCause().getCause() instanceof UnimplementedException) {
            throw (UnimplementedException) ex.getCause().getCause();
          }
        }
        throw ex;
      }
      LOGGER.debug(
          "ExperimentPaginationDTO record count : {}", findExperimentResponse.getTotalRecords());

      List<HydratedExperiment> hydratedExperiments = new ArrayList<>();
      if (request.getIdsOnly()) {
        for (Experiment experiment : findExperimentResponse.getExperimentsList()) {
          hydratedExperiments.add(
              HydratedExperiment.newBuilder().setExperiment(experiment).build());
        }
      } else if (!findExperimentResponse.getExperimentsList().isEmpty()) {
        hydratedExperiments =
            getHydratedExperiments(
                request.getProjectId(), findExperimentResponse.getExperimentsList());
      }

      responseObserver.onNext(
          AdvancedQueryExperimentsResponse.newBuilder()
              .addAllHydratedExperiments(hydratedExperiments)
              .setTotalRecords(findExperimentResponse.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AdvancedQueryExperimentsResponse.getDefaultInstance());
    }
  }

  @Override
  public void findHydratedProjects(
      FindProjects request, StreamObserver<AdvancedQueryProjectsResponse> responseObserver) {
    try {
      // Get the user info from the Context
      var userInfo = authService.getCurrentLoginUserInfo();

      FindProjects.Response projectResponse;
      try {
        projectResponse = futureProjectDAO.findProjects(request).get();
      } catch (Exception ex) {
        if (ex.getCause() != null && ex.getCause().getCause() != null) {
          if (ex.getCause().getCause() instanceof InvalidArgumentException) {
            throw (InvalidArgumentException) ex.getCause().getCause();
          } else if (ex.getCause().getCause() instanceof UnimplementedException) {
            throw (UnimplementedException) ex.getCause().getCause();
          } else if (ex.getCause().getCause() instanceof PermissionDeniedException) {
            throw (PermissionDeniedException) ex.getCause().getCause();
          }
        }
        throw ex;
      }
      LOGGER.debug(ModelDBMessages.PROJECT_RECORD_COUNT_MSG, projectResponse.getTotalRecords());

      List<HydratedProject> hydratedProjects = new ArrayList<>();
      if (request.getIdsOnly()) {
        for (Project project : projectResponse.getProjectsList()) {
          hydratedProjects.add(HydratedProject.newBuilder().setProject(project).build());
        }
      } else if (!projectResponse.getProjectsList().isEmpty()) {
        hydratedProjects = getHydratedProjects(projectResponse.getProjectsList());
      }

      responseObserver.onNext(
          AdvancedQueryProjectsResponse.newBuilder()
              .addAllHydratedProjects(hydratedProjects)
              .setTotalRecords(projectResponse.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AdvancedQueryProjectsResponse.getDefaultInstance());
    }
  }

  private List<HydratedDataset> getHydratedDatasets(List<Dataset> datasets) {

    LOGGER.trace("Hydrating {} datasets.", datasets.size());
    List<HydratedDataset> hydratedDatasets = new ArrayList<>();

    // Map from dataset id to list of users (owners + collaborators) which need to be resolved
    Map<String, List<GetCollaboratorResponseItem>> datasetCollaboratorMap = new HashMap<>();
    Set<String> vertaIds = new HashSet<>();
    Set<String> emailIds = new HashSet<>();
    Metadata requestHeaders = AuthInterceptor.METADATA_INFO.get();
    List<String> resourceIds = new LinkedList<>();

    datasets.forEach(
        dataset -> {
          vertaIds.add(dataset.getOwner());
          resourceIds.add(dataset.getId());
        });

    datasets
        .parallelStream()
        .forEach(
            (dataset) -> {
              List<GetCollaboratorResponseItem> datasetCollaboratorList =
                  mdbRoleService.getResourceCollaborators(
                      ModelDBServiceResourceTypes.DATASET,
                      dataset.getId(),
                      dataset.getOwner(),
                      requestHeaders);
              datasetCollaboratorMap.put(dataset.getId(), datasetCollaboratorList);
            });
    datasetCollaboratorMap.forEach(
        (k, datasetCollaboratorList) -> {
          Map<String, List<String>> vertaIdAndEmailIdMap =
              ModelDBUtils.getVertaIdOrEmailIdMapFromCollaborator(datasetCollaboratorList);
          vertaIds.addAll(vertaIdAndEmailIdMap.get(ModelDBConstants.VERTA_ID));
          emailIds.addAll(vertaIdAndEmailIdMap.get(ModelDBConstants.EMAILID));
        });

    LOGGER.trace("getHydratedDatasets vertaIds : {}", vertaIds.size());
    LOGGER.trace("getHydratedDatasets emailIds : {}", emailIds.size());

    Map<String, UserInfo> userInfoMap =
        authService.getUserInfoFromAuthServer(vertaIds, emailIds, null, false);

    LOGGER.trace("Got results from UAC : {}", userInfoMap.size());

    Map<String, Actions> selfAllowedActions =
        mdbRoleService.getSelfAllowedActionsBatch(resourceIds, ModelDBServiceResourceTypes.DATASET);

    for (Dataset dataset : datasets) {
      // Use the map for vertaId  to UserInfo generated for this batch request to populate the
      // userInfo for individual datasets.
      List<CollaboratorUserInfo> collaboratorUserInfos =
          ModelDBUtils.getHydratedCollaboratorUserInfo(
              authService,
              mdbRoleService,
              datasetCollaboratorMap.get(dataset.getId()),
              userInfoMap);

      var hydratedDatasetBuilder =
          HydratedDataset.newBuilder()
              .setDataset(dataset)
              .addAllCollaboratorUserInfos(collaboratorUserInfos);
      if (dataset.getOwner() != null && userInfoMap.get(dataset.getOwner()) != null) {
        hydratedDatasetBuilder.setOwnerUserInfo(userInfoMap.get(dataset.getOwner()));
      } else {
        LOGGER.info(ModelDBMessages.USER_NOT_FOUND_ERROR_MSG, dataset.getOwner());
      }
      if (selfAllowedActions != null
          && selfAllowedActions.size() > 0
          && selfAllowedActions.containsKey(dataset.getId())
          && selfAllowedActions.get(dataset.getId()).getActionsList().size() > 0) {
        hydratedDatasetBuilder.addAllAllowedActions(
            selfAllowedActions.get(dataset.getId()).getActionsList());
      }
      var hydratedDataset = hydratedDatasetBuilder.build();

      hydratedDatasets.add(hydratedDataset);
    }
    LOGGER.trace("Hydrated {} datasets.", datasets.size());
    return hydratedDatasets;
  }

  private List<HydratedDataset> findHydratedDatasets(
      DatasetPaginationDTO datasetPaginationDTO, Boolean isIdsOnly) {
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
    try {
      // Get the user info from the Context
      var userInfo = authService.getCurrentLoginUserInfo();
      var datasetPaginationDTO =
          datasetDAO.findDatasets(request, userInfo, ResourceVisibility.PRIVATE);
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

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AdvancedQueryDatasetsResponse.getDefaultInstance());
    }
  }

  private HydratedDatasetVersion getHydratedDatasetVersion(DatasetVersion datasetVersion) {
    var ownerUserInfo =
        authService.getUserInfo(datasetVersion.getOwner(), CommonConstants.UserIdentifier.VERTA_ID);

    Map<String, Actions> selfAllowedActions =
        mdbRoleService.getSelfAllowedActionsBatch(
            Collections.singletonList(datasetVersion.getDatasetId()),
            ModelDBServiceResourceTypes.DATASET);

    var hydratedDatasetVersionBuilder =
        HydratedDatasetVersion.newBuilder().setDatasetVersion(datasetVersion);
    if (ownerUserInfo != null) {
      hydratedDatasetVersionBuilder.setOwnerUserInfo(ownerUserInfo);
    }
    if (selfAllowedActions != null && selfAllowedActions.size() > 0) {
      List<Action> actionList =
          new ArrayList<Action>(
              selfAllowedActions.get(datasetVersion.getDatasetId()).getActionsList());
      if (ownerUserInfo != null) {
        var deleteAction =
            Action.newBuilder()
                .setModeldbServiceAction(ModelDBServiceActions.DELETE)
                .setService(Service.MODELDB_SERVICE)
                .build();
        var updateAction =
            Action.newBuilder()
                .setModeldbServiceAction(ModelDBServiceActions.UPDATE)
                .setService(Service.MODELDB_SERVICE)
                .build();
        if (authService.isCurrentUser(datasetVersion.getOwner())
            && !actionList.contains(deleteAction)
            && actionList.contains(updateAction)) {
          actionList.add(deleteAction);
        }
      }
      hydratedDatasetVersionBuilder.addAllAllowedActions(actionList);
    }
    return hydratedDatasetVersionBuilder.build();
  }

  @Override
  public void findHydratedDatasetVersions(
      FindDatasetVersions request,
      StreamObserver<AdvancedQueryDatasetVersionsResponse> responseObserver) {
    try {
      // Get the user info from the Context
      var userInfo = authService.getCurrentLoginUserInfo();

      if (!request.getDatasetId().isEmpty()) {
        // Validate if current user has access to the entity or not
        mdbRoleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.DATASET,
            request.getDatasetId(),
            ModelDBServiceActions.READ);
      }

      DatasetVersionDTO datasetVersionPaginationDTO =
          datasetVersionDAO.findDatasetVersions(datasetDAO, request, userInfo);
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

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AdvancedQueryDatasetVersionsResponse.getDefaultInstance());
    }
  }

  @Override
  public void getHydratedDatasetByName(
      GetHydratedDatasetByName request,
      StreamObserver<GetHydratedDatasetByName.Response> responseObserver) {
    try {
      if (request.getName().isEmpty()) {
        var errorMessage = "Dataset Name not found in GetHydratedDatasetByName request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Get the user info from the Context
      var userInfo = authService.getCurrentLoginUserInfo();

      FindDatasets.Builder findDatasets =
          FindDatasets.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey(ModelDBConstants.NAME)
                      .setValue(Value.newBuilder().setStringValue(request.getName()).build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .setWorkspaceName(
                  request.getWorkspaceName().isEmpty()
                      ? authService.getUsernameFromUserInfo(userInfo)
                      : request.getWorkspaceName());

      var datasetPaginationDTO =
          datasetDAO.findDatasets(findDatasets.build(), userInfo, ResourceVisibility.PRIVATE);

      if (datasetPaginationDTO.getTotalRecords() == 0) {
        throw new NotFoundException("Dataset not found");
      }
      Dataset selfOwnerdataset = null;
      List<Dataset> sharedDatasets = new ArrayList<>();

      for (Dataset dataset : datasetPaginationDTO.getDatasets()) {
        if (userInfo == null
            || dataset.getOwner().equals(authService.getVertaIdFromUserInfo(userInfo))) {
          selfOwnerdataset = dataset;
        } else {
          sharedDatasets.add(dataset);
        }
      }

      List<HydratedDataset> sharedHydratedDatasets = new ArrayList<>();
      if (!sharedDatasets.isEmpty()) {
        sharedHydratedDatasets = getHydratedDatasets(sharedDatasets);
      }

      GetHydratedDatasetByName.Response.Builder getHydratedDatasetByNameResponse =
          GetHydratedDatasetByName.Response.newBuilder()
              .addAllSharedHydratedDatasets(sharedHydratedDatasets);

      if (selfOwnerdataset != null) {
        getHydratedDatasetByNameResponse.setHydratedDatasetByUser(
            getHydratedDatasets(Collections.singletonList(selfOwnerdataset)).get(0));
      }

      responseObserver.onNext(getHydratedDatasetByNameResponse.build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetHydratedDatasetByName.Response.getDefaultInstance());
    }
  }

  private AdvancedQueryProjectsResponse createQueryProjectsResponse(
      FindProjects findProjectsRequest) {

    FindProjects.Response projectResponse = null;
    try {
      projectResponse = futureProjectDAO.findProjects(findProjectsRequest).get();
    } catch (Exception e) {
      throw new ModelDBException(e);
    }
    LOGGER.debug(ModelDBMessages.PROJECT_RECORD_COUNT_MSG, projectResponse.getTotalRecords());

    List<HydratedProject> hydratedProjects = new ArrayList<>();
    if (findProjectsRequest.getIdsOnly()) {
      for (Project project : projectResponse.getProjectsList()) {
        hydratedProjects.add(HydratedProject.newBuilder().setProject(project).build());
      }
    } else if (!projectResponse.getProjectsList().isEmpty()) {
      hydratedProjects = getHydratedProjects(projectResponse.getProjectsList());
    }
    return AdvancedQueryProjectsResponse.newBuilder()
        .addAllHydratedProjects(hydratedProjects)
        .setTotalRecords(projectResponse.getTotalRecords())
        .build();
  }

  @Override
  public void findHydratedProjectsByUser(
      FindHydratedProjectsByUser request,
      StreamObserver<AdvancedQueryProjectsResponse> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getEmail().isEmpty() && request.getVertaId().isEmpty()) {
        errorMessage = "Email OR vertaId not found in the FindHydratedPublicProjects request";
      }

      CollaboratorUser hostCollaboratorBase = null;
      String userEmail = request.getEmail();
      if (!userEmail.isEmpty() && ModelDBUtils.isValidEmail(userEmail)) {
        var hostUserInfo =
            authService.getUserInfo(userEmail, CommonConstants.UserIdentifier.EMAIL_ID);
        hostCollaboratorBase = new CollaboratorUser(authService, hostUserInfo);
      } else if (!userEmail.isEmpty()) {
        errorMessage = "Invalid email found in the FindHydratedPublicProjects request";
      } else if (!request.getVertaId().isEmpty()) {
        var hostUserInfo =
            authService.getUserInfo(request.getVertaId(), CommonConstants.UserIdentifier.VERTA_ID);
        hostCollaboratorBase = new CollaboratorUser(authService, hostUserInfo);
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      var findProjects = request.toBuilder().getFindProjects();
      CollaboratorUserInfo.Builder builder = CollaboratorUserInfo.newBuilder();
      hostCollaboratorBase.addToResponse(builder);
      findProjects =
          findProjects
              .toBuilder()
              .setWorkspaceName(builder.getCollaboratorUserInfo().getVertaInfo().getUsername())
              .build();

      responseObserver.onNext(createQueryProjectsResponse(findProjects));
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AdvancedQueryProjectsResponse.getDefaultInstance());
    }
  }

  @Override
  public void findHydratedProjectsByOrganization(
      FindHydratedProjectsByOrganization request,
      StreamObserver<AdvancedQueryProjectsResponse> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getName().isEmpty()) {
        errorMessage = "id OR name not found in the FindHydratedProjectsByOrganization request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }
      GeneratedMessageV3 hostOrgInfo;
      if (!request.getId().isEmpty()) {
        hostOrgInfo = mdbRoleService.getOrgById(request.getId());
      } else {
        hostOrgInfo = mdbRoleService.getOrgByName(request.getName());
      }

      var findProjects = request.toBuilder().getFindProjects();
      CollaboratorOrg collaboratorOrg = new CollaboratorOrg(hostOrgInfo);
      CollaboratorUserInfo.Builder builder = CollaboratorUserInfo.newBuilder();
      collaboratorOrg.addToResponse(builder);
      findProjects =
          findProjects
              .toBuilder()
              .setWorkspaceName(builder.getCollaboratorOrganization().getName())
              .build();

      responseObserver.onNext(createQueryProjectsResponse(findProjects));
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AdvancedQueryProjectsResponse.getDefaultInstance());
    }
  }

  @Override
  public void findHydratedProjectsByTeam(
      FindHydratedProjectsByTeam request,
      StreamObserver<AdvancedQueryProjectsResponse> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && (request.getName().isEmpty() || request.getOrgId().isEmpty())) {
        errorMessage = "id OR name not found in the FindHydratedProjectsByTeam request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }
      GeneratedMessageV3 hostTeamInfo;
      if (!request.getId().isEmpty()) {
        hostTeamInfo = mdbRoleService.getTeamById(request.getId());
      } else {
        hostTeamInfo = mdbRoleService.getTeamByName(request.getOrgId(), request.getName());
      }

      var findProjects = request.toBuilder().getFindProjects();
      CollaboratorTeam collaboratorTeam = new CollaboratorTeam(hostTeamInfo);
      CollaboratorUserInfo.Builder builder = CollaboratorUserInfo.newBuilder();
      collaboratorTeam.addToResponse(builder);
      findProjects =
          findProjects
              .toBuilder()
              .setWorkspaceName(builder.getCollaboratorTeam().getName())
              .build();

      responseObserver.onNext(createQueryProjectsResponse(findProjects));
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AdvancedQueryProjectsResponse.getDefaultInstance());
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
    try {
      if (request.getProjectId().isEmpty()) {
        var errorMessage = "Project ID not found in GetHydratedDatasetsByProjectId request";
        throw new InvalidArgumentException(errorMessage);
      }
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      List<ExperimentRun> experimentRuns =
          experimentRunDAO.getExperimentRuns(
              ModelDBConstants.PROJECT_ID, request.getProjectId(), null);

      LOGGER.debug("ExperimentRun list record count : {}", experimentRuns.size());
      List<HydratedDataset> hydratedDatasets = new ArrayList<>();
      var totalRecords = 0L;
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
          var findDatasetsRequest =
              FindDatasets.newBuilder()
                  .addAllDatasetIds(datasetIdSet)
                  .setPageNumber(request.getPageNumber())
                  .setPageLimit(request.getPageLimit())
                  .setAscending(request.getAscending())
                  .setSortKey(request.getSortKey())
                  .build();
          // Get the user info from the Context
          var userInfo = authService.getCurrentLoginUserInfo();
          var datasetPaginationDTO =
              datasetDAO.findDatasets(findDatasetsRequest, userInfo, ResourceVisibility.PRIVATE);
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

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetHydratedDatasetsByProjectId.Response.getDefaultInstance());
    }
  }
}
