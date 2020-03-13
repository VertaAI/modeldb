package ai.verta.modeldb.metadata;

import java.util.List;

public interface MetadataDAO {
  boolean addLabels(IdentificationType id, List<String> labels);

  List<String> getLabels(IdentificationType id);

  boolean deleteLabels(IdentificationType id, List<String> labels);
}
