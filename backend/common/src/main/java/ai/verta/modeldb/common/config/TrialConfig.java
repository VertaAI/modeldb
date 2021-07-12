package ai.verta.modeldb.common.config;

import ai.verta.modeldb.config.Config;

public class TrialConfig {
  public TrialRestrictionsConfig restrictions;

  public void Validate() throws InvalidConfigException {
    if (restrictions == null)
      throw new InvalidConfigException("restrictions", Config.MISSING_REQUIRED);
    restrictions.Validate();
  }
}
