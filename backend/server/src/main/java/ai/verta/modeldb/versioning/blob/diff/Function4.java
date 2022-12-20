package ai.verta.modeldb.versioning.blob.diff;

@FunctionalInterface
public interface Function4<One, Two, Three, Four> {
  public Four apply(One one, Two two, Three three);
}
