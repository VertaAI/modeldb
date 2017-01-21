package edu.mit.csail.db.ml.server.algorithm.similarity.comparators;

import edu.mit.csail.db.ml.server.algorithm.similarity.ModelComparator;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This is no-op comparator that simply returns a copy of the given list of similar model IDs.
 */
public class DummyComparator implements ModelComparator {
  public List<Integer> similarModels(int modelId, List<Integer> similarModels, int limit, DSLContext ctx) {
    return similarModels.stream().map(s -> s).collect(Collectors.toList());
  }
}