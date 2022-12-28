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
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
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
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.statement.Query;

public class FutureProjectDAO {
  private static final Logger LOGGER = LogManager.getLogger(FutureProjectDAO.class);

  private final FutureJdbi jdbi;
  private final FutureExecutor executor;
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
  private final ReconcilerInitializer reconcilerInitializer;

  public FutureProjectDAO(
      FutureExecutor executor,
      FutureJdbi jdbi,
      UAC uac,
      ArtifactStoreDAO artifactStoreDAO,
      MDBConfig mdbConfig,
      FutureExperimentRunDAO futureExperimentRunDAO,
      UACApisUtil uacApisUtil,
      ReconcilerInitializer reconcilerInitializer) {
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
            mdbConfig);
    predicatesHandler = new PredicatesHandler("project", "p", uacApisUtil);
    sortingHandler = new SortingHandler("project");
    createProjectHandler =
        new CreateProjectHandler(
            executor, jdbi, mdbConfig, uac, attributeHandler, tagsHandler, artifactHandler);
    this.reconcilerInitializer = reconcilerInitializer;
  }

  public Future<Void> deleteAttributes(DeleteProjectAttributes request) {
    final var projectId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        Future.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              }
            });

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getAttributeKeysList());

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused ->
                jdbi.run(handle -> attributeHandler.deleteKeyValues(handle, projectId, maybeKeys)))
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now))
        .thenCompose(unused -> updateVersionNumber(projectId));
  }

  public Future<List<KeyValue>> getAttributes(GetAttributes request) {
    final var projectId = request.getId();
    final var keys = request.getAttributeKeysList();
    final var getAll = request.getGetAll();

    var validateArgumentFuture =
        Future.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (keys.isEmpty() && !getAll) {
                throw new InvalidArgumentException("Attribute keys not present");
              }
            });

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ))
        .thenCompose(unused -> attributeHandler.getKeyValues(projectId, keys, getAll).toFuture());
  }

  public Future<Void> logAttributes(LogAttributes request) {
    final var projectId = request.getId();
    final var attributes = request.getAttributesList();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        Future.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (attributes.isEmpty()) {
                throw new InvalidArgumentException("Attributes not present");
              }
            });

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused ->
                jdbi.run(handle -> attributeHandler.logKeyValues(handle, projectId, attributes)))
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now))
        .thenCompose(unused -> updateVersionNumber(projectId));
  }

  public Future<Void> updateProjectAttributes(UpdateProjectAttributes request) {
    final var projectId = request.getId();
    final var attribute = request.getAttribute();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        Future.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (attribute.getKey().isEmpty()) {
                throw new InvalidArgumentException("Attribute not present");
              }
            });

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused ->
                jdbi.run(handle -> attributeHandler.updateKeyValue(handle, projectId, attribute)))
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now))
        .thenCompose(unused -> updateVersionNumber(projectId));
  }

  public Future<Void> addTags(AddProjectTags request) {
    final var projectId = request.getId();
    final var tags = request.getTagsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        Future.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (tags.isEmpty()) {
                throw new InvalidArgumentException("Tags not present");
              }
            });

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE))
        .thenCompose(unused -> jdbi.run(handle -> tagsHandler.addTags(handle, projectId, tags)))
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now))
        .thenCompose(unused -> updateVersionNumber(projectId));
  }

  public Future<Void> deleteTags(DeleteProjectTags request) {
    final var projectId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        Future.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              }
            });

    final Optional<List<String>> maybeTags =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getTagsList());

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused -> jdbi.run(handle -> tagsHandler.deleteTags(handle, projectId, maybeTags)))
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now))
        .thenCompose(unused -> updateVersionNumber(projectId));
  }

  public Future<List<String>> getTags(GetTags request) {
    final var projectId = request.getId();
    var validateArgumentFuture =
        Future.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              }
            });

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ))
        .thenCompose(unused -> tagsHandler.getTags(projectId).toFuture());
  }

  private Future<Void> updateModifiedTimestamp(String projectId, Long now) {
    String greatestValueStr;
    if (isMssql) {
      greatestValueStr =
          "(SELECT MAX(value) FROM (VALUES (date_updated),(:now)) AS maxvalues(value))";
    } else {
      greatestValueStr = "greatest(date_updated, :now)";
    }

    return jdbi.run(
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

  private Future<Void> updateVersionNumber(String projectId) {
    return jdbi.run(
        handle ->
            handle
                .createUpdate(
                    "update project set version_number=(version_number + 1) where id=:project_id")
                .bind("project_id", projectId)
                .execute());
  }

  public Future<Void> checkProjectPermission(
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
    return Future.fromListenableFuture(
            uac.getAuthzService()
                .isSelfAllowed(
                    IsSelfAllowed.newBuilder()
                        .addActions(
                            Action.newBuilder()
                                .setModeldbServiceAction(action)
                                .setService(ServiceEnum.Service.MODELDB_SERVICE))
                        .addResources(resourceBuilder.build())
                        .build()))
        .thenAccept(
            response -> {
              if (!response.getAllowed()) {
                throw new PermissionDeniedException("Permission denied");
              }
            });
  }

  public Future<Long> getProjectDatasetCount(String projectId) {
    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(
            unused ->
                jdbi.call(
                    handle -> {
                      var queryStr =
                          "SELECT COUNT(distinct ar.linked_artifact_id) from artifact ar inner join experiment_run er ON er.id = ar.experiment_run_id "
                              + " WHERE er.project_id = :projectId AND ar.experiment_run_id is not null AND ar.linked_artifact_id <> '' ";
                      return handle
                          .createQuery(queryStr)
                          .bind("projectId", projectId)
                          .mapTo(Long.class)
                          .one();
                    }));
  }

  public Future<GetUrlForArtifact.Response> getUrlForArtifact(GetUrlForArtifact request) {
    final var projectId = request.getId();

    Future<Void> permissionCheck;
    if (request.getMethod().equalsIgnoreCase("get")) {
      permissionCheck =
          checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ);
    } else {
      permissionCheck =
          checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);
    }

    return permissionCheck.thenCompose(unused -> artifactHandler.getUrlForArtifact(request));
  }

  public Future<VerifyConnectionResponse> verifyConnection(Empty request) {
    VerifyConnectionResponse thing = VerifyConnectionResponse.newBuilder().setStatus(true).build();
    return Future.of(thing);
  }

  public Future<FindProjects.Response> findProjects(FindProjects request) {
    return Future.fromListenableFuture(
            uac.getUACService().getCurrentUser(ai.verta.uac.Empty.newBuilder().build()))
        .thenCompose(
            userInfo -> {
              Future<List<GetResourcesResponseItem>> resourcesFuture;
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
                      FindProjects.Response thing = FindProjects.Response.newBuilder().build();
                      return Future.of(thing);
                    }

                    final Future<QueryFilterContext> futureLocalContext = getFutureLocalContext();

                    // futurePredicatesContext
                    final var futurePredicatesContext =
                        predicatesHandler.processPredicates(request.getPredicatesList());

                    // futureSortingContext
                    final var futureSortingContext =
                        sortingHandler.processSort(request.getSortKey(), request.getAscending());

                    var futureProjectIdsContext =
                        getFutureProjectIdsContext(request, accessibleResourceIdsWithCollaborator);

                    final var futureProjects =
                        Future.sequence(
                                Arrays.asList(
                                    futureLocalContext,
                                    futurePredicatesContext,
                                    futureSortingContext,
                                    futureProjectIdsContext))
                            .thenCompose(a -> Future.of(QueryFilterContext.combine(a)))
                            .thenCompose(
                                queryContext -> {
                                  return jdbi.call(
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
                                                            getResourcesMap, cacheWorkspaceMap, rs))
                                                .list();
                                          })
                                      .thenCompose(
                                          builders -> {
                                            if (builders == null || builders.isEmpty()) {
                                              return Future.of(new LinkedList<Project>());
                                            }

                                            var futureBuildersStream = Future.of(builders.stream());
                                            final var ids =
                                                builders.stream()
                                                    .map(Project.Builder::getId)
                                                    .collect(Collectors.toSet());

                                            // Get tags
                                            final var futureTags =
                                                tagsHandler.getTagsMap(ids).toFuture();
                                            futureBuildersStream =
                                                futureBuildersStream.thenCombine(
                                                    futureTags,
                                                    (stream, tags) ->
                                                        stream.map(
                                                            builder ->
                                                                builder.addAllTags(
                                                                    tags.get(builder.getId()))));

                                            // Get attributes
                                            final var futureAttributes =
                                                attributeHandler.getKeyValuesMap(ids).toFuture();
                                            futureBuildersStream =
                                                futureBuildersStream.thenCombine(
                                                    futureAttributes,
                                                    (stream, attributes) ->
                                                        stream.map(
                                                            builder ->
                                                                builder.addAllAttributes(
                                                                    attributes.get(
                                                                        builder.getId()))));

                                            // Get artifacts
                                            final var futureArtifacts =
                                                artifactHandler.getArtifactsMap(ids).toFuture();
                                            futureBuildersStream =
                                                futureBuildersStream.thenCombine(
                                                    futureArtifacts,
                                                    (stream, artifacts) ->
                                                        stream.map(
                                                            builder ->
                                                                builder.addAllArtifacts(
                                                                    artifacts.get(
                                                                        builder.getId()))));

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
                                                            }));

                                            return futureBuildersStream.thenCompose(
                                                projectBuilders ->
                                                    Future.of(
                                                        projectBuilders
                                                            .map(Project.Builder::build)
                                                            .collect(Collectors.toList())));
                                          });
                                });

                    final var futureCount =
                        Future.sequence(
                                Arrays.asList(
                                    futureLocalContext,
                                    futurePredicatesContext,
                                    futureProjectIdsContext))
                            .thenCompose(a -> Future.of(QueryFilterContext.combine(a)))
                            .thenCompose(this::getProjectCountBasedOnQueryFilter);

                    return futureProjects
                        .thenCompose(a -> Future.of(this.sortProjectFields(a)))
                        .thenCombine(
                            futureCount,
                            (projects, count) ->
                                FindProjects.Response.newBuilder()
                                    .addAllProjects(projects)
                                    .setTotalRecords(count)
                                    .build());
                  });
            });
  }

  private Future<Long> getProjectCountBasedOnQueryFilter(QueryFilterContext queryContext) {
    return jdbi.call(
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
      try {
        workspace = uacApisUtil.getWorkspaceById(projectResource.getWorkspaceId()).get();
      } catch (Exception e) {
        throw new ModelDBException(e);
      }
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

  private Future<QueryFilterContext> getFutureLocalContext() {
    return Future.supplyAsync(
        () -> {
          final var localQueryContext = new QueryFilterContext();
          localQueryContext.getConditions().add("p.deleted = :deleted");
          localQueryContext.getBinds().add(q -> q.bind("deleted", false));

          localQueryContext.getConditions().add("p.created = :created");
          localQueryContext.getBinds().add(q -> q.bind("created", true));

          return localQueryContext;
        });
  }

  private Future<QueryFilterContext> getFutureProjectIdsContext(
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

    return Future.supplyAsync(
        () -> {
          final var localQueryContext = new QueryFilterContext();
          localQueryContext.getConditions().add(" p.id IN (<projectIds>) ");
          localQueryContext
              .getBinds()
              .add(q -> q.bindList("projectIds", accessibleResourceIdsWithCollaborator));

          return localQueryContext;
        });
  }

  private Future<Workspace> getWorkspaceByWorkspaceName(String workspaceName) {
    return Future.fromListenableFuture(
        uac.getWorkspaceService()
            .getWorkspaceByName(GetWorkspaceByName.newBuilder().setName(workspaceName).build()));
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

  public Future<List<GetResourcesResponseItem>> deleteProjects(List<String> projectIds) {
    // validate argument
    // Request Parameter Validation
    Future<Void> validateArgumentFuture =
        Future.runAsync(
            () -> {
              // Request Parameter Validation
              if (projectIds.isEmpty() || projectIds.stream().allMatch(String::isEmpty)) {
                var errorMessage = "Project ID not found in request";
                throw new InvalidArgumentException(errorMessage);
              }
            });

    Future<Collection<String>> collectionFuture =
        validateArgumentFuture.thenCompose(
            unused -> {
              // Get self allowed resources id where user has delete permission
              return getSelfAllowedResources(ModelDBServiceActions.DELETE, projectIds);
            });
    // Get self allowed resources id where user has delete permission
    return collectionFuture
        .<Collection<String>>thenCompose(
            t ->
                Future.of(
                    ((Function<? super Collection<String>, ? extends Collection<String>>)
                            allowedProjectIds1 -> {
                              if (allowedProjectIds1.isEmpty()) {
                                throw new PermissionDeniedException(
                                    "Delete Access Denied for given project Ids : " + projectIds);
                              }
                              return allowedProjectIds1;
                            })
                        .apply(t)))
        .thenCompose(
            allowedProjectIds ->
                uacApisUtil.getResourceItemsForWorkspace(
                    Optional.empty(),
                    Optional.of(allowedProjectIds),
                    Optional.empty(),
                    ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT))
        .thenCompose(
            allowedProjectResources -> {
              return jdbi.run(
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
                                reconcilerInitializer
                                    .getSoftDeleteProjects()
                                    .insert(allowedResource.getResourceId()));
                        LOGGER.debug("Project deleted successfully");
                      })
                  .thenCompose(unused -> Future.of(allowedProjectResources));
            });
  }

  private Future<Collection<String>> getSelfAllowedResources(
      ModelDBServiceActions modelDBServiceActions, List<String> requestedResourcesIds) {
    // Validate if current user has access to the entity or not
    // resourcesIds.retainAll(requestedResourcesIds);
    Future<List<Resources>> listFuture =
        uacApisUtil.getAllowedEntitiesByResourceType(
            modelDBServiceActions, ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);
    return listFuture.thenCompose(
        t ->
            Future.of(
                ((Function<? super List<Resources>, ? extends Collection<String>>)
                        getAllowedResourcesResponse -> {
                          LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
                          LOGGER.trace(
                              CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG,
                              getAllowedResourcesResponse);
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
                        })
                    .apply(t)));
  }

  public Future<Project> getProjectById(String projectId) {
    try {
      var validateArgumentFuture =
          Future.runAsync(
              () -> {
                if (projectId.isEmpty()) {
                  throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
                }
              });
      Future<FindProjects.Response> responseFuture =
          validateArgumentFuture.thenCompose(
              unused ->
                  findProjects(
                      FindProjects.newBuilder()
                          .addProjectIds(projectId)
                          .setPageLimit(1)
                          .setPageNumber(1)
                          .build()));
      return responseFuture.thenCompose(
          t ->
              Future.of(
                  ((Function<? super FindProjects.Response, ? extends Project>)
                          response -> {
                            if (response.getProjectsList().isEmpty()) {
                              throw new NotFoundException("Project not found for given Id");
                            } else if (response.getProjectsCount() > 1) {
                              throw new InternalErrorException("More then one projects found");
                            }
                            return response.getProjects(0);
                          })
                      .apply(t)));
    } catch (Exception e) {
      return Future.failedStage(e);
    }
  }

  public Future<Project> updateProjectDescription(UpdateProjectDescription request) {
    // Request Parameter Validation
    Future<Void> validateParametersFuture =
        Future.runAsync(
            () -> {
              // Request Parameter Validation
              if (request.getId().isEmpty()) {
                var errorMessage = "Project ID is not found in UpdateProjectDescription request";
                throw new InvalidArgumentException(errorMessage);
              }
            });

    return validateParametersFuture
        .thenCompose(
            unused ->
                checkProjectPermission(
                    request.getId(), ModelDBActionEnum.ModelDBServiceActions.UPDATE))
        .thenCompose(unused -> getProjectById(request.getId()))
        .thenCompose(
            project ->
                jdbi.call(
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
                    }));
  }

  public Future<GetProjectByName.Response> getProjectByName(GetProjectByName request) {
    // Request Parameter Validation
    Future<Void> validateParamFuture =
        Future.runAsync(
            () -> {
              if (request.getName().isEmpty()) {
                throw new InvalidArgumentException(
                    "Project name is not found in GetProjectByName request");
              }
            });

    // Get the user info from the Context
    Future<UserInfo> userInfoFuture =
        validateParamFuture.thenCompose(
            unused ->
                Future.fromListenableFuture(
                    uac.getUACService().getCurrentUser(ai.verta.uac.Empty.newBuilder().build())));
    return userInfoFuture.thenCompose(
        (Function<? super UserInfo, Future<GetProjectByName.Response>>)
            userInfo -> {
              // Get the user info from the Context
              Future<List<GetResourcesResponseItem>> listFuture =
                  uacApisUtil.getResourceItemsForWorkspace(
                      Optional.empty(),
                      Optional.empty(),
                      Optional.of(request.getName()),
                      ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);
              return listFuture.thenCompose(
                  (Function<
                          ? super List<GetResourcesResponseItem>,
                          Future<GetProjectByName.Response>>)
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
                        Future<FindProjects.Response> responseFuture =
                            findProjects(findProjects.build());
                        return responseFuture.thenCompose(
                            t ->
                                Future.of(
                                    ((Function<
                                                ? super FindProjects.Response,
                                                ? extends GetProjectByName.Response>)
                                            response -> {
                                              Project selfOwnerProject = null;
                                              List<Project> sharedProjects = new ArrayList<>();

                                              for (Project project : response.getProjectsList()) {
                                                if (userInfo == null
                                                    || project
                                                        .getOwner()
                                                        .equals(
                                                            userInfo.getVertaInfo().getUserId())) {
                                                  selfOwnerProject = project;
                                                } else {
                                                  sharedProjects.add(project);
                                                }
                                              }

                                              var responseBuilder =
                                                  GetProjectByName.Response.newBuilder();
                                              if (selfOwnerProject != null) {
                                                responseBuilder.setProjectByUser(selfOwnerProject);
                                              }
                                              responseBuilder.addAllSharedProjects(sharedProjects);

                                              return responseBuilder.build();
                                            })
                                        .apply(t)));
                      });
            });
  }

  public Future<Void> logArtifacts(LogProjectArtifacts request) {
    final var projectId = request.getId();
    final var artifacts = request.getArtifactsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Request Parameter Validation
    Future<Void> validateParamFuture =
        Future.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                throw new InvalidArgumentException("Project Id is not found in request");
              }
            });

    Future<Void> voidFuture =
        validateParamFuture.thenCompose(
            (Function<? super Void, Future<Void>>)
                unused2 -> checkProjectPermission(projectId, ModelDBServiceActions.UPDATE));
    return voidFuture
        .thenCompose(
            (Function<? super Void, Future<Void>>)
                unused1 ->
                    jdbi.run(
                        handle ->
                            artifactHandler.logArtifacts(handle, projectId, artifacts, false)))
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now))
        .thenCompose(unused -> updateVersionNumber(projectId));
  }

  public Future<List<Artifact>> getArtifacts(GetArtifacts request) {
    final var projectId = request.getId();
    final var key = request.getKey();
    Optional<String> maybeKey = key.isEmpty() ? Optional.empty() : Optional.of(key);

    Future<Void> validateParamFuture =
        Future.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                throw new InvalidArgumentException("Project ID not found in GetArtifacts request");
              }
            });

    return validateParamFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ))
        .thenCompose(unused -> artifactHandler.getArtifacts(projectId, maybeKey).toFuture());
  }

  public Future<Void> deleteArtifacts(DeleteProjectArtifact request) {
    final var projectId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();
    final var keys =
        request.getKey().isEmpty()
            ? new ArrayList<String>()
            : Collections.singletonList(request.getKey());
    Optional<List<String>> optionalKeys = keys.isEmpty() ? Optional.empty() : Optional.of(keys);

    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> artifactHandler.deleteArtifacts(projectId, optionalKeys).toFuture())
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now))
        .thenCompose(unused -> updateVersionNumber(projectId));
  }

  public Future<GetSummary.Response> getSummary(GetSummary request) {
    // Request Parameter Validation
    Future<Void> validateParamFuture =
        Future.runAsync(
            () -> {
              if (request.getEntityId().isEmpty()) {
                var errorMessage = "Project ID not found in GetSummary request";
                throw new InvalidArgumentException(errorMessage);
              }
            });

    // In double[], Index 0 =
    // minValue, Index 1 =
    // maxValue
    // Index 0 = minValue, Index 1
    // = maxValue
    // Index 0 =
    // minValue
    // Index 1 = maxValue
    // In double[], Index 0 =
    // minValue, Index 1 =
    // maxValue
    // Index 0 = minValue, Index 1
    // = maxValue
    // Index 0 =
    // minValue
    // Index 1 = maxValue
    // In double[], Index 0 =
    // minValue, Index 1 =
    // maxValue
    // Index 0 = minValue, Index 1
    // = maxValue
    // Index 0 =
    // minValue
    // Index 1 = maxValue
    // In double[], Index 0 =
    // minValue, Index 1 =
    // maxValue
    // Index 0 = minValue, Index 1
    // = maxValue
    // Index 0 =
    // minValue
    // Index 1 = maxValue
    Future<Project> projectFuture =
        validateParamFuture
            .thenCompose(
                (Function<? super Void, Future<Void>>)
                    unused1 ->
                        checkProjectPermission(request.getEntityId(), ModelDBServiceActions.READ))
            .thenCompose(unused -> getProjectById(request.getEntityId()));
    return projectFuture.thenCompose(
        (Function<? super Project, Future<GetSummary.Response>>)
            project -> {
              final var responseBuilder =
                  GetSummary.Response.newBuilder()
                      .setName(project.getName())
                      .setLastUpdatedTime(project.getDateUpdated());

              var experimentCountFuture =
                  getExperimentCount(Collections.singletonList(project.getId()))
                      .thenCompose(a -> Future.of(responseBuilder.setTotalExperiment(a)));
              // In double[], Index 0 =
              // minValue, Index 1 =
              // maxValue
              // Index 0 = minValue, Index 1
              // = maxValue
              // Index 0 =
              // minValue
              // Index 1 = maxValue
              // In double[], Index 0 =
              // minValue, Index 1 =
              // maxValue
              // Index 0 = minValue, Index 1
              // = maxValue
              // Index 0 =
              // minValue
              // Index 1 = maxValue
              Future<GetSummary.Response.Builder> builderFuture =
                  experimentCountFuture.thenCompose(
                      (Function<
                              ? super GetSummary.Response.Builder,
                              Future<GetSummary.Response.Builder>>)
                          builder1 -> {
                            return getExperimentRunCount(Collections.singletonList(project.getId()))
                                .thenCompose(a -> Future.of(builder1.setTotalExperimentRuns(a)));
                          });
              return builderFuture.thenCompose(
                  (Function<? super GetSummary.Response.Builder, Future<GetSummary.Response>>)
                      builder -> { // In double[], Index 0 =
                        // minValue, Index 1 =
                        // maxValue
                        // Index 0 = minValue, Index 1
                        // = maxValue
                        // Index 0 =
                        // minValue
                        // Index 1 = maxValue
                        Future<FindExperimentRuns.Response> responseFuture =
                            futureExperimentRunDAO.findExperimentRuns(
                                FindExperimentRuns.newBuilder()
                                    .setProjectId(request.getEntityId())
                                    .build());
                        return responseFuture.thenCompose(
                            t ->
                                Future.of(
                                    ((Function<
                                                ? super FindExperimentRuns.Response,
                                                ? extends GetSummary.Response>)
                                            response -> {
                                              var experimentRuns = response.getExperimentRunsList();
                                              LastModifiedExperimentRunSummary
                                                  lastModifiedExperimentRunSummary = null;
                                              List<MetricsSummary> minMaxMetricsValueList =
                                                  new ArrayList<>();
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

                                                  for (KeyValue keyValue :
                                                      experimentRun.getMetricsList()) {
                                                    keySet.add(keyValue.getKey());
                                                    minMaxMetricsValueMap.putAll(
                                                        getMinMaxMetricsValueMap(
                                                            minMaxMetricsValueMap, keyValue));
                                                  }
                                                }

                                                lastModifiedExperimentRunSummary =
                                                    LastModifiedExperimentRunSummary.newBuilder()
                                                        .setLastUpdatedTime(
                                                            lastModifiedExperimentRun
                                                                .getDateUpdated())
                                                        .setName(
                                                            lastModifiedExperimentRun.getName())
                                                        .build();

                                                for (String key : keySet) {
                                                  Double[] minMaxValueArray =
                                                      minMaxMetricsValueMap.get(
                                                          key); // Index 0 = minValue, Index 1
                                                  // = maxValue
                                                  var minMaxMetricsSummary =
                                                      MetricsSummary.newBuilder()
                                                          .setKey(key)
                                                          .setMinValue(
                                                              minMaxValueArray[0]) // Index 0 =
                                                          // minValue
                                                          .setMaxValue(
                                                              minMaxValueArray[
                                                                  1]) // Index 1 = maxValue
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
                                            })
                                        .apply(t)));
                      });
            });
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

  private Future<Long> getExperimentCount(List<String> projectIds) {
    return jdbi.call(
            handle ->
                handle
                    .createQuery(
                        "SELECT COUNT(ee.id) FROM experiment ee WHERE ee.project_id IN (<project_ids>)")
                    .bindList(ModelDBConstants.PROJECT_IDS, projectIds)
                    .mapTo(Long.class)
                    .findOne())
        .thenCompose(count -> Future.of(count.orElse(0L)));
  }

  private Future<Long> getExperimentRunCount(List<String> projectIds) {
    return jdbi.call(
            handle ->
                handle
                    .createQuery(
                        "SELECT COUNT(er.id) FROM experiment_run er WHERE er.project_id IN (<project_ids>)")
                    .bindList(ModelDBConstants.PROJECT_IDS, projectIds)
                    .mapTo(Long.class)
                    .findOne())
        .thenCompose(count -> Future.of(count.orElse(0L)));
  }

  public Future<Void> setProjectReadme(SetProjectReadme request) {
    // Request Parameter Validation
    Future<Void> validateParamFuture =
        Future.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                throw new InvalidArgumentException(
                    "Project ID not found in SetProjectReadme request");
              }
            });

    Future<Void> voidFuture =
        validateParamFuture.thenCompose(
            (Function<? super Void, Future<Void>>)
                unused1 -> checkProjectPermission(request.getId(), ModelDBServiceActions.UPDATE));
    return voidFuture
        .thenCompose(
            (Function<? super Void, Future<Void>>)
                unused1 ->
                    jdbi.run(
                        handle ->
                            handle
                                .createUpdate(
                                    "update project set readme_text = :readmeText where id = :id")
                                .bind("id", request.getId())
                                .bind("readmeText", request.getReadmeText())
                                .execute()))
        .thenCompose(unused -> updateModifiedTimestamp(request.getId(), new Date().getTime()))
        .thenCompose(unused -> updateVersionNumber(request.getId()));
  }

  public Future<GetProjectReadme.Response> getProjectReadme(GetProjectReadme request) {
    // Request Parameter Validation
    Future<Void> validateParamFuture =
        Future.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                var errorMessage = "Project ID not found in GetProjectReadme request";
                throw new InvalidArgumentException(errorMessage);
              }
            });

    Future<Void> voidFuture =
        validateParamFuture.thenCompose(
            (Function<? super Void, Future<Void>>)
                unused1 -> checkProjectPermission(request.getId(), ModelDBServiceActions.READ));
    Future<Optional<String>> optionalFuture =
        voidFuture.thenCompose(
            (Function<? super Void, Future<Optional<String>>>)
                unused ->
                    jdbi.call(
                        handle ->
                            handle
                                .createQuery("select readme_text from project where id = :id")
                                .bind("id", request.getId())
                                .mapTo(String.class)
                                .findOne()));
    return optionalFuture.thenCompose(
        t ->
            Future.of(
                ((Function<? super Optional<String>, ? extends GetProjectReadme.Response>)
                        readmeTextOptional -> {
                          var response = GetProjectReadme.Response.newBuilder();
                          readmeTextOptional.ifPresent(response::setReadmeText);
                          return response.build();
                        })
                    .apply(t)));
  }

  public Future<Void> setProjectShortName(SetProjectShortName request) {
    // Request Parameter Validation
    Future<Void> validateParamFuture =
        Future.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                throw new InvalidArgumentException(
                    "Project ID not found in SetProjectShortName request");
              } else if (request.getShortName().isEmpty()) {
                throw new InvalidArgumentException(
                    "Project shortName not found in SetProjectShortName request");
              }
            });

    Future<Void> voidFuture =
        validateParamFuture.thenCompose(
            (Function<? super Void, Future<Void>>)
                unused2 -> checkProjectPermission(request.getId(), ModelDBServiceActions.UPDATE));
    Future<Collection<String>> collectionFuture =
        voidFuture.thenCompose(
            (Function<? super Void, Future<Collection<String>>>)
                unused1 ->
                    getSelfAllowedResources(ModelDBServiceActions.READ, Collections.emptyList()));
    Future<Object> objectFuture =
        collectionFuture.thenCompose(
            (Function<? super Collection<String>, Future<Object>>)
                selfAllowedResources -> {
                  Future<Optional<Long>> countFuture = Future.of(Optional.of(0L));
                  if (!selfAllowedResources.isEmpty()) {
                    countFuture =
                        jdbi.call(
                            handle1 ->
                                handle1
                                    .createQuery(
                                        "select count(p.id) from project p where p.deleted = :deleted AND p.short_name = :projectShortName AND p.id IN (<projectIds>)")
                                    .bind("projectShortName", request.getShortName())
                                    .bindList("projectIds", selfAllowedResources)
                                    .bind("deleted", false)
                                    .mapTo(Long.class)
                                    .findOne());
                  }
                  return countFuture.thenCompose(
                      (Function<? super Optional<Long>, Future<Object>>)
                          count -> {
                            if (count.isPresent() && count.get() > 0) {
                              return Future.failedStage(
                                  new AlreadyExistsException(
                                      "Project already exist with given short name"));
                            }
                            return Future.of(null);
                          });
                });
    return objectFuture
        .thenCompose(
            unused1 -> {
              String projectShortName =
                  ModelDBUtils.convertToProjectShortName(request.getShortName());
              if (!projectShortName.equals(request.getShortName())) {
                throw new InternalErrorException("Project short name is not valid");
              }

              return jdbi.run(
                  handle ->
                      handle
                          .createUpdate("update project set short_name = :shortName where id = :id")
                          .bind("id", request.getId())
                          .bind("shortName", projectShortName)
                          .execute());
            })
        .thenCompose(unused -> updateModifiedTimestamp(request.getId(), new Date().getTime()))
        .thenCompose(unused -> updateVersionNumber(request.getId()));
  }

  public Future<GetProjectShortName.Response> getProjectShortName(GetProjectShortName request) {
    // Request Parameter Validation
    Future<Void> validateParamFuture =
        Future.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                var errorMessage = "Project ID not found in GetProjectShortName request";
                throw new InvalidArgumentException(errorMessage);
              }
            });

    Future<Void> voidFuture =
        validateParamFuture.thenCompose(
            (Function<? super Void, Future<Void>>)
                unused1 -> checkProjectPermission(request.getId(), ModelDBServiceActions.READ));
    Future<Optional<String>> optionalFuture =
        voidFuture.thenCompose(
            (Function<? super Void, Future<Optional<String>>>)
                unused ->
                    jdbi.call(
                        handle ->
                            handle
                                .createQuery("select short_name from project where id = :id")
                                .bind("id", request.getId())
                                .mapTo(String.class)
                                .findOne()));
    return optionalFuture.thenCompose(
        t ->
            Future.of(
                ((Function<? super Optional<String>, ? extends GetProjectShortName.Response>)
                        readmeTextOptional -> {
                          var response = GetProjectShortName.Response.newBuilder();
                          readmeTextOptional.ifPresent(response::setShortName);
                          return response.build();
                        })
                    .apply(t)));
  }

  public Future<GetProjectCodeVersion.Response> getProjectCodeVersion(
      GetProjectCodeVersion request) {
    // Request Parameter Validation
    Future<Void> validateParamFuture =
        Future.runAsync(
            () -> {
              if (request.getId().isEmpty()) {
                var errorMessage = "Project ID not found in GetProjectCodeVersion request";
                throw new InvalidArgumentException(errorMessage);
              }
            });

    Future<Project> projectFuture =
        validateParamFuture
            .thenCompose(
                (Function<? super Void, Future<Void>>)
                    unused1 -> checkProjectPermission(request.getId(), ModelDBServiceActions.READ))
            .thenCompose(unused -> getProjectById(request.getId()));
    return projectFuture.thenCompose(
        t ->
            Future.of(
                ((Function<? super Project, ? extends GetProjectCodeVersion.Response>)
                        project ->
                            GetProjectCodeVersion.Response.newBuilder()
                                .setCodeVersion(project.getCodeVersionSnapshot())
                                .build())
                    .apply(t)));
  }

  public Future<Void> logProjectCodeVersion(LogProjectCodeVersion request) {
    // Request Parameter Validation
    Future<Void> validateParamFuture =
        Future.runAsync(
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
            });

    Future<Void> voidFuture =
        validateParamFuture.thenCompose(
            (Function<? super Void, Future<Void>>)
                unused2 -> checkProjectPermission(request.getId(), ModelDBServiceActions.UPDATE));
    return voidFuture
        .thenCompose(
            (Function<? super Void, Future<Void>>)
                unused1 ->
                    jdbi.run(
                        handle ->
                            codeVersionHandler.logCodeVersion(
                                handle, request.getId(), false, request.getCodeVersion())))
        .thenCompose(unused -> updateModifiedTimestamp(request.getId(), new Date().getTime()))
        .thenCompose(unused -> updateVersionNumber(request.getId()));
  }

  public Future<Project> createProject(CreateProject request) {
    // Validate if current user has access to the entity or not
    Future<Project> projectFuture1 =
        checkProjectPermission(null, ModelDBServiceActions.CREATE)
            .thenCompose(unused -> createProjectHandler.convertCreateRequest(request));
    Future<Project> projectFuture =
        projectFuture1
            .thenCompose(
                (Function<? super Project, Future<Project>>)
                    project -> {
                      return deleteEntityResourcesWithServiceUser(project.getName())
                          .thenCompose(status -> Future.of(project));
                    })
            .thenCompose(createProjectHandler::insertProject);
    return projectFuture.thenCompose(
        (Function<? super Project, Future<Project>>)
            createdProject -> {
              Future<ai.verta.uac.UserInfo> userInfoFuture =
                  Future.fromListenableFuture(
                      uac.getUACService().getCurrentUser(ai.verta.uac.Empty.newBuilder().build()));
              return userInfoFuture.thenCompose(
                  (Function<? super UserInfo, Future<Project>>)
                      loginUser -> {
                        String workspaceName;
                        if (!request.getWorkspaceName().isEmpty()) {
                          workspaceName = request.getWorkspaceName();
                        } else {
                          workspaceName = loginUser.getVertaInfo().getUsername();
                        }
                        Future<Workspace> workspaceFuture =
                            getWorkspaceByWorkspaceName(workspaceName);
                        return workspaceFuture.thenCompose(
                            (Function<? super Workspace, Future<Project>>)
                                workspace -> {
                                  ResourceVisibility resourceVisibility =
                                      getResourceVisibility(createdProject, workspace);
                                  var projectBuilder = createdProject.toBuilder();
                                  Future<Boolean> booleanFuture =
                                      createResources(
                                          Optional.of(workspaceName),
                                          createdProject.getId(),
                                          createdProject.getName(),
                                          ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT,
                                          createdProject.getCustomPermission(),
                                          resourceVisibility);
                                  Future<Void> voidFuture =
                                      booleanFuture.thenCompose(
                                          (Function<? super Boolean, Future<Void>>)
                                              unused2 -> updateProjectAsCreated(createdProject));
                                  return voidFuture.thenCompose(
                                      (Function<? super Void, Future<Project>>)
                                          unused ->
                                              populateProjectFieldFromResourceItem(
                                                  createdProject,
                                                  workspaceName,
                                                  workspace,
                                                  projectBuilder));
                                });
                      });
            });
  }

  private Future<Project> populateProjectFieldFromResourceItem(
      Project createdProject,
      String workspaceName,
      Workspace workspace,
      Project.Builder projectBuilder) {
    // Do nothing
    Future<List<GetResourcesResponseItem>> listFuture =
        uacApisUtil.getResourceItemsForLoginUserWorkspace(
            workspaceName,
            Optional.of(Collections.singletonList(createdProject.getId())),
            ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);
    return listFuture.thenCompose(
        t ->
            Future.of(
                ((Function<? super List<GetResourcesResponseItem>, ? extends Project>)
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
                              projectBuilder.setWorkspaceTypeValue(
                                  WorkspaceTypeEnum.WorkspaceType.USER_VALUE);
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
                        })
                    .apply(t)));
  }

  private Future<Void> updateProjectAsCreated(Project createdProject) {
    return jdbi.run(
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

  private Future<Boolean> deleteEntityResourcesWithServiceUser(String projectName) {

    Future<List<String>> listFuture =
        jdbi.call(
            handle ->
                handle
                    .createQuery(
                        "SELECT p.id From project p where p.name = :projectName AND p.deleted = :deleted")
                    .bind("projectName", projectName)
                    .bind("deleted", true)
                    .mapTo(String.class)
                    .list());
    return listFuture.thenCompose(
        (Function<? super List<String>, Future<Boolean>>)
            deletedProjectIds -> {
              if (deletedProjectIds == null || deletedProjectIds.isEmpty()) {
                return Future.of(true);
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
              Future<DeleteResources.Response> responseFuture =
                  Future.fromListenableFuture(
                      uac.getServiceAccountCollaboratorServiceForServiceUser()
                          .deleteResources(deleteResources));
              return responseFuture.thenCompose(
                  (Function<? super DeleteResources.Response, Future<Boolean>>)
                      response -> {
                        LOGGER.trace("DeleteResources message sent.  Response: {}", response);
                        return Future.of(true);
                      });
            });
  }

  private Future<Boolean> createResources(
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

    Future<SetResource.Response> responseFuture =
        Future.fromListenableFuture(
            uac.getCollaboratorService().setResource(setResourcesBuilder.build()));
    return responseFuture.thenCompose(
        (Function<? super SetResource.Response, Future<Boolean>>)
            response -> {
              LOGGER.trace("SetResources message sent.  Response: {}", response);
              return Future.of(true);
            });
  }

  public Future<List<String>> getWorkspaceProjectIDs(String workspaceName) {
    if (uac == null) {
      return jdbi.call(
          handle ->
              handle
                  .createQuery("select id from project where deleted = :deleted")
                  .bind("deleted", false)
                  .mapTo(String.class)
                  .list());
    } else {

      // get list of accessible projects
      Future<List<String>> listFuture =
          uacApisUtil.getAccessibleProjectIdsBasedOnWorkspace(workspaceName, Optional.empty());
      return listFuture.thenCompose(
          (Function<? super List<String>, Future<List<String>>>)
              accessibleProjectIds -> {
                LOGGER.debug("accessibleAllWorkspaceProjectIds : {}", accessibleProjectIds);
                return jdbi.call(
                    handle ->
                        handle
                            .createQuery(
                                "select id from project where deleted = :deleted and id IN (<ids>)")
                            .bind("deleted", false)
                            .bindList("ids", accessibleProjectIds)
                            .mapTo(String.class)
                            .list());
              });
    }
  }
}
