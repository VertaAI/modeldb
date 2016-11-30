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

    int NUM_ITERATIONS = 3;
    List<Duplicator> duplicators = Arrays.asList(
      ExperimentRunDuplicator.getInstance(ctx),
      DataFrameDuplicator.getInstance(ctx),
      TransformerDuplicator.getInstance(ctx),
      TransformerSpecDuplicator.getInstance(ctx),
      TransformEventDuplicator.getInstance(ctx),
      FitEventDuplicator.getInstance(ctx)
    );

    for (Duplicator duplicator : duplicators) {
      duplicator.duplicate(NUM_ITERATIONS);
    }
    System.out.println("Hello world!");
  }
}