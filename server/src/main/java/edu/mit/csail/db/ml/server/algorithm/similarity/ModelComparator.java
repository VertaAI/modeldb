package edu.mit.csail.db.ml.server.algorithm.similarity;

import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The interface for finding models similar to a given model according to some criteria.
 */
public interface ModelComparator {
  /**
   * Finds the list of IDs of models that are similar to the given model ID.
   * @param modelId - We seek models that are similar to the model with the given ID.
   * @param similarModels - If this is an empty list, then we will read the database and
   *                      find similar models there. Otherwise, the given list of model IDs
   *                      will be re-ordered such that the most similar model is first and
   *                      the least similar model will be last.
   * @param limit - The maximum number of model IDs to return.
   * @param ctx - The context for interacting with the database.
   * @return The IDs of models similar to the given model, ordered by most similar model
   * first and least similar model last.
   */
  List<Integer> similarModels(int modelId, List<Integer> similarModels, int limit, DSLContext ctx);

  /**
   * Merges a new list of integer IDs into the original list of IDs.
   * @param returned - The new list of integer IDs.
   * @param original - The old list of integer IDs.
   * @return The original IDs that do NOT appear in the list of new IDs are appended
   * to the list of new IDs and the overall list is then returned.
   */
  default List<Integer> merge(List<Integer> returned, List<Integer> original) {
    List<Integer> copiedOriginal = original.stream().map(s -> s).collect(Collectors.toList());
    List<Integer> copiedReturned = returned.stream().map(s -> s).collect(Collectors.toList());
    copiedOriginal.removeAll(returned);
    copiedReturned.addAll(original);
    return copiedReturned;
  }
}
