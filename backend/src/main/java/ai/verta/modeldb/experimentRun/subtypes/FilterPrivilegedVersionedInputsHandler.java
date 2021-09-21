package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.VersioningEntry;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.interfaces.CheckEntityPermissionBasedOnResourceTypesFunction;
import ai.verta.uac.ModelDBActionEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FilterPrivilegedVersionedInputsHandler {

  private static Logger LOGGER = LogManager.getLogger(FilterPrivilegedVersionedInputsHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;

  public FilterPrivilegedVersionedInputsHandler(Executor executor, FutureJdbi jdbi) {
    this.executor = executor;
    this.jdbi = jdbi;
  }

  public InternalFuture<Map<String, VersioningEntry>> filterVersionedInputsBasedOnPrivileges(
      Set<String> runIds,
      InternalFuture<Map<String, VersioningEntry>> futureVersionedInputs,
      CheckEntityPermissionBasedOnResourceTypesFunction permissionCheck) {
    return futureVersionedInputs.thenCompose(
        versionedInputsMap -> {
          List<InternalFuture<Map<String, VersioningEntry>>> internalFutureList = new ArrayList<>();
          for (String runId : runIds) {
            // Get VersionEntry from fetched map
            var versioningEntry = versionedInputsMap.get(runId);
            if (versioningEntry == null) {
              continue;
            }
            var repoId = String.valueOf(versioningEntry.getRepositoryId());
            // Check repository is accessible or not from versionedInputs If not then we will remove
            // it from fetched map
            internalFutureList.add(
                permissionCheck
                    .getEntityPermissionBasedOnResourceTypes(
                        Collections.singletonList(repoId),
                        ModelDBActionEnum.ModelDBServiceActions.READ,
                        ModelDBResourceEnum.ModelDBServiceResourceTypes.REPOSITORY)
                    .thenCompose(
                        isSelfAllowed -> {
                          // Set null into map if repository is not accessible to the
                          // current user
                          if (isSelfAllowed) {
                            return InternalFuture.completedInternalFuture(
                                Collections.singletonMap(runId, versioningEntry));
                          } else {
                            return InternalFuture.completedInternalFuture(Collections.emptyMap());
                          }
                        },
                        executor));
          }
          return InternalFuture.sequence(internalFutureList, executor)
              .thenCompose(
                  maps -> {
                    Map<String, VersioningEntry> finalVersionedInputsMap = new HashMap<>();
                    maps.forEach(finalVersionedInputsMap::putAll);
                    return InternalFuture.completedInternalFuture(finalVersionedInputsMap);
                  },
                  executor);
        },
        executor);
  }
}
