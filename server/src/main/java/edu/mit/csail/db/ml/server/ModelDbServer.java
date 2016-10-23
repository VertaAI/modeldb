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
import java.util.ArrayList;
import java.util.HashMap;

public class ModelDbServer implements ModelDBService.Iface {
  private DSLContext ctx;

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
    try {
      return FitEventDao.store(fe, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new FitEventResponse();
    }
  }

  public MetricEventResponse storeMetricEvent(MetricEvent me) throws TException {
    try {
      return MetricEventDao.store(me, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new MetricEventResponse();
    }
  }

  public TransformEventResponse storeTransformEvent(TransformEvent te) throws TException {
    try {
      return TransformEventDao.store(te, ctx, true);
    } catch (Exception e) {
      e.printStackTrace();
      return new TransformEventResponse();
    }
  }

  public RandomSplitEventResponse storeRandomSplitEvent(RandomSplitEvent rse) throws TException {
    try {
      return RandomSplitEventDao.store(rse, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new RandomSplitEventResponse();
    } 
  }

  public PipelineEventResponse storePipelineEvent(PipelineEvent pipelineEvent) throws TException {
    try {
      return PipelineEventDao.store(pipelineEvent, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new PipelineEventResponse();
    } 
  }

  public CrossValidationEventResponse storeCrossValidationEvent(CrossValidationEvent cve) throws TException {
    try {
      return CrossValidationEventDao.store(cve, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new CrossValidationEventResponse();
    }
  }

  public GridSearchCrossValidationEventResponse storeGridSearchCrossValidationEvent(GridSearchCrossValidationEvent gscve) throws TException {
    try {
      return GridSearchCrossValidationEventDao.store(gscve, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new GridSearchCrossValidationEventResponse();
    }
  }

  public AnnotationEventResponse storeAnnotationEvent(AnnotationEvent ae) throws TException {
    try {
      return AnnotationEventDao.store(ae, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new AnnotationEventResponse();
    }
  }

  public ProjectEventResponse storeProjectEvent(ProjectEvent pr) throws TException {
    try {
      return ProjectDao.store(pr, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ProjectEventResponse();
    }
  }

  public ExperimentEventResponse storeExperimentEvent(ExperimentEvent ee) throws TException {
    try {
      return ExperimentDao.store(ee, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ExperimentEventResponse();
    }
  }

  public ExperimentRunEventResponse storeExperimentRunEvent(ExperimentRunEvent er) throws TException {
    try {
      return ExperimentRunDao.store(er, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ExperimentRunEventResponse();
    }
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
    try {
      return DataFrameAncestryComputer.compute(dataFrameId, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new modeldb.DataFrameAncestry();
    }
  }

  public String pathForTransformer(int transformerId) throws TException {
    try {
      return TransformerDao.path(transformerId, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return null; 
    }
  }

  public CommonAncestor getCommonAncestor(int dfId1, int dfId2) throws TException {
    try {
      return DataFrameAncestryComputer.computeCommonAncestor(dfId1, dfId2, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new CommonAncestor();
    }
  }

  public CommonAncestor getCommonAncestorForModels(int modelId1, int modelid2) throws TException {
    int dfId1 = FitEventDao.getParentDfId(modelId1, ctx);
    int dfId2 = FitEventDao.getParentDfId(modelid2, ctx);
    try {
      return (dfId1 < 0 || dfId2 < 0)
        ? DataFrameAncestryComputer.getFailedAncestorLookup()
        : getCommonAncestor(dfId1, dfId2);
    } catch (Exception e) {
      e.printStackTrace();
      return new CommonAncestor();
    }
  }

  public int getTrainingRowsCount(int modelId) throws TException {
    try {
      return FitEventDao.getNumRowsForModels(Collections.singletonList(modelId), ctx).get(0);
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }

  public List<Integer> getTrainingRowsCounts(List<Integer> modelIds) throws TException {
    try {
      return FitEventDao.getNumRowsForModels(modelIds, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<Integer>();
    }
  }

  public CompareHyperParametersResponse compareHyperparameters(int modelId1, int modelId2) throws TException {  
    try {
      return HyperparameterComparison.compareHyperParameters(modelId1, modelId2, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new CompareHyperParametersResponse();
    }
  }

  public CompareFeaturesResponse compareFeatures(int modelId1, int modelId2) throws TException {
    try {
      return Feature.compareFeatures(modelId1, modelId2, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new CompareFeaturesResponse();
    }
  }

  public Map<ProblemType, List<Integer>> groupByProblemType(List<Integer> modelIds) {
    try {
      return ProblemTypeGrouper.groupByProblemType(modelIds, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new HashMap<ProblemType, List<Integer>>();
    }
  }

  public List<Integer> similarModels(int modelId, List<ModelCompMetric> compMetrics, int numModels) throws TException {
    try {
      return SimilarModels.similarModels(modelId, compMetrics, numModels, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<Integer>();
    }
  }

  public List<String> linearModelFeatureImportances(int modelId) throws TException {
    try {
      return LinearModelAlgorithms.featureImportances(modelId, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<String>();
    }
  }

  public List<FeatureImportanceComparison> compareLinearModelFeatureImportances(
    int model1Id,
    int model2Id
  ) throws TException {
    try {
      return LinearModelAlgorithms.featureImportances(model1Id, model2Id, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<FeatureImportanceComparison>();
    }
  }

  public List<Integer> iterationsUntilConvergence(List<Integer> modelIds, double tolerance) throws TException {
    try {
      return LinearModelAlgorithms.iterationsUntilConvergence(modelIds, tolerance, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<Integer>();
    }
  }

  public List<Integer> rankModels(List<Integer> modelIds, ModelRankMetric metric) throws TException {
    try {
      return LinearModelAlgorithms.rankModels(modelIds, metric, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<Integer>();
    }
  }

  public List<ConfidenceInterval> confidenceIntervals(int modelId, double significanceLevel) throws TException {
    try {
      return LinearModelAlgorithms.confidenceIntervals(modelId, significanceLevel, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<ConfidenceInterval>();
    }
  }

  public List<Integer> modelsWithFeatures(List<String> featureNames) throws TException {
    try {
      return Feature.modelsWithFeatures(featureNames, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<Integer>();
    }
  }

  public List<Integer> modelsDerivedFromDataFrame(int dfId) throws TException {
    try {
      return DataFrameAncestryComputer.descendentModels(dfId, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<Integer>();
    }
  }
}
