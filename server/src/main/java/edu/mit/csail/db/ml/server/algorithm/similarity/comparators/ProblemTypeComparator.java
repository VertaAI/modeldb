package edu.mit.csail.db.ml.server.algorithm.similarity.comparators;

import edu.mit.csail.db.ml.server.algorithm.similarity.ModelComparator;
import jooq.sqlite.gen.Tables;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.util.List;
import java.util.stream.Collectors;

public class ProblemTypeComparator implements ModelComparator {
  @Override
  public List<Integer> similarModels(int modelId, List<Integer> similarModels, int limit, DSLContext ctx) {
    // Fetch the problem type of the model.
    Record1<String> rec = ctx
      .select(Tables.FITEVENT.PROBLEMTYPE)
      .from(Tables.FITEVENT)
      .where(Tables.FITEVENT.TRANSFORMER.eq(modelId))
      .fetchOne();
    if (rec == null) {
      return similarModels;
    }
    String problemType = rec.value1();

    // Apply the correct WHERE condition.
    Condition whereCondition = Tables.FITEVENT.PROBLEMTYPE.eq(problemType);
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
    );
  }
}
