package edu.mit.csail.db.ml.server.algorithm;

import edu.mit.csail.db.ml.server.storage.ProblemTypeConverter;
import jooq.sqlite.gen.Tables;
import modeldb.ProblemType;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProblemTypeGrouper {
  public static Map<ProblemType, List<Integer>> groupByProblemType(List<Integer> modelIds, DSLContext ctx) {
    Map<ProblemType, List<Integer>> modelIdsForType = new HashMap<>();

    ctx
      .select(Tables.FITEVENT.TRANSFORMER, Tables.FITEVENT.PROBLEMTYPE)
      .from(Tables.FITEVENT)
      .where(Tables.FITEVENT.TRANSFORMER.in(modelIds))
      .fetch()
      .into(pair -> {
        ProblemType ptype = ProblemTypeConverter.fromString(pair.value2());
        if (!modelIdsForType.containsKey(ptype)) {
          modelIdsForType.put(ptype, new ArrayList<>());
        }
        modelIdsForType.get(ptype).add(pair.value1());
      });

    return modelIdsForType;
  }
}
