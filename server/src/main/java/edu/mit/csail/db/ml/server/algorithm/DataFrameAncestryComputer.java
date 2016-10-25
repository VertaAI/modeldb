package edu.mit.csail.db.ml.server.algorithm;

import edu.mit.csail.db.ml.server.storage.DataFrameDao;
import javafx.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.TransformeventRecord;
import modeldb.CommonAncestor;
import modeldb.DataFrame;
import modeldb.ResourceNotFoundException;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataFrameAncestryComputer {

  public static modeldb.DataFrameAncestry compute(int dfId, DSLContext ctx) throws ResourceNotFoundException {
    // Fetch the DataFrame wth the given ID.
    DataframeRecord rec = ctx.selectFrom(Tables.DATAFRAME).where(Tables.DATAFRAME.ID.eq(dfId)).fetchOne();

    // If we can't find one, indicate a failed lookup.
    if (rec == null) {
      throw new ResourceNotFoundException(
        String.format("Tried finding ancestry for DataFrame %d, but the DataFrame does not exist", dfId)
      );
    }

    // Otherwise, fetch all the TransformEvents and create a map from child DataFrame to parent DataFrame.
    // This is expensive because we fetch all the TransformEvents. We could speed it up by only getting the
    // TransformEvents from the current project. We also could also speed it up by making each DataFrame store its
    // entire ancestor chain (rather than just its parent). However, let's not prematurely optimize.
    Map<Integer, Integer> parentIdForDfId = ctx
      .selectFrom(Tables.TRANSFORMEVENT)
      .fetch()
      .stream()
      .collect(Collectors.toMap(TransformeventRecord::getNewdf, TransformeventRecord::getOlddf));

    // Populate the ancestor chain.
    List<Integer> ancestorChain = new ArrayList<>();
    int currId = dfId;
    boolean hasCurrId = true;
    while (hasCurrId) {
      ancestorChain.add(currId);
      if (parentIdForDfId.containsKey(currId)) {
        currId = parentIdForDfId.get(currId);
      } else {
        hasCurrId = false;
      }
    }

    // Create mapping from ID to order in the ancestor chain.
    Map<Integer, Integer> indexForId = IntStream
      .range(0, ancestorChain.size())
      .mapToObj(i -> new Pair<>(ancestorChain.get(i), i))
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    // Fetch the DataFrames for the given chain of ancestor IDs.
    List<DataFrame> dfs = ctx
      .selectFrom(Tables.DATAFRAME)
      .where(Tables.DATAFRAME.ID.in(ancestorChain))
      .fetch()
      .stream()
      .map(df -> new DataFrame(df.getId(), Collections.emptyList(), df.getNumrows(), df.getTag()))
      .collect(Collectors.toList());

    // Sort so that the youngest DataFrame comes first.
    dfs.sort((a, b) -> indexForId.get(a.id).compareTo(indexForId.get(b.id)));

    return new modeldb.DataFrameAncestry(dfs);
  }

  /**
   * This will update a map from DataFrame ID to (chain index, DataFrame) by considering the given DataFrame.
   * If the DataFrame already exists in the map, it returns a CommonAncestor response.
   *
   * This method is used by the computeCommonAncestor function.
   * @param dfForId - map from DataFrame ID to (chain index, DataFrame)
   * @param df - The DataFrame to look up.
   * @param index - The index of this DataFrame in chain chainNum
   * @param chainNum - The number of the chain (either 1 or 2).
   * @return
   */
  private static modeldb.CommonAncestor updateMap(Map<Integer, Pair<Integer, DataFrame>> dfForId,
                                                  DataFrame df,
                                                  int index,
                                                  int chainNum) {
    if (dfForId.containsKey(df.id)) {
      CommonAncestor resp = new modeldb.CommonAncestor(
        (chainNum == 1) ? index : dfForId.get(df.id).getKey(),
        (chainNum == 2) ? index : dfForId.get(df.id).getKey()
      );
      resp.setAncestor(df);
      return resp;
    } else {
      dfForId.put(df.id, new Pair<>(index, df));
      return null;
    }
  }

  public static modeldb.CommonAncestor computeCommonAncestor(int dfId1, int dfId2, DSLContext ctx)
    throws ResourceNotFoundException {
    // Compute the ancestries of each DataFrame.
    modeldb.DataFrameAncestry ancestry1 = compute(dfId1, ctx);
    modeldb.DataFrameAncestry ancestry2 = compute(dfId2, ctx);

    // Get the chain of ancesters and figure out which one is longer.
    List<DataFrame> a1Chain = ancestry1.ancestors;
    List<DataFrame> a2Chain = ancestry2.ancestors;
    int n = Math.max(a1Chain.size(), a2Chain.size());

    // Create a map that goes from DataFrame ID to the (chain index, DataFrame) pair.
    Map<Integer, Pair<Integer, DataFrame>> dfForId = new HashMap<>();

    // This will store the response.
    CommonAncestor response = new CommonAncestor(-1, -1);

    // Iterate through all the indices.
    for (int i = 0; i < n; i++) {
      // Try to update the map for both ancestry 1 and ancestry 2. If we find a common ancestor, return it.
      response = (i < a1Chain.size()) ? updateMap(dfForId, a1Chain.get(i), i, 1) : null;
      if (response != null) {
        return response;
      }
      response = (i < a2Chain.size()) ? updateMap(dfForId, a2Chain.get(i), i, 2) : null;
      if (response != null) {
        return response;
      }
    }

    return response;
  }

  public static List<Integer> descendentModels(int dfId, DSLContext ctx) throws ResourceNotFoundException {
    if (!DataFrameDao.exists(dfId, ctx)) {
      throw new ResourceNotFoundException(String.format(
        "Can't find descendent models for DataFrame %d because that DataFrame doesn't exist",
        dfId
      ));
    }
     // First find the given DF and its project ID.
     Record1<Integer> rec = ctx.select(Tables.EXPERIMENT_RUN_VIEW.PROJECTID)
       .from(
         Tables.DATAFRAME.join(Tables.EXPERIMENT_RUN_VIEW)
           .on(Tables.DATAFRAME.EXPERIMENTRUN.eq(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTRUNID))
       )
       .where(Tables.DATAFRAME.ID.eq(dfId))
       .fetchOne();
     if (rec == null) {
       return Collections.emptyList();
     }
     int projId = rec.value1();

     // We will create a map from DataFrame ID to a set of IDs of descendent DataFrames.
     // We do this by examining all the TransformEvents in the project.
     Map<Integer, Set<Integer>> childrenForDfId = new HashMap<>();
     ctx
       .select(Tables.TRANSFORMEVENT.OLDDF, Tables.TRANSFORMEVENT.NEWDF)
       .from(
         Tables.TRANSFORMEVENT.join(Tables.EXPERIMENT_RUN_VIEW)
         .on(Tables.TRANSFORMEVENT.EXPERIMENTRUN.eq(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTRUNID))
       )
       .where(
         Tables.EXPERIMENT_RUN_VIEW.PROJECTID.eq(projId)
       )
       .forEach(record -> {
         if (!childrenForDfId.containsKey(record.value1())) {
           childrenForDfId.put(record.value1(), new HashSet<>());
         }
         if (!childrenForDfId.containsKey(record.value2())) {
           childrenForDfId.put(record.value2(), new HashSet<>());
         }
         childrenForDfId.get(record.value1()).add(record.value2());
       });

     // Now we'll create a set of DataFrames descended from dfId.
     Set<Integer> descendentIds = new HashSet<>();
     Queue<Integer> toProcess = new LinkedList<>();
     toProcess.add(dfId);

     Integer processMe;
     while (!toProcess.isEmpty()) {
       processMe = toProcess.remove();
       if (childrenForDfId.containsKey(processMe)) {
         descendentIds.add(processMe);
         toProcess.addAll(childrenForDfId.get(processMe).stream().collect(Collectors.toList()));
       }
     }

     // Now we scan the FitEvents in the project and get all the model IDs for models that
     // were trained on one of the descendents of dfId.
     return ctx
       .select(Tables.FITEVENT.TRANSFORMER)
       .from(
         Tables.FITEVENT.join(Tables.EXPERIMENT_RUN_VIEW)
         .on(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTRUNID.eq(Tables.FITEVENT.EXPERIMENTRUN))
       )
       .where(Tables.EXPERIMENT_RUN_VIEW.PROJECTID.eq(projId).and(Tables.FITEVENT.DF.in(descendentIds)))
       .stream()
       .map(Record1::value1)
       .collect(Collectors.toList());
  }
}
