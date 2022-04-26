package ai.verta.modeldb;

import ai.verta.modeldb.advancedService.AdvancedServiceImpl;
import ai.verta.modeldb.comment.CommentServiceImpl;
import ai.verta.modeldb.common.CommonApp;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonDAOSet;
import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.CommonServiceSet;
import ai.verta.modeldb.common.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.dataset.DatasetServiceImpl;
import ai.verta.modeldb.datasetVersion.DatasetVersionServiceImpl;
import ai.verta.modeldb.experiment.FutureExperimentServiceImpl;
import ai.verta.modeldb.experimentRun.FutureExperimentRunServiceImpl;
import ai.verta.modeldb.lineage.LineageServiceImpl;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.project.FutureProjectServiceImpl;
import ai.verta.modeldb.reconcilers.ReconcilerInitializer;
import ai.verta.modeldb.telemetry.TelemetryCron;
import ai.verta.modeldb.utils.JdbiUtil;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.VersioningServiceImpl;
import io.grpc.ServerBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.lang.NonNull;

/** This class is entry point of modeldb server. */
@SpringBootApplication
@EnableAutoConfiguration
// Remove bracket () code if in future define any @component outside of the defined basePackages.
@ComponentScan(basePackages = "ai.verta.modeldb.config")
public class App extends CommonApp {
  private static final Logger LOGGER = LogManager.getLogger(App.class);

  private static App app = null;
  public MDBConfig mdbConfig;

  public static App getInstance() {
    if (app == null) {
      app = new App();
    }
    return app;
  }

  public static void main(String[] args) throws Exception {
    System.getProperties().setProperty("BACKEND_SERVICE_NAME", "verta_backend");
    var path = System.getProperty(CommonConstants.USER_DIR) + "/" + CommonConstants.BACKEND_PID;
    resolvePortCollisionIfExists(path);
    SpringApplication application = new SpringApplication(App.class);
    application.addListeners(new ApplicationPidFileWriter(path));
    application.run(args);
  }

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
    App.getInstance().applicationContext = applicationContext;
    this.applicationContext = applicationContext;
  }

  @Override
  protected Config initConfig() {
    var config = MDBConfig.getInstance();
    App.getInstance().mdbConfig = config;
    return config;
  }

  @Override
  protected Config initTestConfig() {
    var config = TestConfig.getInstance();
    App.getInstance().mdbConfig = config;
    return config;
  }

  @Override
  public CommonDBUtil initJdbiUtil() throws SQLException, InterruptedException {
    return JdbiUtil.getInstance(App.getInstance().mdbConfig);
  }

  @Override
  public CommonServiceSet initServiceSet(
      Config config, ArtifactStoreService artifactStoreService, Executor handleExecutor)
      throws IOException {
    var mdbConfig = (MDBConfig) config;
    // Initialize services that we depend on
    return ServiceSet.fromConfig(mdbConfig, artifactStoreService);
  }

  @Override
  public CommonDAOSet initDAOSet(
      Config config, CommonServiceSet serviceSet, Executor handleExecutor) {
    var mdbConfig = (MDBConfig) config;
    if (config.isMigration()) {
      return null;
    }

    // Initialize data access
    return DAOSet.fromServices(
        (ServiceSet) serviceSet, mdbConfig.getJdbi(), handleExecutor, mdbConfig);
  }

  @Override
  public boolean runMigration(Config config, CommonDBUtil commonDBUtil, Executor handleExecutor)
      throws Exception {
    var liquibaseMigration =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(CommonConstants.LIQUIBASE_MIGRATION))
                .orElse("false"));

    var modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    modelDBHibernateUtil.initializedConfigAndDatabase(config, config.getDatabase());

    JdbiUtil jdbiUtil = (JdbiUtil) commonDBUtil;
    if (liquibaseMigration) {
      LOGGER.info("Liquibase migration starting");
      jdbiUtil.runLiquibaseMigration();
      LOGGER.info("Liquibase migration done");

      modelDBHibernateUtil.createOrGetSessionFactory(config.getDatabase());
      LOGGER.info("Old code migration using hibernate starting");
      modelDBHibernateUtil.runMigration(
          config.getDatabase(), App.getInstance().mdbConfig.migrations);
      LOGGER.info("Old code migration using hibernate done");

      LOGGER.info("Code migration starting");
      jdbiUtil.runMigration(handleExecutor);
      LOGGER.info("Code migration done");

      boolean runLiquibaseSeparate =
          Boolean.parseBoolean(
              Optional.ofNullable(System.getenv(CommonConstants.RUN_LIQUIBASE_SEPARATE))
                  .orElse("false"));
      LOGGER.trace("run Liquibase separate: {}", runLiquibaseSeparate);
      if (runLiquibaseSeparate) {
        return true;
      }
    }

    modelDBHibernateUtil.createOrGetSessionFactory(config.getDatabase());
    jdbiUtil.setIsReady();

    return false;
  }

  @Override
  protected void initReconcilers(
      Config commonConfig, CommonServiceSet services, CommonDAOSet daos, Executor handleExecutor) {
    var config = (MDBConfig) commonConfig;

    // Initialize cron jobs
    CronJobUtils.initializeCronJobs(config, (ServiceSet) services);

    ReconcilerInitializer.initialize(
        config, (ServiceSet) services, (DAOSet) daos, config.getJdbi(), handleExecutor);
  }

  @Override
  public void initGRPCServices(
      ServerBuilder<?> serverBuilder,
      CommonServiceSet serviceSet,
      CommonDAOSet daoSet,
      Executor executor) {
    var services = (ServiceSet) serviceSet;
    var daos = (DAOSet) daoSet;
    wrapService(serverBuilder, new FutureProjectServiceImpl(daos, executor));
    LOGGER.trace("Project serviceImpl initialized");
    wrapService(serverBuilder, new FutureExperimentServiceImpl(daos, executor));
    LOGGER.trace("Experiment serviceImpl initialized");
    wrapService(serverBuilder, new FutureExperimentRunServiceImpl(daos, executor));
    LOGGER.trace("ExperimentRun serviceImpl initialized");
    wrapService(serverBuilder, new CommentServiceImpl(services, daos));
    LOGGER.trace("Comment serviceImpl initialized");
    wrapService(serverBuilder, new DatasetServiceImpl(services, daos));
    LOGGER.trace("Dataset serviceImpl initialized");
    wrapService(serverBuilder, new DatasetVersionServiceImpl(services, daos));
    LOGGER.trace("Dataset Version serviceImpl initialized");
    wrapService(serverBuilder, new AdvancedServiceImpl(services, daos, executor));
    LOGGER.trace("Hydrated serviceImpl initialized");
    wrapService(serverBuilder, new LineageServiceImpl(daos));
    LOGGER.trace("Lineage serviceImpl initialized");

    wrapService(serverBuilder, new VersioningServiceImpl(services, daos, new FileHasher()));
    LOGGER.trace("Versioning serviceImpl initialized");
    wrapService(serverBuilder, new MetadataServiceImpl(daos));
    LOGGER.trace("Metadata serviceImpl initialized");
    LOGGER.info("All services initialized and dependencies resolved");
  }

  @Override
  protected void initServiceSpecificThings(Config config) throws Exception {
    var mdbConfig = (MDBConfig) config;
    // Initialize telemetry
    initializeTelemetryBasedOnConfig(mdbConfig);
  }

  private void initializeTelemetryBasedOnConfig(MDBConfig mdbConfig)
      throws FileNotFoundException, InvalidConfigException {
    if (mdbConfig.telemetry != null && !mdbConfig.telemetry.opt_out) {
      // creating an instance of task to be scheduled
      TimerTask task = new TelemetryCron(mdbConfig.telemetry.consumer);
      ModelDBUtils.scheduleTask(
          task, mdbConfig.telemetry.frequency, mdbConfig.telemetry.frequency, TimeUnit.HOURS);
      LOGGER.info("Telemetry scheduled successfully");
    } else {
      LOGGER.info("Telemetry opt out by user");
    }
  }
}
