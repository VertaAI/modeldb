
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TreemodelRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep3;
import org.jooq.Query;

public class TreeModelDuplicator extends Duplicator<TreemodelRecord> {
  InsertValuesStep3<TreemodelRecord, Integer, Integer, String> query;

  public TreeModelDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.TREEMODEL).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.TREEMODEL,
      Tables.TREEMODEL.ID,
      Tables.TREEMODEL.MODEL,
      Tables.TREEMODEL.MODELTYPE
    );
  }

  @Override
  public void updateQuery(TreemodelRecord rec, int iteration) {
    query = query.values(
      maxId,
      TransformerDuplicator.getInstance(ctx).id(rec.getModel(), iteration),
      rec.getModeltype()
    );
  }

  private static TreeModelDuplicator instance = null;
  public static TreeModelDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new TreeModelDuplicator(ctx);
    }
    return instance;
  }
}

