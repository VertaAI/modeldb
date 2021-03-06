package ai.verta.modeldb.versioning;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import java.util.concurrent.ExecutionException;
import org.hibernate.Session;

public interface CommitFunction {

  CommitEntity apply(Session session, RepositoryFunction repositoryFunction)
      throws ModelDBException, ExecutionException, InterruptedException;
}
