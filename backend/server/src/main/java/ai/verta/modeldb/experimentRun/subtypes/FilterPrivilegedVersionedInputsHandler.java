package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.VersioningEntry;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.interfaces.CheckEntityPermissionBasedOnResourceTypesFunction;
import ai.verta.uac.ModelDBActionEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FilterPrivilegedVersionedInputsHandler {

  private static Logger LOGGER = LogManager.getLogger(FilterPrivilegedVersionedInputsHandler.class);

  private final FutureExecutor executor;
  private final FutureJdbi jdbi;

  public FilterPrivilegedVersionedInputsHandler(FutureExecutor executor, FutureJdbi jdbi) {
    this.executor = executor;
    this.jdbi = jdbi;
  }

  public Future<Map<String, VersioningEntry>> filterVersionedInputsBasedOnPrivileges(
      Set<String> runIds,
      Future<Map<String, VersioningEntry>> futureVersionedInputs,
      CheckEntityPermissionBasedOnResourceTypesFunction permissionCheck) {
    return futureVersionedInputs.thenCompose(
        versionedInputsMap -> {
          List<Future<Map<String, VersioningEntry>>> internalFutureList = new ArrayList<>();
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
                            return Future.of(Collections.singletonMap(runId, versioningEntry));
                          } else {
                            return Future.of(Collections.emptyMap());
                          }
                        }));
          }
          return Future.sequence(internalFutureList)
              .thenCompose(
                  maps -> {
                    Map<String, VersioningEntry> finalVersionedInputsMap = new HashMap<>();
                    maps.forEach(finalVersionedInputsMap::putAll);
                    return Future.of(finalVersionedInputsMap);
                  });
        });
  }
}
