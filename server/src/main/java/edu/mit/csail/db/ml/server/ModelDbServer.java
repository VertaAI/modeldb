package edu.mit.csail.db.ml.server;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.server.algorithm.*;
import edu.mit.csail.db.ml.server.algorithm.similarity.SimilarModels;
import edu.mit.csail.db.ml.server.storage.*;
import edu.mit.csail.db.ml.util.ContextFactory;
import edu.mit.csail.db.ml.util.ExceptionWrapper;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import modeldb.*;
import org.apache.thrift.TException;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;

public class ModelDbServer implements ModelDBService.Iface {
  private DSLContext ctx;

  public ModelDbServer(String username, String password, String jdbcUrl, ModelDbConfig.DatabaseType dbType) {
    try {
      ctx = ContextFactory.create(username, password, jdbcUrl, dbType);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ModelDbServer(DSLContext ctx) {
    this.ctx = ctx;
  }

  public int testConnection() throws TException {
    return 200;
  }

  public int storeDataFrame(DataFrame df, int experimentRunId) throws TException {
    return ExceptionWrapper.run(experimentRunId, ctx, () -> {
      DataframeRecord dfRec = DataFrameDao.store(df, experimentRunId, ctx);
      return dfRec.getId();
    });
  }

  public FitEventResponse storeFitEvent(FitEvent fe) throws TException {
    return ExceptionWrapper.run(fe.experimentRunId, ctx, () -> FitEventDao.store(fe, ctx));
  }

  public MetricEventResponse storeMetricEvent(MetricEvent me) throws TException {
    return ExceptionWrapper.run(me.experimentRunId, ctx, () -> MetricEventDao.store(me, ctx));
  }

  public TransformEventResponse storeTransformEvent(TransformEvent te) throws TException {
    return ExceptionWrapper.run(te.experimentRunId, ctx, () -> TransformEventDao.store(te, ctx));
  }

  public RandomSplitEventResponse storeRandomSplitEvent(RandomSplitEvent rse) throws TException {
    return ExceptionWrapper.run(rse.experimentRunId, ctx, () -> RandomSplitEventDao.store(rse, ctx));
  }

  public PipelineEventResponse storePipelineEvent(PipelineEvent pipelineEvent) throws TException {
    return ExceptionWrapper.run(pipelineEvent.experimentRunId, ctx, () -> PipelineEventDao.store(pipelineEvent, ctx));
  }

  public CrossValidationEventResponse storeCrossValidationEvent(CrossValidationEvent cve) throws TException {
    return ExceptionWrapper.run(cve.experimentRunId, ctx, () -> CrossValidationEventDao.store(cve, ctx));
  }

  public GridSearchCrossValidationEventResponse storeGridSearchCrossValidationEvent(GridSearchCrossValidationEvent gscve)
    throws TException {
    return ExceptionWrapper.run(gscve.experimentRunId, ctx, () -> GridSearchCrossValidationEventDao.store(gscve, ctx));
  }

  public AnnotationEventResponse storeAnnotationEvent(AnnotationEvent ae) throws TException {
    return ExceptionWrapper.run(ae.experimentRunId, ctx, () -> AnnotationDao.store(ae, ctx));
  }

  public ProjectEventResponse storeProjectEvent(ProjectEvent pr) throws TException {
    return ExceptionWrapper.run(() -> ProjectDao.store(pr, ctx));
  }

  public ExperimentEventResponse storeExperimentEvent(ExperimentEvent ee) throws TException {
    return ExceptionWrapper.run(() -> ExperimentDao.store(ee, ctx));
  }

  public ExperimentRunEventResponse storeExperimentRunEvent(ExperimentRunEvent er) throws TException {
    return ExceptionWrapper.run(() -> ExperimentRunDao.store(er, ctx));
  }

  public boolean storeLinearModel(int modelId, LinearModel model) throws TException {
    return ExceptionWrapper.run(() -> LinearModelDao.store(modelId, model, ctx));
  }

  public modeldb.DataFrameAncestry getDataFrameAncestry(int dataFrameId) throws TException {
    return ExceptionWrapper.run(() -> DataFrameAncestryComputer.compute(dataFrameId, ctx));
  }

  public String pathForTransformer(int transformerId) throws TException {
    return ExceptionWrapper.run(() -> TransformerDao.path(transformerId, ctx));
  }

  public CommonAncestor getCommonAncestor(int dfId1, int dfId2) throws TException {
    return ExceptionWrapper.run(() -> DataFrameAncestryComputer.computeCommonAncestor(dfId1, dfId2, ctx));
  }

  public CommonAncestor getCommonAncestorForModels(int modelId1, int modelId2) throws TException {
    return ExceptionWrapper.run(() ->
      DataFrameAncestryComputer.computeCommonAncestorForModels(modelId1, modelId2, ctx)
    );
  }

  public int getTrainingRowsCount(int modelId) throws TException {
    return ExceptionWrapper.run(() -> FitEventDao.getNumRowsForModel(modelId, ctx));
  }

  public List<Integer> getTrainingRowsCounts(List<Integer> modelIds) throws TException {
    return ExceptionWrapper.run(() -> FitEventDao.getNumRowsForModels(modelIds, ctx));
  }

  public CompareHyperParametersResponse compareHyperparameters(int modelId1, int modelId2) throws TException {
    return ExceptionWrapper.run(() -> HyperparameterComparison.compareHyperParameters(modelId1, modelId2, ctx));
  }

  public CompareFeaturesResponse compareFeatures(int modelId1, int modelId2) throws TException {
    return ExceptionWrapper.run(() -> Feature.compareFeatures(modelId1, modelId2, ctx));
  }

  public Map<ProblemType, List<Integer>> groupByProblemType(List<Integer> modelIds) throws TException {
    return ExceptionWrapper.run(() -> ProblemTypeGrouper.groupByProblemType(modelIds, ctx));
  }

  public List<Integer> similarModels(int modelId, List<ModelCompMetric> compMetrics, int numModels) throws TException {
    return ExceptionWrapper.run(() -> SimilarModels.similarModels(modelId, compMetrics, numModels, ctx));
  }

  public List<String> linearModelFeatureImportances(int modelId) throws TException {
    return ExceptionWrapper.run(() -> LinearModelAlgorithms.featureImportances(modelId, ctx));
  }

  public List<FeatureImportanceComparison> compareLinearModelFeatureImportances(int model1Id, int model2Id)
    throws TException {
    return ExceptionWrapper.run(() -> LinearModelAlgorithms.featureImportances(model1Id, model2Id, ctx));
  }

  public List<Integer> iterationsUntilConvergence(List<Integer> modelIds, double tolerance) throws TException {
    return ExceptionWrapper.run(() -> LinearModelAlgorithms.iterationsUntilConvergence(modelIds, tolerance, ctx));
  }

  public List<Integer> rankModels(List<Integer> modelIds, ModelRankMetric metric) throws TException {
    return ExceptionWrapper.run(() -> LinearModelAlgorithms.rankModels(modelIds, metric, ctx));
  }

  public List<ConfidenceInterval> confidenceIntervals(int modelId, double significanceLevel) throws TException {
    return ExceptionWrapper.run(() -> LinearModelAlgorithms.confidenceIntervals(modelId, significanceLevel, ctx));
  }

  public List<Integer> modelsWithFeatures(List<String> featureNames) throws TException {
    return ExceptionWrapper.run(() -> Feature.modelsWithFeatures(featureNames, ctx));
  }

  public List<Integer> modelsDerivedFromDataFrame(int dfId) throws TException {
    return ExceptionWrapper.run(() -> DataFrameAncestryComputer.descendentModels(dfId, ctx));
  }

  public ModelResponse getModel(int modelId) throws TException {
    return ExceptionWrapper.run(() -> TransformerDao.readInfo(modelId, ctx));
  }

  public List<ExperimentRun> getRunsInExperiment(int experimentId) throws TException {
    return ExceptionWrapper.run(() -> ExperimentRunDao.readExperimentRunsInExperiment(experimentId, ctx));
  }

  public ProjectExperimentsAndRuns getRunsAndExperimentsInProject(int projId) throws TException {
    return ExceptionWrapper.run(() -> ExperimentRunDao.readExperimentsAndRunsInProject(projId, ctx));
  }

  public List<ProjectOverviewResponse> getProjectOverviews() throws TException {
    return ExceptionWrapper.run(() -> ProjectDao.getProjectOverviews(ctx));
  }

  public ExperimentRunDetailsResponse getExperimentRunDetails(int experimentRunId) throws TException {
    return ExceptionWrapper.run(() -> ExperimentRunDao.readExperimentRunDetails(experimentRunId, ctx));
  }

  public List<String> originalFeatures(int modelId) throws TException {
    return ExceptionWrapper.run(() -> Feature.originalFeatures(modelId, ctx));
  }

  public boolean storeTreeModel(int modelId, TreeModel model) throws TException {
    return ExceptionWrapper.run(() -> TreeModelDao.store(modelId, model, ctx));
  }

  public List<TransformEventResponse> storePipelineTransformEvent(List<TransformEvent> transformEvents)
    throws TException {
    return ExceptionWrapper.run(() -> PipelineEventDao.storePipelineTransformEvent(transformEvents, ctx));
  }

  public ModelAncestryResponse computeModelAncestry(int modelId) throws TException {
    return ExceptionWrapper.run(() -> DataFrameAncestryComputer.computeModelAncestry(modelId, ctx));
  }

  public String getFilePath(Transformer t, int experimentRunId) throws TException {
    return ExceptionWrapper.run(() -> TransformerDao.getFilePath(t, experimentRunId, ctx));
  }
}
