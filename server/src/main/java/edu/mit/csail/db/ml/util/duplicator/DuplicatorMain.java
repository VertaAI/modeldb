package edu.mit.csail.db.ml.util.duplicator;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.util.ContextFactory;
import org.jooq.DSLContext;

import java.util.Arrays;
import java.util.List;

public class DuplicatorMain {
  public static void main(String[] args) throws Exception {
    ModelDbConfig config = ModelDbConfig.parse(args);
    DSLContext ctx = ContextFactory.create(config.dbUser, config.dbPassword, config.jbdcUrl, config.dbType);

    int NUM_ITERATIONS = 100;
    List<Duplicator> duplicators = Arrays.asList(
      TreeNodeDuplicator.getInstance(ctx),
      ExperimentRunDuplicator.getInstance(ctx),
      DataFrameDuplicator.getInstance(ctx),
      TransformerDuplicator.getInstance(ctx),
      TransformerSpecDuplicator.getInstance(ctx),
      TransformEventDuplicator.getInstance(ctx),
      FitEventDuplicator.getInstance(ctx),
      MetricEventDuplicator.getInstance(ctx),
      RandomSplitEventDuplicator.getInstance(ctx),
      DataFrameColumnDuplicator.getInstance(ctx),
      DataFrameSplitDuplicator.getInstance(ctx),
      FeatureDuplicator.getInstance(ctx),
      HyperParameterDuplicator.getInstance(ctx),
      LinearModelDuplicator.getInstance(ctx),
      LinearModelTermDuplicator.getInstance(ctx),
      TreeModelDuplicator.getInstance(ctx),
      TreeLinkDuplicator.getInstance(ctx),
      TreeModelComponentDuplicator.getInstance(ctx),
      ModelObjectiveHistoryDuplicator.getInstance(ctx),
      CrossValidationEventDuplicator.getInstance(ctx),
      CrossValidationFoldDuplicator.getInstance(ctx),
      GridSearchCrossValidationEventDuplicator.getInstance(ctx),
      GridCellCrossValidationDuplicator.getInstance(ctx),
      EventDuplicator.getInstance(ctx),
      PipelineStageDuplicator.getInstance(ctx)
    );

    for (Duplicator duplicator : duplicators) {
      duplicator.duplicate(NUM_ITERATIONS);
    }
    System.out.println("Hello world!");
  }
}
