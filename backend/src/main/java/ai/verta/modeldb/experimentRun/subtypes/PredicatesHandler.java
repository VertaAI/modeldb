package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.exceptions.InvalidArgumentException;

public class PredicatesHandler {
  private InternalFuture<QueryFilterContext> processPredicate(long index, KeyValueQuery predicate) {
    final var key = predicate.getKey();
    final var value = predicate.getValue();

    if (!key.contains(ModelDBConstants.LINKED_ARTIFACT_ID)
        && predicate.getOperator().equals(OperatorEnum.Operator.IN)) {
      return InternalFuture.failedStage(
          new InvalidArgumentException(
              "Operator `IN` supported only with the linked_artifact_id as a key"));
    }

    final var bindingName = String.format("predicate_%d", index);

    String[] names = key.split("\\.");

    switch (key) {
      case ModelDBConstants.ID:
        return InternalFuture.completedInternalFuture(new QueryFilterContext("experiment_run.id = :"+bindingName, q -> q.bind(bindingName, value.getStringValue()));
      case "owner":
        // case time created/updated:
        // case visibility:
      case "":
        return InternalFuture.failedStage(new InvalidArgumentException("Key is empty"));
    }

    switch (names[0]) {
      case ModelDBConstants.ARTIFACTS:
      case ModelDBConstants.DATASETS:
      case ModelDBConstants.ATTRIBUTES:
      case ModelDBConstants.HYPERPARAMETERS:
      case ModelDBConstants.METRICS:
      case ModelDBConstants.OBSERVATIONS:
        // case ModelDBConstants.FEATURES: TODO?
      case ModelDBConstants.TAGS:
      case ModelDBConstants.VERSIONED_INPUTS:
    }

    // TODO: handle arbitrary key
  }
}
