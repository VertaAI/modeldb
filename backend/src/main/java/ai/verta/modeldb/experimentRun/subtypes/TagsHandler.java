package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.util.*;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TagsHandler {
  private static Logger LOGGER = LogManager.getLogger(TagsHandler.class);
  private static final String ENTITY_ID_QUERY_PARAM = "entity_id";
  private static final String ENTITY_NAME_QUERY_PARAM = "entity_name";

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
                    String.format(
                        "select tags from tag_mapping "
                            + "where entity_name=:entity_name and %s =:entity_id ORDER BY tags ASC",
                        entityIdReferenceColumn))
                .bind(ENTITY_ID_QUERY_PARAM, entityId)
                .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                .mapTo(String.class)
                .list());
  }

  public InternalFuture<MapSubtypes<String>> getTagsMap(Set<String> entityIds) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        String.format(
                            "select tags, %s as entity_id from tag_mapping where entity_name=:entity_name and %s in (<entity_ids>) ORDER BY tags ASC",
                            entityIdReferenceColumn, entityIdReferenceColumn))
                    .bindList("entity_ids", entityIds)
                    .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                    .map(
                        (rs, ctx) ->
                            new AbstractMap.SimpleEntry<>(
                                rs.getString(ENTITY_ID_QUERY_PARAM), rs.getString("tags")))
                    .list())
        .thenApply(MapSubtypes::from, executor);
  }

  public InternalFuture<Void> addTags(String entityId, List<String> tags) {
    // Validate input
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              if (tags.isEmpty()) {
                throw new InvalidArgumentException("Tags not found");
              } else {
                for (String tag : tags) {
                  if (tag.isEmpty()) {
                    throw new InvalidArgumentException("Tag should not be empty");
                  }
                }
              }
            },
            executor);

    // TODO: is there a way to push this to the db?
    return currentFuture
        .thenCompose(unused -> getTags(entityId), executor)
        .thenCompose(
            existingTags -> {
              final var tagsSet = new HashSet<>(ModelDBUtils.checkEntityTagsLength(tags));
              tagsSet.removeAll(existingTags);
              if (tagsSet.isEmpty()) {
                return InternalFuture.completedInternalFuture(null);
              }

              return jdbi.useHandle(
                  handle -> {
                    final var batch =
                        handle.prepareBatch(
                            String.format(
                                "insert into tag_mapping (entity_name, tags, %s) VALUES(:entity_name, :tag, :entity_id)",
                                entityIdReferenceColumn));
                    for (final var tag : tagsSet) {
                      batch
                          .bind("tag", tag)
                          .bind(ENTITY_ID_QUERY_PARAM, entityId)
                          .bind(ENTITY_NAME_QUERY_PARAM, entityName)
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
              String.format(
                  "delete from tag_mapping where entity_name=:entity_name and %s =:entity_id",
                  entityIdReferenceColumn);

          if (maybeTags.isPresent()) {
            sql += " and tags in (<tags>)";
          }

          var query =
              handle
                  .createUpdate(sql)
                  .bind(ENTITY_ID_QUERY_PARAM, entityId)
                  .bind(ENTITY_NAME_QUERY_PARAM, entityName);

          if (maybeTags.isPresent()) {
            query = query.bindList("tags", maybeTags.get());
          }

          query.execute();
        });
  }
}
