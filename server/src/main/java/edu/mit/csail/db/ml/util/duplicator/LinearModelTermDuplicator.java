
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.LinearmodeltermRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.InsertValuesStep7;
import org.jooq.Query;

public class LinearModelTermDuplicator extends Duplicator<LinearmodeltermRecord> {
  InsertValuesStep7<LinearmodeltermRecord, Integer, Integer, Integer, Double, Double, Double, Double> query;

  public LinearModelTermDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.LINEARMODELTERM).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.LINEARMODELTERM,
      Tables.LINEARMODELTERM.ID,
      Tables.LINEARMODELTERM.MODEL,
      Tables.LINEARMODELTERM.TERMINDEX,
      Tables.LINEARMODELTERM.COEFFICIENT,
      Tables.LINEARMODELTERM.TSTAT,
      Tables.LINEARMODELTERM.STDERR,
      Tables.LINEARMODELTERM.PVALUE
    );
  }

  @Override
  public void updateQuery(LinearmodeltermRecord rec, int iteration) {
    query = query.values(
      maxId,
      TransformerDuplicator.getInstance(ctx).id(rec.getModel(), iteration),
      rec.getTermindex(),
      rec.getCoefficient(),
      rec.getTstat(),
      rec.getStderr(),
      rec.getPvalue()
    );
  }

  private static LinearModelTermDuplicator instance = null;
  public static LinearModelTermDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new LinearModelTermDuplicator(ctx);
    }
    return instance;
  }
}

