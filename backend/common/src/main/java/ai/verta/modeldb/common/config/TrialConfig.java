package ai.verta.modeldb.common.config;

public class TrialConfig {
  public TrialRestrictionsConfig restrictions;

  public void Validate(String base) throws InvalidConfigException {
    if (restrictions == null)
      throw new InvalidConfigException("restrictions", Config.MISSING_REQUIRED);
    restrictions.Validate("restrictions");
  }
}
