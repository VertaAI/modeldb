package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.Artifact;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.interfaces.CheckEntityPermissionBasedOnResourceTypesFunction;
import ai.verta.uac.ModelDBActionEnum;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FilterPrivilegedDatasetsHandler {
  private static Logger LOGGER = LogManager.getLogger(FilterPrivilegedDatasetsHandler.class);

  private final FutureExecutor executor;
  private final FutureJdbi jdbi;

  public FilterPrivilegedDatasetsHandler(FutureExecutor executor, FutureJdbi jdbi) {
    this.executor = executor;
    this.jdbi = jdbi;
  }

  /**
   * @param datasets : datasets for privilege check
   * @param errorOut : Throw error while creation (true) otherwise we will keep it silent (false) at
   *     fetch time
   * @return {@link List} : accessible datasets
   */
  public Future<List<Artifact>> filterAndGetPrivilegedDatasetsOnly(
      List<Artifact> datasets,
      boolean errorOut,
      CheckEntityPermissionBasedOnResourceTypesFunction permissionCheck) {
    Set<String> linkedDatasetVersionIds = new HashSet<>();
    for (Artifact dataset : datasets) {
      String datasetVersionId = dataset.getLinkedArtifactId();
      if (!datasetVersionId.isEmpty()) {
        linkedDatasetVersionIds.add(datasetVersionId);
      }
    }

    // If linked dataset version ids do not exist in dataset then no need to check dataset
    // authorization.
    if (linkedDatasetVersionIds.isEmpty()) {
      return Future.of(datasets);
    }
    return jdbi.call(
            handle -> {
              // We have dataset version as a commit and dataset as a repository in MDB so added
              // commit_hash filter here
              return handle
                  .createQuery(
                      " SELECT commit_hash, repository_id FROM repository_commit WHERE commit_hash IN (<linkedDatasetVersionIds>) ")
                  .bindList("linkedDatasetVersionIds", linkedDatasetVersionIds)
                  .map(
                      (rs, ctx) ->
                          new AbstractMap.SimpleEntry<>(
                              rs.getString("commit_hash"), rs.getLong("repository_id")))
                  .list();
            })
        .thenCompose(
            datasetVersionDatasetList -> {
              // Convert list of dataset and dataset version mapping into Map
              Map<String, String> datasetVersionDatasetMap = new HashMap<>();
              for (var entry : datasetVersionDatasetList) {
                datasetVersionDatasetMap.put(entry.getKey(), String.valueOf(entry.getValue()));
              }
              return Future.of(datasetVersionDatasetMap);
            })
        .thenCompose(
            datasetVersionDatasetMap -> {
              // Validate linked dataset in dataset version exists in database records if not then
              // we will throw the error
              Set<String> datasetIds = new HashSet<>();
              for (Artifact dataset : datasets) {
                String datasetVersionId = dataset.getLinkedArtifactId();
                if (datasetVersionDatasetMap.containsKey(datasetVersionId)
                    && datasetVersionDatasetMap.get(datasetVersionId) != null) {
                  datasetIds.add(datasetVersionDatasetMap.get(datasetVersionId));
                } else if (errorOut) { // While reading datasets for 'Find*', 'get*' we should not
                  // error out and just skip it.
                  return Future.failedStage(
                      new InvalidArgumentException(
                          "Dataset not found for dataset version: " + datasetVersionId));
                }
              }

              // If all dataset with linked dataset versions found in database then checking
              // authorization for the current user for those datasets
              List<Future<Optional<String>>> internalFutures = new LinkedList<>();
              for (String datasetId : datasetIds) {
                internalFutures.add(
                    Future.of(datasetId)
                        .thenCompose(
                            id -> {
                              /**
                               * Point 1:
                               *
                               * <p>- While creating ER with dataset which is has linked_artifact_id
                               * .
                               *
                               * <p>- If user do not have access of linked_artifact_id then we are
                               * throwing PermissionDeniedException
                               *
                               * <p>Point 2:
                               *
                               * <p>- While fetching ER if user do not have access on
                               * linked_artifact_id then we are just ignore to throw
                               * PermissionDeniedException and skipped to populate it in response
                               * ER.
                               */
                              Future<Boolean> booleanFuture =
                                  permissionCheck.getEntityPermissionBasedOnResourceTypes(
                                      Collections.singletonList(datasetId),
                                      ModelDBActionEnum.ModelDBServiceActions.READ,
                                      ModelDBResourceEnum.ModelDBServiceResourceTypes.DATASET);
                              return booleanFuture.thenCompose(
                                  t ->
                                      Future.of(
                                          ((Function<? super Boolean, ? extends Optional<String>>)
                                                  allowed -> {
                                                    /**
                                                     * Point 1:
                                                     *
                                                     * <p>- While creating ER with dataset which is
                                                     * has linked_artifact_id .
                                                     *
                                                     * <p>- If user do not have access of
                                                     * linked_artifact_id then we are throwing
                                                     * PermissionDeniedException
                                                     *
                                                     * <p>Point 2:
                                                     *
                                                     * <p>- While fetching ER if user do not have
                                                     * access on linked_artifact_id then we are just
                                                     * ignore to throw PermissionDeniedException and
                                                     * skipped to populate it in response ER.
                                                     */
                                                    if (!allowed && errorOut) {
                                                      throw new PermissionDeniedException(
                                                          "Permission denied");
                                                    } else if (allowed) {
                                                      return Optional.of(id);
                                                    } else {
                                                      return Optional.empty();
                                                    }
                                                  })
                                              .apply(t)));
                            }));
              }

              // If all mapped datasets are allowed for the user then we will filter datasets based
              // on linked dataset version and allowed dataset ids.
              Future<Set<String>> accessibleDatasetIdsFutures =
                  Future.sequence(internalFutures)
                      .thenCompose(
                          accessibleDatasetIds -> {
                            Set<String> accessibleDatasetIdsSet =
                                accessibleDatasetIds.stream()
                                    .filter(s -> s.isPresent() && !s.get().isEmpty())
                                    .map(Optional::get)
                                    .collect(Collectors.toSet());
                            return Future.of(accessibleDatasetIdsSet);
                          });
              return accessibleDatasetIdsFutures.thenCompose(
                  accessibleDatasetIds -> {
                    List<Artifact> accessibleDatasets = new ArrayList<>();
                    for (Artifact artifact : datasets) {
                      if (accessibleDatasetIds.contains(
                          datasetVersionDatasetMap.get(artifact.getLinkedArtifactId()))) {
                        accessibleDatasets.add(artifact);
                      }
                    }
                    return Future.of(accessibleDatasets);
                  });
            });
  }
}
