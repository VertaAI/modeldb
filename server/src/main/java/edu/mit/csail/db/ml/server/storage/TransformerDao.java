package edu.mit.csail.db.ml.server.storage;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import javafx.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.FiteventRecord;
import jooq.sqlite.gen.tables.records.TransformerRecord;
import modeldb.*;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static jooq.sqlite.gen.Tables.TRANSFORMER;

public class TransformerDao {

  public static String path(int id, DSLContext ctx) throws ResourceNotFoundException, InvalidFieldException {
    TransformerRecord rec = ctx.selectFrom(Tables.TRANSFORMER).where(Tables.TRANSFORMER.ID.eq(id)).fetchOne();
    if (rec == null) {
      throw new ResourceNotFoundException(
        String.format("Could not find path to model file of Transformer with id %d", id)
      );
    }
    if (rec.getFilepath() == null || rec.getFilepath().equals(""))  {
      throw new InvalidFieldException(
        String.format("The Transformer with id %d does not have a model file", id)
      );
    }
    return rec.getFilepath();
  }

  public static boolean exists(int id, DSLContext ctx) {
    return ctx.selectFrom(Tables.TRANSFORMER).where(Tables.TRANSFORMER.ID.eq(id)).fetchOne() != null;
  }

  private static String generateFilepath() {
    String uuid = UUID.randomUUID().toString();
    return Paths.get(ModelDbConfig.getInstance().fsPrefix, uuid).toString();
  }

  public static TransformerRecord store(Transformer t, int experimentId, DSLContext ctx, boolean generateFilepath) {
    TransformerRecord rec = ctx.selectFrom(Tables.TRANSFORMER).where(Tables.TRANSFORMER.ID.eq(t.id)).fetchOne();
    if (rec != null) {
      if (generateFilepath && rec.getFilepath().length() == 0) {
        rec.setFilepath(generateFilepath());
        rec.store();
      }
      return rec;
    }

    final TransformerRecord tRec = ctx.newRecord(TRANSFORMER);
    tRec.setId(null);
    tRec.setTransformertype(t.transformerType);
    tRec.setWeights(t.weights.stream().map(String::valueOf).collect(Collectors.joining(",")));
    tRec.setTag(t.tag);
    tRec.setExperimentrun(experimentId);


    // Generate a UUID and filepath.
    String filePath = generateFilepath ? generateFilepath() : "";

    tRec.setFilepath(filePath);

    tRec.store();
    return tRec;
  }

  public static TransformerRecord store(Transformer t, int experimentId, DSLContext ctx) {
    return store(t, experimentId, ctx, false);
  }

  private static TransformerRecord read(int modelId, DSLContext ctx)
    throws ResourceNotFoundException {
    TransformerRecord rec = ctx.selectFrom(Tables.TRANSFORMER)
      .where(Tables.TRANSFORMER.ID.eq(modelId))
      .fetchOne();
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Could not find record for Transformer %d, because it does not exist.",
        modelId
      ));
    }
    return rec;
  }

  private static FiteventRecord readFitEvent(int modelId, DSLContext ctx)
    throws ResourceNotFoundException {
    FiteventRecord rec = ctx
        .selectFrom(Tables.FITEVENT)
        .where(Tables.FITEVENT.TRANSFORMER.eq(modelId))
        .fetchOne();
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Could not find corresponding FitEvent for Transformer %d",
        modelId
      ));
    }
    return rec;
  }

  public static List<String> readFeatures(int transformerId, DSLContext ctx) {
    return ctx
      .select(Tables.FEATURE.NAME)
      .from(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(transformerId))
      .orderBy(Tables.FEATURE.FEATUREINDEX.asc())
      .fetch()
      .map(Record1::value1);
  }

  /**
   * Reads the metrics for the given transformer. Creates a double-map from metric name to DataFrame ID to metric value.
   */
  public static Map<String, Map<Integer, Double>> readMetrics(int transformerId, DSLContext ctx) {
    Map<String, Map<Integer, Double>> metricMap = new HashMap<>();


    ctx
      .select(Tables.METRICEVENT.METRICTYPE, Tables.METRICEVENT.DF, Tables.METRICEVENT.METRICVALUE)
      .from(Tables.METRICEVENT)
      .where(Tables.METRICEVENT.TRANSFORMER.eq(transformerId))
      .fetch()
      .forEach(rec -> {
        String name = rec.value1();
        int df = rec.value2();
        double val = rec.value3();
        if (!metricMap.containsKey(name)) {
          metricMap.put(name, new HashMap<>());
        }

        Map<Integer, Double> oldMap = metricMap.get(name);
        oldMap.put(df, val);

        metricMap.put(name, oldMap);
      });

    return metricMap;
  }

  public static List<String> readAnnotations(int transformerId, DSLContext ctx) {
    // First figure out the IDs of the annotations that contain this transformer.
    List<Integer> annotationIds = ctx
      .selectDistinct(Tables.ANNOTATIONFRAGMENT.ANNOTATION)
      .from(Tables.ANNOTATIONFRAGMENT)
      .where(Tables.ANNOTATIONFRAGMENT.TRANSFORMER.eq(transformerId))
      .fetch()
      .map(Record1::value1);

    // Now create a string out of each of the Annotations.
    return AnnotationDao.readStrings(annotationIds, ctx);
  }

  public static ModelResponse readInfo(int modelId, DSLContext ctx)
    throws ResourceNotFoundException {
    // First read the Transformer record.
    TransformerRecord rec = read(modelId, ctx);

    // Get the experiment and project for the Transformer.
    Pair<Integer, Integer> experimentAndProjectId =
      ExperimentRunDao.getExperimentAndProjectIds(rec.getExperimentrun(), ctx);

    // Find the FitEvent that produced the Transformer.
    FiteventRecord feRec = readFitEvent(modelId, ctx);

    // Find the DataFrame mentioned in the FitEvent.
    DataFrame trainingDf = DataFrameDao.read(feRec.getDf(), ctx);

    // Read the TransformerSpec mentioned in the FitEvent.
    TransformerSpec spec = TransformerSpecDao.readTransformerSpec(feRec.getTransformerspec(), ctx);

    // Read the features.
    List<String> features = readFeatures(modelId, ctx);

    // Get the metrics.
    Map<String, Map<Integer, Double>> metricMap = readMetrics(modelId, ctx);

    // Get the annotations.
    List<String> annotations = readAnnotations(modelId, ctx);

    // For now, all the SHA values will be the empty string.
    String SHA = "";

    // TODO: Read the LinearModel data if applicable.

    return new ModelResponse(
      rec.getId(),
      rec.getExperimentrun(),
      experimentAndProjectId.getKey(),
      experimentAndProjectId.getValue(),
      trainingDf,
      spec,
      ProblemTypeConverter.fromString(feRec.getProblemtype()),
      features,
      Arrays.asList(feRec.getLabelcolumn().split(",")),
      Arrays.asList(feRec.getPredictioncolumns().split(",")),
      metricMap,
      annotations,
      SHA,
      rec.getFilepath()
    );
  }
}
