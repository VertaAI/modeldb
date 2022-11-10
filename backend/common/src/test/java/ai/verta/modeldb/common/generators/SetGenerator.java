package ai.verta.modeldb.common.generators;

import com.pholser.junit.quickcheck.Pair;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class SetGenerator {
  public static <T> Set<T> generate(
      int minSize,
      int maxSize,
      Supplier<T> supplier,
      SourceOfRandomness random,
      GenerationStatus status) {
    int size = random.nextInt(minSize, maxSize);
    Set<T> set = new HashSet<>(size);
    while (size > 0) {
      T item = supplier.get();
      if (!set.contains(item)) {
        set.add(item);
        size--;
      }
    }
    return set;
  }

  public static <T> Set<T> generateByPairs(
      int minSize,
      int maxSize,
      Supplier<Pair<T, T>> supplier,
      SourceOfRandomness random,
      GenerationStatus status) {
    int size = random.nextInt(minSize, maxSize);
    Set<T> set = new HashSet<>(size);
    while (size > 0) {
      Pair<T, T> item = supplier.get();
      if (!set.contains(item.first)) {
        set.add(item.first);
        set.add(item.second);
        size--;
      }
    }
    return set;
  }
}
