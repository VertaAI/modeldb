package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.PipelinestageRecord;
import modeldb.*;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PipelineEventDao {
  public static int storePipelineStage(int pipelineFitEventId,
                                       int stageId,
                                       int stageNumber,
                                       boolean isFit,
                                       int experimentId,
                                       DSLContext ctx) {
    PipelinestageRecord rec = ctx.newRecord(Tables.PIPELINESTAGE);
    rec.setId(null);
    rec.setPipelinefitevent(pipelineFitEventId);
    rec.setTransformorfitevent(stageId);
    rec.setStagenumber(stageNumber);
    rec.setIsfit(isFit ? 1 : 0);
    rec.setExperimentrun(experimentId);
    rec.store();
    return rec.getId();
  }

  public static List<TransformEventResponse> storePipelineTransformEvent(List<TransformEvent> transformEvents,
                                                                         DSLContext ctx) {
    List<TransformEventResponse> result = new ArrayList<>();
    TransformEvent te;
    for (int i = 0; i < transformEvents.size(); i++) {
      te = (i == 0) ?
        transformEvents.get(i) :
        transformEvents.get(i).setOldDataFrame(
          transformEvents.get(i).getOldDataFrame().setId(result.get(i - 1).newDataFrameId)
        );
      result.add(TransformEventDao.store(te, ctx));
    }
    return result;
  }

  public static PipelineEventResponse store(PipelineEvent pe, DSLContext ctx) {
    // Store the FitEvent.
    pe.pipelineFit.setExperimentRunId(pe.experimentRunId);
    FitEventResponse fe = FitEventDao.store(pe.pipelineFit, ctx, true);

    // This is the ID of the next DataFrame to be handled by the Pipeline.
    int dfId = fe.dfId;

    // Ensure the TransformStages and FitStages are sorted by stageNumber and have the proper experiment run ID.
    pe.setTransformStages(
      pe.transformStages
        .stream()
        .sorted((a, b) -> a.stageNumber - b.stageNumber)
        .map(ts -> ts.setTe(ts.te.setExperimentRunId(pe.experimentRunId)))
        .collect(Collectors.toList())
    );
    pe.setFitStages(
      pe.fitStages
        .stream()
        .sorted((a, b) -> a.stageNumber - b.stageNumber)
        .map(fs -> fs.setFe(fs.fe.setExperimentRunId(pe.experimentRunId)))
        .collect(Collectors.toList())
    );

    // Now we will process stages in increasing stage number. We'll use a merge procedure, like the one from merge-sort.

    // The current index into pe.fitStages.
    int fIndex = 0;

    // The current index into pe.transformStages.
    int tIndex = 0;

    // We'll accumulate the responses in these lists.
    List<TransformEventResponse> tes = new ArrayList<>();
    List<FitEventResponse> fes = new ArrayList<>();

    // Loop until we've processed all the stages.
    while (fIndex < pe.fitStages.size() || tIndex < pe.transformStages.size()) {
      // Whether we will be processing a fit stage on this iteration.
      boolean takeFit;

      // Similar to merge sort.
      if (fIndex == pe.fitStages.size()) {
        takeFit = false;
      } else if (tIndex == pe.transformStages.size()) {
        takeFit = true;
      } else {
        // The <= sign below is critically important! It ensures that, when two stages have the same stage number,
        // we process the fit stage first.
        takeFit = pe.fitStages.get(fIndex).stageNumber <= pe.transformStages.get(tIndex).stageNumber;
      }

      // Process the stage. Notice that each case uses the following steps:
      // 1: Get the stage.
      // 2: Set the experiment run ID and DataFrame ID.
      // 3: Store the FitEvent or TransformEvent, and accumulate the response into the appropriate list.
      // 4: Update the ID of the DataFrame that will continue on to the next stage.
      // 5: Increment the index.
      if (takeFit) {
        PipelineFitStage stage = pe.fitStages.get(fIndex);

        stage.fe.setExperimentRunId(pe.experimentRunId);
        stage.fe.setDf(stage.fe.getDf().setId(dfId));

        FitEventResponse resp = FitEventDao.store(stage.fe, ctx);
        fes.add(resp);

        dfId = resp.dfId;
        fIndex++;
      } else {
        PipelineTransformStage stage = pe.transformStages.get(tIndex);

        stage.te.setExperimentRunId(pe.experimentRunId);
        stage.te.setOldDataFrame(stage.te.oldDataFrame.setId(dfId));

        TransformEventResponse resp = TransformEventDao.store(stage.te, ctx);
        tes.add(resp);

        dfId = resp.newDataFrameId;
        tIndex++;
      }
    }

    // Store a PipelineStage row for each of the fit and transform stages.
    IntStream.range(0, pe.transformStages.size())
      .forEach(ind -> storePipelineStage(
        fe.getFitEventId(),
        tes.get(ind).getEventId(),
        pe.transformStages.get(ind).stageNumber,
        false,
        pe.experimentRunId,
        ctx
      ));
    IntStream.range(0, pe.fitStages.size())
      .forEach(ind -> {
        storePipelineStage(
          fe.getFitEventId(),
          fes.get(ind).getEventId(),
          pe.fitStages.get(ind).stageNumber,
          true,
          pe.experimentRunId,
          ctx
        );
      });

    return new PipelineEventResponse(fe, tes, fes);
  }
}
