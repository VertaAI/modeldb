
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TreelinkRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.Query;

public class TreeLinkDuplicator extends Duplicator<TreelinkRecord> {
  InsertValuesStep4<TreelinkRecord, Integer, Integer, Integer, Integer> query;

  public TreeLinkDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.TREELINK).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.TREELINK,
      Tables.TREELINK.ID,
      Tables.TREELINK.PARENT,
      Tables.TREELINK.CHILD,
      Tables.TREELINK.ISLEFT
    );
  }

  @Override
  public void updateQuery(TreelinkRecord rec, int iteration) {
    query = query.values(
      maxId,
      TreeNodeDuplicator.getInstance(ctx).id(rec.getParent(), iteration),
      TreeNodeDuplicator.getInstance(ctx).id(rec.getChild(), iteration),
      rec.getIsleft()
    );
  }

  private static TreeLinkDuplicator instance = null;
  public static TreeLinkDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new TreeLinkDuplicator(ctx);
    }
    return instance;
  }
}

