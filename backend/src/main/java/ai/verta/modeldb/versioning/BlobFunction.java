package ai.verta.modeldb.versioning;

import ai.verta.modeldb.ModelDBException;
import java.security.NoSuchAlgorithmException;
import org.hibernate.Session;

public interface BlobFunction {
  String apply(Session session) throws NoSuchAlgorithmException, ModelDBException;

  interface BlobFunctionAttribute {
    void apply(Session session, Long repoId, String commitHash) throws ModelDBException;
  }
}
