package ai.verta.modeldb.experiment.subtypes;

import ai.verta.modeldb.CreateExperiment;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.LogExperimentRunCodeVersion;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.handlers.TagsHandlerBase;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.utils.HandlerUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.UserInfo;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

public class CreateExperimentHandler extends HandlerUtil {

  private static Logger LOGGER = LogManager.getLogger(CreateExperimentHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final Config config;

  private final AttributeHandler attributeHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final CodeVersionHandler codeVersionHandler;

  public CreateExperimentHandler(
      Executor executor,
      FutureJdbi jdbi,
      Config config,
      AttributeHandler attributeHandler,
      TagsHandler tagsHandler,
      ArtifactHandler artifactHandler) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.config = config;

    this.attributeHandler = attributeHandler;
    this.tagsHandler = tagsHandler;
    this.artifactHandler = artifactHandler;
    this.codeVersionHandler = new CodeVersionHandler(executor, jdbi, "experiment");
  }

  public InternalFuture<Experiment> convertCreateRequest(
      final CreateExperiment request, UserInfo userInfo) {
    return InternalFuture.completedInternalFuture(getExperimentFromRequest(request, userInfo));
  }

  /**
   * Method to convert createExperiment request to Experiment object. This method generates the
   * experiment Id using UUID and puts it in Experiment object.
   *
   * @param request : CreateExperiment
   * @return Experiment
   */
  private Experiment getExperimentFromRequest(CreateExperiment request, UserInfo userInfo) {

    String errorMessage = null;
    if (request.getProjectId().isEmpty()) {
      errorMessage = "Project ID not found in CreateExperiment request";
    } else if (request.getName().isEmpty()) {
      request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
    }

    if (errorMessage != null) {
      throw new InvalidArgumentException(errorMessage);
    }

    /*
     * Create Experiment entity from given CreateExperiment request. generate UUID and put as id in
     * Experiment for uniqueness.
     */
    var experimentBuilder =
        Experiment.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setProjectId(request.getProjectId())
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setDescription(request.getDescription())
            .addAllAttributes(request.getAttributesList())
            .addAllTags(TagsHandlerBase.checkEntityTagsLength(request.getTagsList()))
            .addAllArtifacts(request.getArtifactsList())
            .setVersionNumber(1L);

    if (request.getDateCreated() != 0L) {
      experimentBuilder
          .setDateCreated(request.getDateCreated())
          .setDateUpdated(request.getDateCreated());
    } else {
      experimentBuilder
          .setDateCreated(Calendar.getInstance().getTimeInMillis())
          .setDateUpdated(Calendar.getInstance().getTimeInMillis());
    }
    if (userInfo != null) {
      experimentBuilder.setOwner(userInfo.getVertaInfo().getUserId());
    }

    return experimentBuilder.build();
  }

  public InternalFuture<Experiment> insertExperiment(Experiment newExperiment) {
    final var now = Calendar.getInstance().getTimeInMillis();
    Map<String, Object> valueMap = new LinkedHashMap<>();
    valueMap.put("id", newExperiment.getId());
    valueMap.put("project_id", newExperiment.getProjectId());
    valueMap.put("name", newExperiment.getName());
    valueMap.put("description", newExperiment.getDescription());
    valueMap.put("date_created", newExperiment.getDateCreated());
    valueMap.put("date_updated", newExperiment.getDateUpdated());
    valueMap.put("owner", newExperiment.getOwner());
    valueMap.put("version_number", newExperiment.getVersionNumber());

    valueMap.put("deleted", false);
    valueMap.put("created", false);

    return jdbi.useHandle(
            handle ->
                handle.useTransaction(
                    TransactionIsolationLevel.SERIALIZABLE,
                    handleForTransaction -> {
                      String queryString = buildInsertQuery(valueMap, "experiment");

                      LOGGER.trace("insert experiment query string: " + queryString);
                      var query = handleForTransaction.createUpdate(queryString);

                      // Inserting fields arguments based on the keys and value of map
                      for (Map.Entry<String, Object> objectEntry : valueMap.entrySet()) {
                        query.bind(objectEntry.getKey(), objectEntry.getValue());
                      }

                      try {
                        int count = query.execute();
                        LOGGER.trace("Experiment Inserted : " + (count > 0));
                      } catch (UnableToExecuteStatementException exception) {
                        // take a brief pause before resubmitting its query/transaction
                        Thread.sleep(config.getJdbi_retry_time()); // Time in ms
                        LOGGER.trace("Retry to insert Experiment");
                        int count = query.execute();
                        LOGGER.trace("Experiment Inserted after retry : " + (count > 0));
                      }

                      if (!newExperiment.getTagsList().isEmpty()) {
                        tagsHandler.addTags(
                            handleForTransaction,
                            newExperiment.getId(),
                            newExperiment.getTagsList());
                      }
                      if (!newExperiment.getAttributesList().isEmpty()) {
                        attributeHandler.logKeyValues(
                            handleForTransaction,
                            newExperiment.getId(),
                            newExperiment.getAttributesList());
                      }
                      if (!newExperiment.getArtifactsList().isEmpty()) {
                        artifactHandler.logArtifacts(
                            handleForTransaction,
                            newExperiment.getId(),
                            newExperiment.getArtifactsList(),
                            false);
                      }
                      if (newExperiment.getCodeVersionSnapshot().hasCodeArchive()
                          || newExperiment.getCodeVersionSnapshot().hasGitSnapshot()) {
                        codeVersionHandler.logCodeVersion(
                            handleForTransaction,
                            LogExperimentRunCodeVersion.newBuilder()
                                .setId(newExperiment.getId())
                                .setOverwrite(false)
                                .setCodeVersion(newExperiment.getCodeVersionSnapshot())
                                .build());
                      }
                    }))
        .thenApply(unused -> newExperiment, executor);
  }
}
