package ai.verta.modeldb.telemetry;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TelemetryUtils {
  private static final Logger LOGGER = LogManager.getLogger(TelemetryUtils.class);
  private boolean telemetryInitialized = false;
  public static String telemetryUniqueIdentifier = null;
  private String consumer = ModelDBConstants.TELEMETRY_CONSUMER_URL;

  public TelemetryUtils(String consumer) {
    if (consumer != null && !consumer.isEmpty()) {
      this.consumer = consumer;
    }
    initializeTelemetry();
  }

  public String getConsumer() {
    return consumer;
  }

  public void initializeTelemetry() {
    if (!telemetryInitialized) {
      LOGGER.info("Found value for telemetryInitialized : {}", telemetryInitialized);

      try (Connection connection = ModelDBHibernateUtil.getConnection()) {
        boolean existStatus =
            ModelDBHibernateUtil.tableExists(connection, "modeldb_deployment_info");
        if (!existStatus) {
          LOGGER.warn("modeldb_deployment_info table not found");
          LOGGER.info("Table modeldb_deployment_info creating");

          String createModelDBDeploymentInfoQuery =
              "create table modeldb_deployment_info (md_key varchar(50),md_value varchar(255), creation_timestamp BIGINT)";
          String createTelemetryInformationQuery =
              "Create table telemetry_information (tel_key varchar(50),tel_value varchar(255), collection_timestamp BIGINT, transfer_timestamp BIGINT, telemetry_consumer varchar(256))";

          try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createModelDBDeploymentInfoQuery);
            statement.executeUpdate(createTelemetryInformationQuery);
            LOGGER.info(
                "modeldb_deployment_info & telemetry_information table created successfully");
          } catch (Exception e) {
            LOGGER.error(
                "Error while insertion entry on ModelDB deployment info : {}", e.getMessage(), e);
            throw e;
          }
          LOGGER.info("Table modeldb_deployment_info created successfully");
        } else {
          try (Statement stmt = connection.createStatement()) {
            String selectQuery = "Select * from modeldb_deployment_info where md_key = 'id'";
            ResultSet rs = stmt.executeQuery(selectQuery);
            if (rs.next()) {
              telemetryUniqueIdentifier = rs.getString(2);
            }
          } catch (Exception e) {
            LOGGER.error("Error while getting telemetry unique identifier : {}", e.getMessage(), e);
            throw e;
          }
        }
        connection.commit();
        telemetryInitialized = true;
        LOGGER.info("Set value for telemetryInitialized : {}", telemetryInitialized);
      } catch (SQLException e) {
        LOGGER.error("Error while getting DB connection : {}", e.getMessage(), e);
      }
    }
  }

  public static void insertModelDBDeploymentInfo() {
    if (telemetryUniqueIdentifier != null) {
      return;
    }
    LOGGER.info("Telemetry unique identifier not initialized");
    telemetryUniqueIdentifier = UUID.randomUUID().toString();
    try (Connection connection = ModelDBHibernateUtil.getConnection()) {
      String sql =
          "INSERT INTO modeldb_deployment_info (md_key, md_value, creation_timestamp) VALUES(?,?,?)";
      try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
        pstmt.setString(1, ModelDBConstants.ID);
        pstmt.setString(2, telemetryUniqueIdentifier);
        pstmt.setLong(3, Calendar.getInstance().getTimeInMillis());
        pstmt.executeUpdate();
        LOGGER.info("Telemetry ID Record inserted");
      } catch (Exception e) {
        LOGGER.error(
            "Error while insertion entry on ModelDB deployment info : {}", e.getMessage(), e);
      } finally {
        if (connection != null) {
          connection.commit();
          connection.close();
        }
      }
    } catch (SQLException e) {
      LOGGER.error("Error while getting DB connection : {}", e.getMessage(), e);
    }
  }

  public void deleteTelemetryInformation() {
    try (Connection connection = ModelDBHibernateUtil.getConnection()) {
      Statement stmt = connection.createStatement();
      String query = "DELETE FROM telemetry_information";
      int deletedRows = stmt.executeUpdate(query);
      LOGGER.info("Record deleted successfully : {}", deletedRows);
      connection.commit();
    } catch (SQLException e) {
      LOGGER.error("Error while getting DB connection : {}", e.getMessage(), e);
    }
  }

  public void insertTelemetryInformation(KeyValue telemetryMetric) {
    try (Connection connection = ModelDBHibernateUtil.getConnection()) {
      String sql =
          "INSERT INTO telemetry_information (tel_key, tel_value, collection_timestamp, telemetry_consumer) VALUES(?,?,?,?)";
      try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
        pstmt.setString(1, telemetryMetric.getKey());
        pstmt.setString(2, ModelDBUtils.getStringFromProtoObject(telemetryMetric.getValue()));
        pstmt.setLong(3, Calendar.getInstance().getTimeInMillis());
        pstmt.setString(4, consumer);
        pstmt.executeUpdate();
        LOGGER.info("Record inserted successfully");
      } catch (Exception e) {
        LOGGER.error(
            "Error while insertion entry on ModelDB deployment info : {}", e.getMessage(), e);
      } finally {
        connection.commit();
        connection.close();
      }
    } catch (SQLException e) {
      LOGGER.error("Error while getting DB connection : {}", e.getMessage(), e);
    }
  }
}
