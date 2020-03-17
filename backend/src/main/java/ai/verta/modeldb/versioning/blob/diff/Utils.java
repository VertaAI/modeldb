package ai.verta.modeldb.versioning.blob.diff;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Utils {
  public static <T> Optional<T> removeEmpty(Optional<T> obj) {
    return obj.flatMap(x -> removeEmpty(x));
  }

  public static <T> Optional<T> removeEmpty(T obj) {
    if (obj == null) {
      return Optional.empty();
    }

    if (obj instanceof ProtoType) {
      if (((ProtoType) obj).isEmpty()) return Optional.empty();
    } else if (obj instanceof List) {
      Object ret =
          ((List) obj)
              .stream()
                  .map(x -> removeEmpty(x))
                  .filter(x -> ((Optional) x).isPresent())
                  .collect(Collectors.toList());
      if (((List) ret).isEmpty()) return Optional.empty();
      return Optional.of((T) ret);
    }
    return Optional.of(obj);
  }

  public static <T, T2> T2 getOrNull(T v, Function<T, T2> getter) {
    if (v == null) {
      return null;
    }
    return getter.apply(v);
  }
}
