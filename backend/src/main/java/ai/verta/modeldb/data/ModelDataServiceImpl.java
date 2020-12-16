package ai.verta.modeldb.data;

import com.google.gson.Gson;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModelDataServiceImpl extends ModelDataServiceGrpc.ModelDataServiceImplBase {
  private static final Logger LOGGER = LogManager.getLogger(ModelDataServiceImpl.class);

  private final String modelDataStoragePath;

  public ModelDataServiceImpl(String modelDataStoragePath) {
    this.modelDataStoragePath = modelDataStoragePath;
    // ensure it exists
    new File(modelDataStoragePath).mkdirs();
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

  @Override
  public void getModelData(
      GetModelDataRequest request, StreamObserver<GetModelDataRequest.Response> responseObserver) {
    LOGGER.info("GetModelData: " + request);
    final Instant startAt = Instant.ofEpochMilli(request.getStartTimeMillis());
    final Instant endAt = Instant.ofEpochMilli(request.getEndTimeMillis());
    final List<NGramData> filteredToTimespan = fetchNGramData(request.getModelId(), startAt, endAt);
    Map<String, Object> aggregate = aggregateTimespan(filteredToTimespan);
    Map<String, Object> payload =
        buildPayload(startAt, endAt, request.getModelId(), request.getEndpoint(), aggregate);
    String json = new Gson().toJson(payload);
    responseObserver.onNext(GetModelDataRequest.Response.newBuilder().setData(json).build());
    responseObserver.onCompleted();
  }

  private Map<String, Object> buildPayload(
      Instant start, Instant end, String modelId, String endpoint, Map<String, Object> aggregate) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("start_time_millis", start.toEpochMilli());
    metadata.put("end_time_millis", end.toEpochMilli());
    metadata.put("model_id", modelId);
    metadata.put("endpoint", endpoint);

    Map<String, Object> payload = new HashMap<>();
    payload.put("metadata", metadata);
    payload.put("data", aggregate);
    return payload;
  }

  @Override
  public void getModelDataDiff(
      GetModelDataDiffRequest request,
      StreamObserver<GetModelDataDiffRequest.Response> responseObserver) {
    LOGGER.info("GetModelDataDiff: " + request);
    final Instant startAt = Instant.ofEpochMilli(request.getStartTimeMillis());
    final Instant endAt = Instant.ofEpochMilli(request.getEndTimeMillis());

    final List<NGramData> aFilteredToTimespan =
        fetchNGramData(request.getModelIdA(), startAt, endAt);
    final List<NGramData> bFilteredToTimespan =
        fetchNGramData(request.getModelIdB(), startAt, endAt);

    final Map<String, Object> aggregateA = aggregateTimespan(aFilteredToTimespan);
    final Map<String, Object> aggregateB = aggregateTimespan(bFilteredToTimespan);

    Map<String, Object> leftPayload =
        buildPayload(startAt, endAt, request.getModelIdA(), request.getEndpoint(), aggregateA);
    Map<String, Object> rightPayload =
        buildPayload(startAt, endAt, request.getModelIdB(), request.getEndpoint(), aggregateA);

    Map<String, Object> payload = new HashMap<>();
    payload.put("left", leftPayload);
    payload.put("right", rightPayload);

    String json = new Gson().toJson(payload);
    responseObserver.onNext(GetModelDataDiffRequest.Response.newBuilder().setData(json).build());
    responseObserver.onCompleted();
  }

  private Map<String, Object> aggregateTimespan(List<NGramData> filteredToTimespan) {
    AtomicLong totalPopulation = new AtomicLong(0L);
    Map<List<String>, AtomicLong> allNgrams = new HashMap<>();
    filteredToTimespan.stream()
        .forEach(
            nGramData -> {
              totalPopulation.addAndGet(nGramData.getPopulation());
              for (NGram nGram : nGramData.getNgrams()) {
                AtomicLong count =
                    allNgrams.computeIfAbsent(nGram.getGrams(), strings -> new AtomicLong(0L));
                count.addAndGet(nGram.getCount());
              }
            });
    AtomicLong rank = new AtomicLong(1);
    List<NGram> topNGrams =
        allNgrams.entrySet().stream()
            .sorted((o1, o2) -> (int) (o1.getValue().get() - o2.getValue().get()))
            .map(
                entry -> {
                  return new NGram(entry.getKey(), entry.getValue().get(), rank.getAndIncrement());
                })
            .collect(Collectors.toList())
            .subList(0, 100);
    Map<String, Object> result = new HashMap<>();
    result.put("metadata", "");
    result.put("population", totalPopulation);
    result.put("predictionCount", filteredToTimespan.size());
    result.put("ngrams", topNGrams);
    return result;
  }

  private List<NGramData> fetchNGramData(String modelId, Instant startAt, Instant endAt) {
    final File fileRoot = new File(modelDataStoragePath);
    final File[] filteredToModel =
        fileRoot.listFiles((dir, name) -> name.startsWith(modelId + "-"));
    return IntStream.range(0, filteredToModel.length)
        .mapToObj(i -> Pair.of(i, filteredToModel[i]))
        .filter(
            pair -> {
              final Instant fileTimestamp = extractTimestamp(pair.getValue().getName());
              return fileTimestamp.isAfter(startAt) && fileTimestamp.isBefore(endAt);
            })
        .map(
            pair -> {
              final long rank = pair.getKey();
              final File file = pair.getValue();
              try {
                final String fileContents =
                    Files.lines(Paths.get(file.getAbsolutePath())).collect(Collectors.joining());
                Map<String, Object> rootObject = new Gson().fromJson(fileContents, Map.class);
                final Long populationSize =
                    Long.parseLong((String) rootObject.get("populationSize"));
                final Long n = Long.parseLong((String) rootObject.get("n"));
                final List<Map<String, Object>> ngramMaps =
                    (List<Map<String, Object>>) rootObject.get("data");
                final List<NGram> ngrams =
                    ngramMaps.stream()
                        .map(
                            ngramMap -> {
                              List<String> gram = (List<String>) ngramMap.get("ngram");
                              Long count = Long.parseLong((String) ngramMap.get("count"));
                              return new NGram(gram, count, rank);
                            })
                        .collect(Collectors.toList());
                return new NGramData(populationSize, n, ngrams);
              } catch (IOException e) {
                LOGGER.error(e);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private Instant extractTimestamp(String fileName) {
    final String[] tokens = fileName.split("-");
    final String timestampStr = tokens[1];
    final Long timestamp = Long.parseLong(timestampStr);
    return Instant.ofEpochMilli(timestamp);
  }

  private String extractEndpoint(String fileName) {
    final String[] tokens = fileName.split("-");
    return tokens[1];
  }

  class NGramData {
    final Long population;
    final Long n;
    final List<NGram> ngrams;

    public NGramData(Long population, Long n, List<NGram> ngrams) {
      this.population = population;
      this.n = n;
      this.ngrams = ngrams;
    }

    public Long getPopulation() {
      return population;
    }

    public Long getN() {
      return n;
    }

    public List<NGram> getNgrams() {
      return ngrams;
    }
  }

  class NGram {
    final List<String> grams;
    final Long count;
    final Long rank;

    public NGram(List<String> grams, Long count, Long rank) {
      this.grams = grams;
      this.count = count;
      this.rank = rank;
    }

    public List<String> getGrams() {
      return grams;
    }

    public Long getCount() {
      return count;
    }

    public Long getRank() {
      return rank;
    }
  }
}
