package ai.verta.modeldb.experimentRun;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.amazonaws.services.s3.model.PartETag;
import java.util.List;

public interface CommitMultipartFunction {
  void apply(String s3Key, String uploadId, List<PartETag> partETagList) throws ModelDBException;
}
