package ai.verta.modeldb.common.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class StringGenerator {
  private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
  private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String NUMBERS = "0123456789";
  private static final String LOWER_ALPHANUMERIC = LOWERCASE_CHARS + NUMBERS;
  private static final String ALL_ALPHANUMERIC = LOWERCASE_CHARS + UPPERCASE_CHARS + NUMBERS;
  private static final String SPECIAL_CHARS = ".-\\;:_@[]^/|}{";
  private static final String ALL_MY_CHARS =
      LOWERCASE_CHARS + UPPERCASE_CHARS + NUMBERS + SPECIAL_CHARS;

  private static final String FILENAME_CHARS = LOWERCASE_CHARS + UPPERCASE_CHARS + NUMBERS + "_";

  public static String generateLowerAlphanumeric(
      int minLength, int maxLength, SourceOfRandomness random, GenerationStatus status) {
    int length = random.nextInt(minLength, maxLength);
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      int randomIndex = random.nextInt(LOWER_ALPHANUMERIC.length());
      sb.append(LOWER_ALPHANUMERIC.charAt(randomIndex));
    }
    return sb.toString();
  }

  public static String generateAlphanumeric(
      int minLength, int maxLength, SourceOfRandomness random, GenerationStatus status) {
    int length = random.nextInt(minLength, maxLength);
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      int randomIndex = random.nextInt(ALL_ALPHANUMERIC.length());
      sb.append(ALL_ALPHANUMERIC.charAt(randomIndex));
    }
    return sb.toString();
  }

  public static String generateFilenamePart(
      int minLength, int maxLength, SourceOfRandomness random, GenerationStatus status) {
    int length = random.nextInt(minLength, maxLength);
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      int randomIndex = random.nextInt(FILENAME_CHARS.length());
      sb.append(FILENAME_CHARS.charAt(randomIndex));
    }
    return sb.toString();
  }

  public static String generate(
      int minLength, int maxLength, SourceOfRandomness random, GenerationStatus status) {
    int length = random.nextInt(minLength, maxLength);
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      int randomIndex = random.nextInt(ALL_MY_CHARS.length());
      sb.append(ALL_MY_CHARS.charAt(randomIndex));
    }
    return sb.toString();
  }
}
