package ai.verta.modeldb.common;

@Deprecated(forRemoval = true)
public interface CommonConstants {

  /** @deprecated use {@link ai.verta.modeldb.common.config.ArtifactStoreConfig#S3_TYPE_STORE} */
  @Deprecated(forRemoval = true)
  String S3 = "S3";

  /**
   * @deprecated use {@link
   *     ai.verta.modeldb.common.db.migration.Migration#ENABLE_MIGRATIONS_ENV_VAR}
   */
  @Deprecated(forRemoval = true)
  String ENABLE_LIQUIBASE_MIGRATION_ENV_VAR = "LIQUIBASE_MIGRATION";
  /**
   * @deprecated use {@link
   *     ai.verta.modeldb.common.db.migration.Migration#RUN_MIGRATIONS_SEPARATELY}
   */
  @Deprecated(forRemoval = true)
  String RUN_LIQUIBASE_SEPARATE = "RUN_LIQUIBASE_SEPARATE";
}
