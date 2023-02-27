package ai.verta.modeldb.experimentRun;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import java.util.Optional;

public interface S3KeyFunction {
  Optional<String> apply(String s3Key) throws ModelDBException;
}
