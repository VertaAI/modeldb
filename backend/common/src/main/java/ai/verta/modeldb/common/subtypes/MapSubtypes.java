package ai.verta.modeldb.common.subtypes;

import java.util.*;

public class MapSubtypes<S, T> {
  private Map<S, List<T>> map = null;

  private MapSubtypes() {}

  public static <S, T> MapSubtypes<S, T> from(List<AbstractMap.SimpleEntry<S, T>> entries) {
    final var map = new HashMap<S, List<T>>();
    for (final var entry : entries) {
      final var key = entry.getKey();
      final var val = entry.getValue();
      if (!map.containsKey(key)) {
        map.put(key, new LinkedList<>());
      }
      map.get(key).add(val);
    }

    final var ret = new MapSubtypes<S, T>();
    ret.map = map;
    return ret;
  }

  public List<T> get(S key) {
    return map.computeIfAbsent(key, k -> new LinkedList<>());
  }
}
