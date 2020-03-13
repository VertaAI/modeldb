package ai.verta.modeldb.lineage;

import ai.verta.modeldb.LineageEntryEnum.LineageEntryType;
import ai.verta.modeldb.ModelDBException;
import org.hibernate.Session;

public interface IsExistsPredicate {
  boolean test(Session session, String id, LineageEntryType type) throws ModelDBException;
}
