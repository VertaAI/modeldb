package ai.verta.modeldb.data;

import ai.verta.modeldb.CreateDataset;
import com.google.gson.Gson;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ModelDataServiceImpl extends ModelDataServiceGrpc.ModelDataServiceImplBase {
  private static final Logger LOGGER = LogManager.getLogger(ModelDataServiceImpl.class);

  private final String modelDataStoragePath;

  public ModelDataServiceImpl(String modelDataStoragePath) {
    this.modelDataStoragePath = modelDataStoragePath;
  }

  private String buildFileName(String modelId, Long timestampMillis, String endpoint) {
    return modelDataStoragePath + "/" + modelId + "-" + endpoint + "-" + timestampMillis;
  }

  private String buildFileName(ModelDataMetadata metadata) {
    final String modelId = metadata.getModelId();
    final Long timestampMillis = metadata.getTimestampMillis();
    final String endpoint = metadata.getEndpoint();
    return buildFileName(modelId, timestampMillis, endpoint);
  }

  @Override
  public void storeModelData(
      StoreModelDataRequest request,
      StreamObserver<StoreModelDataRequest.Response> responseObserver) {
    LOGGER.info("StoreModelData: " + request);
    final ModelDataMetadata metadata = request.getModelData().getMetadata();
    final String data = request.getModelData().getData();
    final String fileName = buildFileName(metadata);
    try (FileWriter writer = new FileWriter(fileName)) {
      writer.write(data);
    } catch (IOException ex) {
      LOGGER.error(ex);
    }
    responseObserver.onNext(StoreModelDataRequest.Response.newBuilder().build());
    responseObserver.onCompleted();
  }

  private String extractEndpoint(String fileName) {
    String[] tokens = fileName.split("-");
    return tokens[1];
  }

  @Override
  public void getModelData(
      GetModelDataRequest request, StreamObserver<GetModelDataRequest.Response> responseObserver) {
    LOGGER.info("GetModelData: " + request);
    final Instant startAt = Instant.ofEpochMilli(request.getStartTimeMillis());
    final Instant endAt = Instant.ofEpochMilli(request.getEndTimeMillis());
    final File fileRoot = new File(modelDataStoragePath);

    final File[] filteredToModel =
        fileRoot.listFiles((dir, name) -> name.startsWith(request.getModelId() + "-"));
    final List<Map<String, Object>> filteredToTimespan =
        Arrays.stream(filteredToModel)
            .filter(
                file -> {
                  final Instant fileTimestamp = Instant.ofEpochMilli(file.lastModified());
                  return startAt.isAfter(fileTimestamp) && endAt.isBefore(fileTimestamp);
                })
            .map(
                file -> {
                  try {
                    final String fileContents =
                        Files.lines(Paths.get(file.getAbsolutePath()))
                            .collect(Collectors.joining());
                    return buildModelDataMap(request.getModelId(), file, fileContents);
                  } catch (IOException e) {
                    LOGGER.error(e);
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    String json = new Gson().toJson(filteredToTimespan);
    responseObserver.onNext(GetModelDataRequest.Response.newBuilder().setData(json).build());
    responseObserver.onCompleted();
  }

  private Map<String, Object> buildModelDataMap(String modelId, File file, String fileContents) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("model_id", modelId);
    metadata.put("timestamp", file.lastModified());
    metadata.put("endpoint", extractEndpoint(file.getName()));
    Map<String, Object> result = new HashMap<>();
    result.put("metadata", metadata);
    result.put("data", fileContents);
    return result;
  }

  @Override
  public void getModelDataDiff(
      GetModelDataRequest request, StreamObserver<GetModelDataRequest.Response> responseObserver) {
    LOGGER.info("GetModelDataDiff: " + request);
    super.getModelDataDiff(request, responseObserver);
  }
}
