package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.config.InvalidConfigException;

public class TrialRestrictionsConfig {
  public Integer max_artifact_size_MB = 1024;
  public Integer max_artifact_per_run;
  public Integer max_experiment_run_per_workspace;

  public void Validate(String base) throws InvalidConfigException {}
}
