package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.CreateExperimentRun;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.LogExperimentRunCodeVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.TrialUtils;
import ai.verta.uac.*;
import java.util.*;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

public class CreateExperimentRunHandler {

  private static Logger LOGGER = LogManager.getLogger(CreateExperimentRunHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;
  private final Config config = Config.getInstance();

  private final AttributeHandler attributeHandler;
  private final KeyValueHandler hyperparametersHandler;
  private final KeyValueHandler metricsHandler;
  private final ObservationHandler observationHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final FeatureHandler featureHandler;
  private final CodeVersionHandler codeVersionHandler;
  private final DatasetHandler datasetHandler;

  public CreateExperimentRunHandler(
      Executor executor,
      FutureJdbi jdbi,
      UAC uac,
      AttributeHandler attributeHandler,
      KeyValueHandler hyperparametersHandler,
      KeyValueHandler metricsHandler,
      ObservationHandler observationHandler,
      TagsHandler tagsHandler,
      ArtifactHandler artifactHandler,
      FeatureHandler featureHandler,
      DatasetHandler datasetHandler) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;

    this.attributeHandler = attributeHandler;
    this.hyperparametersHandler = hyperparametersHandler;
    this.metricsHandler = metricsHandler;
    this.observationHandler = observationHandler;
    this.tagsHandler = tagsHandler;
    this.artifactHandler = artifactHandler;
    this.featureHandler = featureHandler;
    this.codeVersionHandler = new CodeVersionHandler(executor, jdbi);
    this.datasetHandler = datasetHandler;
  }

  public InternalFuture<ExperimentRun> createExperimentRun(final CreateExperimentRun request) {
    // Validate arguments
    var futureTask =
        InternalFuture.runAsync(
            () -> {
              if (request.getProjectId().isEmpty()) {
                throw new InvalidArgumentException(
                    "Project ID not found in CreateExperimentRun request");
              } else if (request.getExperimentId().isEmpty()) {
                throw new InvalidArgumentException(
                    "Experiment ID not found in CreateExperimentRun request");
              }
            },
            executor);

    return futureTask
        .thenCompose(
            unused ->
                FutureGrpc.ClientRequest(
                    uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor),
            executor)
        .thenCompose(
            currentLoginUserInfo ->
                TrialUtils.futureValidateExperimentRunPerWorkspaceForTrial(config.trial, executor)
                    .thenCompose(
                        unused -> {
                          final var experimentRun =
                              getExperimentRunFromRequest(request, currentLoginUserInfo);

                          TrialUtils.validateMaxArtifactsForTrial(
                              config.trial, experimentRun.getArtifactsCount(), 0);

                          return insertExperimentRun(experimentRun)
                              .thenCompose(
                                  unused2 -> createRoleBindingsForExperimentRun(experimentRun),
                                  executor)
                              .thenCompose(
                                  unused2 ->
                                      jdbi.useHandle(
                                          handle ->
                                              handle
                                                  .createUpdate(
                                                      "UPDATE experiment_run SET created=:created WHERE id=:id")
                                                  .bind("created", true)
                                                  .bind("id", experimentRun.getId())
                                                  .execute()),
                                  executor)
                              .thenApply(unused2 -> experimentRun, executor);
                        },
                        executor),
            executor);
    /*.thenCompose(
    experimentRun -> {
      // TODO: Fix below logic for checking privileges of linked dataset versions
      */
    /*if (experimentRun.getDatasetsCount() > 0 && config.populateConnectionsBasedOnPrivileges) {
      experimentRun = checkDatasetVersionBasedOnPrivileges(experimentRun, true);
    }*/
    /*
          return InternalFuture.completedInternalFuture(experimentRun);
        },
        executor)
    .thenCompose(
        experimentRun -> {
          // TODO: Fix populating logic of setVersioned_inputs,
          // setHyperparameter_element_mappings here
          return InternalFuture.completedInternalFuture(experimentRun);
        },
        executor)*/
  }

  /**
   * Convert CreateExperimentRun request to Experiment object. This method generate the
   * ExperimentRun Id using UUID and put it in ExperimentRun object.
   *
   * @param request : CreateExperimentRun request
   * @param userInfo : current login UserInfo
   * @return ExperimentRun : experimentRun
   */
  private ExperimentRun getExperimentRunFromRequest(
      CreateExperimentRun request, UserInfo userInfo) {

    /*
     * Create ExperimentRun entity from given CreateExperimentRun request. generate UUID and put as
     * id in ExperimentRun for uniqueness.
     */
    if (request.getName().isEmpty()) {
      request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
    }

    ExperimentRun.Builder experimentRunBuilder =
        ExperimentRun.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setProjectId(request.getProjectId())
            .setExperimentId(request.getExperimentId())
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setDescription(request.getDescription())
            .setStartTime(request.getStartTime())
            .setEndTime(request.getEndTime())
            .setCodeVersion(request.getCodeVersion())
            .setParentId(request.getParentId())
            .addAllTags(ModelDBUtils.checkEntityTagsLength(request.getTagsList()))
            .addAllAttributes(request.getAttributesList())
            .addAllHyperparameters(request.getHyperparametersList())
            .addAllArtifacts(request.getArtifactsList())
            .addAllDatasets(request.getDatasetsList())
            .addAllMetrics(request.getMetricsList())
            .addAllObservations(request.getObservationsList())
            .addAllFeatures(request.getFeaturesList());

    var now = Calendar.getInstance().getTimeInMillis();
    if (request.getDateCreated() != 0L) {
      experimentRunBuilder.setDateCreated(request.getDateCreated());
    } else {
      experimentRunBuilder.setDateCreated(now);
    }

    if (request.getDateUpdated() != 0L) {
      experimentRunBuilder.setDateUpdated(request.getDateCreated());
    } else {
      experimentRunBuilder.setDateUpdated(now);
    }

    if (request.getCodeVersionSnapshot() != null) {
      experimentRunBuilder.setCodeVersionSnapshot(request.getCodeVersionSnapshot());
    }
    if (request.getVersionedInputs() != null && request.hasVersionedInputs()) {
      experimentRunBuilder.setVersionedInputs(request.getVersionedInputs());
    }
    if (userInfo != null) {

      experimentRunBuilder.setOwner(userInfo.getVertaInfo().getUserId());
    }

    return experimentRunBuilder.build();
  }

  private InternalFuture<Void> insertExperimentRun(ExperimentRun newExperimentRun) {
    final var now = Calendar.getInstance().getTimeInMillis();
    return jdbi.useHandle(
            handle ->
                handle.useTransaction(
                    TransactionIsolationLevel.SERIALIZABLE,
                    handleForTransaction -> {
                      Boolean exists =
                          checkInsertedEntityAlreadyExists(handleForTransaction, newExperimentRun);
                      if (exists) {
                        throw new AlreadyExistsException(
                            "ExperimentRun '"
                                + newExperimentRun.getName()
                                + "' already exists in database");
                      }

                      Map<String, Object> runValueMap = new LinkedHashMap<>();
                      runValueMap.put("id", newExperimentRun.getId());
                      runValueMap.put("project_id", newExperimentRun.getProjectId());
                      runValueMap.put("experiment_id", newExperimentRun.getExperimentId());
                      runValueMap.put("name", newExperimentRun.getName());
                      runValueMap.put("description", newExperimentRun.getDescription());
                      runValueMap.put("date_created", newExperimentRun.getDateCreated());
                      runValueMap.put("date_updated", newExperimentRun.getDateUpdated());
                      runValueMap.put("start_time", newExperimentRun.getStartTime());
                      runValueMap.put("end_time", newExperimentRun.getEndTime());
                      runValueMap.put("code_version", newExperimentRun.getCodeVersion());
                      runValueMap.put("job_id", newExperimentRun.getJobId());
                      runValueMap.put("parent_id", newExperimentRun.getParentId());
                      runValueMap.put("owner", newExperimentRun.getOwner());

                      runValueMap.put("environment", null);
                      runValueMap.put("deleted", false);
                      runValueMap.put("created", false);

                      // Created comma separated field names from keys of above map
                      String[] fieldsArr = runValueMap.keySet().toArray(new String[0]);
                      String commaFields = String.join(",", fieldsArr);

                      StringBuilder queryStrBuilder =
                          new StringBuilder("insert into experiment_run ( ")
                              .append(commaFields)
                              .append(") values (");

                      // Created comma separated query bind arguments for the values
                      // based on the
                      // keys of
                      // above the map
                      // Ex: VALUES (:project_id, :experiment_id, :name) etc.
                      String bindArguments =
                          String.join(
                              ",",
                              Arrays.stream(fieldsArr).map(s -> ":" + s).toArray(String[]::new));

                      queryStrBuilder.append(bindArguments);
                      queryStrBuilder.append(" ) ");

                      LOGGER.trace(
                          "insert experiment run query string: " + queryStrBuilder.toString());
                      var query = handleForTransaction.createUpdate(queryStrBuilder.toString());

                      // Inserting fields arguments based on the keys and value of map
                      for (Map.Entry<String, Object> objectEntry : runValueMap.entrySet()) {
                        query.bind(objectEntry.getKey(), objectEntry.getValue());
                      }
                      int count = query.execute();
                      LOGGER.trace("ExperimentRun Inserted : " + (count > 0));
                    }))
        .thenCompose(
            unused -> {
              final var futureLogs = new LinkedList<InternalFuture<Void>>();

              futureLogs.add(
                  tagsHandler.addTags(newExperimentRun.getId(), newExperimentRun.getTagsList()));
              futureLogs.add(
                  attributeHandler.logKeyValues(
                      newExperimentRun.getId(), newExperimentRun.getAttributesList()));
              futureLogs.add(
                  hyperparametersHandler.logKeyValues(
                      newExperimentRun.getId(), newExperimentRun.getHyperparametersList()));
              futureLogs.add(
                  metricsHandler.logKeyValues(
                      newExperimentRun.getId(), newExperimentRun.getMetricsList()));
              futureLogs.add(
                  observationHandler.logObservations(
                      newExperimentRun.getId(), newExperimentRun.getObservationsList(), now));
              futureLogs.add(
                  artifactHandler.logArtifacts(
                      newExperimentRun.getId(), newExperimentRun.getArtifactsList(), false));
              futureLogs.add(
                  featureHandler.logFeatures(
                      newExperimentRun.getId(), newExperimentRun.getFeaturesList()));
              futureLogs.add(
                  codeVersionHandler.logCodeVersion(
                      LogExperimentRunCodeVersion.newBuilder()
                          .setId(newExperimentRun.getId())
                          .setCodeVersion(newExperimentRun.getCodeVersionSnapshot())
                          .setOverwrite(false)
                          .build()));
              futureLogs.add(
                  datasetHandler.logArtifacts(
                      newExperimentRun.getId(), newExperimentRun.getDatasetsList(), false));

              return InternalFuture.sequence(futureLogs, executor)
                  .thenAccept(unused2 -> {}, executor);
            },
            executor);
    // TODO .thenCompose(handle -> versioned_inputs, executor)
  }

  private Boolean checkInsertedEntityAlreadyExists(Handle handle, ExperimentRun experimentRun) {
    String queryStr =
        "SELECT count(id) FROM experiment_run WHERE "
            + " name = :experimentRunName "
            + " AND project_id = :projectId "
            + " AND experiment_id = :experimentId "
            + " AND deleted = false ";

    var query = handle.createQuery(queryStr).bind("experimentRunName", experimentRun.getName());
    query.bind("projectId", experimentRun.getProjectId());
    query.bind("experimentId", experimentRun.getExperimentId());

    long count = query.mapTo(Long.class).one();
    return count > 0;
  }

  private String buildRoleBindingName(
      String roleName, String resourceId, String vertaId, String resourceTypeName) {
    return roleName + "_" + resourceTypeName + "_" + resourceId + "_" + "User_" + vertaId;
  }

  private InternalFuture<Void> createRoleBindingsForExperimentRun(
      final ExperimentRun experimentRun) {
    ModelDBResourceEnum.ModelDBServiceResourceTypes modelDBServiceResourceType =
        ModelDBResourceEnum.ModelDBServiceResourceTypes.EXPERIMENT_RUN;
    String roleName = ModelDBConstants.ROLE_EXPERIMENT_RUN_OWNER;
    return FutureGrpc.ClientRequest(
            uac.getRoleService()
                .setRoleBinding(
                    SetRoleBinding.newBuilder()
                        .setRoleBinding(
                            RoleBinding.newBuilder()
                                .setName(
                                    buildRoleBindingName(
                                        roleName,
                                        experimentRun.getId(),
                                        experimentRun.getOwner(),
                                        modelDBServiceResourceType.name()))
                                .setScope(RoleScope.newBuilder().build())
                                .setRoleName(roleName)
                                .addEntities(
                                    Entities.newBuilder()
                                        .addUserIds(experimentRun.getOwner())
                                        .build())
                                .addResources(
                                    Resources.newBuilder()
                                        .setService(ServiceEnum.Service.MODELDB_SERVICE)
                                        .setResourceType(
                                            ResourceType.newBuilder()
                                                .setModeldbServiceResourceType(
                                                    modelDBServiceResourceType))
                                        .addResourceIds(experimentRun.getId())
                                        .build())
                                .build())
                        .build()),
            executor)
        .thenAccept(
            response -> {
              LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, response);
            },
            executor);
  }
}
