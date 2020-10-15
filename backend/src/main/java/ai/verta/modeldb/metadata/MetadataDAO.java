package ai.verta.modeldb.metadata;

import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.ModelDBException;
import java.util.List;
import org.hibernate.Session;

public interface MetadataDAO {
  boolean addLabels(IdentificationType id, List<String> labels) throws ModelDBException;

  void addProperty(Session session, IdentificationType id, String key, String value);

  void addLabels(Session session, IdentificationType id, List<String> labels)
      throws ModelDBException;

  boolean addProperty(IdentificationType id, String key, String value);

  boolean updateLabels(IdentificationType id, List<String> labels);

  List<String> getLabels(IdentificationType id) throws ModelDBException;

  List<IdentificationType> getLabelIds(List<String> labels, OperatorEnum.Operator operator);

  List<String> getLabels(Session session, IdentificationType id) throws ModelDBException;

  String getProperty(IdentificationType id, String key);

  String getProperty(Session session, IdentificationType id, String key);

  boolean deleteLabels(IdentificationType id, List<String> labels, boolean deleteAll)
      throws ModelDBException;

  boolean deleteProperty(IdentificationType id, String key);

  void addOrUpdateKeyValueProperties(AddKeyValuePropertiesRequest request);

  List<KeyValueStringProperty> getKeyValueProperties(GetKeyValuePropertiesRequest request);

  void deleteKeyValueProperties(DeleteKeyValuePropertiesRequest request);
}
