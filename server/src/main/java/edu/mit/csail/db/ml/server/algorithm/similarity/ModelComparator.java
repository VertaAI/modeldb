package edu.mit.csail.db.ml.server.algorithm.similarity;

import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;

public interface ModelComparator {
  List<Integer> similarModels(int modelId, List<Integer> similarModels, int limit, DSLContext ctx);

  default List<Integer> merge(List<Integer> returned, List<Integer> original) {
    List<Integer> copiedOriginal = original.stream().map(s -> s).collect(Collectors.toList());
    List<Integer> copiedReturned = returned.stream().map(s -> s).collect(Collectors.toList());
    copiedOriginal.removeAll(returned);
    copiedReturned.addAll(original);
    return copiedReturned;
  }
}
