package ai.verta.modeldb.versioning.blob.diff;

@FunctionalInterface
public interface Function3<One, Two, Three> {
  public Three apply(One one, Two two);
}
