package ai.verta.modeldb.experimentRun.subtypes;

import java.util.*;
import java.util.stream.Collectors;

public class MapSubtypes<T> {
  private Map<String, List<T>> map = null;

  private MapSubtypes() {}

  static <T> MapSubtypes<T> from(List<AbstractMap.SimpleEntry<String, T>> entries) {
    final var map =
        entries.stream()
            .collect(
                Collectors.toMap(
                    e -> e.getKey(),
                    e -> Collections.singletonList(e.getValue()),
                    (x, y) -> {
                      x.addAll(y);
                      return x;
                    }));
    final var ret = new MapSubtypes<T>();
    ret.map = map;
    return ret;
  }

  public List<T> get(String key) {
    return map.computeIfAbsent(key, k -> new LinkedList<>());
  }
}
