package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.futures.InternalFuture;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  public static <T, U, V> InternalFuture<Stream<V>> combine(
      InternalFuture<Stream<T>> base,
      InternalFuture<MapSubtypes<U>> map,
      Function<T, String> keyGetter,
      BiFunction<T, List<U>, V> applier,
      Executor executor) {
    return base.thenCombine(
        map, (b, m) -> b.map(v -> applier.apply(v, m.get(keyGetter.apply(v)))), executor);
  }
}
