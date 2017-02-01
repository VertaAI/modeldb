package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.AnnotationRecord;
import jooq.sqlite.gen.tables.records.AnnotationfragmentRecord;
import modeldb.AnnotationEvent;
import modeldb.AnnotationEventResponse;
import modeldb.AnnotationFragment;
import modeldb.AnnotationFragmentResponse;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class contains logic for storing and reading annotations.
 */
public class AnnotationDao {
  /**
   * Type for DataFrame annotation fragments.
   */
  private static final String DATAFRAME_TYPE = "dataframe";

  /**
   * Type for TransformerSpec annotation fragments.
   */
  private static final String SPEC_TYPE = "spec";

  /**
   * Type for Transformer annotation fragments.
   */
  private static final String TRANSFORMER_TYPE = "transformer";

  /**
   * Type for message (i.e. free-form text string) annotation fragments.
   */
  private static final String MESSAGE_TYPE = "message";

  /**
   * Store an annotation event in the database.
   * @param ae - The AnnotationEvent.
   * @param ctx - The database context.
   * @return The response after storing the annotation event.
   */
  public static AnnotationEventResponse store(AnnotationEvent ae, DSLContext ctx) {
    // Store an entry in the Annotation table.
    AnnotationRecord aRec = ctx.newRecord(Tables.ANNOTATION);
    aRec.setId(null);
    aRec.setPosted(new Timestamp((new Date()).getTime()));
    aRec.setExperimentrun(ae.experimentRunId);
    aRec.store();

    // Store the underlying primitives (i.e. DataFrame, TransformerSpec, or Transformer) for each AnnotationFragment.
    List<Integer> fragmentIds = ae
      .fragments
      .stream()
      .map(frag -> {
        switch (frag.type) {
          case DATAFRAME_TYPE:
            return DataFrameDao.store(frag.df, ae.experimentRunId, ctx).getId();
          case SPEC_TYPE:
            return TransformerSpecDao.store(frag.spec, ae.experimentRunId, ctx).getId();
          case TRANSFORMER_TYPE:
            return TransformerDao.store(frag.transformer, ae.experimentRunId, ctx).getId();
          default:
            return -1;
        }
      })
      .collect(Collectors.toList());

    // Store the AnnotationFragments.
    List<AnnotationFragmentResponse> fragResp = IntStream
      .range(0, fragmentIds.size())
      .mapToObj(ind -> {
        AnnotationfragmentRecord afRec = ctx.newRecord(Tables.ANNOTATIONFRAGMENT);
        AnnotationFragment frag = ae.fragments.get(ind);
        afRec.setId(null);
        afRec.setAnnotation(aRec.getId());
        afRec.setFragmentindex(ind);
        afRec.setType(frag.type);
        afRec.setExperimentrun(ae.experimentRunId);
        afRec.setTransformer(frag.type.equals(TRANSFORMER_TYPE) ? fragmentIds.get(ind) : null);
        afRec.setDataframe(frag.type.equals(DATAFRAME_TYPE) ? fragmentIds.get(ind) : null);
        afRec.setSpec(frag.type.equals(SPEC_TYPE) ? fragmentIds.get(ind) : null);
        afRec.setMessage(frag.type.equals(MESSAGE_TYPE) ? frag.message : null);
        afRec.store();
        afRec.getId();
        return new AnnotationFragmentResponse(frag.type, fragmentIds.get(ind));
      })
      .collect(Collectors.toList());

    // Return the response.
    return new AnnotationEventResponse(aRec.getId(), fragResp);
  }

  /**
   * Convert a row of the AnnotationFragment table into a string.
   * @param rec - The row of the AnnotationFragment table.
   * @return A string representation of the row. Basically, the message AnnotationFragments are preserved
   * as is, the Transformer/DataFrame/TransformerSpec fragments indicate their type and ID, and all the strings are
   * then concatenated together.
   */
  public static String fragmentToString(AnnotationfragmentRecord rec) {
    switch (rec.getType()) {
      case TRANSFORMER_TYPE: return String.format("Transformer(%d)", rec.getTransformer());
      case DATAFRAME_TYPE: return String.format("DataFrame(%d)", rec.getDataframe());
      case SPEC_TYPE: return String.format("TransformerSpec(%d)", rec.getSpec());
      default: return rec.getMessage();
    }
  }

  /**
   * Read the given Annotation IDs as a list of strings. If any of them do not exist, then an
   * empty string will be put in their place.
   * @param annotationIds - The IDs of the Annotations that should be read.
   * @param ctx - The database context.
   * @return - The string representation for each of the given Annotation IDs. If the ID is not found in
   * the database, the string representation will be the empty string. The value at the i^th index of this
   * list is the string representation for annotationIds.get(i).
   */
  public static List<String> readStrings(List<Integer> annotationIds, DSLContext ctx) {
    List<String> result = new ArrayList<>();
    annotationIds.forEach(aId -> result.add(""));

    ctx.selectFrom(Tables.ANNOTATIONFRAGMENT)
      .where(Tables.ANNOTATIONFRAGMENT.ANNOTATION.in(annotationIds))
      .orderBy(Tables.ANNOTATIONFRAGMENT.FRAGMENTINDEX.asc())
      .fetch()
      .forEach(rec -> {
        int index = annotationIds.indexOf(rec.getAnnotation());
        String str = fragmentToString(rec);
        result.set(index, result.get(index) + " " + str);
      });

    return result.stream().map(String::trim).collect(Collectors.toList());
  }

  // Like the above, but for one ID.

  /**
   * This is like readStrings(List<Integer> annotationIds, DSLContext ctx), but operates on only one annotation ID.
   */
  public static String readString(int annotationId, DSLContext ctx) {
    return readStrings(Collections.singletonList(annotationId), ctx).get(0);
  }
}
