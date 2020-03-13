package ai.verta.modeldb.telemetry;

import ai.verta.common.KeyValue;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.CollectTelemetry;
import com.google.api.client.http.HttpMethods;
import com.google.protobuf.Value;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class TelemetryCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(TelemetryCron.class);
  private TelemetryUtils telemetryUtils;

  public TelemetryCron(String consumerURL) {
    telemetryUtils = new TelemetryUtils(consumerURL);
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("TelemetryUtils wakeup");

    // Delete existing telemetry information from DB
    telemetryUtils.deleteTelemetryInformation();

    // Get new telemetry data from database
    List<KeyValue> telemetryDataList = collectTelemetryDataFromDB();

    // Insert collected data into telemetry table
    for (KeyValue telemetryMetric : telemetryDataList) {
      telemetryUtils.insertTelemetryInformation(telemetryMetric);
    }

    if (telemetryDataList.size() > 0 && TelemetryUtils.telemetryUniqueIdentifier != null) {
      CollectTelemetry collectTelemetry =
          CollectTelemetry.newBuilder()
              .setId(TelemetryUtils.telemetryUniqueIdentifier)
              .addAllMetrics(telemetryDataList)
              .build();

      try {

        HttpURLConnection httpClient =
            (HttpURLConnection) new URL(telemetryUtils.getConsumer()).openConnection();
        httpClient.setRequestMethod(HttpMethods.POST);
        httpClient.setDoOutput(true);
        httpClient.setRequestProperty("Content-Type", "application/json");
        httpClient.setRequestProperty("Accept", "application/json");
        httpClient.setRequestProperty("grpc-metadata-source", "PythonClient");

        try (OutputStream os = httpClient.getOutputStream()) {
          os.write(ModelDBUtils.getStringFromProtoObject(collectTelemetry).getBytes());
        }

        int responseCode = httpClient.getResponseCode();
        LOGGER.info("POST Response Code :: {}", responseCode);

        try (BufferedReader br =
            new BufferedReader(
                new InputStreamReader(httpClient.getInputStream(), StandardCharsets.UTF_8))) {
          StringBuilder response = new StringBuilder();
          String responseLine;
          while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
          }
          LOGGER.info(" Telemetry Response : {}", response.toString());
        }
      } catch (Exception e) {
        LOGGER.error("Error while uploading telemetry data : {}", e.getMessage(), e);
      }
    }

    LOGGER.info("TelemetryUtils finish tasks and reschedule");
  }

  private List<KeyValue> collectTelemetryDataFromDB() {
    // Get new telemetry data from database
    List<KeyValue> telemetryDataList = new ArrayList<>();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery("select count(*) from ProjectEntity");
      Long projectCount = (Long) query.uniqueResult();
      KeyValue projectCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.PROJECTS)
              .setValue(Value.newBuilder().setNumberValue(projectCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(projectCountKeyValue);

      query = session.createQuery("select count(*) from ExperimentEntity");
      Long experimentCount = (Long) query.uniqueResult();
      KeyValue experimentCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.EXPERIMENTS)
              .setValue(Value.newBuilder().setNumberValue(experimentCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(experimentCountKeyValue);

      query = session.createQuery("select count(*) from ExperimentRunEntity");
      Long experimentRunCount = (Long) query.uniqueResult();
      KeyValue experimentRunCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.EXPERIMENT_RUNS)
              .setValue(Value.newBuilder().setNumberValue(experimentRunCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(experimentRunCountKeyValue);

      query = session.createQuery("select count(*) from DatasetEntity");
      Long datasetCount = (Long) query.uniqueResult();
      KeyValue datasetCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.DATASETS)
              .setValue(Value.newBuilder().setNumberValue(datasetCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(datasetCountKeyValue);

      query = session.createQuery("select count(*) from DatasetVersionEntity");
      Long datasetVersionCount = (Long) query.uniqueResult();
      KeyValue datasetVersionCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.DATASETS_VERSIONS)
              .setValue(Value.newBuilder().setNumberValue(datasetVersionCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(datasetVersionCountKeyValue);

      query = session.createQuery("select count(*) from CommentEntity");
      Long commentCount = (Long) query.uniqueResult();
      KeyValue commentCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.COMMENTS)
              .setValue(Value.newBuilder().setNumberValue(commentCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(commentCountKeyValue);

      query = session.createQuery("select count(*) from CodeVersionEntity");
      Long codeVersionCount = (Long) query.uniqueResult();
      KeyValue codeVersionCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.CODEVERSIONS)
              .setValue(Value.newBuilder().setNumberValue(codeVersionCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(codeVersionCountKeyValue);

      query = session.createQuery("select count(*) from AttributeEntity");
      Long attributeCount = (Long) query.uniqueResult();
      KeyValue attributeCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.ATTRIBUTES)
              .setValue(Value.newBuilder().setNumberValue(attributeCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(attributeCountKeyValue);

      query = session.createQuery("select count(*) from ArtifactEntity");
      Long artifactsCount = (Long) query.uniqueResult();
      KeyValue artifactsCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.ARTIFACTS)
              .setValue(Value.newBuilder().setNumberValue(artifactsCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(artifactsCountKeyValue);

      query = session.createQuery("select count(*) from FeatureEntity");
      Long featuresCount = (Long) query.uniqueResult();
      KeyValue featuresCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.FEATURES)
              .setValue(Value.newBuilder().setNumberValue(featuresCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(featuresCountKeyValue);

      query = session.createQuery("select count(*) from GitSnapshotEntity");
      Long gitSnapshotCount = (Long) query.uniqueResult();
      KeyValue gitSnapshotCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.GIT_SNAPSHOTS)
              .setValue(Value.newBuilder().setNumberValue(gitSnapshotCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(gitSnapshotCountKeyValue);

      query = session.createQuery("select count(*) from KeyValueEntity");
      Long keyValueCount = (Long) query.uniqueResult();
      KeyValue keyValueCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.KEY_VALUES)
              .setValue(Value.newBuilder().setNumberValue(keyValueCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(keyValueCountKeyValue);

      query = session.createQuery("select count(*) from ObservationEntity");
      Long observationCount = (Long) query.uniqueResult();
      KeyValue observationCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.OBSERVATIONS)
              .setValue(Value.newBuilder().setNumberValue(observationCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(observationCountKeyValue);

      query = session.createQuery("select count(*) from TagsMapping");
      Long tagMappingsCount = (Long) query.uniqueResult();
      KeyValue tagMappingsCountKeyValue =
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.TAG_MAPPINGS)
              .setValue(Value.newBuilder().setNumberValue(tagMappingsCount).build())
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      telemetryDataList.add(tagMappingsCountKeyValue);
    } catch (Exception e) {
      LOGGER.error("Error on reading data from DB : {}", e.getMessage(), e);
    }
    return telemetryDataList;
  }
}
