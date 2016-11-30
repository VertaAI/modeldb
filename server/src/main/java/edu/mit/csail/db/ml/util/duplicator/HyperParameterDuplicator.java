
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.HyperparameterRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep8;
import org.jooq.Query;

public class HyperParameterDuplicator extends Duplicator<HyperparameterRecord> {
  InsertValuesStep8<HyperparameterRecord, Integer, Integer, String, String, String, Float, Float, Integer> query;

  public HyperParameterDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.HYPERPARAMETER).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.HYPERPARAMETER,
      Tables.HYPERPARAMETER.ID,
      Tables.HYPERPARAMETER.SPEC,
      Tables.HYPERPARAMETER.PARAMNAME,
      Tables.HYPERPARAMETER.PARAMTYPE,
      Tables.HYPERPARAMETER.PARAMVALUE,
      Tables.HYPERPARAMETER.PARAMMINVALUE,
      Tables.HYPERPARAMETER.PARAMMAXVALUE,
      Tables.HYPERPARAMETER.EXPERIMENTRUN
    );
  }

  @Override
  public void updateQuery(HyperparameterRecord rec, int iteration) {
    query = query.values(
      maxId,
      TransformerSpecDuplicator.getInstance(ctx).id(rec.getSpec(), iteration),
      rec.getParamname(),
      rec.getParamtype(),
      rec.getParamvalue(),
      rec.getParamminvalue(),
      rec.getParammaxvalue(),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static HyperParameterDuplicator instance = null;
  public static HyperParameterDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new HyperParameterDuplicator(ctx);
    }
    return instance;
  }
}

