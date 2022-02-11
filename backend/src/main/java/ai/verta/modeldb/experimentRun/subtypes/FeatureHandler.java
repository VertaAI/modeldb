package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.Feature;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.subtypes.MapSubtypes;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;

public class FeatureHandler {

  private static Logger LOGGER = LogManager.getLogger(FeatureHandler.class);
  private static final String ENTITY_ID_QUERY_PARAM = "entity_id";
  private static final String ENTITY_NAME_QUERY_PARAM = "entity_name";

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

  private List<String> getFeatures(Handle handle, String entityId) {
    return handle
        .createQuery(
            "select feature from feature "
                + "where entity_name=:entity_name and "
                + entityIdReferenceColumn
                + "=:entity_id")
        .bind(ENTITY_ID_QUERY_PARAM, entityId)
        .bind(ENTITY_NAME_QUERY_PARAM, entityName)
        .mapTo(String.class)
        .list();
  }

  public void logFeatures(Handle handle, String entityId, List<Feature> features) {
    // Validate input
    for (final var feature : features) {
      if (feature.getName().isEmpty()) {
        throw new InvalidArgumentException("Empty feature");
      }
    }

    // TODO: is there a way to push this to the db?
    var existingFeatures = getFeatures(handle, entityId);
    final var featuresSet =
        features.stream().map(Feature::getName).collect(Collectors.toCollection(HashSet::new));
    featuresSet.removeAll(existingFeatures);
    if (featuresSet.isEmpty()) {
      return;
    }

    final var batch =
        handle.prepareBatch(
            "insert into feature (entity_name, feature, "
                + entityIdReferenceColumn
                + ") VALUES(:entity_name, :feature, :entity_id)");
    for (final var feature : featuresSet) {
      batch
          .bind("feature", feature)
          .bind(ENTITY_ID_QUERY_PARAM, entityId)
          .bind(ENTITY_NAME_QUERY_PARAM, entityName)
          .add();
    }

    batch.execute();
  }

  public InternalFuture<MapSubtypes<String, Feature>> getFeaturesMap(Set<String> entityIds) {
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
                .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                .map(
                    (rs, ctx) ->
                        new AbstractMap.SimpleEntry<>(
                            rs.getString(ENTITY_ID_QUERY_PARAM),
                            Feature.newBuilder().setName(rs.getString("feature")).build()))
                .list())
        .thenApply(MapSubtypes::from, executor);
  }
}
