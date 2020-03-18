package ai.verta.modeldb.versioning.blob.diff;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Utils {
  public static <T> T removeEmpty(T obj) {
    if (obj instanceof ProtoType) {
      if (((ProtoType) obj).isEmpty()) return null;
    } else if (obj instanceof List) {
      Object ret =
          ((List) obj)
              .stream()
                  .map(x -> removeEmpty(x))
                  .filter(x -> x != null)
                  .collect(Collectors.toList());
      if (((List) ret).isEmpty()) return null;
      return (T) ret;
    }
    return obj;
  }

  public static <T, T2> T2 getOrNull(T v, Function<T, T2> getter) {
    if (v == null) {
      return null;
    }
    return getter.apply(v);
  }

  public static <T, T2> T2 either(T a, T b, Function<T, T2> getter) {
    if (a != null) return getter.apply(a);
    if (b != null) return getter.apply(b);
    return null;
  }
}
