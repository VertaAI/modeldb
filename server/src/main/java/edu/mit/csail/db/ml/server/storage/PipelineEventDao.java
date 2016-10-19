package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.PipelinestageRecord;
import modeldb.*;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PipelineEventDao {
  private static int storePipelineStage(int pipelineFitEventId,
                                         int stageId,
                                         int stageNumber,
                                         int projId,
                                         int experimentId,
                                         DSLContext ctx) {
    PipelinestageRecord rec = ctx.newRecord(Tables.PIPELINESTAGE);
    rec.setId(null);
    rec.setPipelinefitevent(pipelineFitEventId);
    rec.setTransformorfitevent(stageId);
    rec.setStagenumber(stageNumber);
    rec.setProject(projId);
    rec.setExperimentrun(experimentId);
    rec.store();
    return rec.getId();
  }
  public static PipelineEventResponse store(PipelineEvent pe, DSLContext ctx) {
    pe.pipelineFit.setExperimentRunId(pe.experimentRunId);
    FitEventResponse fe = FitEventDao.store(pe.pipelineFit.setProjectId(pe.projectId), ctx, true);

    for (PipelineTransformStage pipelineTransform : pe.transformStages) {
      pipelineTransform.te.setExperimentRunId(pe.experimentRunId);
    }
    for (PipelineFitStage pipelineFit : pe.fitStages) {
      pipelineFit.fe.setExperimentRunId(pe.experimentRunId);
    }

    List<TransformEventResponse> tes = pe
      .transformStages
      .stream()
      .map(ts -> TransformEventDao.store(ts.te.setProjectId(pe.projectId), ctx))
      .collect(Collectors.toList());
    List<FitEventResponse> fes = pe
      .fitStages
      .stream()
      .map(fs -> FitEventDao.store(fs.fe.setProjectId(pe.projectId), ctx))
      .collect(Collectors.toList());

    IntStream.range(0, pe.transformStages.size())
      .forEach(ind -> storePipelineStage(
        fe.getFitEventId(),
        tes.get(ind).getEventId(),
        pe.transformStages.get(ind).stageNumber,
        pe.projectId,
        pe.experimentRunId,
        ctx
      ));
    IntStream.range(0, pe.fitStages.size())
      .forEach(ind -> {
        storePipelineStage(
          fe.getFitEventId(),
          fes.get(ind).getEventId(),
          pe.fitStages.get(ind).stageNumber,
          pe.projectId,
          pe.experimentRunId,
          ctx
        );
      });

    return new PipelineEventResponse(fe, tes, fes);
  }
}
