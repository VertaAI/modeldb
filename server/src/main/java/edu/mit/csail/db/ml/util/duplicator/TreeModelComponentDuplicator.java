
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TreemodelcomponentRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.InsertValuesStep5;
import org.jooq.Query;

public class TreeModelComponentDuplicator extends Duplicator<TreemodelcomponentRecord> {
  InsertValuesStep5<TreemodelcomponentRecord, Integer, Integer, Integer, Double, Integer> query;

  public TreeModelComponentDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.TREEMODELCOMPONENT).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.TREEMODELCOMPONENT,
      Tables.TREEMODELCOMPONENT.ID,
      Tables.TREEMODELCOMPONENT.MODEL,
      Tables.TREEMODELCOMPONENT.COMPONENTINDEX,
      Tables.TREEMODELCOMPONENT.COMPONENTWEIGHT,
      Tables.TREEMODELCOMPONENT.ROOTNODE
    );
  }

  @Override
  public void updateQuery(TreemodelcomponentRecord rec, int iteration) {
    query = query.values(
      maxId,
      TransformerDuplicator.getInstance(ctx).id(rec.getModel(), iteration),
      rec.getComponentindex(),
      rec.getComponentweight(),
      TreeNodeDuplicator.getInstance(ctx).id(rec.getRootnode(), iteration)
    );
  }

  private static TreeModelComponentDuplicator instance = null;
  public static TreeModelComponentDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new TreeModelComponentDuplicator(ctx);
    }
    return instance;
  }
}

