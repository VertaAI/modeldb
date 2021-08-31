package ai.verta.modeldb.config;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.InvalidConfigException;

public class TrialConfig {
  public TrialRestrictionsConfig restrictions;

  public void Validate(String base) throws InvalidConfigException {
    if (restrictions == null)
      throw new InvalidConfigException("restrictions", Config.MISSING_REQUIRED);
    restrictions.Validate("restrictions");
  }
}
