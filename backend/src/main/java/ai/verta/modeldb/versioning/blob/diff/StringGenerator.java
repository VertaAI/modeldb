package ai.verta.modeldb.versioning.blob.diff;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class StringGenerator extends Generator<String> {
  private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
  private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String NUMBERS = "0123456789";
  private static final String SPECIAL_CHARS = ".-\\;:_@[]^/|}{";
  private static final String ALL_MY_CHARS =
      LOWERCASE_CHARS + UPPERCASE_CHARS + NUMBERS + SPECIAL_CHARS;
  public static final int CAPACITY = 5;

  public StringGenerator() {
    super(String.class);
  }

  @Override
  public String generate(SourceOfRandomness random, GenerationStatus status) {
    var sb = new StringBuilder(CAPACITY);
    for (var i = 0; i < CAPACITY; i++) {
      var randomIndex = random.nextInt(ALL_MY_CHARS.length());
      sb.append(ALL_MY_CHARS.charAt(randomIndex));
    }
    return sb.toString();
  }
}
