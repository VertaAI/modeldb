package edu.mit.csail.db.ml.util.duplicator;

import org.jooq.DSLContext;
import org.jooq.Query;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract class that contains logic for duplicating the rows in a given table.
 * @param <T> - The type of the table record. This should be something like DataframeRecord or TransformerRecord.
 */
public abstract class Duplicator<T> {
  /**
   * When a row gets duplicated, we need to assign the new rows primary keys. For example, if we duplicate row 12 twice,
   * we create row 13 and row 14. We would like to track the fact that these three IDs are related. Thus, we use this
   * map to go from the original ID (primary key) to all the primary keys of related rows. In our example
   * above, the entry in the map would be 12 -> (12, 13, 14).
   */
  protected Map<Integer, List<Integer>> idsForOriginalId;

  /**
   * This maps from an original ID to the record with that original ID.
   */
  protected Map<Integer, T> recForOriginalId;

  /**
   * The database context.
   */
  protected DSLContext ctx;

  /**
   * The largest ID seen so far.
   */
  protected int maxId;

  /**
   * Rather than copying all the rows at once (this requires an INSERT statement), we buffer them and do the INSERT
   * periodically. This variable counts the number of copies that have been buffered and should be flushed to the
   * database with the INSERT.
   */
  int numBuffered;

  /**
   * @return The keys (in sorted order) of the idsForOriginalId map. This is the order in which the keys will be
   * processed. You can override this and process the keys in a different order if you'd like.
   */
  protected List<Integer> keys() {
    return idsForOriginalId.keySet().stream().sorted().collect(Collectors.toList());
  }

  /**
   * This duplicates all the rows a total of numIterations times.
   * @param numIterations - The number of times that each row should be copied.
   */
  void duplicate(int numIterations) {
    // Ensure that the query has been reset.
    resetQuery();

    // Get the keys (order matters) to be processed.
    List<Integer> keys = keys();

    // For each key (i.e. primary key).
    for (int id : keys) {
      // Get the original record.
      T rec = recForOriginalId.get(id);

      // Duplicate the row numIterations times.
      for (int i = 1; i < numIterations; i++) {
        maxId++;

        // Buffer the record and new primary key.
        updateQuery(rec, i);

        // Mark that this duplication has been processed.
        processed(id, maxId);

        // Attempt to execute the query (it will execute if the buffer is sufficiently large). If execution occurs,
        // then reset the query.
        if (tryExecute(getQuery())) {
          resetQuery();
        }
      }
    }

    // Force the query to execute to flush any remaining rows.
    forceExecute(getQuery());

    System.out.println("Finished " + this.getClass().getSimpleName());
  }

  /**
   * @return The query that performs the INSERT of all buffered rows.
   */
  abstract protected Query getQuery();

  /**
   * Removes any buffered rows for INSERT and resets the query.
   */
  abstract protected void resetQuery();

  /**
   * Buffer a row that should later be flushed to database.
   * @param rec - The original record (i.e. row) that should be flushed.
   * @param iteration - This is the index of the duplicate (e.g. 2 = second copy).
   */
  abstract protected void updateQuery(T rec, int iteration);

  /**
   * Set up the duplicator.
   * @param ctx - The database context.
   */
  protected void init(DSLContext ctx) {
    numBuffered = 0;
    idsForOriginalId = new HashMap<>();
    maxId= -1;
    recForOriginalId = new HashMap<>();
    this.ctx = ctx;
  }

  /**
   * If the buffer is large enough, execute the query.
   * @param q - The query to potentially execute.
   * @return Whether the query executed.
   */
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

  /**
   * If there are any buffered rows, execute the query.
   * @param q - The query to execute.
   */
  protected void forceExecute(Query q) {
    if (numBuffered > 0) {
      q.execute();
    }
  }

  /**
   * Update the HashMaps with the given record and ID.
   * @param id - The ID (primary key) of the record.
   * @param rec - The record associated with the primary key.
   */
  protected void updateMaps(int id, T rec) {
    ArrayList<Integer> l = new ArrayList<>();
    l.add(id);
    idsForOriginalId.put(id, l);
    recForOriginalId.put(id, rec);
    maxId = Math.max(id, maxId);
  }

  /**
   * Indicate that the row with the given has been original ID has been duplicated to produce a row with the given ID.
   * @param origId - The original ID of the row.
   * @param id - The ID of the copy.
   */
  protected void processed(int origId, int id) {
    idsForOriginalId.get(origId).add(id);
  }

  /**
   * @param originalId - The original ID of the row.
   * @param iteration - The copy of interest.
   * @return The ID of the iteration^th copy of the row with the given original ID.
   */
  public int id(int originalId, int iteration) {
    return idsForOriginalId.get(originalId).get(iteration);
  }
}
