package ai.verta.modeldb.telemetry;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.utils.CommonHibernateUtil;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TelemetryUtils {
  private static final Logger LOGGER = LogManager.getLogger(TelemetryUtils.class);
  private static ModelDBHibernateUtil modelDBHibernateUtil;
  private static boolean telemetryInitialized = false;
  public static String telemetryUniqueIdentifier = null;
  private String consumer = ModelDBConstants.TELEMETRY_CONSUMER_URL;

  public TelemetryUtils(String consumer) throws InvalidConfigException {
    if (consumer != null && !consumer.isEmpty()) {
      this.consumer = consumer;
    }
    modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    initializeTelemetry();
  }

  public String getConsumer() {
    return consumer;
  }

  private static void initializeTelemetry() throws InvalidConfigException {
    if (!telemetryInitialized) {
      LOGGER.info("Found value for telemetryInitialized : {}", telemetryInitialized);

      try (var connection = modelDBHibernateUtil.getConnection()) {
        final var database = App.getInstance().mdbConfig.getDatabase();
        final var existStatus =
            CommonHibernateUtil.tableExists(connection, database, "modeldb_deployment_info");
        if (!existStatus) {
          LOGGER.warn("modeldb_deployment_info table not found");
          LOGGER.info("Table modeldb_deployment_info creating");

          String createModelDBDeploymentInfoQuery;
          String createTelemetryInformationQuery;
          MDBConfig config = App.getInstance().mdbConfig;
          if (config.getDatabase().getRdbConfiguration().isMssql()) {
            createModelDBDeploymentInfoQuery =
                "create table modeldb_deployment_info (md_key nvarchar(50),md_value nvarchar(255), creation_timestamp BIGINT)";
            createTelemetryInformationQuery =
                "Create table telemetry_information (tel_key nvarchar(50),tel_value nvarchar(255), collection_timestamp BIGINT, transfer_timestamp BIGINT, telemetry_consumer nvarchar(256))";
          } else {
            createModelDBDeploymentInfoQuery =
                "create table modeldb_deployment_info (md_key varchar(50),md_value varchar(255), creation_timestamp BIGINT) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci";
            createTelemetryInformationQuery =
                "Create table telemetry_information (tel_key varchar(50),tel_value varchar(255), collection_timestamp BIGINT, transfer_timestamp BIGINT, telemetry_consumer varchar(256)) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci";
          }

          try (var statement = connection.createStatement()) {
            statement.executeUpdate(createModelDBDeploymentInfoQuery);
            statement.executeUpdate(createTelemetryInformationQuery);
            LOGGER.info(
                "modeldb_deployment_info & telemetry_information table created successfully");
          } catch (Exception e) {
            LOGGER.error(
                ModelDBMessages.ERROR_WHILE_INSERTION_ENTRY_ON_MODEL_DB_DEPLOYMENT_INFO_ERROR,
                e.getMessage(),
                e);
            throw e;
          }
          LOGGER.info("Table modeldb_deployment_info created successfully");
        } else {
          if (database.getRdbConfiguration().isMysql()) {
            // UTF migration is only applied to mysql due to db-specific syntax
            try (var stmt = connection.createStatement()) {
              var updateStatements =
                  new String[] {
                    "ALTER TABLE modeldb_deployment_info MODIFY COLUMN md_key varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci",
                    "          ALTER TABLE modeldb_deployment_info MODIFY COLUMN md_value varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci",
                    "          ALTER TABLE telemetry_information MODIFY COLUMN tel_key varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci",
                    "          ALTER TABLE telemetry_information MODIFY COLUMN tel_value varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci",
                    "          ALTER TABLE telemetry_information MODIFY COLUMN telemetry_consumer varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
                  };
              for (String updateStatement : updateStatements) {
                stmt.executeUpdate(updateStatement);
              }
              var selectQuery = "Select * from modeldb_deployment_info where md_key = 'id'";
              ResultSet rs = stmt.executeQuery(selectQuery);
              if (rs.next()) {
                telemetryUniqueIdentifier = rs.getString(2);
              }
            } catch (Exception e) {
              LOGGER.error(
                  "Error while getting telemetry unique identifier : {}", e.getMessage(), e);
              throw e;
            }
          }
        }
        if (!connection.getAutoCommit()) {
          connection.commit();
        }
        telemetryInitialized = true;
        LOGGER.info("Set value for telemetryInitialized : {}", telemetryInitialized);
      } catch (SQLException e) {
        LOGGER.error(ModelDBMessages.ERROR_WHILE_GETTING_DB_CONNECTION_ERROR, e.getMessage(), e);
      }
    }
  }

  public static void insertModelDBDeploymentInfo() {
    if (telemetryUniqueIdentifier != null) {
      return;
    }
    LOGGER.info("Telemetry unique identifier not initialized");
    telemetryUniqueIdentifier = UUID.randomUUID().toString();
    try (var connection = modelDBHibernateUtil.getConnection()) {
      var sql =
          "INSERT INTO modeldb_deployment_info (md_key, md_value, creation_timestamp) VALUES(?,?,?)";
      try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
        pstmt.setString(1, ModelDBConstants.ID);
        pstmt.setString(2, telemetryUniqueIdentifier);
        pstmt.setLong(3, Calendar.getInstance().getTimeInMillis());
        pstmt.executeUpdate();
        LOGGER.info("Telemetry ID Record inserted");
      } catch (Exception e) {
        LOGGER.error(
            ModelDBMessages.ERROR_WHILE_INSERTION_ENTRY_ON_MODEL_DB_DEPLOYMENT_INFO_ERROR,
            e.getMessage(),
            e);
      } finally {
        if (!connection.getAutoCommit()) {
          connection.commit();
        }
      }
    } catch (SQLException e) {
      LOGGER.error(ModelDBMessages.ERROR_WHILE_GETTING_DB_CONNECTION_ERROR, e.getMessage(), e);
    }
  }

  public void deleteTelemetryInformation() {
    try (var connection = modelDBHibernateUtil.getConnection();
        var stmt = connection.createStatement()) {
      var query = "DELETE FROM telemetry_information";
      int deletedRows = stmt.executeUpdate(query);
      LOGGER.info("Record deleted successfully : {}", deletedRows);
      if (!connection.getAutoCommit()) connection.commit();
    } catch (SQLException e) {
      LOGGER.error(ModelDBMessages.ERROR_WHILE_GETTING_DB_CONNECTION_ERROR, e.getMessage(), e);
    }
  }

  public void insertTelemetryInformation(KeyValue telemetryMetric) {
    try (var connection = modelDBHibernateUtil.getConnection()) {
      var sql =
          "INSERT INTO telemetry_information (tel_key, tel_value, collection_timestamp, telemetry_consumer) VALUES(?,?,?,?)";
      try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
        pstmt.setString(1, telemetryMetric.getKey());
        pstmt.setString(2, CommonUtils.getStringFromProtoObject(telemetryMetric.getValue()));
        pstmt.setLong(3, Calendar.getInstance().getTimeInMillis());
        pstmt.setString(4, consumer);
        pstmt.executeUpdate();
        LOGGER.info("Record inserted successfully");
      } catch (Exception e) {
        LOGGER.error(
            ModelDBMessages.ERROR_WHILE_INSERTION_ENTRY_ON_MODEL_DB_DEPLOYMENT_INFO_ERROR,
            e.getMessage(),
            e);
      } finally {
        if (connection != null && !connection.getAutoCommit()) {
          connection.commit();
        }
      }
    } catch (SQLException e) {
      LOGGER.error(ModelDBMessages.ERROR_WHILE_GETTING_DB_CONNECTION_ERROR, e.getMessage(), e);
    }
  }
}
