/* 
  The thrift file specifies the structs and functions that are shared
  among modeldb projects. Thrift automatically generates classes to manipulate
  the data in each language.
*/

namespace java modeldb
namespace py modeldb

/* 
  A project is the highest level in the grouping hierarchy. A project can 
  contain multiple experiments, which in turn can contain experiment runs

  Attributes:
  id: A unique identifier for this project
  name: The name of this project - human readable text assigned as a name
  author: The name of the author, the person who created the project
  description: A human readable description of the project - i.e. it's goals and methods

  Note that in order to flatten this into tables, the experiments contained within this
  project are not in the project struct.
*/
struct Project {
  1: i32 id = -1,
  2: string name,
  3: string author,
  4: string description
}

/* 
  Experiments are the second level in the grouping hierarchy. An experiment can contain
  multiple experiment runs. In order to flatten this into tables, the runs are not stored
  within the experiment.

  id: A unique experiment identifier
  projectId: The project that contains this experiment
  name: The name of this experiment, human readable
  description: A short description of the experiment and what it contains
  isDefault: Whether this is the default experiment of this project (a catch-all for runs)
*/
struct Experiment {
  1: i32 id = -1,
  2: i32 projectId,
  3: string name,
  4: string description,
  5: bool isDefault = 0
}

/*
  Experiment runs are contained within experiments. Note that the
  experiment must be specified, even if it is the default experiment.

  id: a unique identifier for this run (unique across all projects)
  experimentId: The id of the experiment that contains this run
  description: User assigned text to this experiment run. Can be used to summarize
    data, method, etc.
*/
struct ExperimentRun {
  1: i32 id = -1,
  2: i32 experimentId,
  3: string description
}

/* 
  A column in a DataFrame (see below). Stores information about the column,
  but not the elements.

  name: The name of this column
  type: The type of data stored in this column (e.g. Integer, String)
*/
struct DataFrameColumn {
  1: string name,
  2: string type
}

/* 
  A tabular set of data. Contains many columns, each containing a single type
  of data. Each row in a DataFrame represents a new element.

  id: A unique identifier for this DataFrame
  schema: The information about the columns (column headers)
          that are in this data frame
  numRows: The number of elements (rows) that this DataFrame contains
  tag: User assigned human readable text 
  filepath: The path to the file that contains the data in this DataFrame
*/
struct DataFrame {
  1: i32 id = -1, // when unknown
  2: list<DataFrameColumn> schema,
  3: i32 numRows,
  4: string tag = "",
  5: optional string filepath
}

/* 
  A HyperParameter guides the fitting of a model to a set of data.
  (e.g. number of trees in a random forest)

  name: The name of this HyperParameter
  value: The value assigned to this HyperParameter for fitting
  type: The type of data stored in the HyperParameter (e.g. Integer, String)
  min: (for numeric HyperParameters only) The minimum value allowed for this parameter
  max: (for numeric HyperParameters only) The maximum value allowed for this parameter
*/
struct HyperParameter {
  1: string name,
  2: string value,
  3: string type,
  4: double min,
  5: double max
}

/* 
  An event that represents the creation of a project. 

  project: The project to be created by this event
*/
struct ProjectEvent {
  1: Project project
}

/* 
  The server's response to creating a project. 

  projectId: The id of the created project
*/
struct ProjectEventResponse {
  1: i32 projectId
}

/* 
  An event representing the creation of an Experiment.

  experiment: The created Experiment
*/
struct ExperimentEvent {
  1: Experiment experiment
}

/* 
  The response given to the creation of an Experiment.

  experimentId: The id of the experiment created

*/
struct ExperimentEventResponse {
  1: i32 experimentId
}

/* 
  An event representing the creation of an Experiment Run.

  experimentRun: The ExperimentRun to create.
*/
struct ExperimentRunEvent {
  1: ExperimentRun experimentRun
}

/* 
  The response given to the creation of an Experiment Run.

  experimentRunId: The id of the created ExperimentRun
*/
struct ExperimentRunEventResponse {
  1: i32 experimentRunId
}

// update this to be model spec?

/* 
  A TransformerSpec is a machine learning primitive that describes 
  the hyperparameters used to create a model (A Transformer produced
  by fitting a TransformerSpec to a DataFrame).

  id: A unique identifier for this TransformerSpec
  transformerType: The type of the transformer it guides (e.g. linear regression)
  hyperparameters: The hyperparameters that guide this spec
  tag: User assigned content associated with this Spec
*/
struct TransformerSpec {
  1: i32 id = -1,
  2: string transformerType,
  3: list<HyperParameter> hyperparameters,
  4: string tag = ""
}

/* 
  A transformer is a machine learning primitive that takes in a DataFrame and 
  outputs another DataFrame.

  id: A unique identifier for this transformer
  transformerType: The type of transformer this is (e.g. linear regression)
  tag: User assigned content associated with this transformer
  filepath: The path to the serialized version of this transformer
*/
struct Transformer {
  1: i32 id = -1,
  // transformerType is present in spec as well as transformer 
  // because some transformers may not have a spec associated with them
  2: string transformerType, 
  3: string tag ="",
  4: optional string filepath
}

/*
  The types of problems compatible with modeldb
*/
enum ProblemType {
  UNDEFINED,
  BINARY_CLASSIFICATION,
  MULTICLASS_CLASSIFICATION,
  REGRESSION,
  CLUSTERING,
  RECOMMENDATION
}

/* 
  A term in a linear regression model.

  coefficient: The coefficient of the variable
  tStat: An optional T-Value to associate with this term
  stdErr: An optional calculated Standard Error to associate with this term
  pValue: An optional P-Value to associate with this term
*/
struct LinearModelTerm {
  1: double coefficient,
  2: optional double tStat,
  3: optional double stdErr,
  4: optional double pValue
}

/* 
  Contains information about a linear regression model.

  interceptTerm: An optional term to change the intercept of this model
  featureTerms: Information about the terms (particularly, coefficients of 
                each term) for each feature used in this model
  objectiveHistory: An optional history of every value the objective function has
                    obtained while being created, where objectiveHistory[i] is 
                    the value of the objective function on iteration i.
  rmse: An optional root-mean-square error of this model from the data used to calculate it
  explainedVariance: (Optional) The calculated explained variance of the data
      --Explained Variance measures the proportion to which a mathematical model 
      -- accounts for the variation (dispersion) of a given data set (Wikipedia)
  r2: (Optional) The r^2 value that measures how well fitted the model is
*/
struct LinearModel {
  1: optional LinearModelTerm interceptTerm
  2: list<LinearModelTerm> featureTerms,
  3: optional list<double> objectiveHistory,
  4: optional double rmse,
  5: optional double explainedVariance,
  6: optional double r2
}

// this needs to be updated to resemble
// type, params, features
/*
  Contains information related to a single event that fits a Transformer Spec 
  to a DataFrame

  df: The DataFrame that is being fitted to
  spec: The transformerSpec guiding the fitting
  model: The model (fitted Transformer) produced by the fitting
  featureColumns: The names of the features in the data frame
  predictionColumns: The names of the columns produced by the transformer
  labelColumns: The columns the the prediction columns are supposed to 
    predict in the original DataFrame
  experimentRunId: The id of the ExperimentRun which contains this event
  problemType: The type of problem this event is solving e.g. regression
*/
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

/*
  The response given after the creation of a fit event

  dfId: The id of the DataFrame referenced by the fit event
  specId: The id of the TransformerSpec that guided the fit
  modelId: The id of the outputted Transformer
  eventId: The generic event id of this fit event (unique among all events)
  fitEventId: The specific FitEvent id of the created fit event (unique among fit events)
*/
struct FitEventResponse {
  1: i32 dfId,
  2: i32 specId,
  3: i32 modelId,
  4: i32 eventId,
  5: i32 fitEventId
}

/*
  An event containing information regarding the evaluation of a model

  df: The DataFrame used for evaluation
  model: The model being evaluated
  metricType: The type of the calculated metric (e.g. Squared Error, Accuracy)
  labelCol: The column in the original DataFrame that this metric is calculated for
  predictionCol: The column from the predicted columns that this metric is calculated for
  experimentRunId: The id of the Experiment Run that contains this event.
*/
struct MetricEvent {
  1: DataFrame df,
  2: Transformer model,
  3: string metricType,
  4: double metricValue,
  5: string labelCol,
  6: string predictionCol,
  7: i32 experimentRunId
}

/*
  The response given after creating a MetricEvent

  modelId: The id of the Transformer for which this metric was calculated for
  dfId: The id of the DataFrame used for this calculation
  eventId: The generic event id of the created event (unique across all events)
  metricEventId: The id of the MetricEvent (unique across all MetricEvents)
*/
struct MetricEventResponse {
  1: i32 modelId,
  2: i32 dfId,
  3: i32 eventId,
  4: i32 metricEventId
}

/*
  A Transform event records using a transformer to generate an output DataFrame from
  an input DataFrame.

  oldDataFrame: The input DataFrame
  newDataFrame: The output DataFrame
  transformer: The transformer to perform the transformation
  inputColumns: The columns from the input DataFrame for the transformer to use
  outputColumns: The columns that the transformer should output to in the new DataFrame
  experimentRunId: The id of the Experiment Run that contains this event
*/
struct TransformEvent {
  1: DataFrame oldDataFrame,
  2: DataFrame newDataFrame,
  3: Transformer transformer
  4: list<string> inputColumns,
  5: list<string> outputColumns,
  6: i32 experimentRunId
}

/* 
  The response given to the creation of a Transform.

  oldDataFrameId: The id of the input DataFrame of the transform
  newDataFrameId: The id of the output DataFrame of the transform
  transformerId: The id of the transformer used to apply this transformation
  eventId: The id of this 
*/
struct TransformEventResponse {
  1: i32 oldDataFrameId,
  2: i32 newDataFrameId,
  3: i32 transformerId,
  4: i32 eventId,
  5: string filepath
}

/*
  An event represents breaking a DataFrame into 
  smaller DataFrames randomly according to a weight vector that
  specifies the relative sizes of the smaller DataFrames.

  oldDataFrame: The DataFrame to splitIds
  weights: The weight vector to split the DataFrame by (weights of pieces)
  seed: A psuedo-random number generator seed
  splitDataFrames: The output DataFrames (pieces)
  experimentRunId: The Experiment Run that contains this event
*/
struct RandomSplitEvent {
  1: DataFrame oldDataFrame,
  2: list<double> weights,
  3: i64 seed,
  4: list<DataFrame> splitDataFrames,
  5: i32 experimentRunId
}

/* 
  The response to the creation of a Random Split event

  oldDataFrameId: The id of the input DataFrame
  splitIds: A list of ids, to the new smaller DataFrames
  splitEventId: The id of this split event (unique among split events)
*/
struct RandomSplitEventResponse {
  1: i32 oldDataFrameId,
  2: list<i32> splitIds,
  3: i32 splitEventId
}

/*
  A structure to represent a transformation in a pipeline

  stageNumber: Which stage this transformation occurs at in the pipeline
  te: The Transform to apply at this stage
*/
struct PipelineTransformStage {
  1: i32 stageNumber,
  2: TransformEvent te
}

/*
  A structure to represent a fit in a pipeline

  stageNumber: Which stage this transformation occurs at in the pipeline
  fe: The Fitting to do at this stage
*/
struct PipelineFitStage {
  1: i32 stageNumber,
  2: FitEvent fe
}

/*
  A pipeline stores several transformations and fits to do in series,
  that is, it will perform a tranformation, potentially perform a fit, and then
  pass the transformed data to the next Transformer. This event occurs on the creation
  of a pipeline.

  pipelineFit: The final fit to perform at the end of the pipeline
  transformStages: The transformations to apply in this pipeline
  fitStages: The fittings to apply in this pipeline
  experimentRunId: The id of the Experiment Run that contains this event
*/
struct PipelineEvent {
  1: FitEvent pipelineFit,
  2: list<PipelineTransformStage> transformStages,
  3: list<PipelineFitStage> fitStages,
  4: i32 experimentRunId
}

/*
  The response given to the creation of a PipelineEvent

  pipelineFitReponse: The response given to the pipelineFit FitEvent
  transformStagesResponses: The responses given to the each transformation stage
  fitStagesResponses: The responses given to each fit stage
*/
struct PipelineEventResponse {
  1: FitEventResponse pipelineFitResponse,
  2: list<TransformEventResponse> transformStagesResponses,
  3: list<FitEventResponse> fitStagesResponses
}

/*
  Represents an annotation on a DataFrame, TransformerSpec or Transformer

  type: The type of annotation
  df: (if applicable) the DataFrame this is associated with
  spec: (if applicable) the TransformerSpec this is associated with
  transformer: (if applicable) the DataFrame this is associated with
  message: (if applicable) The text associated with this annotation
*/
struct AnnotationFragment {
  1: string type, // Must be "dataframe", "spec", "transformer", or "message".
  2: DataFrame df, // Fill this in if type = "dataframe".
  3: TransformerSpec spec, // Fill this in if type = "spec".
  4: Transformer transformer, // Fill this in if type = "transformer".
  5: string message // Fill this in if type = "message".
}

/*
  The response given to the creation of an Annotation fragments
  type: The type of the Fragment created
  id: The id of the target of the Annotation Fragment (null if "Message")
*/
struct AnnotationFragmentResponse {
  1: string type,
  2: i32 id // The ID of the DataFrame, Transformer, or TransformerSpec. Null if type = "message".
}

/*
  Represents the set of annotations in an experimentRun

  fragments: The Annotation Fragments to add to the run
  experimentRunId: The Experiment Run this is associated with
*/
struct AnnotationEvent {
  1: list<AnnotationFragment> fragments,
  2: i32 experimentRunId
}

/*
  The response given to the creation of an AnnotationEvent

  annotationId: The id of the event
  fragmentResponses: The responses given to the creation of each fragment
*/
struct AnnotationEventResponse {
  1: i32 annotationId,
  2: list<AnnotationFragmentResponse> fragmentResponses
}

/*
  Represents the ancestry of a DataFrame, that is, the DataFrame it derived
  from, and the one its parent derived from, etc.
  
  ancestors: The list of ancestors of this frame, starting from its oldest
*/
struct DataFrameAncestry {
  1: list<DataFrame> ancestors
}

/*
  Represents a common ancestor DataFrame used between two models.
  
  ancestor: The common ancestor
  chainIndexModel1: The first model
  chainIndexModel2: The second model
*/
struct CommonAncestor {
  1: optional DataFrame ancestor,
  2: i32 chainIndexModel1,
  3: i32 chainIndexModel2
}

/*
  A pair of strings.

  first: One string
  second: Another string
*/
struct StringPair {
  1: string first,
  2: string second
}

/*
  The response to a comparison of HyperParameters

  model1OnlyHyperparams: The HyperParameters of model 1
  model2OnlyHyperparams: The HyperParameters of model 2
  sharedHyperparams: The HyperParameters shared between both models
*/
struct CompareHyperParametersResponse {
  1: map<string, string> model1OnlyHyperparams,
  2: map<string, string> model2OnlyHyperparams,
  3: map<string, StringPair> sharedHyperparams
}

/*
  The response to a comparison of features

  model1OnlyFeatures: The features of model 1
  model2OnlyFeatures: The features of model 2
  sharedFeatures: The features shared between both models
*/
struct CompareFeaturesResponse {
  1: list<string> model1OnlyFeatures,
  2: list<string> model2OnlyFeatures,
  3: list<string> commonFeatures
}

/*
  An enum for model comparison metric types
*/
enum ModelCompMetric {
  PROJECT,
  EXPERIMENT_RUN,
  MODEL_TYPE,
  PROBLEM_TYPE,
  RMSE,
  EXPLAINED_VARIANCE,
  R2
}

/*
  A comparison between the importance of a feature in each of two models

  featureName: The name of the feature
  percentileRankInModel1: (optional) The importance in model 1, if it appears in model 1
  percentileRankInModel2: (optional) The importance in model 2, if it appears in model 2
*/
struct FeatureImportanceComparison {
  1: string featureName,
  2: optional double percentileRankInModel1, // Defined if feature is in model 1.
  3: optional double percentileRankInModel2  // Defined if feature is in model 2.
}

/*
  The different types of metrics used to rank models
*/
enum ModelRankMetric {
  RMSE,
  EXPLAINED_VARIANCE,
  R2
}

/*
  Represents a confidence interval of a feature 

  featureIndex: The feature which has this interval of confidence
  low: The interval lower than the value
  high: the interval higher than the value
*/
struct ConfidenceInterval {
  1: i32 featureIndex,
  2: double low,
  3: double high
}

/*
  Stores all the Experiment Runs and Experiments for a project

  projId: The id of the project
  experiments: The list of Experiments in the project
  experimentRuns: The list of runs in the project
*/
struct ProjectExperimentsAndRuns {
  1: i32 projId,
  2: list<Experiment> experiments,
  3: list<ExperimentRun> experimentRuns
}

/*
  The response given when the user requests the overview of a project

  project: The project
  numExperiments: The number of Experiments contained in the project
  numExperimentRuns: The nuber of Experiment Runs 
    contained within the Experiments of the project
*/
struct ProjectOverviewResponse {
  1: Project project,
  2: i32 numExperiments,
  3: i32 numExperimentRuns
}

/*
  The response given when the user requests information about a model.

  id: The id of this response
  experimentRunId: The id of the Experiment Run in which this model is contained
  experimentId: The id of the Experiment that run is contained in
  projectId: The id of the Project under which this model is 
  trainingDataFrame: The DataFrame used to train the model
  specification: The TransformerSpec used to guide the training of the model
  problemType: The type of problem this model solves (e.g. regression)
  featureColumns: The important features of the data (columns in input DataFrame)
  labelColumns: The columns from the input Data Frame that this model is supposed to predict
  predictionColumns: The columns from the output of the model that are supposed
    to predict the label columns
  metrics: The calculated metrics of this model
  annotations: The annotations on this model
  sha: A hash of this model
  filepath: The path to the serialized model (transformer)
  linearModelData: If this is a Linear Model, information specific to Linear models
*/
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

/*
  The response when a user requests information about an Experiment Run

  project: The project this is contained in
  experiment: The experiment this is contained in
  experimentRun: The basic information about this run
  modelResponses: The detailed information for each model in this ExperimentRun
*/
struct ExperimentRunDetailsResponse {
  1: Project project,
  2: Experiment experiment,
  3: ExperimentRun experimentRun,
  4: list<ModelResponse> modelResponses,
}

/*
  A test of how accurate a model is

  model: The model to use
  trainingDf: A DataFrame that the model is allowed to use to train itself
  validationDf: A DataFrame which the model must try and predict
  score: The score of this test
*/
struct CrossValidationFold {
  1: Transformer model,
  2: DataFrame validationDf,
  3: DataFrame trainingDf,
  4: double score
}

/*
  The response to the creation of a CrossValidationFold

  modelId: The model being tested
  validationId: The id of the DataFrame being used for validationDf
  trainingId: The id of the DataFrame being used to train the model
*/
struct CrossValidationFoldResponse {
  1: i32 modelId,
  2: i32 validationId,
  3: i32 trainingId
}

/*
  Evaluates a model given 2 data frames

  df: The DataFrame to use to initiate the model
  spec: The TransformerSpec to guide the fitting
  seed: A seed to use in random number generation
  evaluator: --TODO--
  labelColumns: The columns of the DataFrame that the model is supposed to predict
  predictionColumns: The columns of the DataFrame that the model outputs as predictions
  featureColumns: The columns of the DataFrame that are features
  folds: The cross validation folds to use (see above)
  experimentRunId: The Experiment Run in which this event is contained
  problemType: The type of problem this model is solving (e.g. regression)
*/
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

/*
  The response given to the creation of a CrossValidationEvent.

  dfId: The id of the DataFrame used to initiate the model
  specId: The id of the TransformerSpec used to guide the building of the model
  eventId: The id of this event (unique across all events)
  foldReponses: The responses to each fold within this event
  crossValidationEventId: The id of this cross-validation event (unique across all CrossValidationEvents)
*/
struct CrossValidationEventResponse {
  1: i32 dfId,
  2: i32 specId,
  3: i32 eventId,
  4: list<CrossValidationFoldResponse> foldResponses,
  5: i32 crossValidationEventId
}

/*
  A Cross Validation evaluation 

  numFolds: The number of folds
  bestFit: The fit event that produces the best fit model
  crossValidations: Every cross validation conducted in this GridSearch model
  experimentRunId: The Experiment Run that contains this CrossValidationFold
  problemType: The type of problem this is trying to solves
*/
struct GridSearchCrossValidationEvent {
  1: i32 numFolds,
  2: FitEvent bestFit,
  3: list<CrossValidationEvent> crossValidations,
  4: i32 experimentRunId
  5: optional ProblemType problemType = ProblemType.UNDEFINED
}

/*
  The reponse given to the creation of a GridSearchCrossValidationEvent

  gscveId: The id of the created GridSearchCrossValidationEvent
  eventId: The id of the event (unique across all events)
  fitEventResponse: The response to the best fit FitEvent used to create the GSCVE
  crossValidationEventResponses: The responses to all CrossValidationEvents in this GSCVE
*/
struct GridSearchCrossValidationEventResponse {
  1: i32 gscveId,
  2: i32 eventId,
  3: FitEventResponse fitEventResponse,
  4: list<CrossValidationEventResponse> crossValidationEventResponses,
}

/*
  A node in a tree from a tree model --TODO--

  prediction:
  impurity:
  gain:
  splitIndex: 
*/
struct TreeNode {
  1: double prediction,
  2: double impurity,
  3: optional double gain,
  4: optional i32 splitIndex
}

// Thrift does not allow recursive structs in some languages, so we need
// to flatten the structure to indicate links between TreeNodes.
// This represents a link between a parent node (at some index) and a child
// node (at some index). The indices are given assuming that the list
// containing all the nodes is known.

/*
  A Link between a parent Node in a tree, and a child
  
  parentIndex: The index of the parent in this relation
  childIndex: The index of the child in this relation
  isLeft: Whether or not the child is the left child of the parent (True = Left, False = Right)
*/
struct TreeLink {
  1: i32 parentIndex,
  2: i32 childIndex,
  3: bool isLeft
}

/*
  A component of a tree after a RandomSplitEvent

  weight: The weight of this component
  nodes: All of the nodes contained in this tree
  links: The links that describe the relationship between the nodes
*/
struct TreeComponent {
  1: double weight,
  2: list<TreeNode> nodes,
  3: list<TreeLink> links
}

/*
  A tree model (decision tree, GBT, Random Forest)
  
  modelType: The type of model this represents
  components: The components of the tree (sections of nodes and links)
  featureImportances: How important each feature is relative to each other
*/
struct TreeModel {
  1: string modelType, // Should be "Decision Tree", "GBT", or "Random Forest".
  2: list<TreeComponent> components, // Should have one component for Decision Tree.
  3: list<double> featureImportances
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

  i32 storeDataFrame(1: DataFrame df, 2: i32 experimentRunId) 
    throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  string pathForTransformer(1: i32 transformerId) 
    throws (1: ResourceNotFoundException rnfEx, 2: InvalidFieldException efEx, 3: ServerLogicException svEx),

  FitEventResponse storeFitEvent(1: FitEvent fe)
   throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  MetricEventResponse storeMetricEvent(1: MetricEvent me)
    throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  TransformEventResponse storeTransformEvent(1: TransformEvent te)
    throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  RandomSplitEventResponse storeRandomSplitEvent(1: RandomSplitEvent rse)
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

  ExperimentRunDetailsResponse getExperimentRunDetails(1: i32 experimentRunId) throws (1: ServerLogicException svEx, 2: ResourceNotFoundException rnfEx),

  list<string> originalFeatures(1: i32 modelId) throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  // Associate TreeModel metadata with an already stored model
  // (i.e. Transformer) with the given id. Returns a boolean indicating
  // whether the metadata was correctly stored.
  bool storeTreeModel(1: i32 modelId, 2: TreeModel model) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx)
}
