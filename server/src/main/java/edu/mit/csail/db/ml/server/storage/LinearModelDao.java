package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.FeatureRecord;
import jooq.sqlite.gen.tables.records.LinearmodelRecord;
import jooq.sqlite.gen.tables.records.LinearmodeltermRecord;
import jooq.sqlite.gen.tables.records.ModelobjectivehistoryRecord;
import modeldb.LinearModel;
import modeldb.LinearModelTerm;
import modeldb.ResourceNotFoundException;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class contains logic for reading and storing linear models.
 */
public class LinearModelDao {
  /**
   * Store a LinearModelTerm in the database.
   * @param linearModelId - The ID of the linear model (i.e. a primary key in the Transformer table).
   * @param termIndex - The index of the feature vector that this term (i.e. coefficient) belongs to. Use 0 for the
   *                  intercept term.
   * @param term - The actual term.
   * @param ctx - The database context.
   * @return The row in the LinearModelTerm table after storage.
   */
  private static LinearmodeltermRecord store(int linearModelId, int termIndex, LinearModelTerm term, DSLContext ctx) {
    LinearmodeltermRecord termRec = ctx.newRecord(Tables.LINEARMODELTERM);
    termRec.setId(null);
    termRec.setModel(linearModelId);
    termRec.setTermindex(termIndex);
    termRec.setCoefficient(term.coefficient);
    if (term.isSetTStat()) {
      termRec.setTstat(term.getTStat());
    }
    if (term.isSetStdErr()) {
      termRec.setStderr(term.getStdErr());
    }
    if (term.isSetPValue()) {
      termRec.setPvalue(term.getPValue());
    }
    termRec.store();
    termRec.getId();
    return termRec;
  }

  /**
   * Store a LinearModel.
   * @param modelId - The ID of the underlying model (i.e. a primary key from the Transformer table).
   * @param model - The LinearModel.
   * @param ctx - The database context.
   * @return Whether the storage was successful.
   * @throws ResourceNotFoundException - Thrown if there's no entry in the Transformer table with ID modelId.
   */
  public static boolean store(int modelId, LinearModel model, DSLContext ctx) throws ResourceNotFoundException {
    // Check if the modelId exists.
    if (!TransformerDao.exists(modelId, ctx)) {
      throw new ResourceNotFoundException(
        String.format("Cannot store linear model for Transformer %d because it doesn't exist", modelId)
      );
    }

    // Store the LinearModel.
    LinearmodelRecord lmRec = ctx.newRecord(Tables.LINEARMODEL);
    lmRec.setId(null);
    lmRec.setModel(modelId);
    if (model.isSetRmse()) {
      lmRec.setRmse(model.getRmse());
    }
    if (model.isSetExplainedVariance()) {
      lmRec.setExplainedvariance(model.getExplainedVariance());
    }
    if (model.isSetR2()) {
      lmRec.setR2(model.getR2());
    }
    lmRec.store();
    lmRec.getId();

    // Store the intercept term.
    if (model.isSetInterceptTerm()) {
      store(modelId, 0, model.getInterceptTerm(), ctx);
    }

    // Store the featureTerms.
    IntStream
      .range(0, model.featureTerms.size())
      .forEach(i -> store(modelId, i + 1, model.featureTerms.get(i), ctx));

    // Store the objective history.
    if (model.isSetObjectiveHistory()) {
      IntStream.range(0, model.objectiveHistory.size())
        .forEach(i -> {
          double objectiveValue = model.objectiveHistory.get(i);
          ModelobjectivehistoryRecord mohRec = ctx.newRecord(Tables.MODELOBJECTIVEHISTORY);
          mohRec.setId(null);
          mohRec.setModel(modelId);
          mohRec.setIteration(i + 1);
          mohRec.setObjectivevalue(objectiveValue);
          mohRec.store();
          mohRec.getId();
        });
    }

    // Now we will update the features for the model so that the
    // importance is equal to the absolute value of the coefficient.

    // Get the features for the model.
    List<FeatureRecord> features = ctx.selectFrom(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(modelId))
      .fetch()
      .stream()
      .collect(Collectors.toList());

    // Delete all the features for the models.
    ctx.deleteFrom(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(modelId));

    // Store the features again.
    features.forEach(ft -> {
      ft.setImportance(Math.abs(model.featureTerms.get(ft.getFeatureindex()).coefficient));
      ft.store();
      ft.getImportance();
    });

    // TODO: Is this return value really necessary? After all, we never return false.
    return true;
  }
}
