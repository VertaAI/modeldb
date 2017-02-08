
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TreenodeRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep7;
import org.jooq.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TreeNodeDuplicator extends Duplicator<TreenodeRecord> {
  InsertValuesStep7<TreenodeRecord, Integer, Integer, Double, Double, Double, Integer, Integer> query;

  public TreeNodeDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.TREENODE).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public List<Integer> keys() {
    // First, make a set containing all the root nodes.
    Set<Integer> rootNodes = new HashSet<>();
    recForOriginalId.forEach((key, val) -> {
      if (val.getRootnode() != null) {
        rootNodes.add(val.getRootnode());
      }
    });

    // We'll process the root nodes first.
    List<Integer> keys = new ArrayList<>(rootNodes);

    // Now, add all non-root nodes.
    recForOriginalId.keySet().forEach(key -> {
      if (!rootNodes.contains(key)) {
        keys.add(key);
      }
    });
    assert(recForOriginalId.keySet().size() == idsForOriginalId.keySet().size());
    return keys;
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.TREENODE,
      Tables.TREENODE.ID,
      Tables.TREENODE.ISLEAF,
      Tables.TREENODE.PREDICTION,
      Tables.TREENODE.IMPURITY,
      Tables.TREENODE.GAIN,
      Tables.TREENODE.SPLITINDEX,
      Tables.TREENODE.ROOTNODE
    );
  }

  @Override
  public void updateQuery(TreenodeRecord rec, int iteration) {
    query = query.values(
      maxId,
      rec.getIsleaf(),
      rec.getPrediction(),
      rec.getImpurity(),
      rec.getGain(),
      rec.getSplitindex(),
      rec.getRootnode() != null
        ? TreeNodeDuplicator.getInstance(ctx).id(rec.getRootnode(), iteration)
        : null
    );
  }

  private static TreeNodeDuplicator instance = null;
  public static TreeNodeDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new TreeNodeDuplicator(ctx);
    }
    return instance;
  }
}

