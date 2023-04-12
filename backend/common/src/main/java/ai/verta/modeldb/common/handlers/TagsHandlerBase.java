package ai.verta.modeldb.common.handlers;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.Handle;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.subtypes.MapSubtypes;
import com.google.rpc.Code;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class TagsHandlerBase<T> {
  protected static final String ENTITY_ID_QUERY_PARAM = "entity_id";
  private static final String ENTITY_NAME_QUERY_PARAM = "entity_name";

  private final FutureExecutor executor;
  private final FutureJdbi jdbi;
  private final String entityName;
  protected String entityIdReferenceColumn;

  public TagsHandlerBase(FutureExecutor executor, FutureJdbi jdbi, String entityName) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.entityName = entityName;
    setEntityIdReferenceColumn(entityName);
  }

  public static List<String> checkEntityTagsLength(List<String> tags) {
    for (String tag : tags) {
      if (tag.isEmpty()) {
        var errorMessage = "Invalid tag found, Tag shouldn't be empty";
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      } else if (tag.length() > 40) {
        String errorMessage =
            "Tag name can not be more than " + 40 + " characters. Limit exceeded tag is: " + tag;
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }
    }
    return tags;
  }

  protected abstract void setEntityIdReferenceColumn(String entityName);

  public InternalFuture<List<String>> getTags(T entityId) {
    return jdbi.withHandle(handle -> getTags(entityId, handle))
        .thenApply(tags -> tags.stream().sorted().collect(Collectors.toList()), executor);
  }

  private List<String> getTags(T entityId, Handle handle) {
    try (var query =
        handle.createQuery(
            String.format(
                "select tags from tag_mapping "
                    + "where entity_name=:entity_name and %s =:entity_id ORDER BY tags ASC",
                entityIdReferenceColumn))) {
      return query
          .bind(ENTITY_ID_QUERY_PARAM, entityId)
          .bind(ENTITY_NAME_QUERY_PARAM, entityName)
          .mapTo(String.class)
          .list();
    }
  }

  public InternalFuture<MapSubtypes<T, String>> getTagsMap(Set<T> entityIds) {
    return jdbi.withHandle(
            handle -> {
              try (var query =
                  handle.createQuery(
                      String.format(
                          "select tags, %s as entity_id from tag_mapping where entity_name=:entity_name and %s in (<entity_ids>) ORDER BY tags ASC",
                          entityIdReferenceColumn, entityIdReferenceColumn))) {
                return query
                    .bindList("entity_ids", entityIds)
                    .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                    .map((rs, ctx) -> getSimpleEntryFromResultSet(rs))
                    .list();
              }
            })
        .thenApply(
            simpleEntries ->
                simpleEntries.stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toList()),
            executor)
        .thenApply(MapSubtypes::from, executor);
  }

  protected abstract AbstractMap.SimpleEntry<T, String> getSimpleEntryFromResultSet(ResultSet rs)
      throws SQLException;

  public void addTags(Handle handle, T entityId, List<String> tags) {
    // Validate input
    if (tags.isEmpty()) {
      throw new ModelDBException("Tags not found", Code.INVALID_ARGUMENT);
    } else {
      for (String tag : tags) {
        if (tag.isEmpty()) {
          throw new ModelDBException("Tag should not be empty", Code.INVALID_ARGUMENT);
        }
      }
    }

    // TODO: is there a way to push this to the db?
    var existingTags = getTags(entityId, handle);
    final var tagsSet = new HashSet<>(checkEntityTagsLength(tags));
    tagsSet.removeAll(existingTags);
    if (tagsSet.isEmpty()) {
      return;
    }

    for (final var tag : tagsSet) {
      try (var updateQuery =
          handle.createUpdate(
              String.format(
                  "insert into tag_mapping (entity_name, tags, %s) VALUES(:entity_name, :tag, :entity_id)",
                  entityIdReferenceColumn))) {
        updateQuery
            .bind("tag", tag)
            .bind(ENTITY_ID_QUERY_PARAM, entityId)
            .bind(ENTITY_NAME_QUERY_PARAM, entityName)
            .execute();
      }
    }
  }

  public void deleteTags(Handle handle, T entityId, Optional<List<String>> maybeTags) {
    var sql =
        String.format(
            "delete from tag_mapping where entity_name=:entity_name and %s =:entity_id",
            entityIdReferenceColumn);

    if (maybeTags.isPresent()) {
      sql += " and tags in (<tags>)";
    }

    try (var query = handle.createUpdate(sql)) {
      query.bind(ENTITY_ID_QUERY_PARAM, entityId).bind(ENTITY_NAME_QUERY_PARAM, entityName);
      maybeTags.ifPresent(maybeTagList -> query.bindList("tags", maybeTagList));
      query.execute();
    }
  }
}
