package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.Feature;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FeatureHandler {
  private static Logger LOGGER = LogManager.getLogger(FeatureHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final String entityName;
  private final String entityIdReferenceColumn;

  public FeatureHandler(Executor executor, FutureJdbi jdbi, String entityName) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.entityName = entityName;

    switch (entityName) {
      case "ProjectEntity":
        this.entityIdReferenceColumn = "project_id";
        break;
      case "ExperimentRunEntity":
        this.entityIdReferenceColumn = "experiment_run_id";
        break;
      default:
        throw new InternalErrorException("Invalid entity name: " + entityName);
    }
  }

  public InternalFuture<List<String>> getFeatures(String entityId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "select feature from feature "
                        + "where entity_name=:entity_name and "
                        + entityIdReferenceColumn
                        + "=:entity_id")
                .bind("entity_id", entityId)
                .bind("entity_name", entityName)
                .mapTo(String.class)
                .list());
  }

  public InternalFuture<Void> logFeatures(String entityId, List<Feature> features) {
    // Validate input
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              for (final var feature : features) {
                if (feature.getName().isEmpty()) {
                  throw new InvalidArgumentException("Empty feature");
                }
              }
            },
            executor);

    // TODO: is there a way to push this to the db?
    return currentFuture
        .thenCompose(unused -> getFeatures(entityId), executor)
        .thenCompose(
            existingFeatures -> {
              final var featuresSet =
                  features.stream()
                      .map(Feature::getName)
                      .collect(Collectors.toCollection(HashSet::new));
              featuresSet.removeAll(existingFeatures);
              if (featuresSet.isEmpty()) {
                return InternalFuture.completedInternalFuture(null);
              }

              return jdbi.useHandle(
                  handle -> {
                    final var batch =
                        handle.prepareBatch(
                            "insert into feature (entity_name, feature, "
                                + entityIdReferenceColumn
                                + ") VALUES(:entity_name, :feature, :entity_id)");
                    for (final var feature : featuresSet) {
                      batch
                          .bind("feature", feature)
                          .bind("entity_id", entityId)
                          .bind("entity_name", entityName)
                          .add();
                    }

                    batch.execute();
                  });
            },
            executor);
  }

  public InternalFuture<MapSubtypes<Feature>> getFeaturesMap(Set<String> entityIds) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "select feature, "
                            + entityIdReferenceColumn
                            + " as entity_id from feature "
                            + "where entity_name=:entity_name and "
                            + entityIdReferenceColumn
                            + " in (<entity_ids>)")
                    .bindList("entity_ids", entityIds)
                    .bind("entity_name", entityName)
                    .map(
                        (rs, ctx) ->
                            new AbstractMap.SimpleEntry<>(
                                rs.getString("entity_id"),
                                Feature.newBuilder().setName(rs.getString("feature")).build()))
                    .list())
        .thenApply(MapSubtypes::from, executor);
  }
}
