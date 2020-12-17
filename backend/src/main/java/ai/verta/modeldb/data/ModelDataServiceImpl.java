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
  public static final String SEPARATOR = "_-_";
  public static final int TOP_N = 100;

  private final String modelDataStoragePath;

  public ModelDataServiceImpl(String modelDataStoragePath) {
    this.modelDataStoragePath = modelDataStoragePath;
    // ensure it exists
    new File(modelDataStoragePath).mkdirs();
  }

  private String buildFileName(String modelId, Long timestampMillis, String endpoint) {
    return modelDataStoragePath
        + "/"
        + modelId
        + SEPARATOR
        + endpoint
        + SEPARATOR
        + timestampMillis;
  }

  private String buildFileName(ModelDataMetadata metadata) {
    final String modelId = metadata.getModelId();
    final Long timestampMillis = metadata.getTimestampMillis();
    final String endpoint = trimEndpoint(metadata.getEndpoint());
    return buildFileName(modelId, timestampMillis, endpoint);
  }

  @Override
  public void storeModelData(
      StoreModelDataRequest request,
      StreamObserver<StoreModelDataRequest.Response> responseObserver) {
    LOGGER.info("StoreModelData: " + request);
    final ModelDataMetadata metadata = request.getModelData().getMetadata();
    final String data = request.getModelData().getData();
    LOGGER.info("Data is " + data.length() + " characters.");
    final String fileName = buildFileName(metadata);
    LOGGER.info("Storing to file: " + fileName);
    try (FileWriter writer = new FileWriter(fileName)) {
      writer.write(data);
      LOGGER.info("Write completed successfully.");
    } catch (IOException ex) {
      LOGGER.error("Write failed:");
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
    LOGGER.info("Time window start at: " + startAt);
    final Instant endAt = Instant.ofEpochMilli(request.getEndTimeMillis());
    LOGGER.info("Time window end at: " + endAt);
    final Optional<String> endpoint = resolveEndpoint(request.getEndpoint());
    final Optional<Long> nNess =
        request.getNNess() > 0 ? Optional.of(request.getNNess()) : Optional.empty();
    final List<NGramData> filteredToTimespan =
        fetchNGramData(request.getModelId(), endpoint, nNess, startAt, endAt);
    LOGGER.info("Found " + filteredToTimespan.size() + " predictions in the time window.");
    LOGGER.info("Aggregating predictions.");
    Map<String, Object> aggregate = aggregateTimespan(filteredToTimespan);
    Map<String, Object> payload =
        buildPayload(startAt, endAt, request.getModelId(), request.getEndpoint(), aggregate);
    String json = new Gson().toJson(payload);
    LOGGER.info("Complete, returning response.");
    responseObserver.onNext(GetModelDataRequest.Response.newBuilder().setData(json).build());
    responseObserver.onCompleted();
  }

  @Override
  public void getModelDataDiff(
      GetModelDataDiffRequest request,
      StreamObserver<GetModelDataDiffRequest.Response> responseObserver) {
    LOGGER.info("GetModelDataDiff: " + request);
    final Instant startAt = Instant.ofEpochMilli(request.getStartTimeMillis());
    LOGGER.info("Time window start at: " + startAt);
    final Instant endAt = Instant.ofEpochMilli(request.getEndTimeMillis());
    LOGGER.info("Time window end at: " + endAt);
    final Optional<String> endpoint = resolveEndpoint(request.getEndpoint());
    final Optional<Long> nNess =
        request.getNNess() > 0 ? Optional.of(request.getNNess()) : Optional.empty();
    final List<NGramData> aFilteredToTimespan =
        fetchNGramData(request.getModelIdA(), endpoint, nNess, startAt, endAt);
    LOGGER.info(
        "Found "
            + aFilteredToTimespan.size()
            + " predictions in the time window for model data A.");
    final List<NGramData> bFilteredToTimespan =
        fetchNGramData(request.getModelIdB(), endpoint, nNess, startAt, endAt);
    LOGGER.info(
        "Found "
            + bFilteredToTimespan.size()
            + " predictions in the time window for model data B.");

    LOGGER.info("Aggregating predictions.");
    final Map<String, Object> aggregateA = aggregateTimespan(aFilteredToTimespan);
    final Map<String, Object> aggregateB = aggregateTimespan(bFilteredToTimespan);

    Map<String, Object> leftPayload =
        buildPayload(startAt, endAt, request.getModelIdA(), request.getEndpoint(), aggregateA);
    Map<String, Object> rightPayload =
        buildPayload(startAt, endAt, request.getModelIdB(), request.getEndpoint(), aggregateB);

    LOGGER.info("Building diff.");
    Map<String, Object> diffPayload =
        buildDiffPayload(
            startAt, endAt, request.getModelIdB(), request.getEndpoint(), aggregateA, aggregateB);

    Map<String, Object> payload = new HashMap<>();
    payload.put("left", leftPayload);
    payload.put("right", rightPayload);
    payload.put("diff", diffPayload);
    String json = new Gson().toJson(payload);
    LOGGER.info("Complete, returning response.");
    responseObserver.onNext(GetModelDataDiffRequest.Response.newBuilder().setData(json).build());
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

  private Map<String, Object> buildDiffPayload(
      Instant start,
      Instant end,
      String modelId,
      String endpoint,
      Map<String, Object> aggregateA,
      Map<String, Object> aggregateB) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("start_time_millis", start.toEpochMilli());
    metadata.put("end_time_millis", end.toEpochMilli());
    metadata.put("model_id", modelId);
    metadata.put("endpoint", endpoint);

    final Map<String, Object> dataA = (Map<String, Object>) aggregateA.get("data");
    final Map<String, Object> dataB = (Map<String, Object>) aggregateB.get("data");

    final long predictionCountA = (Long) dataA.get("prediction_count");
    final long predictionCountB = (Long) dataB.get("prediction_count");

    final long populationA = (Long) dataA.get("population_size");
    final long populationB = (Long) dataB.get("population_size");

    final long nA = (Long) dataA.get("n");
    final long nB = (Long) dataB.get("n");

    final List<NGram> nGramsA = (List<NGram>) dataA.get("ngrams");
    final List<NGram> nGramsB = (List<NGram>) dataB.get("ngrams");

    final List<NGram> diffedNGrams = new ArrayList<>();
    for (NGram left : nGramsA) {
      Optional<NGram> rightOptional =
          nGramsB.stream()
              .filter(
                  nGram ->
                      nGram.getGrams().size() == left.getGrams().size()
                          && nGram.getGrams().containsAll(left.getGrams()))
              .findFirst();
      if (!rightOptional.isPresent()) {
        diffedNGrams.add(new NGram(left.getGrams(), 0L, 0L));
      } else {
        NGram right = rightOptional.get();
        diffedNGrams.add(
            new NGram(
                left.getGrams(),
                right.getCount() - left.getCount(),
                right.getRank() - left.getRank()));
      }
    }
    for (NGram right : nGramsB) {
      Optional<NGram> leftOptional =
          nGramsB.stream()
              .filter(
                  nGram ->
                      nGram.getGrams().size() == right.getGrams().size()
                          && nGram.getGrams().containsAll(right.getGrams()))
              .findFirst();
      if (!leftOptional.isPresent()) {
        diffedNGrams.add(new NGram(right.getGrams(), 0L, 0L));
      }
    }
    final List<NGram> sortedDiff =
        diffedNGrams.stream()
            .sorted((o1, o2) -> (int) (o2.getRank() - o1.getRank()))
            .collect(Collectors.toList())
            .subList(0, TOP_N);

    final Map<String, Object> data = new HashMap<>();
    data.put("prediction_count", predictionCountB - predictionCountA);
    data.put("population_size", populationB - populationA);
    data.put("n", nB - nA);

    Map<String, Object> payload = new HashMap<>();
    payload.put("metadata", metadata);
    payload.put("data", sortedDiff);
    return payload;
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
    List<NGram> sortedNGrams =
        allNgrams.entrySet().stream()
            .sorted((o1, o2) -> (int) (o1.getValue().get() - o2.getValue().get()))
            .map(entry -> new NGram(entry.getKey(), entry.getValue().get(), rank.getAndIncrement()))
            .collect(Collectors.toList());
    int topFilter = sortedNGrams.size() > TOP_N ? TOP_N : sortedNGrams.size();
    final List<NGram> topNGrams = sortedNGrams.subList(0, topFilter);
    Map<String, Object> result = new HashMap<>();
    result.put("metadata", "");
    result.put("population", totalPopulation);
    result.put("predictionCount", filteredToTimespan.size());
    result.put("ngrams", topNGrams);
    return result;
  }

  private List<NGramData> fetchNGramData(
      String modelId,
      Optional<String> endpoint,
      Optional<Long> nNess,
      Instant startAt,
      Instant endAt) {
    LOGGER.info("Fetching nGrams for model " + modelId);
    if (endpoint.isPresent()) {
      LOGGER.info("Filtering predictions to endpoint " + endpoint.get());
    }
    if (nNess.isPresent()) {
      LOGGER.info("Filtering predictions to n-ness " + nNess.get());
    }
    LOGGER.info("Filtering predictions to time range " + startAt + " until " + endAt);
    final File fileRoot = new File(modelDataStoragePath);
    final File[] filteredToModel =
        fileRoot.listFiles((dir, name) -> name.startsWith(modelId + SEPARATOR));
    return IntStream.range(0, filteredToModel.length)
        .mapToObj(i -> Pair.of(i, filteredToModel[i]))
        .filter(
            pair -> {
              final Instant fileTimestamp = extractTimestamp(pair.getValue().getName());
              LOGGER.info(
                  "File timestamp is "
                      + fileTimestamp
                      + ", comparing to "
                      + startAt
                      + " and "
                      + endAt);
              LOGGER.info(
                  "File timestamp "
                      + fileTimestamp
                      + (fileTimestamp.isAfter(startAt) ? " is after " : " is NOT after ")
                      + startAt);
              LOGGER.info(
                  "File timestamp "
                      + fileTimestamp
                      + (fileTimestamp.isBefore(endAt) ? " is before " : " is NOT before ")
                      + endAt);
              final boolean inTimeWindow =
                  fileTimestamp.isAfter(startAt) && fileTimestamp.isBefore(endAt);
              LOGGER.info("File " + pair.getValue().getName() + " is in the time window.");
              final String fileEndpoint = extractEndpoint(pair.getValue().getName());
              LOGGER.info(
                  "Comparing file endpoint "
                      + fileEndpoint
                      + " to request endpoint "
                      + endpoint.get());
              boolean endpointMatches =
                  endpoint.isPresent() ? endpoint.get().equals(fileEndpoint) : true;
              if (inTimeWindow && endpointMatches) {
                LOGGER.info(
                    "File "
                        + pair.getValue().getName()
                        + " is in the time window and matches the endpoint filter.");
              }
              return inTimeWindow && endpointMatches;
            })
        .map(
            pair -> {
              final long rank = pair.getKey();
              final File file = pair.getValue();
              try {
                final String fileContents =
                    Files.lines(Paths.get(file.getAbsolutePath())).collect(Collectors.joining());
                Map<String, Object> rootObject = new Gson().fromJson(fileContents, Map.class);
                LOGGER.info("Root object: " + rootObject);
                final Double populationSizeDouble = (Double) rootObject.get("population_size");
                final Long populationSize = populationSizeDouble.longValue();
                //                final Long predictionCount =
                //                    Long.parseLong((String) rootObject.get("prediction_count"));
                final Double nDouble = (Double) rootObject.get("n");
                final Optional<Long> n =
                    nDouble != null
                        ? Optional.of(nDouble.longValue())
                        : Optional.empty();
                if (nNess.isPresent() && n.isPresent()) {
                  if (n.get() != nNess.get()) {
                    return null;
                  }
                }
                final List<Map<String, Object>> ngramMaps =
                    (List<Map<String, Object>>) rootObject.get("ngrams");
                final List<NGram> ngrams =
                    ngramMaps.stream()
                        .map(
                            ngramMap -> {
                              List<String> gram = (List<String>) ngramMap.get("ngram");
                              Long count = Long.parseLong((String) ngramMap.get("count"));
                              return new NGram(gram, count, rank);
                            })
                        .collect(Collectors.toList());
                return new NGramData(populationSize, n.get(), ngrams);
              } catch (IOException e) {
                LOGGER.error(e);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private String trimEndpoint(String endpoint) {
    return endpoint.replace("/", "");
  }

  private Optional<String> resolveEndpoint(String endpoint) {
    if (endpoint == null || endpoint.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(trimEndpoint(endpoint));
  }

  private String extractEndpoint(String fileName) {
    LOGGER.info("Extracting endpoint from filename " + fileName);
    final String[] tokens = fileName.split(SEPARATOR);
    LOGGER.info("Endpoint: " + tokens[1]);
    return tokens[1];
  }

  private Instant extractTimestamp(String fileName) {
    LOGGER.info("Extracting timestamp from filename " + fileName);
    final String[] tokens = fileName.split(SEPARATOR);
    final String timestampStr = tokens[tokens.length - 1];
    LOGGER.info("Timestamp string: " + timestampStr);
    final Long timestamp = Long.parseLong(timestampStr);
    return Instant.ofEpochMilli(timestamp);
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
