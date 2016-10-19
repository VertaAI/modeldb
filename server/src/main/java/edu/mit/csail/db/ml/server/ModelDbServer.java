package edu.mit.csail.db.ml.server;

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

  public ModelDbServer() {
    String userName = "";
    String password = "";
    String url = "jdbc:sqlite:modeldb.db";

    // Connection is the only JDBC resource that we need
    // PreparedStatement and ResultSet are handled by jOOQ, internally
    try {
      Connection conn = DriverManager.getConnection(url, userName, password);
      ctx = DSL.using(conn, SQLDialect.SQLITE);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public int testConnection() throws TException {
    return 200;
  }

  public FitEventResponse storeFitEvent(FitEvent fe) throws TException {
    return FitEventDao.store(fe, ctx);
  }

  public MetricEventResponse storeMetricEvent(MetricEvent me) throws TException {
    return MetricEventDao.store(me, ctx);
  }

  public TransformEventResponse storeTransformEvent(TransformEvent te) throws TException {
    return TransformEventDao.store(te, ctx, true);
  }

  public RandomSplitEventResponse storeRandomSplitEvent(RandomSplitEvent rse) throws TException {
    return RandomSplitEventDao.store(rse, ctx);
  }

  public PipelineEventResponse storePipelineEvent(PipelineEvent pipelineEvent) throws TException {
    return PipelineEventDao.store(pipelineEvent, ctx);
  }

  public CrossValidationEventResponse storeCrossValidationEvent(CrossValidationEvent cve) throws TException {
    return CrossValidationEventDao.store(cve, ctx);
  }

  public GridSearchCrossValidationEventResponse storeGridSearchCrossValidationEvent(GridSearchCrossValidationEvent gscve) throws TException {
    return GridSearchCrossValidationEventDao.store(gscve, ctx);
  }

  public AnnotationEventResponse storeAnnotationEvent(AnnotationEvent ae) throws TException {
    return AnnotationEventDao.store(ae, ctx);
  }

  public ProjectEventResponse storeProjectEvent(ProjectEvent pr) throws TException {
    return ProjectDao.store(pr, ctx);
  }

  public ExperimentRunEventResponse storeExperimentRunEvent(ExperimentRunEvent er) throws TException {
    return ExperimentRunDao.store(er, ctx);
  }

  public boolean storeLinearModel(int modelId, LinearModel model) throws TException {
    try {
      return LinearModelDao.store(modelId, model, ctx);
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  public modeldb.DataFrameAncestry getDataFrameAncestry(int dataFrameId) throws TException {
    return DataFrameAncestryComputer.compute(dataFrameId, ctx);
  }

  public String pathForTransformer(int transformerId) throws TException {
    return TransformerDao.path(transformerId, ctx);
  }

  public CommonAncestor getCommonAncestor(int dfId1, int dfId2) throws TException {
    return DataFrameAncestryComputer.computeCommonAncestor(dfId1, dfId2, ctx);
  }

  public CommonAncestor getCommonAncestorForModels(int modelId1, int modelid2) throws TException {
    int dfId1 = FitEventDao.getParentDfId(modelId1, ctx);
    int dfId2 = FitEventDao.getParentDfId(modelid2, ctx);
    return (dfId1 < 0 || dfId2 < 0)
      ? DataFrameAncestryComputer.getFailedAncestorLookup()
      : getCommonAncestor(dfId1, dfId2);
  }

  public int getTrainingRowsCount(int modelId) throws TException {
    return FitEventDao.getNumRowsForModels(Collections.singletonList(modelId), ctx).get(0);
  }

  public List<Integer> getTrainingRowsCounts(List<Integer> modelIds) throws TException {
    return FitEventDao.getNumRowsForModels(modelIds, ctx);
  }

  public CompareHyperParametersResponse compareHyperparameters(int modelId1, int modelId2) throws TException {
    return HyperparameterComparison.compareHyperParameters(modelId1, modelId2, ctx);
  }

  public CompareFeaturesResponse compareFeatures(int modelId1, int modelId2) throws TException {
    return Feature.compareFeatures(modelId1, modelId2, ctx);
  }

  public Map<ProblemType, List<Integer>> groupByProblemType(List<Integer> modelIds) {
    return ProblemTypeGrouper.groupByProblemType(modelIds, ctx);
  }

  public List<Integer> similarModels(int modelId, List<ModelCompMetric> compMetrics, int numModels) throws TException {
    return SimilarModels.similarModels(modelId, compMetrics, numModels, ctx);
  }

  public List<String> linearModelFeatureImportances(int modelId) throws TException {
    return LinearModelAlgorithms.featureImportances(modelId, ctx);
  }

  public List<FeatureImportanceComparison> compareLinearModelFeatureImportances(
    int model1Id,
    int model2Id
  ) throws TException {
    return LinearModelAlgorithms.featureImportances(model1Id, model2Id, ctx);
  }

  public List<Integer> iterationsUntilConvergence(List<Integer> modelIds, double tolerance) throws TException {
    return LinearModelAlgorithms.iterationsUntilConvergence(modelIds, tolerance, ctx);
  }

  public List<Integer> rankModels(List<Integer> modelIds, ModelRankMetric metric) throws TException {
    return LinearModelAlgorithms.rankModels(modelIds, metric, ctx);
  }

  public List<ConfidenceInterval> confidenceIntervals(int modelId, double significanceLevel) throws TException {
    return LinearModelAlgorithms.confidenceIntervals(modelId, significanceLevel, ctx);
  }

  public List<Integer> modelsWithFeatures(List<String> featureNames) throws TException {
    return Feature.modelsWithFeatures(featureNames, ctx);
  }

  public List<Integer> modelsDerivedFromDataFrame(int dfId) throws TException {
    return DataFrameAncestryComputer.descendentModels(dfId, ctx);
  }
}
