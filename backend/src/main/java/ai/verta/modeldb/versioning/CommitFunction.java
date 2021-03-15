package ai.verta.modeldb.versioning;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import org.hibernate.Session;

import java.util.concurrent.ExecutionException;

public interface CommitFunction {

  CommitEntity apply(Session session, RepositoryFunction repositoryFunction)
      throws ModelDBException, ExecutionException, InterruptedException;
}
