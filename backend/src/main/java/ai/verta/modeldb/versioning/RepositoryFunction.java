package ai.verta.modeldb.versioning;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import java.util.concurrent.ExecutionException;
import org.hibernate.Session;

public interface RepositoryFunction {
  RepositoryEntity apply(Session session)
      throws ModelDBException, InterruptedException, ExecutionException;
}
