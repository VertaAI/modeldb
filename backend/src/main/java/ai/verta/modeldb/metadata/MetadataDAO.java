package ai.verta.modeldb.metadata;

import java.util.List;
import org.hibernate.Session;

public interface MetadataDAO {
  boolean addLabels(IdentificationType id, List<String> labels);

  List<String> getLabels(IdentificationType id);

  List<String> getLabels(Session session, IdentificationType id);

  boolean deleteLabels(IdentificationType id, List<String> labels);
}
