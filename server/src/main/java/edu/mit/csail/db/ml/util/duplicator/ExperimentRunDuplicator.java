package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentrunRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep5;
import org.jooq.Query;

import java.sql.Timestamp;

public class ExperimentRunDuplicator extends Duplicator<ExperimentrunRecord> {
  InsertValuesStep5<ExperimentrunRecord, Integer, Integer, String, String, Timestamp> query;

  public ExperimentRunDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.EXPERIMENTRUN).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.EXPERIMENTRUN,
      Tables.EXPERIMENTRUN.ID,
      Tables.EXPERIMENTRUN.EXPERIMENT,
      Tables.EXPERIMENTRUN.DESCRIPTION,
      Tables.EXPERIMENTRUN.SHA,
      Tables.EXPERIMENTRUN.CREATED
    );
  }

  @Override
  protected void updateQuery(ExperimentrunRecord rec, int iteration) {
    query = query.values(maxId, rec.getExperiment(), rec.getDescription(), rec.getSha(), rec.getCreated());
  }

  private static ExperimentRunDuplicator instance = null;
  public static ExperimentRunDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new ExperimentRunDuplicator(ctx);
    }
    return instance;
  }
}
