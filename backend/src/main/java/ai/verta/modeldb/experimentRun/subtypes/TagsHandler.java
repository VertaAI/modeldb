package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

public class TagsHandler {
  private static Logger LOGGER = LogManager.getLogger(TagsHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;

  public TagsHandler(Executor executor, FutureJdbi jdbi) {
    this.executor = executor;
    this.jdbi = jdbi;
  }

  public InternalFuture<List<String>> getTags(String runId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "select tags from tag_mapping "
                        + "where entity_name=\"ExperimentRunEntity\" and experiment_run_id=:run_id")
                .bind("run_id", runId)
                .mapTo(String.class)
                .list());
  }

  public InternalFuture<Void> addTags(String runId, List<String> tags) {
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
        .thenCompose(unused -> getTags(runId), executor)
        .thenCompose(
            existingTags -> {
              tags.removeAll(existingTags);

              return jdbi.useHandle(
                  handle -> {
                    final var batch =
                        handle.prepareBatch(
                            "insert into tag_mapping (entity_name, tags, experiment_run_id) VALUES(\"ExperimentRunEntity\", :tag, :run_id)");
                    for (final var tag : tags) {
                      batch.bind("tag", tag).bind("run_id", runId).add();
                    }

                    batch.execute();
                  });
            },
            executor);
  }

  public InternalFuture<Void> deleteTags(String runId, Optional<List<String>> maybeTags) {
    return jdbi.useHandle(
        handle -> {
          var sql =
              "delete from tag_mapping "
                  + "where entity_name=\"ExperimentRunEntity\" and experiment_run_id=:run_id";

          if (maybeTags.isPresent()) {
            sql += " and tags in (<tags>)";
          }

          var query = handle.createUpdate(sql).bind("run_id", runId);

          if (maybeTags.isPresent()) {
            query = query.bindList("tags", maybeTags.get());
          }

          query.execute();
        });
  }
}
