package ai.verta.modeldb.experiment;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.Pagination;
import ai.verta.modeldb.CreateExperiment;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.DeleteExperimentArtifact;
import ai.verta.modeldb.DeleteExperimentAttributes;
import ai.verta.modeldb.DeleteExperiments;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.Experiment.Builder;
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetExperimentCodeVersion;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.LogExperimentArtifacts;
import ai.verta.modeldb.LogExperimentCodeVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.UpdateExperimentDescription;
import ai.verta.modeldb.UpdateExperimentName;
import ai.verta.modeldb.UpdateExperimentNameOrDescription;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.FutureUtil;
import ai.verta.modeldb.common.futures.Handle;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.handlers.TagsHandlerBase;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.experiment.subtypes.CreateExperimentHandler;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.DatasetHandler;
import ai.verta.modeldb.experimentRun.subtypes.PredicatesHandler;
import ai.verta.modeldb.experimentRun.subtypes.SortingHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.Empty;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jdbi.v3.core.statement.Query;

public class FutureExperimentDAO {
  private final FutureExecutor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;
  private final boolean isMssql;

  private final FutureProjectDAO futureProjectDAO;
  private final AttributeHandler attributeHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final PredicatesHandler predicatesHandler;
  private final CodeVersionHandler codeVersionHandler;
  private final SortingHandler sortingHandler;
  private final CreateExperimentHandler createExperimentHandler;
  private final UACApisUtil uacApisUtil;

  public FutureExperimentDAO(
      FutureExecutor executor,
      FutureJdbi jdbi,
      UAC uac,
      MDBConfig mdbConfig,
      DAOSet daoSet,
      ServiceSet serviceSet) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;
    this.isMssql = mdbConfig.getDatabase().getRdbConfiguration().isMssql();
    this.futureProjectDAO = daoSet.getFutureProjectDAO();
    this.uacApisUtil = serviceSet.getUacApisUtil();

    var entityName = "ExperimentEntity";
    attributeHandler = new AttributeHandler(executor, jdbi, entityName);
    tagsHandler = new TagsHandler(executor, jdbi, entityName);
    codeVersionHandler =
        new CodeVersionHandler(executor, jdbi, ModelDBConstants.EXPERIMENT_TABLE_NAME);
    DatasetHandler datasetHandler = new DatasetHandler(executor, jdbi, entityName, mdbConfig);
    artifactHandler =
        new ArtifactHandler(
            jdbi,
            entityName,
            codeVersionHandler,
            datasetHandler,
            daoSet.getArtifactStoreDAO(),
            mdbConfig);
    predicatesHandler =
        new PredicatesHandler(
            ModelDBConstants.EXPERIMENT_TABLE_NAME,
            ModelDBConstants.EXPERIMENT_TABLE_NAME /*alias*/,
            uacApisUtil);
    sortingHandler = new SortingHandler(ModelDBConstants.EXPERIMENT_TABLE_NAME);

    createExperimentHandler =
        new CreateExperimentHandler(executor, jdbi, attributeHandler, tagsHandler, artifactHandler);
  }

  public InternalFuture<Experiment> createExperiment(CreateExperiment request) {
    return FutureUtil.clientRequest(
            uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor)
        .thenCompose(
            userInfo ->
                createExperimentHandler
                    .convertCreateRequest(request, userInfo)
                    .thenCompose(
                        experiment ->
                            futureProjectDAO
                                .checkProjectPermission(
                                    request.getProjectId(),
                                    ModelDBActionEnum.ModelDBServiceActions.UPDATE)
                                .thenApply(unused -> experiment, executor),
                        executor)
                    .thenCompose(createExperimentHandler::insertExperiment, executor),
            executor);
  }

  public InternalFuture<FindExperiments.Response> findExperiments(FindExperiments request) {
    final var futureLocalContext =
        InternalFuture.supplyAsync(
            () -> {
              final var localQueryContext = new QueryFilterContext();
              localQueryContext.getConditions().add("experiment.deleted = :deleted");
              localQueryContext.getConditions().add("p.deleted = :deleted");
              localQueryContext.getBinds().add(q -> q.bind("deleted", false));

              if (!request.getProjectId().isEmpty()) {
                localQueryContext.getConditions().add("experiment.project_id=:request_project_id");
                localQueryContext
                    .getBinds()
                    .add(q -> q.bind("request_project_id", request.getProjectId()));
              }

              if (!request.getExperimentIdsList().isEmpty()) {
                localQueryContext
                    .getConditions()
                    .add("experiment.id in (<request_experiment_ids>)");
                localQueryContext
                    .getBinds()
                    .add(q -> q.bindList("request_experiment_ids", request.getExperimentIdsList()));
              }

              return localQueryContext;
            },
            executor);

    // futurePredicatesContext
    final var futurePredicatesContext =
        predicatesHandler.processPredicates(request.getPredicatesList(), executor);

    // futureSortingContext
    final var futureSortingContext =
        sortingHandler.processSort(request.getSortKey(), request.getAscending());

    final InternalFuture<QueryFilterContext> futureProjectIds =
        getAccessibleProjectIdsQueryFilterContext(
            request.getWorkspaceName(), request.getProjectId());

    final var futureExperiments =
        futureProjectIds.thenCompose(
            accessibleProjectIdsQueryContext -> {
              // accessibleProjectIdsQueryContext == null means not allowed anything
              if (accessibleProjectIdsQueryContext == null) {
                return InternalFuture.completedInternalFuture(new ArrayList<Experiment>());
              } else {
                final var futureProjectIdsContext =
                    InternalFuture.completedInternalFuture(accessibleProjectIdsQueryContext);
                return InternalFuture.sequence(
                        Arrays.asList(
                            futureLocalContext,
                            futurePredicatesContext,
                            futureSortingContext,
                            futureProjectIdsContext),
                        executor)
                    .thenApply(QueryFilterContext::combine, executor)
                    .thenCompose(
                        queryContext -> {
                          // TODO: get environment
                          // TODO: get features?
                          // TODO: get versioned inputs
                          // TODO: get code version from blob
                          return jdbi.withHandle(
                                  handle -> {
                                    var sql =
                                        "select experiment.id, experiment.date_created, experiment.date_updated, experiment.name, experiment.project_id, experiment.description, experiment.owner, experiment.version_number from experiment";

                                    sql += " inner join project p ON p.id = experiment.project_id ";

                                    try (Query query =
                                        CommonUtils.buildQueryFromQueryContext(
                                            ModelDBConstants.EXPERIMENT_TABLE_NAME,
                                            Pagination.newBuilder()
                                                .setPageNumber(request.getPageNumber())
                                                .setPageLimit(request.getPageLimit())
                                                .build(),
                                            queryContext,
                                            handle,
                                            sql,
                                            isMssql)) {
                                      return query
                                          .map(
                                              (rs, ctx) ->
                                                  Experiment.newBuilder()
                                                      .setId(rs.getString("id"))
                                                      .setProjectId(rs.getString("project_id"))
                                                      .setName(rs.getString("name"))
                                                      .setDescription(rs.getString("description"))
                                                      .setDateUpdated(rs.getLong("date_updated"))
                                                      .setDateCreated(rs.getLong("date_created"))
                                                      .setOwner(rs.getString("owner"))
                                                      .setVersionNumber(
                                                          rs.getLong("version_number")))
                                          .list();
                                    }
                                  })
                              .thenCompose(
                                  builders -> {
                                    if (builders == null || builders.isEmpty()) {
                                      return InternalFuture.completedInternalFuture(
                                          new LinkedList<Experiment>());
                                    }

                                    var futureBuildersStream =
                                        InternalFuture.completedInternalFuture(builders.stream());
                                    final var ids =
                                        builders.stream()
                                            .map(Builder::getId)
                                            .collect(Collectors.toSet());

                                    // Get tags
                                    final var futureTags =
                                        tagsHandler.getTagsMap(ids).toInternalFuture();
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
                                        attributeHandler.getKeyValuesMap(ids).toInternalFuture();
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureAttributes,
                                            (stream, attributes) ->
                                                stream.map(
                                                    builder ->
                                                        builder.addAllAttributes(
                                                            attributes.get(builder.getId()))),
                                            executor);

                                    // Get artifacts
                                    final var futureArtifacts =
                                        artifactHandler.getArtifactsMap(ids).toInternalFuture();
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureArtifacts,
                                            (stream, artifacts) ->
                                                stream.map(
                                                    builder ->
                                                        builder.addAllArtifacts(
                                                            artifacts.get(builder.getId()))),
                                            executor);

                                    // Get code version snapshot
                                    final var futureCodeVersionSnapshots =
                                        codeVersionHandler.getCodeVersionMap(new ArrayList<>(ids));
                                    futureBuildersStream =
                                        futureBuildersStream.thenCombine(
                                            futureCodeVersionSnapshots,
                                            (stream, codeVersionsMap) ->
                                                stream.peek(
                                                    builder -> {
                                                      if (codeVersionsMap.containsKey(
                                                          builder.getId())) {
                                                        builder.setCodeVersionSnapshot(
                                                            codeVersionsMap.get(builder.getId()));
                                                      }
                                                    }),
                                            executor);

                                    return futureBuildersStream.thenApply(
                                        experimentRunBuilders ->
                                            experimentRunBuilders
                                                .map(Experiment.Builder::build)
                                                .collect(Collectors.toList()),
                                        executor);
                                  },
                                  executor);
                        },
                        executor);
              }
            },
            executor);

    final var futureCount =
        futureProjectIds.thenCompose(
            accessibleProjectIdsQueryContext -> {
              // accessibleProjectIdsQueryContext == null means not allowed anything
              if (accessibleProjectIdsQueryContext == null) {
                return InternalFuture.completedInternalFuture(0L);
              } else {
                final var futureProjectIdsContext =
                    InternalFuture.completedInternalFuture(accessibleProjectIdsQueryContext);
                return InternalFuture.sequence(
                        Arrays.asList(
                            futureLocalContext, futurePredicatesContext, futureProjectIdsContext),
                        executor)
                    .thenApply(QueryFilterContext::combine, executor)
                    .thenCompose(
                        queryContext ->
                            jdbi.withHandle(
                                handle -> {
                                  var sql = "select count(experiment.id) from experiment";

                                  sql += " inner join project p ON p.id = experiment.project_id ";

                                  if (!queryContext.getConditions().isEmpty()) {
                                    sql +=
                                        " WHERE "
                                            + String.join(" AND ", queryContext.getConditions());
                                  }

                                  try (var query = handle.createQuery(sql)) {
                                    queryContext.getBinds().forEach(b -> b.accept(query));
                                    return query.mapTo(Long.class).one();
                                  }
                                }),
                        executor);
              }
            },
            executor);

    return futureExperiments.thenCombine(
        futureCount,
        (experiments, count) ->
            FindExperiments.Response.newBuilder()
                .addAllExperiments(experiments)
                .setTotalRecords(count)
                .build(),
        executor);
  }

  private InternalFuture<QueryFilterContext> getAccessibleProjectIdsQueryFilterContext(
      String workspaceName, String requestedProjectId) {
    if (workspaceName.isEmpty()) {
      return uacApisUtil
          .getAllowedEntitiesByResourceType(
              ModelDBActionEnum.ModelDBServiceActions.READ,
              ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT)
          .toInternalFuture()
          .thenApply(
              resources -> {
                boolean allowedAllResources = RoleServiceUtils.checkAllResourceAllowed(resources);
                if (allowedAllResources) {
                  return new QueryFilterContext();
                } else {
                  Set<String> accessibleProjectIds = RoleServiceUtils.getResourceIds(resources);
                  if (accessibleProjectIds.isEmpty()) {
                    return null;
                  } else {
                    return new QueryFilterContext()
                        .addCondition("experiment.project_id in (<authz_project_ids>)")
                        .addBind(q -> q.bindList("authz_project_ids", accessibleProjectIds));
                  }
                }
              },
              executor);
    } else {
      // futureProjectIds based on workspace
      return uacApisUtil
          .getAccessibleProjectIdsBasedOnWorkspace(workspaceName, Optional.of(requestedProjectId))
          .toInternalFuture()
          .thenApply(
              accessibleProjectIds -> {
                if (accessibleProjectIds.isEmpty()) {
                  return null;
                } else {
                  return new QueryFilterContext()
                      .addCondition("experiment.project_id in (<authz_project_ids>)")
                      .addBind(q -> q.bindList("authz_project_ids", accessibleProjectIds));
                }
              },
              executor);
    }
  }

  public InternalFuture<Experiment> updateExperimentNameOrDescription(
      UpdateExperimentNameOrDescription request) {
    return getProjectIdByExperimentId(Collections.singletonList(request.getId()))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO
                    .checkProjectPermission(
                        projectIdFromExperimentMap.get(request.getId()),
                        ModelDBServiceActions.UPDATE)
                    .thenApply(unused -> projectIdFromExperimentMap.get(request.getId()), executor),
            executor)
        .thenCompose(
            projectId -> updateExperimentName(request.getName(), projectId, request.getId()),
            executor)
        .thenCompose(
            unused -> {
              // FIXME: this code never allows us to set the description as an empty string
              if (!request.getDescription().isEmpty()) {
                return updateExperimentField(
                    request.getId(), "description", request.getDescription());
              }
              return InternalFuture.completedInternalFuture(null);
            },
            executor)
        .thenCompose(unused -> getExperimentById(request.getId()), executor);
  }

  private InternalFuture<Void> updateExperimentField(
      String expId, String fieldName, String fieldValue) {
    return jdbi.useHandle(
        handle -> {
          try (var updateQuery =
              handle.createUpdate(
                  String.format(
                      "update experiment set %s = :fieldValue, date_updated = :updatedTime, version_number=(version_number + 1) where id = :id ",
                      fieldName))) {
            updateQuery
                .bind("fieldValue", fieldValue)
                .bind("id", expId)
                .bind("updatedTime", new Date().getTime())
                .execute();
          }
        });
  }

  private InternalFuture<Map<String, String>> getProjectIdByExperimentId(
      List<String> experimentIds) {
    return jdbi.withHandle(
        handle -> {
          try (var query =
              handle.createQuery(
                  "select id, project_id from experiment where id IN (<ids>) AND deleted = :deleted")) {
            List<Map<String, String>> experimentEntitiesMap =
                query
                    .bind("deleted", false)
                    .bindList("ids", experimentIds)
                    .map(
                        (rs, ctx) ->
                            Collections.singletonMap(
                                rs.getString("id"), rs.getString("project_id")))
                    .list();

            Map<String, String> projectIdFromExperimentMap = new HashMap<>();
            for (var result : experimentEntitiesMap) {
              projectIdFromExperimentMap.putAll(result);
            }
            for (var expId : experimentIds) {
              if (!projectIdFromExperimentMap.containsKey(expId)) {
                projectIdFromExperimentMap.put(expId, "");
              }
            }
            return projectIdFromExperimentMap;
          }
        });
  }

  public InternalFuture<Experiment> getExperimentById(String experimentId) {
    return findExperiments(FindExperiments.newBuilder().addExperimentIds(experimentId).build())
        .thenApply(
            findResponse -> {
              if (findResponse.getExperimentsCount() > 1) {
                throw new InternalErrorException(
                    "More than one Experiment found for ID: " + experimentId);
              } else if (findResponse.getExperimentsCount() == 0) {
                throw new NotFoundException("Experiment not found for the ID: " + experimentId);
              } else {
                return findResponse.getExperiments(0);
              }
            },
            executor);
  }

  public InternalFuture<Experiment> updateExperimentName(UpdateExperimentName request) {
    return getProjectIdByExperimentId(Collections.singletonList(request.getId()))
        .thenCompose(
            projectIdFromExperimentMap -> {
              var projectId = projectIdFromExperimentMap.get(request.getId());
              return futureProjectDAO
                  .checkProjectPermission(projectId, ModelDBServiceActions.UPDATE)
                  .thenApply(unused -> projectId, executor);
            },
            executor)
        .thenCompose(
            projectId -> updateExperimentName(request.getName(), projectId, request.getId()),
            executor)
        .thenCompose(unused -> getExperimentById(request.getId()), executor);
  }

  private InternalFuture<Void> updateExperimentName(
      String name, String projectId, String experimentId) {
    if (name.isEmpty()) {
      name = MetadataServiceImpl.createRandomName();
    }

    name = ModelDBUtils.checkEntityNameLength(name);

    String finalName = name;
    return jdbi.useHandle(
            handle -> {
              try (var query =
                  handle.createQuery(
                      "select count(id) from experiment where project_id = :projectId and name = :name and deleted = :deleted")) {
                Optional<Long> countOptional =
                    query
                        .bind("projectId", projectId)
                        .bind("name", finalName)
                        .bind("deleted", false)
                        .mapTo(Long.class)
                        .findOne();
                if (countOptional.isPresent() && countOptional.get() > 0) {
                  throw new AlreadyExistsException(
                      String.format(
                          "Experiment with name '%s' already exists in project", finalName));
                }
              }
            })
        .thenCompose(unused -> updateExperimentField(experimentId, "name", finalName), executor);
  }

  public InternalFuture<Experiment> updateExperimentDescription(
      UpdateExperimentDescription request) {
    return getProjectIdByExperimentId(Collections.singletonList(request.getId()))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(request.getId()), ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                updateExperimentField(request.getId(), "description", request.getDescription()),
            executor)
        .thenCompose(unused -> getExperimentById(request.getId()), executor);
  }

  public InternalFuture<Experiment> addTags(String expId, List<String> tags) {
    final var now = new Date().getTime();

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> {
                      tagsHandler.addTags(
                          handle, expId, TagsHandlerBase.checkEntityTagsLength(tags));
                      updateModifiedTimestamp(handle, expId, now);
                      updateVersionNumber(handle, expId);
                    }),
            executor)
        .thenCompose(unused -> getExperimentById(expId), executor);
  }

  private void updateModifiedTimestamp(Handle handle, String experimentId, Long now) {
    try (var query = handle.createQuery("SELECT date_updated FROM experiment WHERE id=:exp_id")) {
      final var currentDateUpdated = query.bind("exp_id", experimentId).mapTo(Long.class).one();
      final var dateUpdated = Math.max(currentDateUpdated, now);
      try (var updatequery =
          handle.createUpdate(
              "update experiment set date_updated=:date_updated where id=:exp_id")) {
        updatequery.bind("exp_id", experimentId).bind("date_updated", dateUpdated).execute();
      }
    }
  }

  private void updateVersionNumber(Handle handle, String expId) {
    try (var updateQuery =
        handle.createUpdate(
            "update experiment set version_number=(version_number + 1) where id=:exp_id")) {
      updateQuery.bind("exp_id", expId).execute();
    }
  }

  public InternalFuture<GetTags.Response> getTags(String expId) {
    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.READ),
            executor)
        .thenSupply(() -> tagsHandler.getTags(expId).toInternalFuture(), executor)
        .thenApply(tags -> GetTags.Response.newBuilder().addAllTags(tags).build(), executor);
  }

  public InternalFuture<Experiment> deleteTags(String expId, List<String> tags, boolean deleteAll) {
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeTags = deleteAll ? Optional.empty() : Optional.of(tags);

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> {
                      tagsHandler.deleteTags(handle, expId, maybeTags);
                      updateModifiedTimestamp(handle, expId, now);
                      updateVersionNumber(handle, expId);
                    }),
            executor)
        .thenCompose(unused -> getExperimentById(expId), executor);
  }

  public InternalFuture<Experiment> logAttributes(String expId, List<KeyValue> attributes) {
    final var now = new Date().getTime();

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> {
                      attributeHandler.logKeyValues(handle, expId, attributes);
                      updateModifiedTimestamp(handle, expId, now);
                      updateVersionNumber(handle, expId);
                    }),
            executor)
        .thenCompose(unused -> getExperimentById(expId), executor);
  }

  public InternalFuture<GetAttributes.Response> getExperimentAttributes(GetAttributes request) {
    final var expId = request.getId();
    final var keys = request.getAttributeKeysList();
    final var getAll = request.getGetAll();

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.READ),
            executor)
        .thenCompose(
            unused -> attributeHandler.getKeyValues(expId, keys, getAll).toInternalFuture(),
            executor)
        .thenApply(
            keyValues -> GetAttributes.Response.newBuilder().addAllAttributes(keyValues).build(),
            executor);
  }

  public InternalFuture<Experiment> deleteAttributes(DeleteExperimentAttributes request) {
    final var experimentId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getAttributeKeysList());

    return getProjectIdByExperimentId(Collections.singletonList(experimentId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(experimentId), ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> {
                      attributeHandler.deleteKeyValues(handle, experimentId, maybeKeys);
                      updateModifiedTimestamp(handle, experimentId, now);
                      updateVersionNumber(handle, experimentId);
                    }),
            executor)
        .thenCompose(unused -> getExperimentById(experimentId), executor);
  }

  public InternalFuture<Map<String, String>> deleteExperiments(DeleteExperiments request) {
    final var experimentIds = request.getIdsList();

    return getProjectIdByExperimentId(experimentIds)
        .thenCompose(
            projectIdFromExperimentMap ->
                uacApisUtil
                    .getResourceItemsForWorkspace(
                        Optional.empty(),
                        Optional.of(new ArrayList<>(projectIdFromExperimentMap.values())),
                        Optional.empty(),
                        ModelDBServiceResourceTypes.PROJECT)
                    .toInternalFuture()
                    .thenCompose(unused -> deleteExperiments(experimentIds), executor)
                    .thenApply(unused -> projectIdFromExperimentMap, executor),
            executor);
  }

  private InternalFuture<Void> deleteExperiments(List<String> experimentIds) {
    return InternalFuture.runAsync(
        () ->
            jdbi.withHandle(
                handle -> {
                  try (var updateQuery =
                      handle.createUpdate(
                          "Update experiment SET deleted = :deleted WHERE id IN (<ids>)")) {
                    return updateQuery
                        .bindList("ids", experimentIds)
                        .bind("deleted", true)
                        .execute();
                  }
                }),
        executor);
  }

  public InternalFuture<Experiment> logCodeVersion(LogExperimentCodeVersion request) {
    final var expId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();
    return getExperimentById(expId)
        .thenApply(
            experiment -> {
              futureProjectDAO.checkProjectPermission(
                  experiment.getProjectId(), ModelDBServiceActions.UPDATE);
              return experiment;
            },
            executor)
        .thenApply(
            existingExperiment -> {
              if (existingExperiment.getCodeVersionSnapshot().hasCodeArchive()
                  || existingExperiment.getCodeVersionSnapshot().hasGitSnapshot()) {
                var errorMessage =
                    "Code version already logged for experiment " + existingExperiment.getId();
                throw new AlreadyExistsException(errorMessage);
              }
              return existingExperiment;
            },
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> {
                      codeVersionHandler.logCodeVersion(
                          handle, expId, true, request.getCodeVersion());
                      updateModifiedTimestamp(handle, expId, now);
                      updateVersionNumber(handle, expId);
                    }),
            executor)
        .thenCompose(unused -> getExperimentById(expId), executor);
  }

  public InternalFuture<Experiment> logArtifacts(LogExperimentArtifacts request) {
    final var experimentId = request.getId();
    final var artifacts = request.getArtifactsList();
    final var now = new Date().getTime();

    return getProjectIdByExperimentId(Collections.singletonList(experimentId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(experimentId), ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                artifactHandler.getArtifacts(experimentId, Optional.empty()).toInternalFuture(),
            executor)
        .thenAccept(
            existingArtifacts -> {
              for (Artifact existingArtifact : existingArtifacts) {
                for (Artifact newArtifact : artifacts) {
                  if (existingArtifact.getKey().equals(newArtifact.getKey())) {
                    throw new AlreadyExistsException(
                        "Artifact being logged already exists. existing artifact key : "
                            + newArtifact.getKey());
                  }
                }
              }
            },
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> {
                      List<Artifact> artifactList =
                          ModelDBUtils.getArtifactsWithUpdatedPath(request.getId(), artifacts);
                      artifactHandler.logArtifacts(handle, experimentId, artifactList, false);
                      updateModifiedTimestamp(handle, experimentId, now);
                      updateVersionNumber(handle, experimentId);
                    }),
            executor)
        .thenCompose(unused -> getExperimentById(experimentId), executor);
  }

  public InternalFuture<List<Artifact>> getArtifacts(GetArtifacts request) {
    final var expId = request.getId();
    final var key = request.getKey();
    Optional<String> maybeKey = key.isEmpty() ? Optional.empty() : Optional.of(key);

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.READ),
            executor)
        .thenCompose(
            unused -> artifactHandler.getArtifacts(expId, maybeKey).toInternalFuture(), executor);
  }

  public InternalFuture<Experiment> deleteArtifacts(DeleteExperimentArtifact request) {
    final var expId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();
    final var keys =
        request.getKey().isEmpty()
            ? new ArrayList<String>()
            : Collections.singletonList(request.getKey());
    Optional<List<String>> optionalKeys = keys.isEmpty() ? Optional.empty() : Optional.of(keys);

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(
            unused ->
                jdbi.useHandle(
                    handle -> {
                      artifactHandler.deleteArtifactsWithHandle(expId, optionalKeys, handle);
                      updateModifiedTimestamp(handle, expId, now);
                      updateVersionNumber(handle, expId);
                    }),
            executor)
        .thenCompose(unused -> getExperimentById(expId), executor);
  }

  public InternalFuture<GetExperimentCodeVersion.Response> getExperimentCodeVersion(
      GetExperimentCodeVersion request) {
    final var expId = request.getId();

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.READ),
            executor)
        .thenCompose(unused -> codeVersionHandler.getCodeVersion(expId), executor)
        .thenApply(
            codeVersion -> {
              var builder = GetExperimentCodeVersion.Response.newBuilder();
              codeVersion.ifPresent(builder::setCodeVersion);
              return builder.build();
            },
            executor);
  }

  public InternalFuture<GetUrlForArtifact.Response> getUrlForArtifact(GetUrlForArtifact request) {
    final var experimentId = request.getId();

    var permissionCheck =
        getProjectIdByExperimentId(Collections.singletonList(experimentId))
            .thenCompose(
                projectIdFromExperimentMap ->
                    futureProjectDAO.checkProjectPermission(
                        projectIdFromExperimentMap.get(experimentId), ModelDBServiceActions.READ),
                executor);

    return permissionCheck.thenCompose(
        unused -> artifactHandler.getUrlForArtifact(request).toInternalFuture(), executor);
  }
}
