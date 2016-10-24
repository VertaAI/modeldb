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

  public FitEventResponse storeFitEvent(FitEvent fe) throws InvalidExperimentRunException {
    try {
      ExperimentRunDao.validateExperimentRunId(fe.experimentRunId, ctx);
      return FitEventDao.store(fe, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public MetricEventResponse storeMetricEvent(MetricEvent me) throws InvalidExperimentRunException {
    try {
      ExperimentRunDao.validateExperimentRunId(me.experimentRunId, ctx);
      return MetricEventDao.store(me, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public TransformEventResponse storeTransformEvent(TransformEvent te) throws InvalidExperimentRunException {
    try {
      ExperimentRunDao.validateExperimentRunId(te.experimentRunId, ctx);
      return TransformEventDao.store(te, ctx, true);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public RandomSplitEventResponse storeRandomSplitEvent(RandomSplitEvent rse) throws InvalidExperimentRunException {
    try {
      ExperimentRunDao.validateExperimentRunId(rse.experimentRunId, ctx);
      return RandomSplitEventDao.store(rse, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    } 
  }

  public PipelineEventResponse storePipelineEvent(PipelineEvent pipelineEvent) throws InvalidExperimentRunException {
    try {
      ExperimentRunDao.validateExperimentRunId(pipelineEvent.experimentRunId, ctx);
      return PipelineEventDao.store(pipelineEvent, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    } 
  }

  public CrossValidationEventResponse storeCrossValidationEvent(CrossValidationEvent cve)
    throws InvalidExperimentRunException {
    try {
      ExperimentRunDao.validateExperimentRunId(cve.experimentRunId, ctx);
      return CrossValidationEventDao.store(cve, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public GridSearchCrossValidationEventResponse storeGridSearchCrossValidationEvent(GridSearchCrossValidationEvent gscve)
    throws InvalidExperimentRunException {
    try {
      ExperimentRunDao.validateExperimentRunId(gscve.experimentRunId, ctx);
      return GridSearchCrossValidationEventDao.store(gscve, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public AnnotationEventResponse storeAnnotationEvent(AnnotationEvent ae) throws InvalidExperimentRunException {
    try {
      ExperimentRunDao.validateExperimentRunId(ae.experimentRunId, ctx);
      return AnnotationEventDao.store(ae, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public ProjectEventResponse storeProjectEvent(ProjectEvent pr) {
    try {
      return ProjectDao.store(pr, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
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

  public boolean storeLinearModel(int modelId, LinearModel model) throws ResourceNotFoundException {
    try {
      return LinearModelDao.store(modelId, model, ctx);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public modeldb.DataFrameAncestry getDataFrameAncestry(int dataFrameId) throws ResourceNotFoundException {
    try {
      return DataFrameAncestryComputer.compute(dataFrameId, ctx);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public String pathForTransformer(int transformerId) throws ResourceNotFoundException, InvalidFieldException {
    try {
      return TransformerDao.path(transformerId, ctx);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public CommonAncestor getCommonAncestor(int dfId1, int dfId2) throws ResourceNotFoundException {
    try {
      return DataFrameAncestryComputer.computeCommonAncestor(dfId1, dfId2, ctx);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public CommonAncestor getCommonAncestorForModels(int modelId1, int modelid2) throws ResourceNotFoundException {
    try {
      int dfId1 = FitEventDao.getParentDfId(modelId1, ctx);
      int dfId2 = FitEventDao.getParentDfId(modelid2, ctx);
      return getCommonAncestor(dfId1, dfId2);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public int getTrainingRowsCount(int modelId) throws ResourceNotFoundException {
    try {
      int numRows = FitEventDao.getNumRowsForModels(Collections.singletonList(modelId), ctx).get(0);
      if (numRows < 0) {
        throw new ResourceNotFoundException(String.format(
          "Could not find number of rows used to train Transformer %d because the Transformer doesn't exist",
          modelId
        ));
      }
      return numRows;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public List<Integer> getTrainingRowsCounts(List<Integer> modelIds) throws TException {
    try {
      return FitEventDao.getNumRowsForModels(modelIds, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  public CompareHyperParametersResponse compareHyperparameters(int modelId1, int modelId2)
    throws ResourceNotFoundException {
    try {
      return HyperparameterComparison.compareHyperParameters(modelId1, modelId2, ctx);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public CompareFeaturesResponse compareFeatures(int modelId1, int modelId2) throws ResourceNotFoundException {
    try {
      return Feature.compareFeatures(modelId1, modelId2, ctx);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public Map<ProblemType, List<Integer>> groupByProblemType(List<Integer> modelIds) {
    try {
      return ProblemTypeGrouper.groupByProblemType(modelIds, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new HashMap<>();
    }
  }

  public List<Integer> similarModels(int modelId, List<ModelCompMetric> compMetrics, int numModels)
    throws ResourceNotFoundException, BadRequestException {
    try {
      return SimilarModels.similarModels(modelId, compMetrics, numModels, ctx);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public List<String> linearModelFeatureImportances(int modelId)
    throws ResourceNotFoundException, IllegalOperationException {
    try {
      return LinearModelAlgorithms.featureImportances(modelId, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public List<FeatureImportanceComparison> compareLinearModelFeatureImportances(
    int model1Id,
    int model2Id
  ) throws ResourceNotFoundException, IllegalOperationException {
    try {
      return LinearModelAlgorithms.featureImportances(model1Id, model2Id, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public List<Integer> iterationsUntilConvergence(List<Integer> modelIds, double tolerance) throws TException {
    try {
      return LinearModelAlgorithms.iterationsUntilConvergence(modelIds, tolerance, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  public List<Integer> rankModels(List<Integer> modelIds, ModelRankMetric metric) throws TException {
    try {
      return LinearModelAlgorithms.rankModels(modelIds, metric, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  public List<ConfidenceInterval> confidenceIntervals(int modelId, double significanceLevel)
    throws BadRequestException, IllegalOperationException, ResourceNotFoundException {
    try {
      return LinearModelAlgorithms.confidenceIntervals(modelId, significanceLevel, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public List<Integer> modelsWithFeatures(List<String> featureNames) throws TException {
    try {
      return Feature.modelsWithFeatures(featureNames, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  public List<Integer> modelsDerivedFromDataFrame(int dfId) throws ResourceNotFoundException {
    try {
      return DataFrameAncestryComputer.descendentModels(dfId, ctx);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}
