package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Executor;

public class TagsHandler {
  private static Logger LOGGER = LogManager.getLogger(TagsHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final String entityName;
  private final String entityIdReferenceColumn;

  public TagsHandler(Executor executor, FutureJdbi jdbi, String entityName) {
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

  public InternalFuture<List<String>> getTags(String entityId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "select tags from tag_mapping "
                        + "where entity_name=:entity_name and "
                        + entityIdReferenceColumn
                        + "=:entity_id")
                .bind("entity_id", entityId)
                .bind("entity_name", entityName)
                .mapTo(String.class)
                .list());
  }

  public InternalFuture<MapSubtypes<String>> getTagsMap(Set<String> entityIds) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "select tags, "
                            + entityIdReferenceColumn
                            + " as entity_id from tag_mapping "
                            + "where entity_name=:entity_name and "
                            + entityIdReferenceColumn
                            + " in (<entity_ids>")
                    .bindList("entity_ids", entityIds)
                    .bind("entity_name", entityName)
                    .map(
                        (rs, ctx) ->
                            new AbstractMap.SimpleEntry<>(
                                rs.getString("entity_id"), rs.getString("tags")))
                    .list())
        .thenApply(MapSubtypes::from, executor);
  }

  public InternalFuture<Void> addTags(String entityId, List<String> tags) {
    // Validate input
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              for (final var tag : tags) {
                if (tag.isEmpty()) {
                  throw new InvalidArgumentException("Empty tag");
                }
              }
            },
            executor);

    // TODO: is there a way to push this to the db?
    return currentFuture
        .thenCompose(unused -> getTags(entityId), executor)
        .thenCompose(
            existingTags -> {
              final var tagsSet = new HashSet<>(tags);
              tagsSet.removeAll(existingTags);
              if (tagsSet.isEmpty()) {
                return InternalFuture.completedInternalFuture(null);
              }

              return jdbi.useHandle(
                  handle -> {
                    final var batch =
                        handle.prepareBatch(
                            "insert into tag_mapping (entity_name, tags, "
                                + entityIdReferenceColumn
                                + ") VALUES(:entity_name, :tag, :entity_id)");
                    for (final var tag : tagsSet) {
                      batch
                          .bind("tag", tag)
                          .bind("entity_id", entityId)
                          .bind("entity_name", entityName)
                          .add();
                    }

                    batch.execute();
                  });
            },
            executor);
  }

  public InternalFuture<Void> deleteTags(String entityId, Optional<List<String>> maybeTags) {
    return jdbi.useHandle(
        handle -> {
          var sql =
              "delete from tag_mapping "
                  + "where entity_name=:entity_name and "
                  + entityIdReferenceColumn
                  + "=:entity_id";

          if (maybeTags.isPresent()) {
            sql += " and tags in (<tags>)";
          }

          var query =
              handle.createUpdate(sql).bind("entity_id", entityId).bind("entity_name", entityName);

          if (maybeTags.isPresent()) {
            query = query.bindList("tags", maybeTags.get());
          }

          query.execute();
        });
  }
}
