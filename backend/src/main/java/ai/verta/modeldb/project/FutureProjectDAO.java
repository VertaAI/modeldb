package ai.verta.modeldb.project;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.Pagination;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.AddProjectTags;
import ai.verta.modeldb.CreateProject;
import ai.verta.modeldb.DeleteProjectArtifact;
import ai.verta.modeldb.DeleteProjectAttributes;
import ai.verta.modeldb.DeleteProjectTags;
import ai.verta.modeldb.Empty;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.FindProjects;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetProjectByName;
import ai.verta.modeldb.GetProjectCodeVersion;
import ai.verta.modeldb.GetProjectReadme;
import ai.verta.modeldb.GetProjectShortName;
import ai.verta.modeldb.GetSummary;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.LastModifiedExperimentRunSummary;
import ai.verta.modeldb.LogAttributes;
import ai.verta.modeldb.LogProjectArtifacts;
import ai.verta.modeldb.LogProjectCodeVersion;
import ai.verta.modeldb.MetricsSummary;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.SetProjectReadme;
import ai.verta.modeldb.SetProjectShortName;
import ai.verta.modeldb.UpdateProjectAttributes;
import ai.verta.modeldb.UpdateProjectDescription;
import ai.verta.modeldb.VerifyConnectionResponse;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.FutureUtil;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.experimentRun.FutureExperimentRunDAO;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.DatasetHandler;
import ai.verta.modeldb.experimentRun.subtypes.PredicatesHandler;
import ai.verta.modeldb.experimentRun.subtypes.SortingHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.modeldb.project.subtypes.CreateProjectHandler;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.utils.UACApisUtil;
import ai.verta.uac.Action;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.DeleteResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetWorkspaceByName;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.SetResource;
import ai.verta.uac.Workspace;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.statement.Query;

public class FutureProjectDAO {
  private static final Logger LOGGER = LogManager.getLogger(FutureProjectDAO.class);

  private final FutureJdbi jdbi;
  private final Executor executor;
  private final UAC uac;
  private final boolean isMssql;

  private final AttributeHandler attributeHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final PredicatesHandler predicatesHandler;
  private final CodeVersionHandler codeVersionHandler;
  private final SortingHandler sortingHandler;
  private final FutureExperimentRunDAO futureExperimentRunDAO;
  private final UACApisUtil uacApisUtil;
  private final CreateProjectHandler createProjectHandler;

  public FutureProjectDAO(
      Executor executor,
      FutureJdbi jdbi,
      UAC uac,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetVersionDAO datasetVersionDAO,
      MDBConfig mdbConfig,
      FutureExperimentRunDAO futureExperimentRunDAO,
      UACApisUtil uacApisUtil) {
    this.jdbi = jdbi;
    this.isMssql = mdbConfig.getDatabase().getRdbConfiguration().isMssql();
    this.executor = executor;
    this.uac = uac;
    this.futureExperimentRunDAO = futureExperimentRunDAO;
    this.uacApisUtil = uacApisUtil;

    var entityName = "ProjectEntity";
    attributeHandler = new AttributeHandler(executor, jdbi, entityName);
    tagsHandler = new TagsHandler(executor, jdbi, entityName);
    codeVersionHandler = new CodeVersionHandler(executor, jdbi, "project");
    DatasetHandler datasetHandler = new DatasetHandler(executor, jdbi, entityName, mdbConfig);
    artifactHandler =
        new ArtifactHandler(
            executor,
            jdbi,
            entityName,
            codeVersionHandler,
            datasetHandler,
            artifactStoreDAO,
            datasetVersionDAO,
            mdbConfig);
    predicatesHandler = new PredicatesHandler(executor, "project", "p", uacApisUtil);
    sortingHandler = new SortingHandler("project");
    createProjectHandler =
        new CreateProjectHandler(
            executor, jdbi, mdbConfig, uac, attributeHandler, tagsHandler, artifactHandler);
  }

  public InternalFuture<Void> deleteAttributes(DeleteProjectAttributes request) {
    final var projectId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              }
            },
            executor);

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getAttributeKeysList());

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> attributeHandler.deleteKeyValues(handle, projectId, maybeKeys)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<List<KeyValue>> getAttributes(GetAttributes request) {
    final var projectId = request.getId();
    final var keys = request.getAttributeKeysList();
    final var getAll = request.getGetAll();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (keys.isEmpty() && !getAll) {
                throw new InvalidArgumentException("Attribute keys not present");
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ),
            executor)
        .thenCompose(unused -> attributeHandler.getKeyValues(projectId, keys, getAll), executor);
  }

  public InternalFuture<Void> logAttributes(LogAttributes request) {
    final var projectId = request.getId();
    final var attributes = request.getAttributesList();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (attributes.isEmpty()) {
                throw new InvalidArgumentException("Attributes not present");
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> attributeHandler.logKeyValues(handle, projectId, attributes)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<Void> updateProjectAttributes(UpdateProjectAttributes request) {
    final var projectId = request.getId();
    final var attribute = request.getAttribute();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (attribute.getKey().isEmpty()) {
                throw new InvalidArgumentException("Attribute not present");
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> attributeHandler.updateKeyValue(handle, projectId, attribute)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<Void> addTags(AddProjectTags request) {
    final var projectId = request.getId();
    final var tags = request.getTagsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (tags.isEmpty()) {
                throw new InvalidArgumentException("Tags not present");
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused -> jdbi.useHandle(handle -> tagsHandler.addTags(handle, projectId, tags)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<Void> deleteTags(DeleteProjectTags request) {
    final var projectId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              }
            },
            executor);

    final Optional<List<String>> maybeTags =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getTagsList());

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(handle -> tagsHandler.deleteTags(handle, projectId, maybeTags)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<List<String>> getTags(GetTags request) {
    final var projectId = request.getId();
    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ),
            executor)
        .thenCompose(unused -> tagsHandler.getTags(projectId), executor);
  }

  private InternalFuture<Void> updateModifiedTimestamp(String projectId, Long now) {
    String greatestValueStr;
    if (isMssql) {
      greatestValueStr =
          "(SELECT MAX(value) FROM (VALUES (date_updated),(:now)) AS maxvalues(value))";
    } else {
      greatestValueStr = "greatest(date_updated, :now)";
    }

    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    String.format(
                        "update project set date_updated=%s where id=:project_id",
                        greatestValueStr))
                .bind("project_id", projectId)
                .bind("now", now)
                .execute());
  }

  private InternalFuture<Void> updateVersionNumber(String projectId) {
    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "update project set version_number=(version_number + 1) where id=:project_id")
                .bind("project_id", projectId)
                .execute());
  }

  public InternalFuture<Void> checkProjectPermission(
      String projId, ModelDBActionEnum.ModelDBServiceActions action) {
    var resourceBuilder =
        Resources.newBuilder()
            .setService(ServiceEnum.Service.MODELDB_SERVICE)
            .setResourceType(
                ResourceType.newBuilder()
                    .setModeldbServiceResourceType(
                        ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT));
    if (projId != null) {
      resourceBuilder.addResourceIds(projId);
    }
    return FutureUtil.clientRequest(
            uac.getAuthzService()
                .isSelfAllowed(
                    IsSelfAllowed.newBuilder()
                        .addActions(
                            Action.newBuilder()
                                .setModeldbServiceAction(action)
                                .setService(ServiceEnum.Service.MODELDB_SERVICE))
                        .addResources(resourceBuilder.build())
                        .build()),
            executor)
        .thenAccept(
            response -> {
              if (!response.getAllowed()) {
                throw new PermissionDeniedException("Permission denied");
              }
            },
            executor);
  }

  public InternalFuture<Long> getProjectDatasetCount(String projectId) {
    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(
            unused ->
                jdbi.withHandle(
                    handle -> {
                      var queryStr =
                          "SELECT COUNT(distinct ar.linked_artifact_id) from artifact ar inner join experiment_run er ON er.id = ar.experiment_run_id "
                              + " WHERE er.project_id = :projectId AND ar.experiment_run_id is not null AND ar.linked_artifact_id <> '' ";
                      return handle
                          .createQuery(queryStr)
                          .bind("projectId", projectId)
                          .mapTo(Long.class)
                          .one();
                    }),
            executor);
  }

  public InternalFuture<GetUrlForArtifact.Response> getUrlForArtifact(GetUrlForArtifact request) {
    final var projectId = request.getId();

    InternalFuture<Void> permissionCheck;
    if (request.getMethod().equalsIgnoreCase("get")) {
      permissionCheck =
          checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ);
    } else {
      permissionCheck =
          checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);
    }

    return permissionCheck.thenCompose(
        unused -> artifactHandler.getUrlForArtifact(request), executor);
  }

  public InternalFuture<VerifyConnectionResponse> verifyConnection(Empty request) {
    return InternalFuture.completedInternalFuture(
        VerifyConnectionResponse.newBuilder().setStatus(true).build());
  }

  public InternalFuture<FindProjects.Response> findProjects(FindProjects request) {
    return FutureUtil.clientRequest(
            uac.getUACService().getCurrentUser(ai.verta.uac.Empty.newBuilder().build()), executor)
        .thenCompose(
            userInfo -> {
              InternalFuture<List<GetResourcesResponseItem>> resourcesFuture;
              if (request.getWorkspaceName().isEmpty()
                  || request.getWorkspaceName().equals(userInfo.getVertaInfo().getUsername())) {
                resourcesFuture =
                    uacApisUtil.getResourceItemsForLoginUserWorkspace(
                        request.getWorkspaceName(),
                        Optional.of(request.getProjectIdsList()),
                        ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);
              } else {
                resourcesFuture =
                    uacApisUtil.getResourceItemsForWorkspace(
                        Optional.of(request.getWorkspaceName()),
                        Optional.of(request.getProjectIdsList()),
                        Optional.empty(),
                        ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);
              }

              return resourcesFuture.thenCompose(
                  getResourceItems -> {
                    Map<String, GetResourcesResponseItem> getResourcesMap = new HashMap<>();
                    Set<String> accessibleResourceIdsWithCollaborator =
                        getResourceItems.stream()
                            .peek(
                                responseItem ->
                                    getResourcesMap.put(responseItem.getResourceId(), responseItem))
                            .map(GetResourcesResponseItem::getResourceId)
                            .collect(Collectors.toSet());

                    if (accessibleResourceIdsWithCollaborator.isEmpty()) {
                      LOGGER.debug("Accessible Project Ids not found, size 0");
                      return InternalFuture.completedInternalFuture(
                          FindProjects.Response.newBuilder().build());
                    }

                    final InternalFuture<QueryFilterContext> futureLocalContext =
                        getFutureLocalContext();

                    // futurePredicatesContext
                    final var futurePredicatesContext =
                        predicatesHandler.processPredicates(request.getPredicatesList(), executor);

                    // futureSortingContext
                    final var futureSortingContext =
                        sortingHandler.processSort(request.getSortKey(), request.getAscending());

                    var futureProjectIdsContext =
                        getFutureProjectIdsContext(request, accessibleResourceIdsWithCollaborator);

                    final var futureProjects =
                        InternalFuture.sequence(
                                Arrays.asList(
                                    futureLocalContext,
                                    futurePredicatesContext,
                                    futureSortingContext,
                                    futureProjectIdsContext),
                                executor)
                            .thenApply(QueryFilterContext::combine, executor)
                            .thenCompose(
                                queryContext ->
                                    jdbi.withHandle(
                                            handle -> {
                                              var sql =
                                                  "select p.id, p.date_created, p.date_updated, p.name, p.description, p.owner, "
                                                      + "p.short_name, p.project_visibility, p.readme_text, "
                                                      + "p.deleted, p.version_number from project p ";

                                              Query query =
                                                  CommonUtils.buildQueryFromQueryContext(
                                                      "p",
                                                      Pagination.newBuilder()
                                                          .setPageLimit(request.getPageLimit())
                                                          .setPageNumber(request.getPageNumber())
                                                          .build(),
                                                      queryContext,
                                                      handle,
                                                      sql,
                                                      isMssql);

                                              Map<Long, Workspace> cacheWorkspaceMap =
                                                  new HashMap<>();
                                              return query
                                                  .map(
                                                      (rs, ctx) ->
                                                          buildProjectBuilderFromResultSet(
                                                              getResourcesMap,
                                                              cacheWorkspaceMap,
                                                              rs))
                                                  .list();
                                            })
                                        .thenCompose(
                                            builders -> {
                                              if (builders == null || builders.isEmpty()) {
                                                return InternalFuture.completedInternalFuture(
                                                    new LinkedList<Project>());
                                              }

                                              var futureBuildersStream =
                                                  InternalFuture.completedInternalFuture(
                                                      builders.stream());
                                              final var ids =
                                                  builders.stream()
                                                      .map(Project.Builder::getId)
                                                      .collect(Collectors.toSet());

                                              // Get tags
                                              final var futureTags = tagsHandler.getTagsMap(ids);
                                              futureBuildersStream =
                                                  futureBuildersStream.thenCombine(
                                                      futureTags,
                                                      (stream, tags) ->
                                                          stream.map(
                                                              builder ->
                                                                  builder.addAllTags(
                                                                      tags.get(builder.getId()))),
                                                      executor);

                                              // Get attributes
                                              final var futureAttributes =
                                                  attributeHandler.getKeyValuesMap(ids);
                                              futureBuildersStream =
                                                  futureBuildersStream.thenCombine(
                                                      futureAttributes,
                                                      (stream, attributes) ->
                                                          stream.map(
                                                              builder ->
                                                                  builder.addAllAttributes(
                                                                      attributes.get(
                                                                          builder.getId()))),
                                                      executor);

                                              // Get artifacts
                                              final var futureArtifacts =
                                                  artifactHandler.getArtifactsMap(ids);
                                              futureBuildersStream =
                                                  futureBuildersStream.thenCombine(
                                                      futureArtifacts,
                                                      (stream, artifacts) ->
                                                          stream.map(
                                                              builder ->
                                                                  builder.addAllArtifacts(
                                                                      artifacts.get(
                                                                          builder.getId()))),
                                                      executor);

                                              final var futureCodeVersions =
                                                  codeVersionHandler.getCodeVersionMap(
                                                      new ArrayList<>(ids));
                                              futureBuildersStream =
                                                  futureBuildersStream.thenCombine(
                                                      futureCodeVersions,
                                                      (stream, codeVersionMap) ->
                                                          stream.map(
                                                              builder -> {
                                                                if (codeVersionMap.containsKey(
                                                                    builder.getId())) {
                                                                  return builder
                                                                      .setCodeVersionSnapshot(
                                                                          codeVersionMap.get(
                                                                              builder.getId()));
                                                                } else {
                                                                  return builder;
                                                                }
                                                              }),
                                                      executor);

                                              return futureBuildersStream.thenApply(
                                                  projectBuilders ->
                                                      projectBuilders
                                                          .map(Project.Builder::build)
                                                          .collect(Collectors.toList()),
                                                  executor);
                                            },
                                            executor),
                                executor);

                    final var futureCount =
                        InternalFuture.sequence(
                                Arrays.asList(
                                    futureLocalContext,
                                    futurePredicatesContext,
                                    futureProjectIdsContext),
                                executor)
                            .thenApply(QueryFilterContext::combine, executor)
                            .thenCompose(this::getProjectCountBasedOnQueryFilter, executor);

                    return futureProjects
                        .thenApply(this::sortProjectFields, executor)
                        .thenCombine(
                            futureCount,
                            (projects, count) ->
                                FindProjects.Response.newBuilder()
                                    .addAllProjects(projects)
                                    .setTotalRecords(count)
                                    .build(),
                            executor);
                  },
                  executor);
            },
            executor);
  }

  private InternalFuture<Long> getProjectCountBasedOnQueryFilter(QueryFilterContext queryContext) {
    return jdbi.withHandle(
        handle -> {
          var sql = "select count(p.id) from project p ";

          if (!queryContext.getConditions().isEmpty()) {
            sql += " WHERE " + String.join(" AND ", queryContext.getConditions());
          }

          var query = handle.createQuery(sql);
          queryContext.getBinds().forEach(b -> b.accept(query));

          return query.mapTo(Long.class).one();
        });
  }

  private Project.Builder buildProjectBuilderFromResultSet(
      Map<String, GetResourcesResponseItem> getResourcesMap,
      Map<Long, Workspace> cacheWorkspaceMap,
      java.sql.ResultSet rs)
      throws SQLException {
    var projectBuilder =
        Project.newBuilder()
            .setId(rs.getString("id"))
            .setName(rs.getString("name"))
            .setDescription(rs.getString("description"))
            .setDateUpdated(rs.getLong("date_updated"))
            .setDateCreated(rs.getLong("date_created"))
            .setOwner(rs.getString("owner"))
            .setVersionNumber(rs.getLong("version_number"))
            .setShortName(rs.getString("short_name"))
            .setReadmeText(rs.getString("readme_text"));

    var projectResource = getResourcesMap.get(projectBuilder.getId());
    projectBuilder.setVisibility(projectResource.getVisibility());
    projectBuilder.setWorkspaceServiceId(projectResource.getWorkspaceId());
    projectBuilder.setOwner(String.valueOf(projectResource.getOwnerId()));
    projectBuilder.setCustomPermission(projectResource.getCustomPermission());

    Workspace workspace;
    if (cacheWorkspaceMap.containsKey(projectResource.getWorkspaceId())) {
      workspace = cacheWorkspaceMap.get(projectResource.getWorkspaceId());
    } else {
      workspace = uacApisUtil.getWorkspaceById(projectResource.getWorkspaceId()).get();
      cacheWorkspaceMap.put(workspace.getId(), workspace);
    }
    switch (workspace.getInternalIdCase()) {
      case ORG_ID:
        projectBuilder.setWorkspaceId(workspace.getOrgId());
        projectBuilder.setWorkspaceTypeValue(WorkspaceTypeEnum.WorkspaceType.ORGANIZATION_VALUE);
        break;
      case USER_ID:
        projectBuilder.setWorkspaceId(workspace.getUserId());
        projectBuilder.setWorkspaceTypeValue(WorkspaceTypeEnum.WorkspaceType.USER_VALUE);
        break;
      default:
        // Do nothing
        break;
    }

    ProjectVisibility visibility =
        (ProjectVisibility)
            ModelDBUtils.getOldVisibility(
                ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT,
                projectResource.getVisibility());
    projectBuilder.setProjectVisibility(visibility);

    return projectBuilder;
  }

  private InternalFuture<QueryFilterContext> getFutureLocalContext() {
    return InternalFuture.supplyAsync(
        () -> {
          final var localQueryContext = new QueryFilterContext();
          localQueryContext.getConditions().add("p.deleted = :deleted");
          localQueryContext.getBinds().add(q -> q.bind("deleted", false));

          localQueryContext.getConditions().add("p.created = :created");
          localQueryContext.getBinds().add(q -> q.bind("created", true));

          return localQueryContext;
        },
        executor);
  }

  private InternalFuture<QueryFilterContext> getFutureProjectIdsContext(
      FindProjects request, Set<String> accessibleResourceIdsWithCollaborator) {
    List<KeyValueQuery> predicates = new ArrayList<>(request.getPredicatesList());
    for (KeyValueQuery predicate : predicates) {
      // Validate if current user has access to the entity or not where predicate
      // key has an id
      RdbmsUtils.validatePredicates(
          ModelDBConstants.PROJECTS,
          new ArrayList<>(accessibleResourceIdsWithCollaborator),
          predicate,
          true);
    }

    return InternalFuture.supplyAsync(
        () -> {
          final var localQueryContext = new QueryFilterContext();
          localQueryContext.getConditions().add(" p.id IN (<projectIds>) ");
          localQueryContext
              .getBinds()
              .add(q -> q.bindList("projectIds", accessibleResourceIdsWithCollaborator));

          return localQueryContext;
        },
        executor);
  }

  private InternalFuture<Workspace> getWorkspaceByWorkspaceName(String workspaceName) {
    return FutureUtil.clientRequest(
        uac.getWorkspaceService()
            .getWorkspaceByName(GetWorkspaceByName.newBuilder().setName(workspaceName).build()),
        executor);
  }

  private List<Project> sortProjectFields(List<Project> projects) {
    List<Project> sortedProjects = new LinkedList<>();
    for (Project project : projects) {
      var projectBuilder = Project.newBuilder(project);
      projectBuilder
          .clearTags()
          .addAllTags(project.getTagsList().stream().sorted().collect(Collectors.toList()))
          .clearAttributes()
          .addAllAttributes(
              project.getAttributesList().stream()
                  .sorted(Comparator.comparing(KeyValue::getKey))
                  .collect(Collectors.toList()))
          .clearArtifacts()
          .addAllArtifacts(
              project.getArtifactsList().stream()
                  .sorted(Comparator.comparing(Artifact::getKey))
                  .collect(Collectors.toList()));
      sortedProjects.add(projectBuilder.build());
    }
    return sortedProjects;
  }

  public InternalFuture<List<GetResourcesResponseItem>> deleteProjects(List<String> projectIds) {
    // validate argument
    InternalFuture<Void> validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              // Request Parameter Validation
              if (projectIds.isEmpty() || projectIds.stream().allMatch(String::isEmpty)) {
                var errorMessage = "Project ID not found in request";
                throw new InvalidArgumentException(errorMessage);
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused -> {
              // Get self allowed resources id where user has delete permission
              return getSelfAllowedResources(
                  ModelDBActionEnum.ModelDBServiceActions.DELETE, projectIds);
            },
            executor)
        .thenApply(
            allowedProjectIds -> {
              if (allowedProjectIds.isEmpty()) {
                throw new PermissionDeniedException(
                    "Delete Access Denied for given project Ids : " + projectIds);
              }
              return allowedProjectIds;
            },
            executor)
        .thenCompose(
            allowedProjectIds ->
                uacApisUtil.getResourceItemsForWorkspace(
                    Optional.empty(),
                    Optional.of(allowedProjectIds),
                    Optional.empty(),
                    ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT),
            executor)
        .thenCompose(
            allowedProjectResources ->
                jdbi.useHandle(
                        handle -> {
                          var updatedCount =
                              handle
                                  .createUpdate(
                                      "update project set deleted = :deleted where id IN (<projectIds>)")
                                  .bind("deleted", true)
                                  .bindList(
                                      "projectIds",
                                      allowedProjectResources.stream()
                                          .map(GetResourcesResponseItem::getResourceId)
                                          .collect(Collectors.toList()))
                                  .execute();
                          LOGGER.debug(
                              "Mark Projects as deleted : {}, count : {}",
                              allowedProjectResources,
                              updatedCount);
                          allowedProjectResources.forEach(
                              allowedResource ->
                                  ReconcilerInitializer.softDeleteProjects.insert(
                                      allowedResource.getResourceId()));
                          LOGGER.debug("Project deleted successfully");
                        })
                    .thenApply(unused -> allowedProjectResources, executor),
            executor);
  }

  private InternalFuture<Collection<String>> getSelfAllowedResources(
      ModelDBServiceActions modelDBServiceActions, List<String> requestedResourcesIds) {
    return uacApisUtil
        .getAllowedEntitiesByResourceType(
            modelDBServiceActions, ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT)
        .thenApply(
            getAllowedResourcesResponse -> {
              LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
              LOGGER.trace(
                  CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedResourcesResponse);
              boolean allowedAllResources =
                  RoleServiceUtils.checkAllResourceAllowed(getAllowedResourcesResponse);
              if (allowedAllResources) {
                return new ArrayList<>(requestedResourcesIds);
              }
              Set<String> allowedProjectIds =
                  RoleServiceUtils.getResourceIds(getAllowedResourcesResponse);
              // Validate if current user has access to the entity or not
              // resourcesIds.retainAll(requestedResourcesIds);
              if (requestedResourcesIds.isEmpty()) {
                return allowedProjectIds;
              }
              if (allowedProjectIds.containsAll(requestedResourcesIds)) {
                return requestedResourcesIds;
              }
              for (var requestedId : requestedResourcesIds) {
                if (!allowedProjectIds.contains(requestedId)) {
                  allowedProjectIds.remove(requestedId);
                }
              }
              return allowedProjectIds;
            },
            executor);
  }

  public InternalFuture<Project> getProjectById(String projectId) {
    try {
      var validateArgumentFuture =
          InternalFuture.runAsync(
              () -> {
                if (projectId.isEmpty()) {
                  throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
                }
              },
              executor);
      return validateArgumentFuture
          .thenCompose(
              unused ->
                  findProjects(
                      FindProjects.newBuilder()
                          .addProjectIds(projectId)
                          .setPageLimit(1)
                          .setPageNumber(1)
                          .build()),
              executor)
          .thenApply(
              response -> {
                if (response.getProjectsList().isEmpty()) {
                  throw new NotFoundException("Project not found for given Id");
                } else if (response.getProjectsCount() > 1) {
                  throw new InternalErrorException("More then one projects found");
                }
                return response.getProjects(0);
              },
              executor);
    } catch (Exception e) {
      return InternalFuture.failedStage(e);
    }
  }

  public InternalFuture<Project> updateProjectDescription(UpdateProjectDescription request) {
    InternalFuture<Void> validateParametersFuture =
        InternalFuture.runAsync(
            () -> {
              // Request Parameter Validation
              if (request.getId().isEmpty()) {
                var errorMessage = "Project ID is not found in UpdateProjectDescription request";
                throw new InvalidArgumentException(errorMessage);
              }
            },
            executor);

    return validateParametersFuture
        .thenCompose(
            unused ->
                checkProjectPermission(
                    request.getId(), ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(unused -> getProjectById(request.getId()), executor)
        .thenCompose(
            project ->
                jdbi.withHandle(
                    handle -> {
                      var now = new Date().getTime();
                      handle
                          .createUpdate(
                              "update project set description = :description, date_updated = :dateUpdated, version_number=(version_number + 1) where id = :id")
                          .bind("id", project.getId())
                          .bind("description", request.getDescription())
                          .bind("dateUpdated", now)
                          .execute();
                      return project
                          .toBuilder()
                          .setDateUpdated(now)
                          .setDescription(request.getDescription())
                          .setVersionNumber(project.getVersionNumber() + 1L)
                          .build();
                    }),
            executor);
  }

  public InternalFuture<GetProjectByName.Response> getProjectByName(GetProjectByName request) {
    // Request Parameter Validation
    InternalFuture<Void> validateParamFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getName().isEmpty()) {
                throw new InvalidArgumentException(
                    "Project name is not found in GetProjectByName request");
              }
            },
            executor);

    return validateParamFuture
        .thenCompose(
            unused ->
                FutureUtil.clientRequest(
                    uac.getUACService().getCurrentUser(ai.verta.uac.Empty.newBuilder().build()),
                    executor),
            executor)
        .thenCompose(
            userInfo -> {
              // Get the user info from the Context
              return uacApisUtil
                  .getResourceItemsForWorkspace(
                      Optional.empty(),
                      Optional.empty(),
                      Optional.of(request.getName()),
                      ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT)
                  .thenCompose(
                      responseItem -> {
                        if (responseItem.size() == 0) {
                          throw new NotFoundException("Project not found");
                        }

                        String workspaceName =
                            request.getWorkspaceName().isEmpty()
                                ? userInfo.getVertaInfo().getUsername()
                                : request.getWorkspaceName();

                        FindProjects.Builder findProjects =
                            FindProjects.newBuilder()
                                .addAllProjectIds(
                                    responseItem.stream()
                                        .map(GetResourcesResponseItem::getResourceId)
                                        .collect(Collectors.toList()))
                                .setWorkspaceName(workspaceName);
                        return findProjects(findProjects.build())
                            .thenApply(
                                response -> {
                                  Project selfOwnerProject = null;
                                  List<Project> sharedProjects = new ArrayList<>();

                                  for (Project project : response.getProjectsList()) {
                                    if (userInfo == null
                                        || project
                                            .getOwner()
                                            .equals(userInfo.getVertaInfo().getUserId())) {
                                      selfOwnerProject = project;
                                    } else {
                                      sharedProjects.add(project);
                                    }
                                  }

                                  var responseBuilder = GetProjectByName.Response.newBuilder();
                                  if (selfOwnerProject != null) {
                                    responseBuilder.setProjectByUser(selfOwnerProject);
                                  }
                                  responseBuilder.addAllSharedProjects(sharedProjects);

                                  return responseBuilder.build();
                                },
                                executor);
                      },
                      executor);
            },
            executor);
  }

  public InternalFuture<Void> logArtifacts(LogProjectArtifacts request) {
    final var projectId = request.getId();
    final var artifacts = request.getArtifactsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Request Parameter Validation
    InternalFuture<Void> validateParamFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                throw new InvalidArgumentException("Project Id is not found in request");
              }
            },
            executor);

    return validateParamFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> artifactHandler.logArtifacts(handle, projectId, artifacts, false)),
            executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<List<Artifact>> getArtifacts(GetArtifacts request) {
    final var projectId = request.getId();
    final var key = request.getKey();
    Optional<String> maybeKey = key.isEmpty() ? Optional.empty() : Optional.of(key);

    InternalFuture<Void> validateParamFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                throw new InvalidArgumentException("Project ID not found in GetArtifacts request");
              }
            },
            executor);

    return validateParamFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ),
            executor)
        .thenCompose(unused -> artifactHandler.getArtifacts(projectId, maybeKey), executor);
  }

  public InternalFuture<Void> deleteArtifacts(DeleteProjectArtifact request) {
    final var projectId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();
    final var keys =
        request.getKey().isEmpty()
            ? new ArrayList<String>()
            : Collections.singletonList(request.getKey());
    Optional<List<String>> optionalKeys = keys.isEmpty() ? Optional.empty() : Optional.of(keys);

    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> artifactHandler.deleteArtifacts(projectId, optionalKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<GetSummary.Response> getSummary(GetSummary request) {
    // Request Parameter Validation
    InternalFuture<Void> validateParamFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getEntityId().isEmpty()) {
                var errorMessage = "Project ID not found in GetSummary request";
                throw new InvalidArgumentException(errorMessage);
              }
            },
            executor);

    return validateParamFuture
        .thenCompose(
            unused ->
                checkProjectPermission(
                    request.getEntityId(), ModelDBActionEnum.ModelDBServiceActions.READ),
            executor)
        .thenCompose(unused -> getProjectById(request.getEntityId()), executor)
        .thenCompose(
            project -> {
              final var responseBuilder =
                  GetSummary.Response.newBuilder()
                      .setName(project.getName())
                      .setLastUpdatedTime(project.getDateUpdated());

              var experimentCountFuture =
                  getExperimentCount(Collections.singletonList(project.getId()))
                      .thenApply(responseBuilder::setTotalExperiment, executor);
              return experimentCountFuture
                  .thenCompose(
                      builder ->
                          getExperimentRunCount(Collections.singletonList(project.getId()))
                              .thenApply(builder::setTotalExperimentRuns, executor),
                      executor)
                  .thenCompose(
                      builder ->
                          futureExperimentRunDAO
                              .findExperimentRuns(
                                  FindExperimentRuns.newBuilder()
                                      .setProjectId(request.getEntityId())
                                      .build())
                              .thenApply(
                                  response -> {
                                    var experimentRuns = response.getExperimentRunsList();
                                    LastModifiedExperimentRunSummary
                                        lastModifiedExperimentRunSummary = null;
                                    List<MetricsSummary> minMaxMetricsValueList = new ArrayList<>();
                                    if (!experimentRuns.isEmpty()) {
                                      ExperimentRun lastModifiedExperimentRun = null;
                                      Map<String, Double[]> minMaxMetricsValueMap =
                                          new HashMap<>(); // In double[], Index 0 =
                                      // minValue, Index 1 =
                                      // maxValue
                                      Set<String> keySet = new HashSet<>();

                                      for (ExperimentRun experimentRun : experimentRuns) {
                                        if (lastModifiedExperimentRun == null
                                            || lastModifiedExperimentRun.getDateUpdated()
                                                < experimentRun.getDateUpdated()) {
                                          lastModifiedExperimentRun = experimentRun;
                                        }

                                        for (KeyValue keyValue : experimentRun.getMetricsList()) {
                                          keySet.add(keyValue.getKey());
                                          minMaxMetricsValueMap.putAll(
                                              getMinMaxMetricsValueMap(
                                                  minMaxMetricsValueMap, keyValue));
                                        }
                                      }

                                      lastModifiedExperimentRunSummary =
                                          LastModifiedExperimentRunSummary.newBuilder()
                                              .setLastUpdatedTime(
                                                  lastModifiedExperimentRun.getDateUpdated())
                                              .setName(lastModifiedExperimentRun.getName())
                                              .build();

                                      for (String key : keySet) {
                                        Double[] minMaxValueArray =
                                            minMaxMetricsValueMap.get(
                                                key); // Index 0 = minValue, Index 1
                                        // = maxValue
                                        var minMaxMetricsSummary =
                                            MetricsSummary.newBuilder()
                                                .setKey(key)
                                                .setMinValue(minMaxValueArray[0]) // Index 0 =
                                                // minValue
                                                .setMaxValue(
                                                    minMaxValueArray[1]) // Index 1 = maxValue
                                                .build();
                                        minMaxMetricsValueList.add(minMaxMetricsSummary);
                                      }
                                    }

                                    builder.addAllMetrics(minMaxMetricsValueList);

                                    if (lastModifiedExperimentRunSummary != null) {
                                      builder.setLastModifiedExperimentRunSummary(
                                          lastModifiedExperimentRunSummary);
                                    }

                                    return builder.build();
                                  },
                                  executor),
                      executor);
            },
            executor);
  }

  private Map<String, Double[]> getMinMaxMetricsValueMap(
      Map<String, Double[]> minMaxMetricsValueMap, KeyValue keyValue) {
    Double value = keyValue.getValue().getNumberValue();
    Double[] minMaxValueArray = minMaxMetricsValueMap.get(keyValue.getKey());
    if (minMaxValueArray == null) {
      minMaxValueArray = new Double[2]; // Index 0 = minValue, Index 1 = maxValue
    }
    if (minMaxValueArray[0] == null || minMaxValueArray[0] > value) {
      minMaxValueArray[0] = value;
    }
    if (minMaxValueArray[1] == null || minMaxValueArray[1] < value) {
      minMaxValueArray[1] = value;
    }
    return Collections.singletonMap(keyValue.getKey(), minMaxValueArray);
  }

  private InternalFuture<Long> getExperimentCount(List<String> projectIds) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT COUNT(ee.id) FROM experiment ee WHERE ee.project_id IN (<project_ids>)")
                    .bindList(ModelDBConstants.PROJECT_IDS, projectIds)
                    .mapTo(Long.class)
                    .findOne())
        .thenApply(count -> count.orElse(0L), executor);
  }

  private InternalFuture<Long> getExperimentRunCount(List<String> projectIds) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT COUNT(er.id) FROM experiment_run er WHERE er.project_id IN (<project_ids>)")
                    .bindList(ModelDBConstants.PROJECT_IDS, projectIds)
                    .mapTo(Long.class)
                    .findOne())
        .thenApply(count -> count.orElse(0L), executor);
  }

  public InternalFuture<Void> setProjectReadme(SetProjectReadme request) {
    // Request Parameter Validation
    InternalFuture<Void> validateParamFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                throw new InvalidArgumentException(
                    "Project ID not found in SetProjectReadme request");
              }
            },
            executor);

    return validateParamFuture
        .thenCompose(
            unused ->
                checkProjectPermission(
                    request.getId(), ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle ->
                        handle
                            .createUpdate(
                                "update project set readme_text = :readmeText where id = :id")
                            .bind("id", request.getId())
                            .bind("readmeText", request.getReadmeText())
                            .execute()),
            executor)
        .thenCompose(
            unused -> updateModifiedTimestamp(request.getId(), new Date().getTime()), executor)
        .thenCompose(unused -> updateVersionNumber(request.getId()), executor);
  }

  public InternalFuture<GetProjectReadme.Response> getProjectReadme(GetProjectReadme request) {
    // Request Parameter Validation
    InternalFuture<Void> validateParamFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                var errorMessage = "Project ID not found in GetProjectReadme request";
                throw new InvalidArgumentException(errorMessage);
              }
            },
            executor);

    return validateParamFuture
        .thenCompose(
            unused ->
                checkProjectPermission(
                    request.getId(), ModelDBActionEnum.ModelDBServiceActions.READ),
            executor)
        .thenCompose(
            unused ->
                jdbi.withHandle(
                    handle ->
                        handle
                            .createQuery("select readme_text from project where id = :id")
                            .bind("id", request.getId())
                            .mapTo(String.class)
                            .findOne()),
            executor)
        .thenApply(
            readmeTextOptional -> {
              var response = GetProjectReadme.Response.newBuilder();
              readmeTextOptional.ifPresent(response::setReadmeText);
              return response.build();
            },
            executor);
  }

  public InternalFuture<Void> setProjectShortName(SetProjectShortName request) {
    // Request Parameter Validation
    InternalFuture<Void> validateParamFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                throw new InvalidArgumentException(
                    "Project ID not found in SetProjectShortName request");
              } else if (request.getShortName().isEmpty()) {
                throw new InvalidArgumentException(
                    "Project shortName not found in SetProjectShortName request");
              }
            },
            executor);

    return validateParamFuture
        .thenCompose(
            unused ->
                checkProjectPermission(
                    request.getId(), ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                getSelfAllowedResources(
                    ModelDBActionEnum.ModelDBServiceActions.READ, Collections.emptyList()),
            executor)
        .thenCompose(
            selfAllowedResources -> {
              InternalFuture<Optional<Long>> countFuture =
                  InternalFuture.completedInternalFuture(Optional.of(0L));
              if (!selfAllowedResources.isEmpty()) {
                countFuture =
                    jdbi.withHandle(
                        handle ->
                            handle
                                .createQuery(
                                    "select count(p.id) from project p where p.deleted = :deleted AND p.short_name = :projectShortName AND p.id IN (<projectIds>)")
                                .bind("projectShortName", request.getShortName())
                                .bindList("projectIds", selfAllowedResources)
                                .bind("deleted", false)
                                .mapTo(Long.class)
                                .findOne());
              }
              return countFuture.thenCompose(
                  count -> {
                    if (count.isPresent() && count.get() > 0) {
                      return InternalFuture.failedStage(
                          new AlreadyExistsException(
                              "Project already exist with given short name"));
                    }
                    return InternalFuture.completedInternalFuture(null);
                  },
                  executor);
            },
            executor)
        .thenCompose(
            unused -> {
              String projectShortName =
                  ModelDBUtils.convertToProjectShortName(request.getShortName());
              if (!projectShortName.equals(request.getShortName())) {
                throw new InternalErrorException("Project short name is not valid");
              }

              return jdbi.useHandle(
                  handle ->
                      handle
                          .createUpdate("update project set short_name = :shortName where id = :id")
                          .bind("id", request.getId())
                          .bind("shortName", projectShortName)
                          .execute());
            },
            executor)
        .thenCompose(
            unused -> updateModifiedTimestamp(request.getId(), new Date().getTime()), executor)
        .thenCompose(unused -> updateVersionNumber(request.getId()), executor);
  }

  public InternalFuture<GetProjectShortName.Response> getProjectShortName(
      GetProjectShortName request) {
    // Request Parameter Validation
    InternalFuture<Void> validateParamFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                var errorMessage = "Project ID not found in GetProjectShortName request";
                throw new InvalidArgumentException(errorMessage);
              }
            },
            executor);

    return validateParamFuture
        .thenCompose(
            unused ->
                checkProjectPermission(
                    request.getId(), ModelDBActionEnum.ModelDBServiceActions.READ),
            executor)
        .thenCompose(
            unused ->
                jdbi.withHandle(
                    handle ->
                        handle
                            .createQuery("select short_name from project where id = :id")
                            .bind("id", request.getId())
                            .mapTo(String.class)
                            .findOne()),
            executor)
        .thenApply(
            readmeTextOptional -> {
              var response = GetProjectShortName.Response.newBuilder();
              readmeTextOptional.ifPresent(response::setShortName);
              return response.build();
            },
            executor);
  }

  public InternalFuture<GetProjectCodeVersion.Response> getProjectCodeVersion(
      GetProjectCodeVersion request) {
    // Request Parameter Validation
    InternalFuture<Void> validateParamFuture =
        InternalFuture.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                var errorMessage = "Project ID not found in GetProjectCodeVersion request";
                throw new InvalidArgumentException(errorMessage);
              }
            },
            executor);

    return validateParamFuture
        .thenCompose(
            unused ->
                checkProjectPermission(
                    request.getId(), ModelDBActionEnum.ModelDBServiceActions.READ),
            executor)
        .thenCompose(unused -> getProjectById(request.getId()), executor)
        .thenApply(
            project ->
                GetProjectCodeVersion.Response.newBuilder()
                    .setCodeVersion(project.getCodeVersionSnapshot())
                    .build(),
            executor);
  }

  public InternalFuture<Void> logProjectCodeVersion(LogProjectCodeVersion request) {
    // Request Parameter Validation
    InternalFuture<Void> validateParamFuture =
        InternalFuture.runAsync(
            () -> {
              String errorMessage = null;
              if (request.getId().isEmpty() && request.getCodeVersion() == null) {
                throw new InvalidArgumentException(
                    "Project ID and Code version not found in LogProjectCodeVersion request");
              } else if (request.getId().isEmpty()) {
                throw new InvalidArgumentException(
                    "Project ID not found in LogProjectCodeVersion request");
              } else if (request.getCodeVersion() == null) {
                throw new InvalidArgumentException(
                    "CodeVersion not found in LogProjectCodeVersion request");
              }
            },
            executor);

    return validateParamFuture
        .thenCompose(
            unused ->
                checkProjectPermission(
                    request.getId(), ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle ->
                        codeVersionHandler.logCodeVersion(
                            handle, request.getId(), false, request.getCodeVersion())),
            executor)
        .thenCompose(
            unused -> updateModifiedTimestamp(request.getId(), new Date().getTime()), executor)
        .thenCompose(unused -> updateVersionNumber(request.getId()), executor);
  }

  public InternalFuture<Project> createProject(CreateProject request) {
    // Validate if current user has access to the entity or not
    return checkProjectPermission(null, ModelDBActionEnum.ModelDBServiceActions.CREATE)
        .thenCompose(unused -> createProjectHandler.convertCreateRequest(request), executor)
        .thenCompose(
            project ->
                deleteEntityResourcesWithServiceUser(project.getName())
                    .thenApply(status -> project, executor),
            executor)
        .thenCompose(createProjectHandler::insertProject, executor)
        .thenCompose(
            createdProject ->
                FutureUtil.clientRequest(
                        uac.getUACService().getCurrentUser(ai.verta.uac.Empty.newBuilder().build()),
                        executor)
                    .thenCompose(
                        loginUser -> {
                          String workspaceName;
                          if (!request.getWorkspaceName().isEmpty()) {
                            workspaceName = request.getWorkspaceName();
                          } else {
                            workspaceName = loginUser.getVertaInfo().getUsername();
                          }
                          return getWorkspaceByWorkspaceName(workspaceName)
                              .thenCompose(
                                  workspace -> {
                                    ResourceVisibility resourceVisibility =
                                        getResourceVisibility(createdProject, workspace);
                                    var projectBuilder = createdProject.toBuilder();
                                    return createResources(
                                            Optional.of(workspaceName),
                                            createdProject.getId(),
                                            createdProject.getName(),
                                            ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT,
                                            createdProject.getCustomPermission(),
                                            resourceVisibility)
                                        .thenCompose(
                                            unused2 -> updateProjectAsCreated(createdProject),
                                            executor)
                                        .thenCompose(
                                            unused ->
                                                populateProjectFieldFromResourceItem(
                                                    createdProject,
                                                    workspaceName,
                                                    workspace,
                                                    projectBuilder),
                                            executor);
                                  },
                                  executor);
                        },
                        executor),
            executor);
  }

  private InternalFuture<Project> populateProjectFieldFromResourceItem(
      Project createdProject,
      String workspaceName,
      Workspace workspace,
      Project.Builder projectBuilder) {
    return uacApisUtil
        .getResourceItemsForLoginUserWorkspace(
            workspaceName,
            Optional.of(Collections.singletonList(createdProject.getId())),
            ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT)
        .thenApply(
            getResourcesResponseItems -> {
              Optional<GetResourcesResponseItem> responseItem =
                  getResourcesResponseItems.stream().findFirst();
              if (!responseItem.isPresent()) {
                throw new NotFoundException(
                    String.format(
                        "Failed to locate Project resources in UAC for ID : %s",
                        createdProject.getId()));
              }
              GetResourcesResponseItem projectResource = responseItem.get();

              projectBuilder.setVisibility(projectResource.getVisibility());
              projectBuilder.setWorkspaceServiceId(projectResource.getWorkspaceId());
              projectBuilder.setOwner(String.valueOf(projectResource.getOwnerId()));
              projectBuilder.setCustomPermission(projectResource.getCustomPermission());

              switch (workspace.getInternalIdCase()) {
                case ORG_ID:
                  projectBuilder.setWorkspaceId(workspace.getOrgId());
                  projectBuilder.setWorkspaceTypeValue(
                      WorkspaceTypeEnum.WorkspaceType.ORGANIZATION_VALUE);
                  break;
                case USER_ID:
                  projectBuilder.setWorkspaceId(workspace.getUserId());
                  projectBuilder.setWorkspaceTypeValue(WorkspaceTypeEnum.WorkspaceType.USER_VALUE);
                  break;
                default:
                  // Do nothing
                  break;
              }

              ProjectVisibility visibility =
                  (ProjectVisibility)
                      ModelDBUtils.getOldVisibility(
                          ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT,
                          projectResource.getVisibility());
              projectBuilder.setProjectVisibility(visibility);

              return projectBuilder.build();
            },
            executor);
  }

  private InternalFuture<Void> updateProjectAsCreated(Project createdProject) {
    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE project SET created=:created WHERE id=:id")
                .bind("created", true)
                .bind("id", createdProject.getId())
                .execute());
  }

  private ResourceVisibility getResourceVisibility(Project createdProject, Workspace workspace) {
    var resourceVisibility = createdProject.getVisibility();
    if (createdProject.getVisibility().equals(ResourceVisibility.UNKNOWN)) {
      resourceVisibility =
          ModelDBUtils.getResourceVisibility(
              Optional.of(workspace), createdProject.getProjectVisibility());
    }
    return resourceVisibility;
  }

  private InternalFuture<Boolean> deleteEntityResourcesWithServiceUser(String projectName) {

    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT p.id From project p where p.name = :projectName AND p.deleted = :deleted")
                    .bind("projectName", projectName)
                    .bind("deleted", true)
                    .mapTo(String.class)
                    .list())
        .thenCompose(
            deletedProjectIds -> {
              if (deletedProjectIds == null || deletedProjectIds.isEmpty()) {
                return InternalFuture.completedInternalFuture(true);
              }
              var modeldbServiceResourceType =
                  ResourceType.newBuilder()
                      .setModeldbServiceResourceType(
                          ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT)
                      .build();
              var resources =
                  Resources.newBuilder()
                      .setResourceType(modeldbServiceResourceType)
                      .setService(ServiceEnum.Service.MODELDB_SERVICE)
                      .addAllResourceIds(deletedProjectIds)
                      .build();

              LOGGER.trace("Calling CollaboratorService to delete resources");
              var deleteResources = DeleteResources.newBuilder().setResources(resources).build();
              return FutureUtil.clientRequest(
                      uac.getServiceAccountCollaboratorServiceForServiceUser()
                          .deleteResources(deleteResources),
                      executor)
                  .thenCompose(
                      response -> {
                        LOGGER.trace("DeleteResources message sent.  Response: {}", response);
                        return InternalFuture.completedInternalFuture(true);
                      },
                      executor);
            },
            executor);
  }

  private InternalFuture<Boolean> createResources(
      Optional<String> workspaceName,
      String resourceId,
      String resourceName,
      ModelDBResourceEnum.ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility resourceVisibility) {
    LOGGER.trace("Calling CollaboratorService to create resources");
    var modeldbServiceResourceType =
        ResourceType.newBuilder().setModeldbServiceResourceType(resourceType).build();
    var setResourcesBuilder =
        SetResource.newBuilder()
            .setService(ServiceEnum.Service.MODELDB_SERVICE)
            .setResourceType(modeldbServiceResourceType)
            .setResourceId(resourceId)
            .setResourceName(resourceName)
            .setVisibility(resourceVisibility);

    if (resourceVisibility.equals(ResourceVisibility.ORG_CUSTOM)) {
      setResourcesBuilder.setCollaboratorType(permissions.getCollaboratorType());
      setResourcesBuilder.setCanDeploy(permissions.getCanDeploy());
    }

    if (workspaceName.isPresent()) {
      setResourcesBuilder = setResourcesBuilder.setWorkspaceName(workspaceName.get());
    } else {
      throw new IllegalArgumentException(
          "workspaceId and workspaceName are both empty.  One must be provided.");
    }

    return FutureUtil.clientRequest(
            uac.getCollaboratorService().setResource(setResourcesBuilder.build()), executor)
        .thenCompose(
            response -> {
              LOGGER.trace("SetResources message sent.  Response: {}", response);
              return InternalFuture.completedInternalFuture(true);
            },
            executor);
  }

  public InternalFuture<List<String>> getWorkspaceProjectIDs(String workspaceName) {
    if (uac == null) {
      return jdbi.withHandle(
          handle ->
              handle
                  .createQuery("select id from project where deleted = :deleted")
                  .bind("deleted", false)
                  .mapTo(String.class)
                  .list());
    } else {

      // get list of accessible projects
      return uacApisUtil
          .getAccessibleProjectIdsBasedOnWorkspace(workspaceName, Optional.empty())
          .thenCompose(
              accessibleProjectIds -> {
                LOGGER.debug("accessibleAllWorkspaceProjectIds : {}", accessibleProjectIds);
                return jdbi.withHandle(
                    handle ->
                        handle
                            .createQuery(
                                "select id from project where deleted = :deleted and id IN (<ids>)")
                            .bind("deleted", false)
                            .bindList("ids", accessibleProjectIds)
                            .mapTo(String.class)
                            .list());
              },
              executor);
    }
  }
}
