package ai.verta.modeldb.metadata;

import java.util.List;
import org.hibernate.Session;

public interface MetadataDAO {
  boolean addLabels(IdentificationType id, List<String> labels);

  void addParameter(Session session, IdentificationType id, String key, String value);

  void addLabels(Session session, IdentificationType id, List<String> labels);

  boolean addParameter(IdentificationType id, String key, String value);

  List<String> getLabels(IdentificationType id);

  List<String> getLabels(Session session, IdentificationType id);

  String getParameter(IdentificationType id, String key);

  boolean deleteLabels(IdentificationType id, List<String> labels);

  boolean deleteParameter(IdentificationType id, String key);
}
