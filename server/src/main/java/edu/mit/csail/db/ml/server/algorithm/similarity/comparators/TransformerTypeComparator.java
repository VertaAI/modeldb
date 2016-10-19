package edu.mit.csail.db.ml.server.algorithm.similarity.comparators;

import edu.mit.csail.db.ml.server.algorithm.similarity.ModelComparator;
import jooq.sqlite.gen.Tables;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.util.List;
import java.util.stream.Collectors;

public class TransformerTypeComparator implements ModelComparator {
  @Override
  public List<Integer> similarModels(int modelId, List<Integer> similarModels, int limit, DSLContext ctx) {
    // Fetch the problem type of the model.
    Record1<String> rec = ctx
      .select(Tables.TRANSFORMER.TRANSFORMERTYPE)
      .from(Tables.TRANSFORMER)
      .where(Tables.TRANSFORMER.ID.eq(modelId))
      .fetchOne();
    if (rec == null) {
      return similarModels;
    }
    String transformerType = rec.value1();

    // Apply the correct WHERE condition.
    Condition whereCondition = Tables.TRANSFORMER.TRANSFORMERTYPE.eq(transformerType);
    if (!similarModels.isEmpty()) {
      whereCondition = whereCondition.and(Tables.TRANSFORMER.TRANSFORMERTYPE.in(similarModels));
    }

    // Filter down to models with the same project ID.
    return merge(
      ctx
        .select(Tables.TRANSFORMER.ID)
        .from(Tables.TRANSFORMER)
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
