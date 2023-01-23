package ai.verta.modeldb.common.futures;

@FunctionalInterface
public interface HandleCallback<T, X extends Exception> {
  T withHandle(Handle handle) throws X;
}
