package ai.verta.modeldb.versioning.blob.diff;

import java.security.NoSuchAlgorithmException;

public interface ProtoType {
  Boolean isEmpty();

  String getSHA() throws NoSuchAlgorithmException;
}
