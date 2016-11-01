namespace java modeldb
namespace py modeldb

struct Project {
  1: i32 id = -1,
  2: string name,
  3: string author,
  4: string description
}

struct Experiment {
  1: i32 id = -1,
  2: i32 projectId,
  3: string name,
  4: string description,
  5: bool isDefault = 0
}

// Each experiment belongs to an experiment,
// even if it is just the default experiment
struct ExperimentRun {
  1: i32 id = -1,
  2: i32 experimentId,
  3: string description
}

struct DataFrameColumn {
  1: string name,
  2: string type
}

struct DataFrame {
  1: i32 id = -1, // when unknown
  2: list<DataFrameColumn> schema,
  3: i32 numRows,
  4: string tag = "",
  5: optional string filepath
}

struct HyperParameter {
  1: string name,
  2: string value,
  3: string type,
  4: double min,
  5: double max
}

struct ProjectEvent {
  1: Project project
}

struct ProjectEventResponse {
  1: i32 projectId
}

struct ExperimentEvent {
  1: Experiment experiment
}

struct ExperimentEventResponse {
  1: i32 experimentId
}

struct ExperimentRunEvent {
  1: ExperimentRun experimentRun
}

struct ExperimentRunEventResponse {
  1: i32 experimentRunId
}

// update this to be model spec?
// would make sense because it won't be
// specific to the ML env
struct TransformerSpec {
  1: i32 id = -1,
  2: string transformerType,
  3: list<string> features,
  4: list<HyperParameter> hyperparameters,
  5: string tag = ""
}

// Simplified for now. Only LinReg, LogReg
struct Transformer {
  1: i32 id = -1,
  2: list<double> weights,
  3: string transformerType,
  4: string tag ="",
  5: optional string filepath
}

enum ProblemType {
  UNDEFINED,
  BINARY_CLASSIFICATION,
  MULTICLASS_CLASSIFICATION,
  REGRESSION,
  CLUSTERING,
  RECOMMENDATION
}

struct LinearModelTerm {
  1: double coefficient,
  2: optional double tStat,
  3: optional double stdErr,
  4: optional double pValue
}

struct LinearModel {
  1: optional LinearModelTerm interceptTerm
  2: list<LinearModelTerm> featureTerms,
  // objectiveHistory[i] is the value of the objective function on iteration i.
  3: optional list<double> objectiveHistory,
  4: optional double rmse,
  5: optional double explainedVariance,
  6: optional double r2
}

// this needs to be updated to resemble
// type, params, features
struct FitEvent {
  1: DataFrame df,
  2: TransformerSpec spec,
  3: Transformer model,
  4: list<string> featureColumns,
  5: list<string> predictionColumns,
  6: list<string> labelColumns,
  7: i32 experimentRunId,
  8: optional ProblemType problemType = ProblemType.UNDEFINED
}

struct FitEventResponse {
  1: i32 dfId,
  2: i32 specId,
  3: i32 modelId,
  4: i32 eventId,
  5: i32 fitEventId
}

struct MetricEvent {
  1: DataFrame df,
  2: Transformer model,
  3: string metricType,
  4: double metricValue,
  5: string labelCol,
  6: string predictionCol,
  7: i32 experimentRunId
}

struct MetricEventResponse {
  1: i32 modelId,
  2: i32 dfId,
  3: i32 eventId,
  4: i32 metricEventId
}

struct TransformEvent {
  1: DataFrame oldDataFrame,
  2: DataFrame newDataFrame,
  3: Transformer transformer
  4: list<string> inputColumns,
  5: list<string> outputColumns,
  6: i32 experimentRunId
}

struct TransformEventResponse {
  1: i32 oldDataFrameId,
  2: i32 newDataFrameId,
  3: i32 transformerId,
  4: i32 eventId,
  5: string filepath
}

struct RandomSplitEvent {
  1: DataFrame oldDataFrame,
  2: list<double> weights,
  3: i64 seed,
  4: list<DataFrame> splitDataFrames,
  5: i32 experimentRunId
}

struct RandomSplitEventResponse {
  1: i32 oldDataFrameId,
  2: list<i32> splitIds,
  3: i32 splitEventId
}

struct PipelineTransformStage {
  1: i32 stageNumber,
  2: TransformEvent te
}

struct PipelineFitStage {
  1: i32 stageNumber,
  2: FitEvent fe
}

struct PipelineEvent {
  1: FitEvent pipelineFit,
  2: list<PipelineTransformStage> transformStages,
  3: list<PipelineFitStage> fitStages,
  4: i32 experimentRunId
}

struct PipelineEventResponse {
  1: FitEventResponse pipelineFitResponse,
  2: list<TransformEventResponse> transformStagesResponses,
  3: list<FitEventResponse> fitStagesResponses
}

struct AnnotationFragment {
  1: string type, // Must be "dataframe", "spec", "transformer", or "message".
  2: DataFrame df, // Fill this in if type = "dataframe".
  3: TransformerSpec spec, // Fill this in if type = "spec".
  4: Transformer transformer, // Fill this in if type = "transformer".
  5: string message // Fill this in if type = "message".
}

struct AnnotationFragmentResponse {
  1: string type,
  2: i32 id // The ID of the DataFrame, Transformer, or TransformerSpec. Null if type = "message".
}

struct AnnotationEvent {
  1: list<AnnotationFragment> fragments,
  2: i32 experimentRunId
}

struct AnnotationEventResponse {
  1: i32 annotationId,
  2: list<AnnotationFragmentResponse> fragmentResponses
}


struct DataFrameAncestry {
  1: list<DataFrame> ancestors
}

struct CommonAncestor {
  1: optional DataFrame ancestor,
  2: i32 chainIndexModel1,
  3: i32 chainIndexModel2
}

struct StringPair {
  1: string first,
  2: string second
}

struct CompareHyperParametersResponse {
  1: map<string, string> model1OnlyHyperparams,
  2: map<string, string> model2OnlyHyperparams,
  3: map<string, StringPair> sharedHyperparams
}

struct CompareFeaturesResponse {
  1: list<string> model1OnlyFeatures,
  2: list<string> model2OnlyFeatures,
  3: list<string> commonFeatures
}

enum ModelCompMetric {
  PROJECT,
  EXPERIMENT_RUN,
  MODEL_TYPE,
  PROBLEM_TYPE,
  RMSE,
  EXPLAINED_VARIANCE,
  R2
}

struct FeatureImportanceComparison {
  1: string featureName,
  2: optional double percentileRankInModel1, // Defined if feature is in model 1.
  3: optional double percentileRankInModel2  // Defined if feature is in model 2.
}

enum ModelRankMetric {
  RMSE,
  EXPLAINED_VARIANCE,
  R2
}

struct ConfidenceInterval {
  1: i32 featureIndex,
  2: double low,
  3: double high
}

struct ProjectExperimentsAndRuns {
  1: i32 projId,
  2: list<Experiment> experiments,
  3: list<ExperimentRun> experimentRuns
}

struct ProjectOverviewResponse {
  1: Project project,
  2: i32 numExperiments,
  3: i32 numExperimentRuns
}

struct ModelResponse {
  1: i32 id,
  2: i32 experimentRunId,
  3: i32 experimentId,
  4: i32 projectId,
  5: DataFrame trainingDataFrame,
  6: TransformerSpec specification,
  7: ProblemType problemType,
  8: list<string> featureColumns,
  9: list<string> labelColumns,
  10: list<string> predictionColumns,
  // Map from metric name to evaluation DataFrame ID to metric value.
  // We need the evaluation DataFrame ID because we could compute the same
  // metric on multiple DataFrames.
  11: map<string, map<i32, double>> metrics,
  // Turn each annotation into a string.
  12: list<string> annotations,
  13: string sha,
  14: string filepath,
  15: optional LinearModel linearModelData
}

struct ExperimentRunDetailsResponse {
  1: Project project,
  2: Experiment experiment,
  3: ExperimentRun experimentRun,
  4: list<ModelResponse> modelResponses,
}

struct CrossValidationFold {
  1: Transformer model,
  2: DataFrame validationDf,
  3: DataFrame trainingDf,
  4: double score
}

struct CrossValidationFoldResponse {
  1: i32 modelId,
  2: i32 validationId,
  3: i32 trainingId
}

struct CrossValidationEvent {
  1: DataFrame df,
  2: TransformerSpec spec,
  3: i64 seed,
  4: string evaluator,
  5: list<string> labelColumns,
  6: list<string> predictionColumns,
  7: list<string> featureColumns,
  // Note that we don't need to store numFolds, because we can infer that from from the length of this list.
  8: list<CrossValidationFold> folds,
  9: i32 experimentRunId,
  10: optional ProblemType problemType = ProblemType.UNDEFINED
}

struct CrossValidationEventResponse {
  1: i32 dfId,
  2: i32 specId,
  3: i32 eventId,
  4: list<CrossValidationFoldResponse> foldResponses,
  5: i32 crossValidationEventId
}

struct GridSearchCrossValidationEvent {
  1: i32 numFolds,
  2: FitEvent bestFit,
  3: list<CrossValidationEvent> crossValidations,
  4: i32 experimentRunId
  5: optional ProblemType problemType = ProblemType.UNDEFINED
}

struct GridSearchCrossValidationEventResponse {
  1: i32 gscveId,
  2: i32 eventId,
  3: FitEventResponse fitEventResponse,
  4: list<CrossValidationEventResponse> crossValidationEventResponses,
}





// Thrown when a specified resource (e.g. DataFrame, Transformer) is not found.
// For example, if you try to read Transformer with ID 1, then we throw this
// exception if that Transformer does not exist.
exception ResourceNotFoundException {
  1: string message
}

// Thrown when field of a structure is empty or incorrect.
// For example, if you try to get the path for a Transformer, but the server
// finds that the path is empty, then this exception gets thrown.
exception InvalidFieldException {
  1: string message
}

// Thrown when the request is not properly constructed.
// For example, if you say that you want to find -3 similar models, then this
// exception gets thrown.
exception BadRequestException {
  1: string message
}

// Thrown when the server is told to perform an operation that it cannot do.
// For example, if you try to compute feature importances for a linear model
// that isn't standardized, then it is not possible to compute feature 
// importance, so this exception gets thrown.
exception IllegalOperationException {
  1: string message
}

// Thrown when the server has thrown an exception that we did not think of.
exception ServerLogicException {
  1: string message
}

// Thrown when an Experiment Run ID is not defined.
exception InvalidExperimentRunException {
  1: string message
}

service ModelDBService {
  // This is just a method to test connection to the server. It returns 200.
  i32 testConnection(), 

  string pathForTransformer(1: i32 transformerId) 
    throws (1: ResourceNotFoundException rnfEx, 2: InvalidFieldException efEx, 3: ServerLogicException svEx),

  FitEventResponse storeFitEvent(1:FitEvent fe) 
   throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  MetricEventResponse storeMetricEvent(1:MetricEvent me) 
    throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  TransformEventResponse storeTransformEvent(1:TransformEvent te) 
    throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  RandomSplitEventResponse storeRandomSplitEvent(1:RandomSplitEvent rse)
    throws (1: ServerLogicException svEx),

  PipelineEventResponse storePipelineEvent(1: PipelineEvent pipelineEvent)
    throws (1: ServerLogicException svEx),

  CrossValidationEventResponse storeCrossValidationEvent(1: CrossValidationEvent cve)
    throws (1: ServerLogicException svEx),

  GridSearchCrossValidationEventResponse storeGridSearchCrossValidationEvent(1: GridSearchCrossValidationEvent gscve)
    throws (1: ServerLogicException svEx),

  AnnotationEventResponse storeAnnotationEvent(1: AnnotationEvent ae)
    throws (1: ServerLogicException svEx),

  ProjectEventResponse storeProjectEvent(1: ProjectEvent pr)
    throws (1: ServerLogicException svEx),

  ExperimentEventResponse storeExperimentEvent(1: ExperimentEvent er)
    throws (1: ServerLogicException svEx),

  ExperimentRunEventResponse storeExperimentRunEvent(1: ExperimentRunEvent er)
    throws (1: ServerLogicException svEx),

  // Associate LinearModel metadata with an already stored model
  // (i.e. Transformer) with the given id. Returns a boolean indicating
  // whether the metadata was correctly stored.
  bool storeLinearModel(1: i32 modelId, 2: LinearModel model) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  DataFrameAncestry getDataFrameAncestry(1: i32 dataFrameId) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  CommonAncestor getCommonAncestor(1: i32 dfId1, 2: i32 dfId2) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  CommonAncestor getCommonAncestorForModels(1: i32 modelId1, 2: i32 modelId2) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  i32 getTrainingRowsCount(1: i32 modelId) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  // Returns the number of training rows in each model. If a model cannot be found,
  // we mark that it has -1 training rows.
  list<i32> getTrainingRowsCounts(1: list<i32> modelIds) 
    throws (1: ServerLogicException svEx),

  CompareHyperParametersResponse compareHyperparameters(1: i32 modelId1, 2: i32 modelId2) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  CompareFeaturesResponse compareFeatures(1: i32 modelId1, 2: i32 modelId2) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  // Maps from ProblemType to the list of models with that problem type.
  // If any of the model IDs cannot be found, it will be left out of the map.
  map<ProblemType, list<i32>> groupByProblemType(1: list<i32> modelIds)
    throws (1: ServerLogicException svEx),

  list<i32> similarModels(1: i32 modelId, 2: list<ModelCompMetric> compMetrics, 3: i32 numModels) 
    throws (1: ResourceNotFoundException rnfEx, 2: BadRequestException brEx, 3: ServerLogicException svEx),

  // Return the features, ordered by importance, for a linear model.
  // The returned list will be empty if the model does not exist, is not 
  // linear, or if its features are not standardized (i.e. it must have been
  // trained with a hyperparameter called "standardization" set to "true").
  list<string> linearModelFeatureImportances(1: i32 modelId) 
    throws (1: ResourceNotFoundException rnfEx, 2: IllegalOperationException ioEx, 3: ServerLogicException svEx),

  // Compares the feature importances of the two models.
  list<FeatureImportanceComparison> compareLinearModelFeatureImportances(1: i32 model1Id, 2: i32 model2Id) 
    throws (1: ResourceNotFoundException rnfEx, 2: IllegalOperationException ioEx, 3: ServerLogicException svEx),

  // Given the a list model IDs, return the number of iterations that each
  // took to converge (convergence specified via tolerance). 
  // If any model does not exist or does not have an objective history, 
  // we give it -1 iterations.
  list<i32> iterationsUntilConvergence(1: list<i32> modelIds, 2: double tolerance)
    throws (1: ServerLogicException svEx),

  // Rank the given models by some metric. The returned list will contain
  // the models ordered by highest metric to lowest metric. If we cannot
  // find the corresponding metric value for a given model, its id will be
  // omitted from the returned list.
  list<i32> rankModels(1: list<i32> modelIds, 2: ModelRankMetric metric)
    throws (1: ServerLogicException svEx),

  list<ConfidenceInterval> confidenceIntervals(1: i32 modelId, 2: double sigLevel) 
    throws (1: ResourceNotFoundException rnfEx, 2: IllegalOperationException ioEx, 3: BadRequestException brEx, 4: ServerLogicException svEx),

  // Get the IDs of the models that use the given set of features.
  list<i32> modelsWithFeatures(1: list<string> featureNames)
    throws (1: ServerLogicException svEx),

  // Get the IDs of the models that are derived from the DataFrame with the 
  // given ID, or one of its descendent DataFrames. This will only consider
  // models and DataFrames in the same project as the given dfId.
  list<i32> modelsDerivedFromDataFrame(1: i32 dfId) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx)

  ModelResponse getModel(1: i32 modelId) throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  list<ExperimentRun> getRunsInExperiment(1: i32 experimentId) throws (1: ServerLogicException svEx),

  ProjectExperimentsAndRuns getRunsAndExperimentsInProject(1: i32 projId) throws (1: ServerLogicException svEx),

  list<ProjectOverviewResponse> getProjectOverviews() throws (1: ServerLogicException svEx),

  ExperimentRunDetailsResponse getExperimentRunDetails(1: i32 experimentRunId) throws (1: ServerLogicException svEx, 2: ResourceNotFoundException rnfEx)
}
