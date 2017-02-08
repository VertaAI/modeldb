package edu.mit.csail.db.ml.util.duplicator;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.util.ContextFactory;
import org.jooq.DSLContext;

import java.util.Arrays;
import java.util.List;

/**
 * This is a command line program that basically copies all the rows in all the tables of a ModelDB SQLite database
 * a specified number of times.
 *
 * You basically execute as follows:
 *
 * mvn exec:java -Dexec.mainClass="edu.mit.csail.db.ml.util.duplicator.DuplicatorMain" -Dexec.args="numcopies"
 *
 * where numcopies is the number of copies you want for each row.
 *
 * The reason we need a command line program for this is because when we copy a row, its primary key must change.
 * Also, since the row may have foreign keys that point to rows in other tables, those rows must be copied first (and
 * given their own new primary keys).
 *
 * The path to the SQLite file is specified in the default configuration file that ModelDbConfig uses.
 */
public class DuplicatorMain {
  public static void main(String[] args) throws Exception {
    System.out.println("Duplicating a total of " + args[0] + " times.");

    // Read the configuration file and connect to the database.
    ModelDbConfig config = ModelDbConfig.parse(new String[] {});
    DSLContext ctx = ContextFactory.create(config.dbUser, config.dbPassword, config.jbdcUrl, config.dbType);

    // Create the duplicators. They are TOPOLOGICALLY sorted based on foreign key dependencies. For example,
    // If table T2 has a foreign key pointing to table T1, then T1 should appear before T2 in the list.
    int NUM_ITERATIONS = Integer.parseInt(args[0]);
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

    // Run every duplicator.
    for (Duplicator duplicator : duplicators) {
      duplicator.duplicate(NUM_ITERATIONS);
    }
    System.out.println("Finished duplication!");
  }
}
