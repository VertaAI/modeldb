package ai.verta.modeldb.versioning.blob.diff;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;

public interface ProtoType {
  Boolean isEmpty();

  void preVisitDeep(Visitor visitor) throws ModelDBException;
}
