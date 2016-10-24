// namespace scala modeldb
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
  4: string tag = ""
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
  4: string tag =""
}

enum ProblemType {
  UNDEFINED,
  BINARY_CLASSIFICATION,
  MULTICLASS_CLASSIFICATION,
  REGRESSION,
  CLUSTERING,
  RECOMMENDATION
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
  1: bool dataframeExists,
  2: list<DataFrame> ancestors
}

struct CommonAncestor {
  1: bool foundAncestor,
  2: DataFrame ancestor,
  3: i32 chainIndexModel1,
  4: i32 chainIndexModel2
}

struct StringPair {
  1: string first,
  2: string second
}

struct CompareHyperParametersResponse {
  1: bool computedComparison,
  2: map<string, string> model1OnlyHyperparams,
  3: map<string, string> model2OnlyHyperparams,
  4: map<string, StringPair> sharedHyperparams
}

struct CompareFeaturesResponse {
  1: bool computedComparison,
  2: list<string> model1OnlyFeatures,
  3: list<string> model2OnlyFeatures,
  4: list<string> commonFeatures
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

exception ResourceNotFoundException {
  1: string message
}

exception EmptyFieldException {
  1: string message
}

service ModelDBService {
  // This is just a test method to test connection to the server
  i32 testConnection(), // 0 if success, -1 failure

  string pathForTransformer(1: i32 transformerId) throws (1: ResourceNotFoundException rnfEx, 2: EmptyFieldException efEx),

  FitEventResponse storeFitEvent(1:FitEvent fe),

  MetricEventResponse storeMetricEvent(1:MetricEvent me),

  TransformEventResponse storeTransformEvent(1:TransformEvent te),

  RandomSplitEventResponse storeRandomSplitEvent(1:RandomSplitEvent rse),

  PipelineEventResponse storePipelineEvent(1: PipelineEvent pipelineEvent),

  CrossValidationEventResponse storeCrossValidationEvent(1: CrossValidationEvent cve),

  GridSearchCrossValidationEventResponse storeGridSearchCrossValidationEvent(1: GridSearchCrossValidationEvent gscve),

  AnnotationEventResponse storeAnnotationEvent(1: AnnotationEvent ae),

  ProjectEventResponse storeProjectEvent(1: ProjectEvent pr),

  ExperimentEventResponse storeExperimentEvent(1: ExperimentEvent er),

  ExperimentRunEventResponse storeExperimentRunEvent(1: ExperimentRunEvent er),

  // Associate LinearModel metadata with an already stored model
  // (i.e. Transformer) with the given id. Returns a boolean indicating
  // whether the metadata was correctly stored.
  bool storeLinearModel(1: i32 modelId, 2: LinearModel model),

  DataFrameAncestry getDataFrameAncestry(1: i32 dataFrameId),

  CommonAncestor getCommonAncestor(1: i32 dfId1, 2: i32 dfId2),

  CommonAncestor getCommonAncestorForModels(1: i32 modelId1, 2: i32 modelId2),

  i32 getTrainingRowsCount(1: i32 modelId),

  list<i32> getTrainingRowsCounts(1: list<i32> modelIds),

  CompareHyperParametersResponse compareHyperparameters(1: i32 modelId1, 2: i32 modelId2),

  CompareFeaturesResponse compareFeatures(1: i32 modelId1, 2: i32 modelId2),

  map<ProblemType, list<i32>> groupByProblemType(1: list<i32> modelIds),

  list<i32> similarModels(1: i32 modelId, 2: list<ModelCompMetric> compMetrics, 3: i32 numModels),

  // Return the features, ordered by importance, for a linear model.
  // The returned list will be empty if the model does not exist, is not 
  // linear, or if its features are not standardized (i.e. it must have been
  // trained with a hyperparameter called "standardization" set to "true").
  list<string> linearModelFeatureImportances(1: i32 modelId),

  // Compares the feature importances of the two models.
  // The returned list will be empty if:
  // 1: Either modelId does not have an associated Transformer and FitEvent.
  // 2: Either model is not linear
  // 3: Either model does not have the hyperparameter "standardization" set 
  //  to "true".
  list<FeatureImportanceComparison> compareLinearModelFeatureImportances(1: i32 model1Id, 2: i32 model2Id),

  // Given the a list model IDs, return the number of iterations that each
  // took to converge (convergence specified via tolerance). 
  // If any model does not exist or does not have an objective history, 
  // we give it -1 iterations.
  list<i32> iterationsUntilConvergence(1: list<i32> modelIds, 2: double tolerance),

  // Rank the given models by some metric. The returned list will contain
  // the models ordered by highest metric to lowest metric. If we cannot
  // find the corresponding metric value for a given model, its id will be
  // omitted from the returned list.
  list<i32> rankModels(1: list<i32> modelIds, 2: ModelRankMetric metric),

  list<ConfidenceInterval> confidenceIntervals(1: i32 modelId, 2: double sigLevel),

  // Get the IDs of the models that use the given set of features.
  list<i32> modelsWithFeatures(1: list<string> featureNames),

  // Get the IDs of the models that are derived from the DataFrame with the 
  // given ID, or one of its descendent DataFrames. This will only consider
  // models and DataFrames in the same project as the given dfId.
  list<i32> modelsDerivedFromDataFrame(1: i32 dfId)
}
