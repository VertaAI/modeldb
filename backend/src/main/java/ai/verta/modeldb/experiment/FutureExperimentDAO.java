package ai.verta.modeldb.experiment;

import ai.verta.common.CodeVersion;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.Pagination;
import ai.verta.modeldb.CreateExperiment;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
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
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.modeldb.utils.UACApisUtil;
import ai.verta.uac.Empty;
import ai.verta.uac.ModelDBActionEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.statement.Query;

public class FutureExperimentDAO {
  private static final Logger LOGGER = LogManager.getLogger(FutureExperimentDAO.class);

  private final Executor executor;
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
      Executor executor, FutureJdbi jdbi, UAC uac, MDBConfig mdbConfig, DAOSet daoSet) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;
    this.isMssql = mdbConfig.getDatabase().getRdbConfiguration().isMssql();
    this.futureProjectDAO = daoSet.futureProjectDAO;
    this.uacApisUtil = daoSet.uacApisUtil;

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
            daoSet.artifactStoreDAO,
            daoSet.datasetVersionDAO,
            mdbConfig);
    predicatesHandler = new PredicatesHandler("experiment", "experiment");
    sortingHandler = new SortingHandler("experiment");

    createExperimentHandler =
        new CreateExperimentHandler(
            executor, jdbi, mdbConfig, uac, attributeHandler, tagsHandler, artifactHandler);
  }

  public InternalFuture<Experiment> createExperiment(CreateExperiment request) {
    return FutureGrpc.ClientRequest(
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
                                      return InternalFuture.completedInternalFuture(
                                          new LinkedList<Experiment>());
                                    }

                                    var futureBuildersStream =
                                        InternalFuture.completedInternalFuture(builders.stream());
                                    final var ids =
                                        builders.stream()
                                            .map(x -> x.getId())
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
                                                            attributes.get(builder.getId()))),
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
                                                      } else {
                                                        builder.setCodeVersionSnapshot(
                                                            CodeVersion.getDefaultInstance());
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

                                  var query = handle.createQuery(sql);
                                  queryContext.getBinds().forEach(b -> b.accept(query));

                                  return query.mapTo(Long.class).one();
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
          .thenApply(
              resources -> {
                boolean allowedAllResources = uacApisUtil.checkAllResourceAllowed(resources);
                if (allowedAllResources) {
                  return new QueryFilterContext();
                } else {
                  List<String> accessibleProjectIds =
                      resources.stream()
                          .flatMap(x -> x.getResourceIdsList().stream())
                          .collect(Collectors.toList());
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
}
