package ai.verta.modeldb.versioning;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import org.hibernate.Session;

import java.util.concurrent.ExecutionException;

public interface RepositoryFunction {
  RepositoryEntity apply(Session session)
      throws ModelDBException, InterruptedException, ExecutionException;
}
