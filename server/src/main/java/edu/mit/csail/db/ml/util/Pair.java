package edu.mit.csail.db.ml.util;

/**
 * A simple utility class for representing a pair.
 * @param <F> - The type of the first entry in the pair.
 * @param <S> - The type of the second entry in the pair.
 */
public class Pair<F, S> {
  private final F first;
  private final S second;

  public Pair(F first, S second) {
    this.first = first;
    this.second = second;
  }

  public F getFirst() {
    return first;
  }

  public S getSecond() {
    return second;
  }
}
