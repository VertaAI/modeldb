package ai.verta.modeldb.versioning;

import ai.verta.modeldb.ModelDBException;
import java.security.NoSuchAlgorithmException;

public interface BlobFunction {
  String apply() throws NoSuchAlgorithmException, ModelDBException;
}
