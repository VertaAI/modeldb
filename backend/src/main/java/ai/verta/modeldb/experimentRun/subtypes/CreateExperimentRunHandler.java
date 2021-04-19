package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.CreateExperimentRun;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.experimentRun.FutureExperimentRunDAO;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.TrialUtils;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.PythonEnvironmentBlob;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import ai.verta.uac.Empty;
import ai.verta.uac.Entities;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ResourceType;
import ai.verta.uac.Resources;
import ai.verta.uac.RoleBinding;
import ai.verta.uac.RoleScope;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.SetRoleBinding;
import ai.verta.uac.UserInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

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

    public CreateExperimentRunHandler(Executor executor, FutureJdbi jdbi, UAC uac) {
        this.executor = executor;
        this.jdbi = jdbi;
        this.uac = uac;

        attributeHandler = new AttributeHandler(executor, jdbi, "ExperimentRunEntity");
        hyperparametersHandler =
                new KeyValueHandler(executor, jdbi, "hyperparameters", "ExperimentRunEntity");
        metricsHandler = new KeyValueHandler(executor, jdbi, "metrics", "ExperimentRunEntity");
        observationHandler = new ObservationHandler(executor, jdbi);
        tagsHandler = new TagsHandler(executor, jdbi, "ExperimentRunEntity");
        artifactHandler = new ArtifactHandler(executor, jdbi, "artifacts", "ExperimentRunEntity");
    }

    private InternalFuture<UserInfo> getCurrentLoginUserInfo() {
        return FutureGrpc.ClientRequest(
                uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor);
    }

    public InternalFuture<ExperimentRun> createExperimentRun(final CreateExperimentRun request) {
        // Validate arguments
        var futureTask =
                InternalFuture.runAsync(
                        () -> {
                            String errorMessage = null;
                            if (request.getProjectId().isEmpty() && request.getExperimentId().isEmpty()) {
                                errorMessage =
                                        "Project ID and Experiment ID not found in CreateExperimentRun request";
                            } else if (request.getProjectId().isEmpty()) {
                                errorMessage = "Project ID not found in CreateExperimentRun request";
                            } else if (request.getExperimentId().isEmpty()) {
                                errorMessage = "Experiment ID not found in CreateExperimentRun request";
                            }

                            if (errorMessage != null) {
                                throw new InvalidArgumentException(errorMessage);
                            }
                        },
                        executor);

    return futureTask
        .thenCompose(
            unused ->
                TrialUtils.futureValidateExperimentRunPerWorkspaceForTrial(config.trial, executor),
            executor)
        .thenCompose(
            unused ->
                FutureGrpc.ClientRequest(
                    uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor),
            executor)
        .thenCompose(
            currentLoginUserInfo -> {
              final var experimentRun = getExperimentRunFromRequest(request, currentLoginUserInfo);
              TrialUtils.validateMaxArtifactsForTrial(
                  config.trial, experimentRun.getArtifactsCount(), 0);
                checkInsertedEntityAlreadyExists(experimentRun)
                        .thenAccept(exists -> {
                            if (exists) {
                                throw new AlreadyExistsException(
                                        "ExperimentRun already exists in database");
                            }
                        }, executor).thenRun(() -> insertExperimentRun(experimentRun), executor);
            },
            executor)
        /*.thenCompose(
            experimentRun -> {
              // TODO: Fix below logic for checking privileges of linked dataset versions
              *//*if (experimentRun.getDatasetsCount() > 0 && config.populateConnectionsBasedOnPrivileges) {
                experimentRun = checkDatasetVersionBasedOnPrivileges(experimentRun, true);
              }*//*
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
        .thenCompose(this::insertExperimentRun, executor)
        .thenCompose(experimentRun -> createRoleBindingsForExperimentRun(experimentRun), executor);
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

        if (request.getDateCreated() != 0L) {
            experimentRunBuilder
                    .setDateCreated(request.getDateCreated())
                    .setDateUpdated(request.getDateCreated());
        } else {
            experimentRunBuilder
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .setDateUpdated(Calendar.getInstance().getTimeInMillis());
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
                handle -> {
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
                    // TODO: code version snapshot
                    /*runValueMap.put("code_version_snapshot_id", newExperimentRun.getCodeVersionSnapshot());*/
                    runValueMap.put("job_id", newExperimentRun.getJobId());
                    runValueMap.put("parent_id", newExperimentRun.getParentId());
                    runValueMap.put("owner", newExperimentRun.getOwner());

                    EnvironmentBlob environmentBlob =
                            sortPythonEnvironmentBlob(newExperimentRun.getEnvironment());
                    runValueMap.put(
                            "environment", ModelDBUtils.getStringFromProtoObject(environmentBlob));
                    runValueMap.put("deleted", false);

                    String[] fieldsArr = runValueMap.keySet().toArray(new String[0]);
                    String commaFields = String.join(",", fieldsArr);

                    StringBuilder queryStrBuilder =
                            new StringBuilder("insert into experiment_run ( ")
                                    .append(commaFields)
                                    .append(") values (");

                    for (int i = 0; i < fieldsArr.length; i++) {
                        queryStrBuilder.append(":").append(fieldsArr[i]);
                        if (i < fieldsArr.length - 1) {
                            queryStrBuilder.append(",");
                        }
                    }
                    queryStrBuilder.append(" ) ");

                    LOGGER.trace("insert experiment run query string: " + queryStrBuilder.toString());
                    var query = handle.createUpdate(queryStrBuilder.toString());
                    for (Map.Entry<String, Object> objectEntry : runValueMap.entrySet()) {
                        query.bind(objectEntry.getKey(), objectEntry.getValue());
                    }
                    query.execute();
                })
                .thenCompose(
                        handle -> tagsHandler.addTags(newExperimentRun.getId(), newExperimentRun.getTagsList()),
                        executor)
                .thenCompose(
                        handle ->
                                attributeHandler.logKeyValues(
                                        newExperimentRun.getId(), newExperimentRun.getAttributesList()),
                        executor)
                .thenCompose(
                        handle ->
                                hyperparametersHandler.logKeyValues(
                                        newExperimentRun.getId(), newExperimentRun.getHyperparametersList()),
                        executor)
                .thenCompose(
                        handle ->
                                metricsHandler.logKeyValues(
                                        newExperimentRun.getId(), newExperimentRun.getMetricsList()),
                        executor)
                // TODO .thenCompose(handle -> artifactHandler.logArtifacts(newExperimentRun.getId(),
                // newExperimentRun.getArtifactsList()), executor)
                // TODO .thenCompose(handle -> datasetHandler.logDatasets(newExperimentRun.getId(),
                // newExperimentRun.getDatasetsList()), executor)
                .thenCompose(
                        handle ->
                                observationHandler.logObservations(
                                        newExperimentRun.getId(), newExperimentRun.getObservationsList(), now),
                        executor)
                // TODO .thenCompose(handle -> featureHandler.logFeatures(newExperimentRun.getId(),
                // newExperimentRun.getFeaturesList()), executor)
                // TODO .thenCompose(handle -> addCodeVersionSnapShot(), executor)
                // TODO .thenCompose(handle -> versioned_inputs, executor)

                .thenCompose(unused -> InternalFuture.completedInternalFuture(newExperimentRun), executor);
    }

    private EnvironmentBlob sortPythonEnvironmentBlob(EnvironmentBlob environmentBlob) {
        EnvironmentBlob.Builder builder = environmentBlob.toBuilder();
        if (builder.hasPython()) {
            PythonEnvironmentBlob.Builder pythonEnvironmentBlobBuilder = builder.getPython().toBuilder();

            // Compare requirementEnvironmentBlobs
            List<PythonRequirementEnvironmentBlob> requirementEnvironmentBlobs =
                    new ArrayList<>(pythonEnvironmentBlobBuilder.getRequirementsList());
            requirementEnvironmentBlobs.sort(
                    Comparator.comparing(PythonRequirementEnvironmentBlob::getLibrary));
            pythonEnvironmentBlobBuilder
                    .clearRequirements()
                    .addAllRequirements(requirementEnvironmentBlobs);

            // Compare
            List<PythonRequirementEnvironmentBlob> constraintsBlobs =
                    new ArrayList<>(pythonEnvironmentBlobBuilder.getConstraintsList());
            constraintsBlobs.sort(Comparator.comparing(PythonRequirementEnvironmentBlob::getLibrary));
            pythonEnvironmentBlobBuilder.clearConstraints().addAllConstraints(constraintsBlobs);

            builder.setPython(pythonEnvironmentBlobBuilder.build());
        }
        return builder.build();
    }

    private InternalFuture<Boolean> checkInsertedEntityAlreadyExists(
            ExperimentRun experimentRun) {
        return jdbi.withHandle(handle -> {
            String queryStr = "SELECT count(id) FROM experiment_run WHERE "
                    + " name = :experimentRunName "
                    + " AND project_id = :projectId "
                    + " AND experiment_id = :experimentId "
                    + " AND deleted = false ";

            var query = handle.createQuery(queryStr)
                    .bind("experimentRunName", experimentRun.getName());
            query.bind("projectId", experimentRun.getProjectId());
            query.bind("experimentId", experimentRun.getExperimentId());

            long count = query.mapTo(Long.class).one();
            boolean existStatus = false;
            if (count > 0) {
                existStatus = true;
            }

            // Throw error if it is an insert request and ExperimentRun with same name already
            // exists
            return existStatus;
        });
    }

    private String buildRoleBindingName(
            String roleName, String resourceId, String vertaId, String resourceTypeName) {
        return roleName + "_" + resourceTypeName + "_" + resourceId + "_" + "User_" + vertaId;
    }

    private InternalFuture<ExperimentRun> createRoleBindingsForExperimentRun(
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
                        executor)
                .thenCompose(unused -> InternalFuture.completedInternalFuture(experimentRun), executor);
    }
}
