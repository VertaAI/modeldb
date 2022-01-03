package ai.verta.modeldb.project;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.AddProjectTags;
import ai.verta.modeldb.DeleteProjectAttributes;
import ai.verta.modeldb.DeleteProjectTags;
import ai.verta.modeldb.FindProjects;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.LogAttributes;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.UpdateProjectAttributes;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.EnumerateList;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.OrderColumn;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.DatasetHandler;
import ai.verta.modeldb.experimentRun.subtypes.PredicatesHandler;
import ai.verta.modeldb.experimentRun.subtypes.SortingHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.Action;
import ai.verta.uac.Empty;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetWorkspaceById;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ResourceType;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.Workspace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FutureProjectDAO {
  private static final Logger LOGGER = LogManager.getLogger(FutureProjectDAO.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;
  private final boolean isMssql;

  private final AttributeHandler attributeHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final PredicatesHandler predicatesHandler;
  private final SortingHandler sortingHandler;

  public FutureProjectDAO(
      Executor executor,
      FutureJdbi jdbi,
      UAC uac,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetVersionDAO datasetVersionDAO,
      MDBConfig mdbConfig) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;
    this.isMssql = mdbConfig.getDatabase().getRdbConfiguration().isMssql();

    var entityName = "ProjectEntity";
    attributeHandler = new AttributeHandler(executor, jdbi, entityName);
    tagsHandler = new TagsHandler(executor, jdbi, entityName);
    CodeVersionHandler codeVersionHandler = new CodeVersionHandler(executor, jdbi, "project");
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
    predicatesHandler = new PredicatesHandler("project", "p");
    sortingHandler = new SortingHandler("project");
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
        .thenCompose(unused -> attributeHandler.deleteKeyValues(projectId, maybeKeys), executor)
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
        .thenCompose(unused -> attributeHandler.logKeyValues(projectId, attributes), executor)
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
        .thenCompose(unused -> attributeHandler.updateKeyValue(projectId, attribute), executor)
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
        .thenCompose(unused -> tagsHandler.addTags(projectId, tags), executor)
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
        .thenCompose(unused -> tagsHandler.deleteTags(projectId, maybeTags), executor)
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
    return FutureGrpc.ClientRequest(
            uac.getAuthzService()
                .isSelfAllowed(
                    IsSelfAllowed.newBuilder()
                        .addActions(
                            Action.newBuilder()
                                .setModeldbServiceAction(action)
                                .setService(ServiceEnum.Service.MODELDB_SERVICE))
                        .addResources(
                            Resources.newBuilder()
                                .setService(ServiceEnum.Service.MODELDB_SERVICE)
                                .setResourceType(
                                    ResourceType.newBuilder()
                                        .setModeldbServiceResourceType(
                                            ModelDBResourceEnum.ModelDBServiceResourceTypes
                                                .PROJECT))
                                .addResourceIds(projId))
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

  public InternalFuture<FindProjects.Response> findProjects(FindProjects request) {
    return FutureGrpc.ClientRequest(
            uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor)
        .thenCompose(
            userInfo -> {
              InternalFuture<List<GetResourcesResponseItem>> resourcesFuture;
              if (request.getWorkspaceName().isEmpty()
                  || request.getWorkspaceName().equals(userInfo.getVertaInfo().getUsername())) {
                resourcesFuture =
                    getResourceItemsForLoginUserWorkspace(
                        request.getWorkspaceName(),
                        Optional.of(request.getProjectIdsList()),
                        ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);
              } else {
                resourcesFuture =
                    getResourceItemsForWorkspace(
                        request.getWorkspaceName(),
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

                    final var futureLocalContext =
                        InternalFuture.supplyAsync(
                            () -> {
                              final var localQueryContext = new QueryFilterContext();
                              localQueryContext.getConditions().add("p.deleted = :deleted");
                              localQueryContext.getBinds().add(q -> q.bind("deleted", false));

                              localQueryContext.getConditions().add("p.created = :created");
                              localQueryContext.getBinds().add(q -> q.bind("created", true));

                              return localQueryContext;
                            },
                            executor);

                    // futurePredicatesContext
                    final var futurePredicatesContext =
                        predicatesHandler.processPredicates(request.getPredicatesList(), executor);

                    // futureSortingContext
                    final var futureSortingContext =
                        sortingHandler.processSort(request.getSortKey(), request.getAscending());

                    final InternalFuture<QueryFilterContext> futureProjectIdsContext =
                        InternalFuture.supplyAsync(
                            () -> {
                              final var localQueryContext = new QueryFilterContext();
                              localQueryContext.getConditions().add(" p.id IN (<projectIds>) ");
                              localQueryContext
                                  .getBinds()
                                  .add(
                                      q ->
                                          q.bindList(
                                              "projectIds", accessibleResourceIdsWithCollaborator));

                              return localQueryContext;
                            },
                            executor);

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

                                              // Add the sorting tables
                                              for (final var item :
                                                  new EnumerateList<>(queryContext.getOrderItems())
                                                      .getList()) {
                                                if (item.getValue().getTable() != null) {
                                                  sql +=
                                                      String.format(
                                                          " left join (%s) as join_table_%d on p.id=join_table_%d.entityId ",
                                                          item.getValue().getTable(),
                                                          item.getIndex(),
                                                          item.getIndex());
                                                }
                                              }

                                              if (!queryContext.getConditions().isEmpty()) {
                                                sql +=
                                                    " WHERE "
                                                        + String.join(
                                                            " AND ", queryContext.getConditions());
                                              }

                                              if (!queryContext.getOrderItems().isEmpty()) {
                                                sql += " ORDER BY ";
                                                List<String> orderColumnQueryString =
                                                    new ArrayList<>();
                                                for (final var item :
                                                    new EnumerateList<>(
                                                            queryContext.getOrderItems())
                                                        .getList()) {
                                                  if (item.getValue().getTable() != null) {
                                                    for (OrderColumn orderColumn :
                                                        item.getValue().getColumns()) {
                                                      var orderColumnStr =
                                                          String.format(
                                                              " join_table_%d.%s ",
                                                              item.getIndex(),
                                                              orderColumn.getColumn());
                                                      orderColumnStr +=
                                                          String.format(
                                                              " %s ",
                                                              orderColumn.getAscending()
                                                                  ? "ASC"
                                                                  : "DESC");
                                                      orderColumnQueryString.add(orderColumnStr);
                                                    }
                                                  } else if (item.getValue().getColumn() != null) {
                                                    var orderColumnStr =
                                                        String.format(
                                                            " %s ", item.getValue().getColumn());
                                                    orderColumnStr +=
                                                        String.format(
                                                            " %s ",
                                                            item.getValue().getAscending()
                                                                ? "ASC"
                                                                : "DESC");
                                                    orderColumnQueryString.add(orderColumnStr);
                                                  }
                                                }
                                                sql += String.join(",", orderColumnQueryString);
                                              }

                                              // Backwards compatibility: fetch everything
                                              if (request.getPageNumber() != 0
                                                  && request.getPageLimit() != 0) {
                                                final var offset =
                                                    (request.getPageNumber() - 1)
                                                        * request.getPageLimit();
                                                final var limit = request.getPageLimit();
                                                if (isMssql) {
                                                  sql +=
                                                      " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY ";
                                                } else {
                                                  sql += " LIMIT :limit OFFSET :offset";
                                                }
                                                queryContext.addBind(q -> q.bind("limit", limit));
                                                queryContext.addBind(q -> q.bind("offset", offset));
                                              }

                                              var query = handle.createQuery(sql);
                                              queryContext.getBinds().forEach(b -> b.accept(query));

                                              Map<Long, Workspace> cacheWorkspaceMap =
                                                  new HashMap<>();
                                              return query
                                                  .map(
                                                      (rs, ctx) -> {
                                                        var projectBuilder =
                                                            Project.newBuilder()
                                                                .setId(rs.getString("p.id"))
                                                                .setName(rs.getString("p.name"))
                                                                .setDescription(
                                                                    rs.getString("p.description"))
                                                                .setDateUpdated(
                                                                    rs.getLong("p.date_updated"))
                                                                .setDateCreated(
                                                                    rs.getLong("p.date_created"))
                                                                .setOwner(rs.getString("p.owner"))
                                                                .setVersionNumber(
                                                                    rs.getLong("p.version_number"))
                                                                .setShortName(
                                                                    rs.getString("p.short_name"))
                                                                .setReadmeText(
                                                                    rs.getString("p.readme_text"));

                                                        var projectResource =
                                                            getResourcesMap.get(
                                                                projectBuilder.getId());
                                                        projectBuilder.setVisibility(
                                                            projectResource.getVisibility());
                                                        projectBuilder.setWorkspaceServiceId(
                                                            projectResource.getWorkspaceId());
                                                        projectBuilder.setOwner(
                                                            String.valueOf(
                                                                projectResource.getOwnerId()));
                                                        projectBuilder.setCustomPermission(
                                                            projectResource.getCustomPermission());

                                                        Workspace workspace;
                                                        if (cacheWorkspaceMap.containsKey(
                                                            projectResource.getWorkspaceId())) {
                                                          workspace =
                                                              cacheWorkspaceMap.get(
                                                                  projectResource.getWorkspaceId());
                                                        } else {
                                                          workspace =
                                                              getWorkspaceById(
                                                                      projectResource
                                                                          .getWorkspaceId())
                                                                  .get();
                                                          cacheWorkspaceMap.put(
                                                              workspace.getId(), workspace);
                                                        }
                                                        switch (workspace.getInternalIdCase()) {
                                                          case ORG_ID:
                                                            projectBuilder.setWorkspaceId(
                                                                workspace.getOrgId());
                                                            projectBuilder.setWorkspaceTypeValue(
                                                                WorkspaceTypeEnum.WorkspaceType
                                                                    .ORGANIZATION_VALUE);
                                                            break;
                                                          case USER_ID:
                                                            projectBuilder.setWorkspaceId(
                                                                workspace.getUserId());
                                                            projectBuilder.setWorkspaceTypeValue(
                                                                WorkspaceTypeEnum.WorkspaceType
                                                                    .USER_VALUE);
                                                            break;
                                                          default:
                                                            // Do nothing
                                                            break;
                                                        }

                                                        ProjectVisibility visibility =
                                                            (ProjectVisibility)
                                                                ModelDBUtils.getOldVisibility(
                                                                    ModelDBResourceEnum
                                                                        .ModelDBServiceResourceTypes
                                                                        .PROJECT,
                                                                    projectResource
                                                                        .getVisibility());
                                                        projectBuilder.setProjectVisibility(
                                                            visibility);

                                                        return projectBuilder;
                                                      })
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
                            .thenCompose(
                                queryContext ->
                                    jdbi.withHandle(
                                        handle -> {
                                          var sql = "select count(p.id) from project p ";

                                          if (!queryContext.getConditions().isEmpty()) {
                                            sql +=
                                                " WHERE "
                                                    + String.join(
                                                        " AND ", queryContext.getConditions());
                                          }

                                          var query = handle.createQuery(sql);
                                          queryContext.getBinds().forEach(b -> b.accept(query));

                                          return query.mapTo(Long.class).one();
                                        }),
                                executor);

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

  private InternalFuture<Workspace> getWorkspaceById(long workspaceId) {
    return FutureGrpc.ClientRequest(
        uac.getWorkspaceService()
            .getWorkspaceById(GetWorkspaceById.newBuilder().setId(workspaceId).build()),
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

  private InternalFuture<List<GetResourcesResponseItem>> getResourceItemsForLoginUserWorkspace(
      String workspaceName,
      Optional<List<String>> resourceIdsOptional,
      ModelDBResourceEnum.ModelDBServiceResourceTypes resourceTypes) {
    var resourceType =
        ResourceType.newBuilder().setModeldbServiceResourceType(resourceTypes).build();
    Resources.Builder resources =
        Resources.newBuilder()
            .setResourceType(resourceType)
            .setService(ServiceEnum.Service.MODELDB_SERVICE);

    if (!resourceIdsOptional.isEmpty() && resourceIdsOptional.isPresent()) {
      resources.addAllResourceIds(
          resourceIdsOptional.get().stream().map(String::valueOf).collect(Collectors.toSet()));
    }

    var builder = GetResources.newBuilder().setResources(resources.build());
    builder.setWorkspaceName(workspaceName);
    return FutureGrpc.ClientRequest(
            uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(builder.build()),
            executor)
        .thenApply(GetResources.Response::getItemList, executor);
  }

  private InternalFuture<List<GetResourcesResponseItem>> getResourceItemsForWorkspace(
      String workspaceName,
      Optional<List<String>> resourceIdsOptional,
      Optional<String> resourceName,
      ModelDBResourceEnum.ModelDBServiceResourceTypes resourceTypes) {
    var resourceType =
        ResourceType.newBuilder().setModeldbServiceResourceType(resourceTypes).build();
    Resources.Builder resources =
        Resources.newBuilder()
            .setResourceType(resourceType)
            .setService(ServiceEnum.Service.MODELDB_SERVICE);

    if (!resourceIdsOptional.isEmpty() && resourceIdsOptional.isPresent()) {
      resources.addAllResourceIds(
          resourceIdsOptional.get().stream().map(String::valueOf).collect(Collectors.toSet()));
    }

    var builder = GetResources.newBuilder().setResources(resources.build());
    builder.setWorkspaceName(workspaceName);
    resourceName.ifPresent(builder::setResourceName);
    return FutureGrpc.ClientRequest(
            uac.getCollaboratorService().getResources(builder.build()), executor)
        .thenApply(GetResources.Response::getItemList, executor);
  }
}
