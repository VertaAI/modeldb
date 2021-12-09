package ai.verta.modeldb.common.subtypes;

import java.util.*;

public class MapSubtypes<T> {
  private Map<String, List<T>> map = null;

  private MapSubtypes() {}

  public static <T> MapSubtypes<T> from(List<AbstractMap.SimpleEntry<String, T>> entries) {
    final var map = new HashMap<String, List<T>>();
    for (final var entry : entries) {
      final var key = entry.getKey();
      final var val = entry.getValue();
      if (!map.containsKey(key)) {
        map.put(key, new LinkedList<>());
      }
      map.get(key).add(val);
    }

    final var ret = new MapSubtypes<T>();
    ret.map = map;
    return ret;
  }

  public List<T> get(String key) {
    return map.computeIfAbsent(key, k -> new LinkedList<>());
  }
}
