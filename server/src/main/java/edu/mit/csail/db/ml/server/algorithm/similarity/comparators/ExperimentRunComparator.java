package edu.mit.csail.db.ml.server.algorithm.similarity.comparators;

import edu.mit.csail.db.ml.server.algorithm.similarity.ModelComparator;
import jooq.sqlite.gen.Tables;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This comparator finds a models with the same experiment ID as the experiment
 * run that contains the model with the given ID.
 */
public class ExperimentRunComparator implements ModelComparator {
  /**
   * @param modelId - We seek models that are similar to the model with the given ID.
   * @param similarModels - If this is an empty list, then we will read the database and
   *                      find similar models there. Otherwise, the given list of model IDs
   *                      will be re-ordered such that the most similar model is first and
   *                      the least similar model will be last.
   * @param limit - The maximum number of model IDs to return.
   * @param ctx - The context for interacting with the database.
   * @return The models in the same experiment run as the model with ID modelID
   * are searched and placed at the start of the returned list.
   */
  @Override
  public List<Integer> similarModels(int modelId, List<Integer> similarModels, int limit, DSLContext ctx) {
    // Fetch the project ID and experiment run ID for the model.
    Record1<Integer> rec = ctx
      .select(Tables.FITEVENT.EXPERIMENTRUN)
      .from(Tables.FITEVENT)
      .where(Tables.FITEVENT.TRANSFORMER.eq(modelId))
      .fetchOne();
    if (rec == null) {
      return similarModels;
    }
    int experimentRunId = rec.value1();

    // Apply the correct WHERE condition to find models in the same experiment run as model modelID.
    Condition whereCondition = Tables.FITEVENT.EXPERIMENTRUN.eq(experimentRunId);
    if (!similarModels.isEmpty()) {
      whereCondition = whereCondition.and(Tables.FITEVENT.TRANSFORMER.in(similarModels));
    }

    // Filter down to models with the same project ID.
    return merge(
      ctx
        .select(Tables.FITEVENT.TRANSFORMER)
        .from(Tables.FITEVENT)
        .where(whereCondition)
        .limit(limit)
        .stream()
        .distinct()
        .map(Record1::value1)
        .collect(Collectors.toList()),
      similarModels
    ) ;
  }
}
