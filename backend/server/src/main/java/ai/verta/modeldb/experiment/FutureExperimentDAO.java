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
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetExperimentCodeVersion;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.LogExperimentArtifacts;
import ai.verta.modeldb.LogExperimentCodeVersion;
import ai.verta.modeldb.UpdateExperimentDescription;
import ai.verta.modeldb.UpdateExperimentName;
import ai.verta.modeldb.UpdateExperimentNameOrDescription;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.*;
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
import ai.verta.modeldb.utils.UACApisUtil;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.statement.Query;

public class FutureExperimentDAO {
  private static final Logger LOGGER = LogManager.getLogger(FutureExperimentDAO.class);

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
      FutureExecutor executor, FutureJdbi jdbi, UAC uac, MDBConfig mdbConfig, DAOSet daoSet) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;
    this.isMssql = mdbConfig.getDatabase().getRdbConfiguration().isMssql();
    this.futureProjectDAO = daoSet.getFutureProjectDAO();
    this.uacApisUtil = daoSet.getUacApisUtil();

    var entityName = "ExperimentEntity";
    attributeHandler = new AttributeHandler(executor, jdbi, entityName);
    tagsHandler = new TagsHandler(executor, jdbi, entityName);
    codeVersionHandler = new CodeVersionHandler(executor, jdbi, "experiment");
    DatasetHandler datasetHandler = new DatasetHandler(executor, jdbi, entityName, mdbConfig);
    artifactHandler =
        new ArtifactHandler(
            executor,
            jdbi,
            entityName,
            codeVersionHandler,
            datasetHandler,
            daoSet.getArtifactStoreDAO(),
            mdbConfig);
    predicatesHandler = new PredicatesHandler("experiment", "experiment", uacApisUtil);
    sortingHandler = new SortingHandler("experiment");

    createExperimentHandler =
        new CreateExperimentHandler(
            executor, jdbi, mdbConfig, uac, attributeHandler, tagsHandler, artifactHandler);
  }

  public Future<Experiment> createExperiment(CreateExperiment request) {
    return FutureUtil.clientRequest(
            uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor)
        .toFuture()
        .thenCompose(
            userInfo ->
                createExperimentHandler
                    .convertCreateRequest(request, userInfo)
                    .thenCompose(
                        experiment -> {
                          return futureProjectDAO
                              .checkProjectPermission(
                                  request.getProjectId(), ModelDBServiceActions.UPDATE)
                              .thenCompose(unused -> Future.of(experiment));
                        })
                    .thenCompose(createExperimentHandler::insertExperiment));
  }

  public Future<FindExperiments.Response> findExperiments(FindExperiments request) {
    final var futureLocalContext =
        Future.supplyAsync(
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
            });

    // futurePredicatesContext
    final var futurePredicatesContext =
        predicatesHandler.processPredicates(request.getPredicatesList());

    // futureSortingContext
    final var futureSortingContext =
        sortingHandler.processSort(request.getSortKey(), request.getAscending());

    final Future<QueryFilterContext> futureProjectIds =
        getAccessibleProjectIdsQueryFilterContext(
            request.getWorkspaceName(), request.getProjectId());

    final var futureExperiments =
        futureProjectIds.thenCompose(
            accessibleProjectIdsQueryContext -> {
              // accessibleProjectIdsQueryContext == null means not allowed anything
              if (accessibleProjectIdsQueryContext == null) {
                return Future.of(new ArrayList<Experiment>());
              } else {
                final var futureProjectIdsContext = Future.of(accessibleProjectIdsQueryContext);
                return Future.sequence(
                        Arrays.asList(
                            futureLocalContext,
                            futurePredicatesContext,
                            futureSortingContext,
                            futureProjectIdsContext))
                    .thenCompose(a -> Future.of(QueryFilterContext.combine(a)))
                    .thenCompose(
                        queryContext -> {
                          // TODO: get environment
                          // TODO: get features?
                          // TODO: get versioned inputs
                          // TODO: get code version from blob
                          return jdbi.call(
                                  handle -> {
                                    var sql =
                                        "select experiment.id, experiment.date_created, experiment.date_updated, experiment.name, experiment.project_id, experiment.description, experiment.owner, experiment.version_number from experiment";

                                    sql += " inner join project p ON p.id = experiment.project_id ";

                                    Query query =
                                        CommonUtils.buildQueryFromQueryContext(
                                            "experiment",
                                            Pagination.newBuilder()
                                                .setPageNumber(request.getPageNumber())
                                                .setPageLimit(request.getPageLimit())
                                                .build(),
                                            queryContext,
                                            handle,
                                            sql,
                                            isMssql);

                                    return query
                                        .map(
                                            (rs, ctx) -> {
                                              var runBuilder =
                                                  Experiment.newBuilder()
                                                      .setId(rs.getString("id"))
                                                      .setProjectId(rs.getString("project_id"))
                                                      .setName(rs.getString("name"))
                                                      .setDescription(rs.getString("description"))
                                                      .setDateUpdated(rs.getLong("date_updated"))
                                                      .setDateCreated(rs.getLong("date_created"))
                                                      .setOwner(rs.getString("owner"))
                                                      .setVersionNumber(
                                                          rs.getLong("version_number"));
                                              return runBuilder;
                                            })
                                        .list();
                                  })
                              .thenCompose(
                                  builders -> {
                                    if (builders == null || builders.isEmpty()) {
                                      return Future.of(new LinkedList<Experiment>());
                                    }

                                    var futureBuildersStream = Future.of(builders.stream());
                                    final var ids =
                                        builders.stream()
                                            .map(x -> x.getId())
                                            .collect(Collectors.toSet());

                                    // Get tags
                                    final var futureTags = tagsHandler.getTagsMap(ids).toFuture();
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
                                                            attributes.get(builder.getId()))));

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
                                                            artifacts.get(builder.getId()))));

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
                                                    }));

                                    return futureBuildersStream.thenCompose(
                                        experimentRunBuilders ->
                                            Future.of(
                                                experimentRunBuilders
                                                    .map(Experiment.Builder::build)
                                                    .collect(Collectors.toList())));
                                  });
                        });
              }
            });

    final var futureCount =
        futureProjectIds.thenCompose(
            accessibleProjectIdsQueryContext -> {
              // accessibleProjectIdsQueryContext == null means not allowed anything
              if (accessibleProjectIdsQueryContext == null) {
                return Future.of(0L);
              } else {
                final var futureProjectIdsContext = Future.of(accessibleProjectIdsQueryContext);
                return Future.sequence(
                        Arrays.asList(
                            futureLocalContext, futurePredicatesContext, futureProjectIdsContext))
                    .thenCompose(a -> Future.of(QueryFilterContext.combine(a)))
                    .thenCompose(
                        queryContext ->
                            jdbi.call(
                                handle -> {
                                  var sql = "select count(experiment.id) from experiment";

                                  sql += " inner join project p ON p.id = experiment.project_id ";

                                  if (!queryContext.getConditions().isEmpty()) {
                                    sql +=
                                        " WHERE "
                                            + String.join(" AND ", queryContext.getConditions());
                                  }

                                  var query = handle.createQuery(sql);
                                  queryContext.getBinds().forEach(b -> b.accept(query));

                                  return query.mapTo(Long.class).one();
                                }));
              }
            });

    return futureExperiments.thenCombine(
        futureCount,
        (experiments, count) ->
            FindExperiments.Response.newBuilder()
                .addAllExperiments(experiments)
                .setTotalRecords(count)
                .build());
  }

  private Future<QueryFilterContext> getAccessibleProjectIdsQueryFilterContext(
      String workspaceName, String requestedProjectId) {
    if (workspaceName.isEmpty()) {
      return uacApisUtil
          .getAllowedEntitiesByResourceType(
              ModelDBActionEnum.ModelDBServiceActions.READ,
              ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT)
          .thenCompose(
              resources -> {
                boolean allowedAllResources = RoleServiceUtils.checkAllResourceAllowed(resources);
                if (allowedAllResources) {
                  return Future.of(new QueryFilterContext());
                } else {
                  Set<String> accessibleProjectIds = RoleServiceUtils.getResourceIds(resources);
                  if (accessibleProjectIds.isEmpty()) {
                    return Future.of(null);
                  } else {
                    return Future.of(
                        new QueryFilterContext()
                            .addCondition("experiment.project_id in (<authz_project_ids>)")
                            .addBind(q -> q.bindList("authz_project_ids", accessibleProjectIds)));
                  }
                }
              });
    } else {
      // futureProjectIds based on workspace
      return uacApisUtil
          .getAccessibleProjectIdsBasedOnWorkspace(workspaceName, Optional.of(requestedProjectId))
          .thenCompose(
              accessibleProjectIds -> {
                if (accessibleProjectIds.isEmpty()) {
                  return Future.of(null);
                } else {
                  return Future.of(
                      new QueryFilterContext()
                          .addCondition("experiment.project_id in (<authz_project_ids>)")
                          .addBind(q -> q.bindList("authz_project_ids", accessibleProjectIds)));
                }
              });
    }
  }

  public Future<Experiment> updateExperimentNameOrDescription(
      UpdateExperimentNameOrDescription request) {
    return getProjectIdByExperimentId(Collections.singletonList(request.getId()))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO
                    .checkProjectPermission(
                        projectIdFromExperimentMap.get(request.getId()),
                        ModelDBServiceActions.UPDATE)
                    .thenCompose(
                        unused -> Future.of(projectIdFromExperimentMap.get(request.getId()))))
        .thenCompose(
            projectId -> updateExperimentName(request.getName(), projectId, request.getId()))
        .thenCompose(
            unused -> {
              // FIXME: this code never allows us to set the description as an empty string
              if (!request.getDescription().isEmpty()) {
                return updateExperimentField(
                    request.getId(), "description", request.getDescription());
              }
              return Future.of(null);
            })
        .thenCompose(unused -> getExperimentById(request.getId()));
  }

  private Future<Void> updateExperimentField(String expId, String fieldName, String fieldValue) {
    return jdbi.run(
        handle ->
            handle
                .createUpdate(
                    String.format(
                        "update experiment set %s = :fieldValue, date_updated = :updatedTime, version_number=(version_number + 1) where id = :id ",
                        fieldName))
                .bind("fieldValue", fieldValue)
                .bind("id", expId)
                .bind("updatedTime", new Date().getTime())
                .execute());
  }

  private Future<Map<String, String>> getProjectIdByExperimentId(List<String> experimentIds) {
    return jdbi.call(
        handle -> {
          List<Map<String, String>> experimentEntitiesMap =
              handle
                  .createQuery(
                      "select id, project_id from experiment where id IN (<ids>) AND deleted = :deleted")
                  .bind("deleted", false)
                  .bindList("ids", experimentIds)
                  .map(
                      (rs, ctx) ->
                          Collections.singletonMap(rs.getString("id"), rs.getString("project_id")))
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
        });
  }

  public Future<Experiment> getExperimentById(String experimentId) {
    return findExperiments(FindExperiments.newBuilder().addExperimentIds(experimentId).build())
        .thenCompose(
            findResponse -> {
              if (findResponse.getExperimentsCount() > 1) {
                return Future.failedStage(
                    new InternalErrorException(
                        "More than one Experiment found for ID: " + experimentId));
              } else if (findResponse.getExperimentsCount() == 0) {
                return Future.failedStage(
                    new NotFoundException("Experiment not found for the ID: " + experimentId));
              } else {
                return Future.of(findResponse.getExperiments(0));
              }
            });
  }

  public Future<Experiment> updateExperimentName(UpdateExperimentName request) {
    return getProjectIdByExperimentId(Collections.singletonList(request.getId()))
        .thenCompose(
            projectIdFromExperimentMap -> {
              var projectId = projectIdFromExperimentMap.get(request.getId());
              return futureProjectDAO
                  .checkProjectPermission(projectId, ModelDBServiceActions.UPDATE)
                  .thenCompose(unused -> Future.of(projectId));
            })
        .thenCompose(
            projectId -> updateExperimentName(request.getName(), projectId, request.getId()))
        .thenCompose(unused -> getExperimentById(request.getId()));
  }

  private Future<Void> updateExperimentName(String name, String projectId, String experimentId) {
    if (name.isEmpty()) {
      name = MetadataServiceImpl.createRandomName();
    }

    name = ModelDBUtils.checkEntityNameLength(name);

    String finalName = name;
    return jdbi.run(
            handle -> {
              Optional<Long> countOptional =
                  handle
                      .createQuery(
                          "select count(id) from experiment where project_id = :projectId and name = :name and deleted = :deleted")
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
            })
        .thenCompose(unused -> updateExperimentField(experimentId, "name", finalName));
  }

  public Future<Experiment> updateExperimentDescription(UpdateExperimentDescription request) {
    return getProjectIdByExperimentId(Collections.singletonList(request.getId()))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(request.getId()), ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused ->
                updateExperimentField(request.getId(), "description", request.getDescription()))
        .thenCompose(unused -> getExperimentById(request.getId()));
  }

  public Future<Experiment> addTags(String expId, List<String> tags) {
    final var now = new Date().getTime();

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused ->
                jdbi.run(
                    handle -> {
                      tagsHandler.addTags(
                          handle, expId, TagsHandlerBase.checkEntityTagsLength(tags));
                      updateModifiedTimestamp(handle, expId, now);
                      updateVersionNumber(handle, expId);
                    }))
        .thenCompose(unused -> getExperimentById(expId));
  }

  private void updateModifiedTimestamp(Handle handle, String experimentId, Long now) {
    final var currentDateUpdated =
        handle
            .createQuery("SELECT date_updated FROM experiment WHERE id=:exp_id")
            .bind("exp_id", experimentId)
            .mapTo(Long.class)
            .one();
    final var dateUpdated = Math.max(currentDateUpdated, now);
    handle
        .createUpdate("update experiment set date_updated=:date_updated where id=:exp_id")
        .bind("exp_id", experimentId)
        .bind("date_updated", dateUpdated)
        .execute();
  }

  private void updateVersionNumber(Handle handle, String expId) {
    handle
        .createUpdate("update experiment set version_number=(version_number + 1) where id=:exp_id")
        .bind("exp_id", expId)
        .execute();
  }

  public Future<GetTags.Response> getTags(String expId) {
    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.READ))
        .thenCompose(unused -> tagsHandler.getTags(expId).toFuture())
        .thenCompose(tags -> Future.of(GetTags.Response.newBuilder().addAllTags(tags).build()));
  }

  public Future<Experiment> deleteTags(String expId, List<String> tags, boolean deleteAll) {
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeTags = deleteAll ? Optional.empty() : Optional.of(tags);

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused ->
                jdbi.run(
                    handle -> {
                      tagsHandler.deleteTags(handle, expId, maybeTags);
                      updateModifiedTimestamp(handle, expId, now);
                      updateVersionNumber(handle, expId);
                    }))
        .thenCompose(unused -> getExperimentById(expId));
  }

  public Future<Experiment> logAttributes(String expId, List<KeyValue> attributes) {
    final var now = new Date().getTime();

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused ->
                jdbi.run(
                    handle -> {
                      attributeHandler.logKeyValues(handle, expId, attributes);
                      updateModifiedTimestamp(handle, expId, now);
                      updateVersionNumber(handle, expId);
                    }))
        .thenCompose(unused -> getExperimentById(expId));
  }

  public Future<GetAttributes.Response> getExperimentAttributes(GetAttributes request) {
    final var expId = request.getId();
    final var keys = request.getAttributeKeysList();
    final var getAll = request.getGetAll();

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.READ))
        .thenCompose(unused -> attributeHandler.getKeyValues(expId, keys, getAll).toFuture())
        .thenCompose(
            keyValues ->
                Future.of(GetAttributes.Response.newBuilder().addAllAttributes(keyValues).build()));
  }

  public Future<Experiment> deleteAttributes(DeleteExperimentAttributes request) {
    final var experimentId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getAttributeKeysList());

    return getProjectIdByExperimentId(Collections.singletonList(experimentId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(experimentId), ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused ->
                jdbi.run(
                    handle -> {
                      attributeHandler.deleteKeyValues(handle, experimentId, maybeKeys);
                      updateModifiedTimestamp(handle, experimentId, now);
                      updateVersionNumber(handle, experimentId);
                    }))
        .thenCompose(unused -> getExperimentById(experimentId));
  }

  public Future<Map<String, String>> deleteExperiments(DeleteExperiments request) {
    final var experimentIds = request.getIdsList();

    return getProjectIdByExperimentId(experimentIds)
        .thenCompose(
            projectIdFromExperimentMap -> {
              return uacApisUtil
                  .getResourceItemsForWorkspace(
                      Optional.empty(),
                      Optional.of(new ArrayList<>(projectIdFromExperimentMap.values())),
                      Optional.empty(),
                      ModelDBServiceResourceTypes.PROJECT)
                  .thenCompose(unused -> deleteExperiments(experimentIds))
                  .thenCompose(unused -> Future.of(projectIdFromExperimentMap));
            });
  }

  private Future<Void> deleteExperiments(List<String> experimentIds) {
    return Future.runAsync(
        () ->
            jdbi.withHandle(
                handle ->
                    handle
                        .createUpdate(
                            "Update experiment SET deleted = :deleted WHERE id IN (<ids>)")
                        .bindList("ids", experimentIds)
                        .bind("deleted", true)
                        .execute()));
  }

  public Future<Experiment> logCodeVersion(LogExperimentCodeVersion request) {
    final var expId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();
    return getExperimentById(expId)
        .thenCompose(
            experiment ->
                futureProjectDAO
                    .checkProjectPermission(experiment.getProjectId(), ModelDBServiceActions.UPDATE)
                    .thenSupply(() -> Future.of(experiment)))
        .thenCompose(
            existingExperiment -> {
              if (existingExperiment.getCodeVersionSnapshot().hasCodeArchive()
                  || existingExperiment.getCodeVersionSnapshot().hasGitSnapshot()) {
                var errorMessage =
                    "Code version already logged for experiment " + existingExperiment.getId();
                return Future.failedStage(new AlreadyExistsException(errorMessage));
              }
              return Future.of(existingExperiment);
            })
        .thenCompose(
            unused ->
                jdbi.run(
                    handle -> {
                      codeVersionHandler.logCodeVersion(
                          handle, expId, true, request.getCodeVersion());
                      updateModifiedTimestamp(handle, expId, now);
                      updateVersionNumber(handle, expId);
                    }))
        .thenCompose(unused -> getExperimentById(expId));
  }

  public Future<Experiment> logArtifacts(LogExperimentArtifacts request) {
    final var experimentId = request.getId();
    final var artifacts = request.getArtifactsList();
    final var now = new Date().getTime();

    return getProjectIdByExperimentId(Collections.singletonList(experimentId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(experimentId), ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused -> artifactHandler.getArtifacts(experimentId, Optional.empty()).toFuture())
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
            })
        .thenCompose(
            unused ->
                jdbi.run(
                    handle -> {
                      List<Artifact> artifactList =
                          ModelDBUtils.getArtifactsWithUpdatedPath(request.getId(), artifacts);
                      artifactHandler.logArtifacts(handle, experimentId, artifactList, false);
                      updateModifiedTimestamp(handle, experimentId, now);
                      updateVersionNumber(handle, experimentId);
                    }))
        .thenCompose(unused -> getExperimentById(experimentId));
  }

  public Future<List<Artifact>> getArtifacts(GetArtifacts request) {
    final var expId = request.getId();
    final var key = request.getKey();
    Optional<String> maybeKey = key.isEmpty() ? Optional.empty() : Optional.of(key);

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.READ))
        .thenCompose(unused -> artifactHandler.getArtifacts(expId, maybeKey).toFuture());
  }

  public Future<Experiment> deleteArtifacts(DeleteExperimentArtifact request) {
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
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.UPDATE))
        .thenCompose(
            unused ->
                jdbi.run(
                    handle -> {
                      artifactHandler.deleteArtifactsWithHandle(expId, optionalKeys, handle);
                      updateModifiedTimestamp(handle, expId, now);
                      updateVersionNumber(handle, expId);
                    }))
        .thenCompose(unused -> getExperimentById(expId));
  }

  public Future<GetExperimentCodeVersion.Response> getExperimentCodeVersion(
      GetExperimentCodeVersion request) {
    final var expId = request.getId();

    return getProjectIdByExperimentId(Collections.singletonList(expId))
        .thenCompose(
            projectIdFromExperimentMap ->
                futureProjectDAO.checkProjectPermission(
                    projectIdFromExperimentMap.get(expId), ModelDBServiceActions.READ))
        .thenCompose(unused -> codeVersionHandler.getCodeVersion(expId))
        .thenCompose(
            codeVersion -> {
              var builder = GetExperimentCodeVersion.Response.newBuilder();
              codeVersion.ifPresent(builder::setCodeVersion);
              return Future.of(builder.build());
            });
  }

  public Future<GetUrlForArtifact.Response> getUrlForArtifact(GetUrlForArtifact request) {
    final var experimentId = request.getId();

    var permissionCheck =
        getProjectIdByExperimentId(Collections.singletonList(experimentId))
            .thenCompose(
                projectIdFromExperimentMap ->
                    futureProjectDAO.checkProjectPermission(
                        projectIdFromExperimentMap.get(experimentId), ModelDBServiceActions.READ));

    return permissionCheck.thenCompose(unused -> artifactHandler.getUrlForArtifact(request));
  }
}
