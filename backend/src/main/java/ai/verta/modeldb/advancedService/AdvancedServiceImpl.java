package ai.verta.modeldb.advancedService;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.*;
import ai.verta.modeldb.HydratedServiceGrpc.HydratedServiceImplBase;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.comment.CommentDAO;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.collaborator.CollaboratorOrg;
import ai.verta.modeldb.common.collaborator.CollaboratorTeam;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.dto.*;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.experimentRun.FutureExperimentRunDAO;
import ai.verta.modeldb.project.ProjectDAO;
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
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancedServiceImpl extends HydratedServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(AdvancedServiceImpl.class);
  private final AuthService authService;
  private final RoleService roleService;
  private final ProjectDAO projectDAO;
  private final ExperimentRunDAO experimentRunDAO;
  private final FutureExperimentRunDAO futureExperimentRunDAO;
  private final CommentDAO commentDAO;
  private final ExperimentDAO experimentDAO;
  private final DatasetDAO datasetDAO;
  private final DatasetVersionDAO datasetVersionDAO;
  private final Executor executor;

  public AdvancedServiceImpl(ServiceSet serviceSet, DAOSet daoSet, Executor executor) {
    this.authService = serviceSet.authService;
    this.roleService = serviceSet.roleService;
    this.projectDAO = daoSet.projectDAO;
    this.experimentRunDAO = daoSet.experimentRunDAO;
    this.commentDAO = daoSet.commentDAO;
    this.experimentDAO = daoSet.experimentDAO;
    this.datasetDAO = daoSet.datasetDAO;
    this.datasetVersionDAO = daoSet.datasetVersionDAO;
    this.futureExperimentRunDAO = daoSet.futureExperimentRunDAO;
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

    projects
        .parallelStream()
        .forEach(
            (project) -> {
              vertaIds.add(project.getOwner());
              resourceIds.add(project.getId());
              List<GetCollaboratorResponseItem> projectCollaboratorList =
                  roleService.getResourceCollaborators(
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
          projectDAO.findProjects(findProjects, null, userInfo, ResourceVisibility.PRIVATE);
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
        String errorMessage = "Project ID not found in GetHydratedProjectById request";
        throw new InvalidArgumentException(errorMessage);
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
        String errorMessage = "Project ID not found in GetHydratedExperimentsByProjectId request";
        throw new InvalidArgumentException(errorMessage);
      }
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      ExperimentPaginationDTO experimentPaginationDTO =
          experimentDAO.getExperimentsInProject(
              projectDAO,
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
        String errorMessage =
            "Project ID not found in GetHydratedExperimentRunsByProjectId request";
        throw new InvalidArgumentException(errorMessage);
      }
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.getExperimentRunsFromEntity(
              projectDAO,
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
          roleService.getSelfAllowedActionsBatch(
              new ArrayList<>(projectIdSet), ModelDBServiceResourceTypes.PROJECT);
    }

    LOGGER.trace("vertaIdList {}", vertaIdList);
    LOGGER.trace("experimentIdSet {}", experimentIdSet);
    // Fetch the experiment list
    List<String> experimentIds = new ArrayList<>(experimentIdSet);
    LOGGER.trace("experimentIds {}", experimentIds);
    List<Experiment> experimentList = new ArrayList<>();
    if (!experimentIds.isEmpty()) {
      experimentList = experimentDAO.getExperimentsByBatchIds(experimentIds);
    }
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
    String currentUserVertaID =
        authService.getVertaIdFromUserInfo(authService.getCurrentLoginUserInfo());
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

        List<Action> actionList =
            ModelDBUtils.getActionsList(new ArrayList<>(projectIdSet), actions);
        LOGGER.info("actionList {}", actionList);
        Action deleteAction =
            Action.newBuilder()
                .setModeldbServiceAction(ModelDBServiceActions.DELETE)
                .setService(Service.MODELDB_SERVICE)
                .build();
        Action updateAction =
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
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetHydratedExperimentRunById request";
        throw new InvalidArgumentException(errorMessage);
      }
      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();

      FindExperimentRuns findExperimentRuns =
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

      GetHydratedExperimentRunById.Response.Builder response =
          GetHydratedExperimentRunById.Response.newBuilder();
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

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.sortExperimentRuns(projectDAO, request);
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

      List<ExperimentRun> experimentRuns =
          experimentRunDAO.getTopExperimentRuns(projectDAO, request);
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
        roleService.getSelfAllowedActionsBatch(
            Collections.singletonList(projectId), ModelDBServiceResourceTypes.PROJECT);
    LOGGER.debug("experiments count in getHydratedExperiments method : {}", experiments.size());
    Set<String> vertaIdList = new HashSet<>();
    for (Experiment experiment : experiments) {
      vertaIdList.add(experiment.getOwner());
    }

    // Fetch the experiment owners userInfo
    Map<String, UserInfo> userInfoMap =
        authService.getUserInfoFromAuthServer(vertaIdList, null, null);

    String currentUserVertaID =
        authService.getVertaIdFromUserInfo(authService.getCurrentLoginUserInfo());
    List<HydratedExperiment> hydratedExperiments = new LinkedList<>();
    for (Experiment experiment : experiments) {
      HydratedExperiment.Builder hydratedExperimentBuilder =
          HydratedExperiment.newBuilder().setExperiment(experiment);

      UserInfo userInfoValue = userInfoMap.get(experiment.getOwner());
      if (userInfoValue != null) {
        hydratedExperimentBuilder.setOwnerUserInfo(userInfoValue);
        List<Action> actionList = new LinkedList<>();
        if (actions != null && actions.size() > 0) {
          actionList = ModelDBUtils.getActionsList(Collections.singletonList(projectId), actions);
        }
        Action deleteAction =
            Action.newBuilder()
                .setModeldbServiceAction(ModelDBServiceActions.DELETE)
                .setService(Service.MODELDB_SERVICE)
                .build();
        Action updateAction =
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
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);
      }

      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      ExperimentPaginationDTO experimentPaginationDTO =
          experimentDAO.findExperiments(projectDAO, userInfo, request);
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
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      ProjectPaginationDTO projectPaginationDTO =
          projectDAO.findProjects(request, null, userInfo, ResourceVisibility.PRIVATE);
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

    datasets
        .parallelStream()
        .forEach(
            (dataset) -> {
              vertaIds.add(dataset.getOwner());
              resourceIds.add(dataset.getId());
              List<GetCollaboratorResponseItem> datasetCollaboratorList =
                  roleService.getResourceCollaborators(
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
        LOGGER.info(ModelDBMessages.USER_NOT_FOUND_ERROR_MSG, dataset.getOwner());
      }
      if (selfAllowedActions != null
          && selfAllowedActions.size() > 0
          && selfAllowedActions.containsKey(dataset.getId())
          && selfAllowedActions.get(dataset.getId()).getActionsList().size() > 0) {
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
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      DatasetPaginationDTO datasetPaginationDTO =
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
    UserInfo ownerUserInfo =
        authService.getUserInfo(datasetVersion.getOwner(), CommonConstants.UserIdentifier.VERTA_ID);

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
      List<Action> actionList =
          new ArrayList<Action>(
              selfAllowedActions.get(datasetVersion.getDatasetId()).getActionsList());
      if (ownerUserInfo != null) {
        Action deleteAction =
            Action.newBuilder()
                .setModeldbServiceAction(ModelDBServiceActions.DELETE)
                .setService(Service.MODELDB_SERVICE)
                .build();
        Action updateAction =
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
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      if (!request.getDatasetId().isEmpty()) {
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
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
        String errorMessage = "Dataset Name not found in GetHydratedDatasetByName request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

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

      DatasetPaginationDTO datasetPaginationDTO =
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
      FindProjects findProjectsRequest,
      UserInfo currentLoginUserInfo,
      CollaboratorBase host,
      ResourceVisibility visibility) {

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
    try {
      String errorMessage = null;
      if (request.getEmail().isEmpty() && request.getVertaId().isEmpty()) {
        errorMessage = "Email OR vertaId not found in the FindHydratedPublicProjects request";
      }

      CollaboratorUser hostCollaboratorBase = null;
      String userEmail = request.getEmail();
      if (!userEmail.isEmpty() && ModelDBUtils.isValidEmail(userEmail)) {
        UserInfo hostUserInfo =
            authService.getUserInfo(userEmail, CommonConstants.UserIdentifier.EMAIL_ID);
        hostCollaboratorBase = new CollaboratorUser(authService, hostUserInfo);
      } else if (!userEmail.isEmpty()) {
        errorMessage = "Invalid email found in the FindHydratedPublicProjects request";
      } else if (!request.getVertaId().isEmpty()) {
        UserInfo hostUserInfo =
            authService.getUserInfo(request.getVertaId(), CommonConstants.UserIdentifier.VERTA_ID);
        hostCollaboratorBase = new CollaboratorUser(authService, hostUserInfo);
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      responseObserver.onNext(
          createQueryProjectsResponse(
              request.getFindProjects(),
              authService.getCurrentLoginUserInfo(),
              hostCollaboratorBase,
              ResourceVisibility.PRIVATE));
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
        hostOrgInfo = roleService.getOrgById(request.getId());
      } else {
        hostOrgInfo = roleService.getOrgByName(request.getName());
      }

      responseObserver.onNext(
          createQueryProjectsResponse(
              request.getFindProjects(),
              null,
              new CollaboratorOrg(hostOrgInfo),
              ResourceVisibility.PRIVATE));
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
        hostTeamInfo = roleService.getTeamById(request.getId());
      } else {
        hostTeamInfo = roleService.getTeamByName(request.getOrgId(), request.getName());
      }

      responseObserver.onNext(
          createQueryProjectsResponse(
              request.getFindProjects(),
              null,
              new CollaboratorTeam(hostTeamInfo),
              ResourceVisibility.PRIVATE));
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
        String errorMessage = "Project ID not found in GetHydratedDatasetsByProjectId request";
        throw new InvalidArgumentException(errorMessage);
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
