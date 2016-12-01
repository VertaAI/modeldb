package edu.mit.csail.db.ml.util.duplicator;


import jooq.sqlite.gen.tables.records.DataframeRecord;
import org.jooq.DSLContext;
import org.jooq.Query;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Duplicator<T> {
  protected Map<Integer, List<Integer>> idsForOriginalId;
  protected Map<Integer, T> recForOriginalId;
  protected DSLContext ctx;
  protected int maxId;
  int numBuffered;

  protected List<Integer> keys() {
    return idsForOriginalId.keySet().stream().sorted().collect(Collectors.toList());
  }

  void duplicate(int numIterations) {
    resetQuery();

    List<Integer> keys = keys();
    for (int id : keys) {
      T rec = recForOriginalId.get(id);
      for (int i = 1; i < numIterations; i++) {
        maxId++;
        updateQuery(rec, i);
        processed(id, maxId);
        if (tryExecute(getQuery())) {
          resetQuery();
        }
      }
    }
    forceExecute(getQuery());

    System.out.println("Finished " + this.getClass().getSimpleName());
  }

  abstract protected Query getQuery();

  abstract protected void resetQuery();

  abstract protected void updateQuery(T rec, int iteration);

  protected void init(DSLContext ctx) {
    numBuffered = 0;
    idsForOriginalId = new HashMap<>();
    maxId= -1;
    recForOriginalId = new HashMap<>();
    this.ctx = ctx;
  }

  protected boolean tryExecute(Query q) {
    if (numBuffered < 100) {
      numBuffered++;
      return false;
    } else {
      q.execute();
      numBuffered = 0;
      return true;
    }
  }

  protected void forceExecute(Query q) {
    if (numBuffered > 0) {
      q.execute();
    }
  }

  protected void updateMaps(int id, T rec) {
    ArrayList<Integer> l = new ArrayList<>();
    l.add(id);
    idsForOriginalId.put(id, l);
    recForOriginalId.put(id, rec);
    maxId = Math.max(id, maxId);
  }

  protected void processed(int origId, int id) {
    idsForOriginalId.get(origId).add(id);
  }

  public int id(int originalId, int iteration) {
    return idsForOriginalId.get(originalId).get(iteration);
  }
}
