package ai.verta.modeldb.versioning;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import org.hibernate.Session;

public interface CommitFunction {

  CommitEntity apply(Session session, RepositoryFunction repositoryFunction)
      throws ModelDBException;
}
