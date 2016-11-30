
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.PipelinestageRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.InsertValuesStep6;
import org.jooq.Query;

public class PipelineStageDuplicator extends Duplicator<PipelinestageRecord> {
  InsertValuesStep6<PipelinestageRecord, Integer, Integer, Integer, Integer, Integer, Integer> query;

  public PipelineStageDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.PIPELINESTAGE).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.PIPELINESTAGE,
      Tables.PIPELINESTAGE.ID,
      Tables.PIPELINESTAGE.PIPELINEFITEVENT,
      Tables.PIPELINESTAGE.TRANSFORMORFITEVENT,
      Tables.PIPELINESTAGE.ISFIT,
      Tables.PIPELINESTAGE.STAGENUMBER,
      Tables.PIPELINESTAGE.EXPERIMENTRUN
    );
  }

  @Override
  public void updateQuery(PipelinestageRecord rec, int iteration) {
    query = query.values(
      maxId,
      FitEventDuplicator.getInstance(ctx).id(rec.getPipelinefitevent(), iteration),
      EventDuplicator.getInstance(ctx).id(rec.getTransformorfitevent(), iteration),
      rec.getIsfit(),
      rec.getStagenumber(),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static PipelineStageDuplicator instance = null;
  public static PipelineStageDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new PipelineStageDuplicator(ctx);
    }
    return instance;
  }
}

