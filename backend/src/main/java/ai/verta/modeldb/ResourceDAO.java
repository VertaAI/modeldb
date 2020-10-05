package ai.verta.modeldb;

import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import java.util.List;

public interface ResourceDAO<T> {

  List<T> getOrganizationResources();

  void deleteResources(List<String> resourceIds, ExperimentRunDAO experimentRunDAO)
      throws ModelDBException;
}
