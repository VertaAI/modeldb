package edu.mit.csail.db.ml.server.algorithm.similarity.comparators;

import edu.mit.csail.db.ml.server.algorithm.similarity.ModelComparator;
import jooq.sqlite.gen.Tables;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record2;

import java.util.List;
import java.util.stream.Collectors;

public class ExperimentRunComparator implements ModelComparator {
  @Override
  public List<Integer> similarModels(int modelId, List<Integer> similarModels, int limit, DSLContext ctx) {
    // Fetch the project ID and experiment run ID for the model.
    Record2<Integer, Integer> rec = ctx
      .select(Tables.FITEVENT.PROJECT, Tables.FITEVENT.EXPERIMENTRUN)
      .from(Tables.FITEVENT)
      .where(Tables.FITEVENT.TRANSFORMER.eq(modelId))
      .fetchOne();
    if (rec == null) {
      return similarModels;
    }
    int projectId = rec.value1();
    int experimentRunId = rec.value1();

    // Apply the correct WHERE condition.
    Condition whereCondition = Tables.FITEVENT.PROJECT.eq(projectId)
      .and(Tables.FITEVENT.EXPERIMENTRUN.eq(experimentRunId));
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
