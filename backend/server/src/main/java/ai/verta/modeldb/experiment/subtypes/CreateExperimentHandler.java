package ai.verta.modeldb.experiment.subtypes;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.modeldb.CreateExperiment;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.futures.*;
import ai.verta.modeldb.common.handlers.TagsHandlerBase;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.utils.HandlerUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.UserInfo;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreateExperimentHandler extends HandlerUtil {

  private static final Logger LOGGER = LogManager.getLogger(CreateExperimentHandler.class);

  private final FutureExecutor executor;
  private final FutureJdbi jdbi;

  private final AttributeHandler attributeHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final CodeVersionHandler codeVersionHandler;

  public CreateExperimentHandler(
      FutureExecutor executor,
      FutureJdbi jdbi,
      AttributeHandler attributeHandler,
      TagsHandler tagsHandler,
      ArtifactHandler artifactHandler) {
    this.executor = executor;
    this.jdbi = jdbi;

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

    if (request.getProjectId().isEmpty()) {
      var errorMessage = "Project ID not found in CreateExperiment request";
      throw new InvalidArgumentException(errorMessage);
    }

    if (request.getName().isEmpty()) {
      request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
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
            .addAllAttributes(
                request.getAttributesList().stream()
                    .sorted(Comparator.comparing(KeyValue::getKey))
                    .collect(Collectors.toList()))
            .addAllTags(TagsHandlerBase.checkEntityTagsLength(request.getTagsList()))
            .addAllArtifacts(
                request.getArtifactsList().stream()
                    .sorted(Comparator.comparing(Artifact::getKey))
                    .collect(Collectors.toList()))
            .setVersionNumber(1L);

    if (request.getDateCreated() != 0L) {
      experimentBuilder
          .setDateCreated(request.getDateCreated())
          .setDateUpdated(request.getDateCreated());
    } else {
      experimentBuilder
          .setDateCreated(Instant.now().toEpochMilli())
          .setDateUpdated(Instant.now().toEpochMilli());
    }
    if (userInfo != null) {
      experimentBuilder.setOwner(userInfo.getVertaInfo().getUserId());
    }

    return experimentBuilder.build();
  }

  public InternalFuture<Experiment> insertExperiment(Experiment newExperiment) {
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

    Supplier<InternalFuture<Experiment>> insertFutureSupplier =
        () -> { // Inserting fields arguments based on the keys and value of map
          return InternalFuture.fromFuture(
              jdbi.inTransaction(
                  handle -> {
                    final var builder = newExperiment.toBuilder();
                    Boolean exists = checkInsertedEntityAlreadyExists(handle, newExperiment);
                    if (exists) {
                      throw new AlreadyExistsException(
                          "Experiment '" + builder.getName() + "' already exists in database");
                    }

                    String queryString = buildInsertQuery(valueMap, "experiment");

                    LOGGER.trace("insert experiment query string: " + queryString);
                    try (var query = handle.createUpdate(queryString)) {

                      // Inserting fields arguments based on the keys and value of map
                      for (Map.Entry<String, Object> objectEntry : valueMap.entrySet()) {
                        query.bind(objectEntry.getKey(), objectEntry.getValue());
                      }

                      int count = query.execute();
                      LOGGER.trace("Experiment Inserted : " + (count > 0));
                    }

                    if (!builder.getTagsList().isEmpty()) {
                      tagsHandler.addTags(handle, builder.getId(), builder.getTagsList());
                    }
                    if (!builder.getAttributesList().isEmpty()) {
                      attributeHandler.logKeyValues(
                          handle, builder.getId(), builder.getAttributesList());
                    }
                    if (!builder.getArtifactsList().isEmpty()) {
                      var updatedArtifacts =
                          artifactHandler.logArtifacts(
                              handle, builder.getId(), builder.getArtifactsList(), false);
                      builder.clearArtifacts().addAllArtifacts(updatedArtifacts).build();
                    }
                    if (builder.getCodeVersionSnapshot().hasCodeArchive()
                        || builder.getCodeVersionSnapshot().hasGitSnapshot()) {
                      codeVersionHandler.logCodeVersion(
                          handle, builder.getId(), false, builder.getCodeVersionSnapshot());
                    }
                    return builder.build();
                  }));
        };
    return InternalFuture.retriableStage(insertFutureSupplier, CommonDBUtil::needToRetry, executor)
        .thenCompose(
            createdExperiment ->
                InternalFuture.fromFuture(
                        jdbi.run(
                            handle -> {
                              try (var updateQuery =
                                  handle.createUpdate(
                                      "UPDATE experiment SET created=:created WHERE id=:id")) {
                                updateQuery
                                    .bind("created", true)
                                    .bind("id", createdExperiment.getId())
                                    .execute();
                              }
                            }))
                    .thenApply(unused -> createdExperiment, executor),
            executor);
  }

  private Boolean checkInsertedEntityAlreadyExists(Handle handle, Experiment experiment) {
    String queryStr =
        "SELECT count(id) FROM experiment WHERE "
            + " name = :experimentName "
            + " AND project_id = :projectId "
            + " AND deleted = :deleted ";

    try (var query = handle.createQuery(queryStr)) {
      query.bind("experimentName", experiment.getName());
      query.bind("projectId", experiment.getProjectId());
      query.bind("deleted", false);

      long count = query.mapTo(Long.class).one();
      return count > 0;
    }
  }
}
