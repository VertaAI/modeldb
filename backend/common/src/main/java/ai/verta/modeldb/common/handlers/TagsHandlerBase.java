package ai.verta.modeldb.common.handlers;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.Handle;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class TagsHandlerBase<T> {
  private static final Logger LOGGER = LogManager.getLogger(TagsHandlerBase.class);
  protected static final String ENTITY_ID_QUERY_PARAM = "entity_id";
  private static final String ENTITY_NAME_QUERY_PARAM = "entity_name";

  private final FutureJdbi jdbi;
  private final String entityName;
  protected String entityIdReferenceColumn;

  public TagsHandlerBase(FutureJdbi jdbi, String entityName) {
    this.jdbi = jdbi;
    this.entityName = entityName;
    setEntityIdReferenceColumn(entityName);
  }

  public static List<String> checkEntityTagsLength(List<String> tags) {
    for (String tag : tags) {
      if (tag.isEmpty()) {
        var errorMessage = "Invalid tag found, Tag shouldn't be empty";
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      } else if (tag.length() > CommonConstants.TAG_LENGTH) {
        String errorMessage =
            "Tag name can not be more than "
                + CommonConstants.TAG_LENGTH
                + " characters. Limit exceeded tag is: "
                + tag;
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }
    }
    return tags;
  }

  protected abstract void setEntityIdReferenceColumn(String entityName);

  public Future<List<String>> getTags(T entityId) {
    return jdbi.call(handle -> getTags(entityId, handle))
        .thenApply(tags -> tags.stream().sorted().collect(Collectors.toList()));
  }

  private List<String> getTags(T entityId, Handle handle) {
    return handle
        .createQuery(
            String.format(
                "select tags from tag_mapping "
                    + "where entity_name=:entity_name and %s =:entity_id ORDER BY tags ASC",
                entityIdReferenceColumn))
        .bind(ENTITY_ID_QUERY_PARAM, entityId)
        .bind(ENTITY_NAME_QUERY_PARAM, entityName)
        .mapTo(String.class)
        .list();
  }

  public Future<MapSubtypes<T, String>> getTagsMap(Set<T> entityIds) {
    return jdbi.call(
            handle ->
                handle
                    .createQuery(
                        String.format(
                            "select tags, %s as entity_id from tag_mapping where entity_name=:entity_name and %s in (<entity_ids>) ORDER BY tags ASC",
                            entityIdReferenceColumn, entityIdReferenceColumn))
                    .bindList("entity_ids", entityIds)
                    .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                    .map((rs, ctx) -> getSimpleEntryFromResultSet(rs))
                    .list())
        .thenApply(
            simpleEntries ->
                MapSubtypes.from(
                    simpleEntries.stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toList())));
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
      handle
          .createUpdate(
              String.format(
                  "insert into tag_mapping (entity_name, tags, %s) VALUES(:entity_name, :tag, :entity_id)",
                  entityIdReferenceColumn))
          .bind("tag", tag)
          .bind(ENTITY_ID_QUERY_PARAM, entityId)
          .bind(ENTITY_NAME_QUERY_PARAM, entityName)
          .execute();
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

    var query =
        handle
            .createUpdate(sql)
            .bind(ENTITY_ID_QUERY_PARAM, entityId)
            .bind(ENTITY_NAME_QUERY_PARAM, entityName);

    if (maybeTags.isPresent()) {
      query = query.bindList("tags", maybeTags.get());
    }

    query.execute();
  }
}
