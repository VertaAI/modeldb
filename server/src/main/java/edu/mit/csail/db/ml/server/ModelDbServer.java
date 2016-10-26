package edu.mit.csail.db.ml.server;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.server.algorithm.*;
import edu.mit.csail.db.ml.server.algorithm.similarity.SimilarModels;
import edu.mit.csail.db.ml.server.storage.*;
import modeldb.*;
import org.apache.thrift.TException;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModelDbServer implements ModelDBService.Iface {
  private DSLContext ctx;

  private interface CheckedSupplier<T> {
    T get() throws Exception;
  }

  private<T> T run(CheckedSupplier<T> fn) throws TException {
    try {
      return fn.get();
    } catch (Exception ex) {
      ex.printStackTrace();
      if (ex instanceof TException) {
        throw (TException) ex;
      } else {
        throw new ServerLogicException(ex.getClass().getSimpleName() + ": " + ex.getMessage());
      }
    }
  }

  private<T> T run(int expRunId, CheckedSupplier<T> fn) throws TException {
    return run(() -> {
      ExperimentRunDao.validateExperimentRunId(expRunId, ctx);
      return fn.get();
    });
  }

  public ModelDbServer(String username, String password, String jdbcUrl, ModelDbConfig.DatabaseType dbType) {
    try {
      Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
      switch (dbType) {
        case SQLITE: ctx = DSL.using(conn, SQLDialect.SQLITE); break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public int testConnection() throws TException {
    return 200;
  }

  public FitEventResponse storeFitEvent(FitEvent fe) throws TException {
    return run(fe.experimentRunId, () -> FitEventDao.store(fe, ctx));
  }

  public MetricEventResponse storeMetricEvent(MetricEvent me) throws TException {
    return run(me.experimentRunId, () -> MetricEventDao.store(me, ctx));
  }

  public TransformEventResponse storeTransformEvent(TransformEvent te) throws TException {
    return run(te.experimentRunId, () -> TransformEventDao.store(te, ctx, true));
  }

  public RandomSplitEventResponse storeRandomSplitEvent(RandomSplitEvent rse) throws TException {
    return run(rse.experimentRunId, () -> RandomSplitEventDao.store(rse, ctx));
  }

  public PipelineEventResponse storePipelineEvent(PipelineEvent pipelineEvent) throws TException {
    return run(pipelineEvent.experimentRunId, () -> PipelineEventDao.store(pipelineEvent, ctx));
  }

  public CrossValidationEventResponse storeCrossValidationEvent(CrossValidationEvent cve) throws TException {
    return run(cve.experimentRunId, () -> CrossValidationEventDao.store(cve, ctx));
  }

  public GridSearchCrossValidationEventResponse storeGridSearchCrossValidationEvent(GridSearchCrossValidationEvent gscve)
    throws TException {
    return run(gscve.experimentRunId, () -> GridSearchCrossValidationEventDao.store(gscve, ctx));
  }

  public AnnotationEventResponse storeAnnotationEvent(AnnotationEvent ae) throws TException {
    return run(ae.experimentRunId, () -> AnnotationEventDao.store(ae, ctx));
  }

  public ProjectEventResponse storeProjectEvent(ProjectEvent pr) throws TException {
    return run(() -> ProjectDao.store(pr, ctx));
  }

  public ExperimentEventResponse storeExperimentEvent(ExperimentEvent ee) throws TException {
    return run(() -> ExperimentDao.store(ee, ctx));
  }

  public ExperimentRunEventResponse storeExperimentRunEvent(ExperimentRunEvent er) throws TException {
    return run(() -> ExperimentRunDao.store(er, ctx));
  }

  public boolean storeLinearModel(int modelId, LinearModel model) throws TException {
    return run(() -> LinearModelDao.store(modelId, model, ctx));
  }

  public modeldb.DataFrameAncestry getDataFrameAncestry(int dataFrameId) throws TException {
    return run(() -> DataFrameAncestryComputer.compute(dataFrameId, ctx));
  }

  public String pathForTransformer(int transformerId) throws TException {
    return run(() -> TransformerDao.path(transformerId, ctx));
  }

  public CommonAncestor getCommonAncestor(int dfId1, int dfId2) throws TException {
    return run(() -> DataFrameAncestryComputer.computeCommonAncestor(dfId1, dfId2, ctx));
  }

  public CommonAncestor getCommonAncestorForModels(int modelId1, int modelid2) throws TException {
    return run(() -> {
      int dfId1 = FitEventDao.getParentDfId(modelId1, ctx);
      int dfId2 = FitEventDao.getParentDfId(modelid2, ctx);
      return getCommonAncestor(dfId1, dfId2);
    });
  }

  public int getTrainingRowsCount(int modelId) throws TException {
    return run(() -> {
      int numRows = FitEventDao.getNumRowsForModels(Collections.singletonList(modelId), ctx).get(0);
      if (numRows < 0) {
        throw new ResourceNotFoundException(String.format(
          "Could not find number of rows used to train Transformer %d because the Transformer doesn't exist",
          modelId
        ));
      }
      return numRows;
    });
  }

  public List<Integer> getTrainingRowsCounts(List<Integer> modelIds) throws TException {
    return run(() -> FitEventDao.getNumRowsForModels(modelIds, ctx));
  }

  public CompareHyperParametersResponse compareHyperparameters(int modelId1, int modelId2) throws TException {
    return run(() -> HyperparameterComparison.compareHyperParameters(modelId1, modelId2, ctx));
  }

  public CompareFeaturesResponse compareFeatures(int modelId1, int modelId2) throws TException {
    return run(() -> Feature.compareFeatures(modelId1, modelId2, ctx));
  }

  public Map<ProblemType, List<Integer>> groupByProblemType(List<Integer> modelIds) throws TException {
    return run(() -> ProblemTypeGrouper.groupByProblemType(modelIds, ctx));
  }

  public List<Integer> similarModels(int modelId, List<ModelCompMetric> compMetrics, int numModels) throws TException {
    return run(() -> SimilarModels.similarModels(modelId, compMetrics, numModels, ctx));
  }

  public List<String> linearModelFeatureImportances(int modelId) throws TException {
    return run(() -> LinearModelAlgorithms.featureImportances(modelId, ctx));
  }

  public List<FeatureImportanceComparison> compareLinearModelFeatureImportances(int model1Id, int model2Id)
    throws TException {
    return run(() -> LinearModelAlgorithms.featureImportances(model1Id, model2Id, ctx));
  }

  public List<Integer> iterationsUntilConvergence(List<Integer> modelIds, double tolerance) throws TException {
    return run(() -> LinearModelAlgorithms.iterationsUntilConvergence(modelIds, tolerance, ctx));
  }

  public List<Integer> rankModels(List<Integer> modelIds, ModelRankMetric metric) throws TException {
    return run(() -> LinearModelAlgorithms.rankModels(modelIds, metric, ctx));
  }

  public List<ConfidenceInterval> confidenceIntervals(int modelId, double significanceLevel) throws TException {
    return run(() -> LinearModelAlgorithms.confidenceIntervals(modelId, significanceLevel, ctx));
  }

  public List<Integer> modelsWithFeatures(List<String> featureNames) throws TException {
    return run(() -> Feature.modelsWithFeatures(featureNames, ctx));
  }

  public List<Integer> modelsDerivedFromDataFrame(int dfId) throws TException {
    return run(() -> DataFrameAncestryComputer.descendentModels(dfId, ctx));
  }

  public ModelResponse getModel(int modelId) throws TException {
    return run(() -> TransformerDao.readInfo(modelId, ctx));
  }

  public List<ExperimentRun> getRunsInExperiment(int experimentId) throws TException {
    return run(() -> ExperimentRunDao.readExperimentRunsInExperiment(experimentId, ctx));
  }

  public ProjectExperimentsAndRuns getRunsAndExperimentsInProject(int projId) throws TException {
    return run(() -> ExperimentRunDao.readExperimentsAndRunsInProject(projId, ctx));
  }

  public List<ProjectOverviewResponse> getProjectOverviews() throws TException {
    return run(() -> ProjectDao.getProjectOverviews(ctx));
  }
}
