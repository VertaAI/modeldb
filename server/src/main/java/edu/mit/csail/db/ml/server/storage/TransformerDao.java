package edu.mit.csail.db.ml.server.storage;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.FiteventRecord;
import jooq.sqlite.gen.tables.records.TransformerRecord;
import edu.mit.csail.db.ml.server.storage.metadata.MetadataDb;
import modeldb.*;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.nio.file.Paths;
import java.util.*;

import static jooq.sqlite.gen.Tables.TRANSFORMER;

/**
 * This class contains logic for reading and storing Transformers in the database.
 *
 * It is worth pointing out that a "model" is defined as a Transformer that was created by a FitEvent.
 */
public class TransformerDao {
  /**
   * Return the filepath to the serialized model.
   * @param id - The ID of a model (i.e. entry in the Transformer table). We will return the filepath where this
   *           model is serialized.
   * @param ctx - The database context.
   * @return The path to the file that contains a serialized version of the model with ID modelId.
   * @throws ResourceNotFoundException - Thrown if there's no entry in the Transformer table with ID id.
   * @throws InvalidFieldException - Thrown if the filepath of the model with ID id is either null or the empty string.
   */
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

  /**
   * Check if there's a serialized model file at the given filepath.
   * @param filepath - The filepath to check.
   * @param ctx - The database context.
   * @return Whether any model has been serialized to the given filepath.
   */
  public static boolean filePathExists(String filepath, DSLContext ctx) {
    return ctx.selectFrom(Tables.TRANSFORMER).where(Tables.TRANSFORMER.FILEPATH.eq(filepath)).fetchOne() != null;
  }

  /**
   * Create or fetch the filepath to the serialized model file of the given Transformer.
   * @param t - The Transformer. The ID should be -1 if you want to create a new Transformer. It should be positive
   *          if you want to refer to an existing Transformer.
   * @param experimentRunId - The experiment run that contains the given Transformer.
   * @param desiredFilename - The desired filename (note that this is NOT the same thing as the filepath, because
   *                        a filepath = prefix + filename). If this Transformer does not already have a filepath and if
   *                        there's no file with this name, then this name will be used in the filepath for the
   *                        Transformer. If this Transformer does not have a filepath, but the filename is already
   *                        taken, then a random UUID will be appended to this filename, and the result will be
   *                        used as the filename for the serialized Transformer. If the Transformer already has
   *                        a filepath, then this argument is ignored.
   * @param ctx - The database context.
   * @return The filepath to where the serialized Transformer should be stored.
   * @throws ResourceNotFoundException - Thrown if there is no Transformer in the database that has the ID t.id.
   */
  public static String getFilePath(Transformer t,
                                   int experimentRunId,
                                   String desiredFilename,
                                   DSLContext ctx) throws ResourceNotFoundException {
    // If the ID is postive, then ensure that the Transformer exists in the database.
    if (t.id > 0 && !exists(t.id, ctx)) {
      throw new ResourceNotFoundException(String.format(
        "Cannot fetch or create a filepath for Transformer %d because it does not exist",
        t.id
      ));
    }

    // Ensure that the Transformer is stored.
    TransformerRecord rec = store(t, experimentRunId, ctx);

    // Check if the Transforomer has a filepath.
    boolean hasFilepath = rec.getFilepath() != null && rec.getFilepath().length() > 0;

    // Generate a filepath.
    String newFilepath = generateFilepath();

    // If a desired filename is given...
    if (desiredFilename != null && desiredFilename.length() > 0) {
      // Check if there's already uses this filename. If so, then append a random UUID to the filename. Otherwise,
      // use the filename as-is.
      if (filePathExists(Paths.get(ModelDbConfig.getInstance().fsPrefix, desiredFilename).toString(), ctx)) {
        newFilepath = Paths.get(ModelDbConfig.getInstance().fsPrefix,
          desiredFilename + UUID.randomUUID().toString()).toString();
      } else {
        newFilepath = Paths.get(ModelDbConfig.getInstance().fsPrefix, desiredFilename).toString();
      }
    }
    // Set the filepath (or leave it unchanged if one already exists).
    rec.setFilepath(hasFilepath ? rec.getFilepath() : newFilepath);

    // Store the Transformer with the correct filepath and return the filepath.
    rec.store();
    rec.getId();
    return rec.getFilepath();
  }

  /**
   * Check if there exists a Transformer with the given ID.
   * @param id - The ID to check.
   * @param ctx - The database context.
   * @return Whether there exists a row in the Transformer table that has primary key equal to id.
   */
  public static boolean exists(int id, DSLContext ctx) {
    return ctx.selectFrom(Tables.TRANSFORMER).where(Tables.TRANSFORMER.ID.eq(id)).fetchOne() != null;
  }

  /**
   * @return A randomly generated filepath, which takes the form: filesystem prefix + "/model_" + random UUID.
   */
  public static String generateFilepath() {
    String uuid = UUID.randomUUID().toString();
    return Paths.get(ModelDbConfig.getInstance().fsPrefix, "model_" + uuid).toString();
  }

  /**
   * Store the given Transformer in the database.
   * @param t - The Transformer.
   * @param experimentId - The experiment run that should contain the given Transformer.
   * @param ctx - The database context.
   * @return The row of the Transformer table reflecting the given Transformer after it has been stored.
   */
  public static TransformerRecord store(Transformer t, int experimentId, DSLContext ctx) {
    // Check if there's already a Transformer with the given ID. If so, just return it.
    TransformerRecord rec = ctx.selectFrom(Tables.TRANSFORMER).where(Tables.TRANSFORMER.ID.eq(t.id)).fetchOne();
    if (rec != null) {
      return rec;
    }

    // Store an entry in the Transformer table.
    final TransformerRecord tRec = ctx.newRecord(TRANSFORMER);
    tRec.setId(null);
    tRec.setTransformertype(t.transformerType);
    tRec.setTag(t.tag);
    tRec.setExperimentrun(experimentId);
    tRec.store();
    return tRec;
  }

  /**
   * Read the Transformer with the given ID.
   * @param transformerId - The ID of the Transformer.
   * @param ctx - The database context.
   * @return The row of the Transformer table that reflects the given Transformer.
   * @throws ResourceNotFoundException - Thrown if there's no entry in the Transformer table with the given
   * transformerId.
   */
  private static TransformerRecord read(int transformerId, DSLContext ctx)
    throws ResourceNotFoundException {
    // Query for the given Transformer.
    TransformerRecord rec = ctx.selectFrom(Tables.TRANSFORMER)
      .where(Tables.TRANSFORMER.ID.eq(transformerId))
      .fetchOne();

    // Throw exception if it doesn't exist. Otherwise, return it.
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Could not find record for Transformer %d, because it does not exist.",
        transformerId
      ));
    }
    return rec;
  }

  /**
   * Read the FitEvent that created the given model (i.e. Transformer with an associated FitEvent).
   * @param modelId - The ID of the model (i.e. a primary key in the Transformer table).
   * @param ctx - The database context.
   * @return The row of the FitEvent table that reflects the event that created this Transformer.
   * @throws ResourceNotFoundException - Thrown if there's no FitEvent that created the Transformer with ID modelId.
   */
  private static FiteventRecord readFitEvent(int modelId, DSLContext ctx)
    throws ResourceNotFoundException {
    // Query for the FitEvent that created Transformer modelId.
    FiteventRecord rec = ctx
        .selectFrom(Tables.FITEVENT)
        .where(Tables.FITEVENT.TRANSFORMER.eq(modelId))
        .fetchOne();

    // Throw exception if we can't find the FitEvent. Otherwise, return the FitEvent.
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Could not find corresponding FitEvent for Transformer %d",
        modelId
      ));
    }
    return rec;
  }

  /**
   * Read the names of the features that are used by the given Transformer.
   * @param transformerId - The ID of a Transformer. This MUST correspond to an actual Transformer in the Transformer
   *                      table.
   * @param ctx - The database context.
   * @return The names of the features that the Transformer with ID transformerId uses.
   */
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
  /**
   * Reads the metrics evaluated on a given Transformer.
   * @param transformerId - The ID of a Transformer. This MUST correspond to an entry in the Transformer table.
   * @param ctx - The database context.
   * @return A map that goes from metric name to DataFrame ID to metric value. For example. if we call the resulting
   * map "metricMap", then if metricMap.get("accuracy").get(12) equals some value (say 0.96), then that means that
   * the Transformer with ID transformerId had an accuracy of 0.96 when evaluated on the DataFrame with ID 12.
   */
  public static Map<String, Map<Integer, Double>> readMetrics(int transformerId, DSLContext ctx) {
    Map<String, Map<Integer, Double>> metricMap = new HashMap<>();

    ctx
      .select(Tables.METRICEVENT.METRICTYPE, Tables.METRICEVENT.DF, Tables.METRICEVENT.METRICVALUE)
      .from(Tables.METRICEVENT)
      .where(Tables.METRICEVENT.TRANSFORMER.eq(transformerId))
      .fetch()
      .forEach(rec -> {
        String metricName = rec.value1();
        int dataframeId = rec.value2();
        double metricValue = rec.value3();
        if (!metricMap.containsKey(metricName)) {
          metricMap.put(metricName, new HashMap<>());
        }

        Map<Integer, Double> oldMap = metricMap.get(metricName);
        oldMap.put(dataframeId, metricValue);

        metricMap.put(metricName, oldMap);
      });

    return metricMap;
  }

  /**
   * Read all the Annotations that mention the given Transformer.
   * @param transformerId - The ID of a Transformer. This MUST correspond to an entry in the Transformer table.
   * @param ctx - The database context.
   * @return The string representations of the Annotations that mention the Transformer with ID transformerId.
   */
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

  /**
   * Read various pieces of information that we know about a given model.
   * @param modelId - The ID of a model. Recall that a model is a Transformer that was created by a FitEvent.
   * @param ctx - The database context.
   * @return A response containing various information (e.g. the FitEvent that created the model, the feature names,
   * the annotations) of the given model.
   * @throws ResourceNotFoundException - Thrown if there's no Transformer with ID modelId or if there's no FitEvent
   * that created the Transformer with ID modelId.
   */
  public static ModelResponse readInfo(int modelId, DSLContext ctx, MetadataDb metadataDb)
    throws ResourceNotFoundException {
    // First read the Transformer record.
    TransformerRecord rec = read(modelId, ctx);

    String metadata = MetadataDao.get(modelId, metadataDb);

    // get experiment run associated with the transformer
    ExperimentRun er = ExperimentRunDao.read(rec.getExperimentrun(), ctx);

    // Get the experiment and project for the Transformer.
    Pair<Integer, Integer> experimentAndProjectId =
      ExperimentRunDao.getExperimentAndProjectIds(rec.getExperimentrun(), ctx);

    // Find the FitEvent that produced the Transformer.
    FiteventRecord feRec = readFitEvent(modelId, ctx);

    // Find the DataFrame mentioned in the FitEvent.
    DataFrame trainingDf = DataFrameDao.read(feRec.getDf(), ctx);

    // Read the TransformerSpec mentioned in the FitEvent.
    TransformerSpec spec = TransformerSpecDao.read(feRec.getTransformerspec(), ctx);

    // Read the features.
    List<String> features = readFeatures(modelId, ctx);

    // Get the metrics.
    Map<String, Map<Integer, Double>> metricMap = readMetrics(modelId, ctx);

    // Get the annotations.
    List<String> annotations = readAnnotations(modelId, ctx);

    String sha = er.getSha();


    // TODO: Read the LinearModel data if applicable.
    ModelResponse response = new ModelResponse(
      rec.getId(),
      rec.getExperimentrun(),
      experimentAndProjectId.getFirst(),
      experimentAndProjectId.getSecond(),
      trainingDf,
      spec,
      ProblemTypeConverter.fromString(feRec.getProblemtype()),
      features,
      Arrays.asList(feRec.getLabelcolumns().split(",")),
      Arrays.asList(feRec.getPredictioncolumns().split(",")),
      metricMap,
      annotations,
      sha,
      er.getCreated(),
      rec.getFilepath()
    );

    response.setMetadata(metadata);
    return response;
  }
}
