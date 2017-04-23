package edu.mit.csail.db.ml.server;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.server.algorithm.*;
import edu.mit.csail.db.ml.server.algorithm.similarity.SimilarModels;
import edu.mit.csail.db.ml.server.storage.*;
import edu.mit.csail.db.ml.server.storage.metadata.MetadataDb;
import edu.mit.csail.db.ml.util.ContextFactory;
import edu.mit.csail.db.ml.util.ExceptionWrapper;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import modeldb.*;
import org.apache.thrift.TException;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;

/**
 * This class represents the processors that handles the requests that the ModelDB service can receive.
 *
 * Try to make the handlers in this class very short. Ideally, each one should be just a single line. The advantage in
 * keeping them short is that it is easier to test the codebase.
 *
 * For documentation on the API methods, see the ModelDB.thrift file.
 */
public class ModelDbServer implements ModelDBService.Iface {
  /**
   * The database context.
   */
  private DSLContext ctx;

  /**
   * Metadata connections
   */
  private MetadataDb metadataDb;

  /**
   * Create the service and connect to the database.
   * @param username - The username to connect to the database.
   * @param password - The password to connect to the database.
   * @param jdbcUrl - The JDBC URL that points to the database.
   * @param dbType - The type of the database (only SQLite is supported for now).
   * @param metadataDbHost - Host for metadataDb
   * @param metadataDbPort - Port for metadataDb
   * @param metadataDbName - Name of DB in metadataDB
   * @param metadataDbType - type of DB used for metadata
   */
  public ModelDbServer(
    String username, 
    String password, 
    String jdbcUrl, 
    ModelDbConfig.DatabaseType dbType, 
    String metadataDbHost, 
    int metadataDbPort, 
    String metadataDbName, 
    ModelDbConfig.MetadataDbType metadataDbType) {
    try {
      this.ctx = ContextFactory.create(username, password, jdbcUrl, dbType);
      this.metadataDb = ContextFactory.createMetadataDb(metadataDbHost, 
        metadataDbPort, metadataDbName, metadataDbType);
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
    return ExceptionWrapper.run(fe.experimentRunId, ctx, metadataDb, () -> {
      FitEventResponse fer = FitEventDao.store(fe, ctx);
      if (!MetadataDao.store(fer, fe, metadataDb)) {
        throw new TException("Metadata was not stored successfully.");
      }
      return fer;
    });
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
    return ExceptionWrapper.run(metadataDb, () -> TransformerDao.readInfo(modelId, ctx, metadataDb));
  }

  public List<Integer> getProjectIds(Map<String, String> keyValuePairs) throws TException {
    return ExceptionWrapper.run(() -> ProjectDao.getProjectIds(keyValuePairs, ctx));
  }

  public List<Integer> getModelIds(Map<String, String> keyValuePairs) throws TException {
    return ExceptionWrapper.run(() -> MetadataDao.getModelIds(keyValuePairs, metadataDb));
  }

  public boolean updateProject(int projectId, String key, String value) throws TException {
    return ExceptionWrapper.run(() -> ProjectDao.updateProject(projectId, key, value, ctx));
  }

  public boolean createOrUpdateScalarField(int modelId, String key, String value, String valueType) throws TException {
    return ExceptionWrapper.run(() -> MetadataDao.createOrUpdateScalarField(modelId, key, value, valueType, metadataDb));
  }

  public boolean createVectorField(int modelId, String vectorName, Map<String, String> vectorConfig) throws TException {
    return ExceptionWrapper.run(() -> MetadataDao.createVectorField(modelId, vectorName, vectorConfig, metadataDb));
  }

  public boolean updateVectorField(int modelId, String key, int valueIndex, String value, String valueType) throws TException {
    return ExceptionWrapper.run(() -> MetadataDao.updateVectorField(modelId, key, valueIndex, value, valueType, metadataDb));
  }

  public boolean appendToVectorField(int modelId, String vectorName, String value, String valueType) throws TException {
    return ExceptionWrapper.run(() -> MetadataDao.appendToVectorField(modelId, vectorName, value, valueType, metadataDb));
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
    return ExceptionWrapper.run(() -> ExperimentRunDao.readExperimentRunDetails(experimentRunId, ctx, metadataDb));
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

  public String getFilePath(Transformer t, int experimentRunId, String filepath) throws TException {
    return ExceptionWrapper.run(() -> TransformerDao.getFilePath(t, experimentRunId, filepath, ctx));
  }

  public ExtractedPipelineResponse extractPipeline(int modelId) throws TException {
    return ExceptionWrapper.run(() -> DataFrameAncestryComputer.extractPipeline(modelId, ctx));
  }
}
