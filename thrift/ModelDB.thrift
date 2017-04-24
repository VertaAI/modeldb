/* 
  The thrift file specifies the structs and functions that are shared
  among modeldb projects. Thrift automatically generates classes to manipulate
  the data in each language.

  Currently, we support Java, Python, and Scala clients. You'll notice below that
  scala is not mentioned. The reason for this is that adding "namespace scala modeldb" causes
  an error with the Python client. Consequently, the Scala client inserts that line before it
  compiles.

  For the code below, it is worth pointing out that a "model" is defined as a Transformer that is produced by
  a FitEvent.
*/

namespace java modeldb
namespace py modeldb

/* 
  A project is the highest level in the grouping hierarchy. A project can 
  contain multiple experiments, which in turn can contain experiment runs.

  Attributes:
  id: A unique identifier for this project.
  name: The name of this project.
  author: The name of the author, the person who created the project.
  description: A human readable description of the project - i.e. it's goals and methods.

  Note that in order to flatten this into tables, the experiments contained within this
  project are not in the project struct.

  // TODO: Maybe we should have an email field instead of a name field. Also, can there be multiple authors?
*/
struct Project {
  1: i32 id = -1,
  2: string name,
  3: string author,
  4: string description
}

/* 
  Experiments are the second level in the grouping hierarchy. An experiment can contain
  multiple experiment runs.

  id: A unique experiment identifier.
  projectId: The ID of the project that contains this experiment.
  name: The name of this experiment.
  description: A short description of the experiment and what it contains.
  isDefault: Whether this is the default experiment of its project (a catch-all for runs). A project has
    exactly one default experiment.
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

  id: a unique identifier for this run.
  experimentId: The id of the experiment that contains this run.
  description: User assigned text to this experiment run. Can be used to summarize
    data, method, etc.
  sha: Commit hash of the code used in the current run.
  created: Timestamp for when the ExperimentRun was created.
*/
struct ExperimentRun {
  1: i32 id = -1,
  2: i32 experimentId,
  3: string description,
  4: optional string sha,
  5: optional string created
}

/* 
  A column in a DataFrame.

  name: The name of this column.
  type: The type of data stored in this column (e.g. Integer, String).
*/
struct DataFrameColumn {
  1: string name,
  2: string type
}

/*
  A MetadataKV is any key-value pair along with the type of the value. 
  It is used to associate arbitrary metadata with ModelDB entities

  key: key.
  value: value.
  valueType: The type of value.
*/
struct MetadataKV {
  1: string key,
  2: string value,
  3: string valueType
}

/* 
  A tabular set of data. Contains many columns, each containing a single type
  of data. Each row in a DataFrame is a distinct example in the dataset.

  id: A unique identifier for this DataFrame.
  schema: The columns in the DataFrame.
  numRows: The number of elements (rows) that this DataFrame contains.
  tag: Short, human-readable text to identify this DataFrame.
  filepath: The path to the file that contains the data in this DataFrame.
  metadata: Key-value pairs associated with this DataFrame.
*/
struct DataFrame {
  1: i32 id = -1, // when unknown
  2: list<DataFrameColumn> schema,
  3: i32 numRows,
  4: string tag = "",
  5: optional string filepath,
  6: optional list<MetadataKV> metadata
}

/* 
  A HyperParameter guides the fitting of a TransformerSpec to a DataFrame in order to produce a Transformer.
  Some example hyperparameters include the number of trees in a random forest, the regularization parameter in
  linear regression, or the value "k" in k-means clustering.

  name: The name of this hyperparameter.
  value: The value assigned to this hyperparameter.
  type: The type of data stored in the hyperparameter (e.g. Integer, String).
  min: (for numeric hyperparameters only) The minimum value allowed for this hyperparameter.
  max: (for numeric hyperparameters only) The maximum value allowed for this hyperparameter.
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

  project: The project to be created by this event.
*/
struct ProjectEvent {
  1: Project project
}

/* 
  The server's response to creating a project. 

  projectId: The id of the created project.
*/
struct ProjectEventResponse {
  1: i32 projectId
}

/* 
  An event representing the creation of an Experiment.

  experiment: The created Experiment.
*/
struct ExperimentEvent {
  1: Experiment experiment
}

/* 
  The response given to the creation of an Experiment.

  experimentId: The id of the experiment created.

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

  experimentRunId: The id of the created ExperimentRun.
*/
struct ExperimentRunEventResponse {
  1: i32 experimentRunId
}

/*
  A TransformerSpec is a machine learning primitive that describes 
  the hyperparameters used to create a model (A Transformer produced
  by fitting a TransformerSpec to a DataFrame).

  id: A unique identifier for this TransformerSpec.
  transformerType: The type of the Transformer that is created by using this TransformerSpec (e.g. linear regression).
  hyperparameters: The hyperparameters that guide this spec.
  tag: User assigned content associated with this Spec.
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

  id: A unique identifier for this transformer.
  transformerType: The type of transformer this is (e.g. linear regression).
  tag: User assigned content associated with this transformer.
  filepath: The path to the serialized version of this transformer.
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
  // A catch-all for problem types we haven't anticipated.
  UNDEFINED,
  // A problem in which a binary label (e.g. 0 or 1) is outputted for each example.
  BINARY_CLASSIFICATION,
  // A problem in which one of many possible labels is outputted for each example.
  MULTICLASS_CLASSIFICATION,
  // A problem in which real valued output is produced for each example.
  REGRESSION,
  // A problem in which examples a grouped together.
  CLUSTERING,
  // A problem in which a set of items are selected and (possibly) ordered for a given user.
  RECOMMENDATION
}

/* 
  A term in a linear model.

  coefficient: The coefficient of the feature.
  tStat: An optional T-Value to associate with this term.
  stdErr: An optional calculated Standard Error to associate with this term.
  pValue: An optional P-Value to associate with this term.
*/
struct LinearModelTerm {
  1: double coefficient,
  2: optional double tStat,
  3: optional double stdErr,
  4: optional double pValue
}

/* 
  Contains information about linear model (e.g. linear regression, logistic regression).

  interceptTerm: An optional term that represents the intercept of the linear model.
  featureTerms: The non-intercept terms. These are ordered based on their correspondence with
                the feature vectors. For example, the first term corresponds to the first entry of the
                feature vector.
  objectiveHistory: An optional history of every value the objective function has
                    obtained while being created, where objectiveHistory[i] is 
                    the value of the objective function on iteration i.
  rmse: An optional root-mean-square error of this model from the training set.
  explainedVariance: An optional explained variance of the training set.
      --Explained Variance measures the proportion to which a mathematical model 
      -- accounts for the variation (dispersion) of a given data set (Wikipedia)
  r2: The optional r^2 value that measures how well fitted the model is with respect to its training set.
*/
struct LinearModel {
  1: optional LinearModelTerm interceptTerm
  2: list<LinearModelTerm> featureTerms,
  3: optional list<double> objectiveHistory,
  4: optional double rmse,
  5: optional double explainedVariance,
  6: optional double r2
}

/*
  Represents the fitting of a DataFrame with a TransformerSpec to produce a Transformer.

  df: The DataFrame that is being fitted.
  spec: The TransformerSpec guiding the fitting.
  model: The model (fitted Transformer) produced by the fitting.
  featureColumns: The names of the features (i.e. columns of the DataFrame) used by the Transformer.
  predictionColumns: The names of the columns that the Transformer will put its predictions into.
  labelColumns: The columns of the DataFrame that contains the label (i.e. true prediction value) associated with
    each example.
  experimentRunId: The id of the ExperimentRun which contains this event.
  problemType: The type of problem that the produced Transformer solves.
*/
struct FitEvent {
  1: DataFrame df,
  2: TransformerSpec spec,
  3: Transformer model,
  4: list<string> featureColumns,
  5: list<string> predictionColumns,
  6: list<string> labelColumns,
  7: i32 experimentRunId,
  8: optional ProblemType problemType = ProblemType.UNDEFINED,
  9: optional string metadata
}

/*
  The response created after a FitEvent occurs.

  dfId: The id of the DataFrame referenced by the fit event.
  specId: The id of the TransformerSpec that guided the fit.
  modelId: The id of the outputted Transformer.
  eventId: The generic event id of this fit event (unique among all events).
  fitEventId: The specific FitEvent id of the created fit event (unique among fit events).
*/
struct FitEventResponse {
  1: i32 dfId,
  2: i32 specId,
  3: i32 modelId,
  4: i32 eventId,
  5: i32 fitEventId
}

/*
  This event indicates that a metric (e.g. accuracy) was evaluated on a DataFrame using a given Transformer.

  df: The DataFrame used for evaluation.
  model: The model (a Transformer) being evaluated.
  metricType: The kind of metric being evaluated (e.g. accuracy, squared error).
  labelCol: The column in the original DataFrame that this metric is calculated for.
  predictionCol: The column from the predicted columns that this metric is calculated for.
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
  The response given after creating a MetricEvent.

  modelId: The id of the Transformer for which this metric was calculated for.
  dfId: The id of the DataFrame used for this calculation.
  eventId: The generic event id of the created event (unique across all events).
  metricEventId: The id of the MetricEvent (unique across all MetricEvents).
*/
struct MetricEventResponse {
  1: i32 modelId,
  2: i32 dfId,
  3: i32 eventId,
  4: i32 metricEventId
}

/*
  This event indicates that an output DataFrame was created from an input DataFrame using a Transformer.

  oldDataFrame: The input DataFrame.
  newDataFrame: The output DataFrame.
  transformer: The transformer that produced the output DataFrame from the input DataFrame.
  inputColumns: The columns of the input DataFrame that the Transformer depends on.
  outputColumns: The columns that the Transformer outputs in the new DataFrame.
  experimentRunId: The id of the Experiment Run that contains this event.
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
  The response given to the creation of a TransformEvent.

  oldDataFrameId: The id of the input DataFrame of the transformation.
  newDataFrameId: The id of the output DataFrame of the transformation.
  transformerId: The id of the Transformer that performed the transformation.
  eventId: The generic event id of this transform event (unique among all events).
  filepath: The filepath to which the serialized Transformer should be written.
  // TODO: I think we can remove the filepath field. It may not be used.
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

  oldDataFrame: The DataFrame that is being split into pieces.
  weights: The weight vector to split the DataFrame by (weights of pieces).
  seed: A psuedo-random number generator seed.
  splitDataFrames: The output DataFrames (pieces).
  experimentRunId: The ID of the experiment run that contains this event.
*/
struct RandomSplitEvent {
  1: DataFrame oldDataFrame,
  2: list<double> weights,
  3: i64 seed,
  4: list<DataFrame> splitDataFrames,
  5: i32 experimentRunId
}

/* 
  The response to the creation of a Random Split event.

  oldDataFrameId: The id of the input DataFrame.
  splitIds: A list of ids of the new smaller DataFrames.
  splitEventId: The id of this split event (unique among split events).
*/
struct RandomSplitEventResponse {
  1: i32 oldDataFrameId,
  2: list<i32> splitIds,
  3: i32 splitEventId
}

/*
  This represents a transformation that occurs in the fitting of a pipeline.

  stageNumber: Which stage this transformation occurs at in the pipeline.
  te: The TransformEvent to apply at this stage.
*/
struct PipelineTransformStage {
  1: i32 stageNumber,
  2: TransformEvent te
}

/*
  This represents a fitting that occurs in the overall fitting of a pipeline.

  stageNumber: Which stage this transformation occurs at in the pipeline.
  fe: The FitEvent to apply at this stage.
*/
struct PipelineFitStage {
  1: i32 stageNumber,
  2: FitEvent fe
}

/*
  This represents the fitting of a PipelineModel.

  pipelineFit: This is the overall fit of the PipelineModel.
  transformStages: These are all the transformations that occurred during the fitting of the PipelineModel.
  fitStages: These are all the fittings that occurred during the fitting of the PipelineModel.
  experimentRunId: The id of the experiment run that contains this event.
*/
struct PipelineEvent {
  1: FitEvent pipelineFit,
  2: list<PipelineTransformStage> transformStages,
  3: list<PipelineFitStage> fitStages,
  4: i32 experimentRunId
}

/*
  The response given to the creation of a PipelineEvent

  pipelineFitReponse: The response to the fitting of the overall PipelineModel.
  transformStagesResponses: The responses to each of the transform stages.
  fitStagesResponses: The responses to each of the fit stages.
*/
struct PipelineEventResponse {
  1: FitEventResponse pipelineFitResponse,
  2: list<TransformEventResponse> transformStagesResponses,
  3: list<FitEventResponse> fitStagesResponses
}

/*
  Represents an annotation on a DataFrame, TransformerSpec or Transformer

  type: The type of annotation. It must be "dataframe", "spec", "transformer", or "message".
  df: (if applicable) the DataFrame this is associated with.
  spec: (if applicable) the TransformerSpec this is associated with
  transformer: (if applicable) the DataFrame this is associated with
  message: (if applicable) The text associated with this annotation
*/
struct AnnotationFragment {
  1: string type,
  2: DataFrame df,
  3: TransformerSpec spec,
  4: Transformer transformer,
  5: string message
}

/*
  The response given to the creation of an annotation fragment.

  type: The type of the fragment created.  It must be "dataframe", "spec", "transformer", or "message".
  id: The id of the target of the annotation fragment (null if "message").
*/
struct AnnotationFragmentResponse {
  1: string type,
  2: i32 id
}

/*
  Represents an annotation that describes a number of primitives in an experiment run.

  fragments: The annotation fragments.
  experimentRunId: The ID of the experiment run that should contain this annotation.
*/
struct AnnotationEvent {
  1: list<AnnotationFragment> fragments,
  2: i32 experimentRunId
}

/*
  The response given to the creation of an AnnotationEvent.

  annotationId: The generic event id of this annotation event (unique among all events).
  fragmentResponses: The responses given to the creation of each fragment.
*/
struct AnnotationEventResponse {
  1: i32 annotationId,
  2: list<AnnotationFragmentResponse> fragmentResponses
}

/*
  Represents the ancestry of a DataFrame, that is, the DataFrame it derived
  from, and the one its parent derived from, etc.
  
  ancestors: The list of ancestors of this frame, starting from the oldest DataFrame (i.e. the one DataFrame from which
    the successive DataFrames derive from).
*/
struct DataFrameAncestry {
  1: list<DataFrame> ancestors
}

/*
  Represents a common ancestor DataFrame used between two models.
  
  ancestor: The common ancestor.
  chainIndexModel1: The number of steps to get from the first model to the common ancestor (e.g. 1 step = parent,
    2 steps = grandparent).
  chainIndexModel2: The number of steps to get from the second model to the common ancestor.
*/
struct CommonAncestor {
  1: optional DataFrame ancestor,
  2: i32 chainIndexModel1,
  3: i32 chainIndexModel2
}

/*
  A pair of strings.
*/
struct StringPair {
  1: string first,
  2: string second
}

/*
  The response to a comparison of HyperParameters

  model1OnlyHyperparams: The hyperparameters found only in the first model. This maps from hyperparameter name to
    value.
  model2OnlyHyperparams: The hyperparameters found only in the second model. This maps from hyperparameter name to
    value.
  sharedHyperparams: The hyperparameters shared between both models. This maps from hyperparameter name from
    (value in model 1, value in model 2).
*/
struct CompareHyperParametersResponse {
  1: map<string, string> model1OnlyHyperparams,
  2: map<string, string> model2OnlyHyperparams,
  3: map<string, StringPair> sharedHyperparams
}

/*
  The response to a comparison of features

  model1OnlyFeatures: The names of the features that appear only in model 1.
  model2OnlyFeatures: The names of the features that appear only in model 2.
  sharedFeatures: The names of the features that appear in both models.
*/
struct CompareFeaturesResponse {
  1: list<string> model1OnlyFeatures,
  2: list<string> model2OnlyFeatures,
  3: list<string> commonFeatures
}

/*
  An enum for model comparison metric types

  PROJECT: Search for models that appear in the same project.
  EXPERIMENT_RUN: Search for models that appear in the same experiment run.
  MODEL_TYPE: Search for models with the same transformerType.
  PROBLEM_TYPE: Search for models with the same problem type.
  RMSE: Search for models with similar root-mean-squared-error values.
  EXPLAINED_VARIANCE: Search for models with similar explained variance values.
  R2: Search for models with similar R^2 values.
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

  featureName: The name of the feature.
  percentileRankInModel1: (optional) The importance (expressed as percentile rank) in model 1, if it appears in model 1.
  percentileRankInModel2: (optional) The importance (expressed as percentile rank) in model 2, if it appears in model 2.
*/
struct FeatureImportanceComparison {
  1: string featureName,
  2: optional double percentileRankInModel1,
  3: optional double percentileRankInModel2
}

/*
  The different types of metrics used to rank models.

  RMSE: Rank by root-mean-squared-error.
  EXPAINED_VARIANCE: Rank by explained variance.
  R2: Rank by R^2.
*/
enum ModelRankMetric {
  RMSE,
  EXPLAINED_VARIANCE,
  R2
}

/*
  Represents a confidence interval around a coefficient (or term) in a linear model.

  featureIndex: The index in the feature vector that that the coefficient corresponds to.
  low: The confidence interval lower bound.
  high: The confidence interval upper bound.
*/
struct ConfidenceInterval {
  1: i32 featureIndex,
  2: double low,
  3: double high
}

/*
  The response that indicates all the experiment runs and experiments for a project.

  projId: The id of the project.
  experiments: The list of experiments in the project.
  experimentRuns: The list of experiment runs in the project.
*/
struct ProjectExperimentsAndRuns {
  1: i32 projId,
  2: list<Experiment> experiments,
  3: list<ExperimentRun> experimentRuns
}

/*
  The response given when the user requests the overview of a project.

  project: The project.
  numExperiments: The number of experiments contained in the project.
  numExperimentRuns: The number of experiment runs contained in the experiments contained in the project.
*/
struct ProjectOverviewResponse {
  1: Project project,
  2: i32 numExperiments,
  3: i32 numExperimentRuns
}

/*
  The response given when the user requests information about a model.

  id: The id of the model (i.e. its primary key in the Transformer table).
  experimentRunId: The id of the experiment run in which this model is contained.
  experimentId: The id of the experiment that run is contained in.
  projectId: The id of the project under which this model is.
  trainingDataFrame: The DataFrame used to train the model.
  specification: The TransformerSpec used to guide the training of the model.
  problemType: The type of problem this model solves (e.g. regression).
  featureColumns: The important features of the data (columns in input DataFrame).
  labelColumns: The columns from the input DataFrame that were used to guide fitting (e.g. true class, true target).
  predictionColumns: The columns of the output DataFrame that will contain this model's predictions.
  metrics: The calculated metrics of this model. The map goes from metric name to DataFrame ID to metric value. For
    example, if metrics["accuracy"][12] has the value 0.95, then that means that the model achieves an accuracy
    of 95% on the DataFrame with ID 12.
  annotations: The annotations that mention this model.
  sha: A hash of this model.
  filepath: The path to the serialized model (transformer).
  timestamp: Timestamp of the experimentrun that created this model.
  linearModelData: If this is a Linear Model, information specific to Linear models.
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
  11: map<string, map<i32, double>> metrics,
  12: list<string> annotations,
  13: string sha,
  14: string filepath,
  15: string timestamp,
  16: optional LinearModel linearModelData,
  17: optional string metadata
}

/*
  The response when a user requests information about an experiment run.

  project: The project this is contained in.
  experiment: The experiment this is contained in.
  experimentRun: The basic information about this run.
  modelResponses: The detailed information for each model in this ExperimentRun.
*/
struct ExperimentRunDetailsResponse {
  1: Project project,
  2: Experiment experiment,
  3: ExperimentRun experimentRun,
  4: list<ModelResponse> modelResponses,
}

/*
  Represents one fold of cross validation.

  model: The model that was trained from the trainingDf in this cross validation fold.
  trainingDf: The DataFrame that trained the model.
  validationDf: The DataFrame used to compute the score.
  score: The score (e.g. accuracy, RMSE, F1 score) computed for this model.
*/
struct CrossValidationFold {
  1: Transformer model,
  2: DataFrame validationDf,
  3: DataFrame trainingDf,
  4: double score
}

/*
  The response to the creation of a CrossValidationFold.

  modelId: The ID of the model trained for the fold.
  validationId: The ID of the DataFrame being used for evaluating the score.
  trainingId: The ID of the DataFrame that trained the model.
*/
struct CrossValidationFoldResponse {
  1: i32 modelId,
  2: i32 validationId,
  3: i32 trainingId
}

/*
  Represents an event where a TransformerSpec (i.e. configuration of hyperparameters) is evaluated by doing cross
  validation on an input DataFrame. The DataFrame is split into pieces. Then, for each of pieces (or folds), a model
  is trained using all the other folds and finally evaluated on the given fold to compute a score.

  df: The overall DataFrame that is broken into pieces (or folds).
  spec: The TransformerSpec to guide the fitting
  seed: A seed for the random number generation that breaks the DataFrame into pieces.
  evaluator: A string representation of the score computation (e.g. accuracy).
  labelColumns: The columns of the DataFrame that contain the true labels/targets to use to guide training.
  predictionColumns: The columns that will contains the predictions of the produced model.
  featureColumns: The columns of the input DataFrame that are used as features in the model.
  folds: The cross validation folds to use (see above).
  experimentRunId: The ID of the experiment run that contains this event.
  problemType: The type of problem that the model solves (e.g. regression, classification).
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
  Represents a search over hyperparameter configurations (where each hyperparameter configuration is evaluated with
  a cross validation event). Note that while the name mentions grid search, this event can be used to capture other
  searches over different hyperparameter configurations (e.g. random search).

  numFolds: The number of folds to use for each cross validation event.
  bestFit: After evaluating each hyperparameter configuration via cross validation, the best one is chosen and used
    to train a model on the overall dataset. This is reflected by the given FitEvent.
  crossValidations: There is one cross validation event for each hyperparameter configuration being evaluated.
  experimentRunId: The ID of the experiment run that contains this event.
  problemType: The type of problem (e.g. regression) that the trained models solve.
*/
struct GridSearchCrossValidationEvent {
  1: i32 numFolds,
  2: FitEvent bestFit,
  3: list<CrossValidationEvent> crossValidations,
  4: i32 experimentRunId
  5: optional ProblemType problemType = ProblemType.UNDEFINED
}

/*
  The reponse given to the creation of a GridSearchCrossValidationEvent.

  gscveId: The id of the created GridSearchCrossValidationEvent.
  eventId: The id of the event (unique across all events).
  fitEventResponse: The response to the best fit FitEvent used to create the GSCVE.
  crossValidationEventResponses: The responses to all CrossValidationEvents in this GSCVE.
*/
struct GridSearchCrossValidationEventResponse {
  1: i32 gscveId,
  2: i32 eventId,
  3: FitEventResponse fitEventResponse,
  4: list<CrossValidationEventResponse> crossValidationEventResponses,
}

/*
  Represents a node in a decision tree.

  //TODO: There should be a field for internal nodes that says what value the feature is split on.
  prediction: The prediction made by node (regression target or class label).
  impurity: Represents the impurity measure (e.g. entropy, Gini coefficient) at the given node.
  gain: The information gain acheived by the split at this node.
  splitIndex: The index of the feature (in the feature vector) that is being split at the given node.
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
  A link between a parent and child TreeNode in a decision tree.
  
  parentIndex: The index of the parent in this relation.
  childIndex: The index of the child in this relation.
  isLeft: Whether or not the child is the left child of the parent (true = Left, false = Right).
*/
struct TreeLink {
  1: i32 parentIndex,
  2: i32 childIndex,
  3: bool isLeft
}

/*
  Represents a component of a tree model. Each component is a decision tree with a weight indicating how important
  it is in the overall tree model. A regular decision tree model can be viewed as a single TreeComponent that has
  all the weight.

  weight: The weight of this component. The relative values of all the weights for a given TreeComponent indicate
    their relative importances in the overall TreeModel.
  nodes: All of the nodes contained in this tree.
  links: The links that describe the relationship between the nodes.
*/
struct TreeComponent {
  1: double weight,
  2: list<TreeNode> nodes,
  3: list<TreeLink> links
}

/*
  Represents a tree model.
  
  modelType: The type of model this represents (e.g. decision tree, gradient boosted trees, random forest).
  components: The components of the model. There should be one for each tree in the ensemble. You can view a decision
    tree model as having exactly one component.
  featureImportances: How important each feature is relative to each other.
*/
struct TreeModel {
  1: string modelType, // Should be "Decision Tree", "GBT", or "Random Forest".
  2: list<TreeComponent> components, // Should have one component for Decision Tree.
  3: list<double> featureImportances
}

/*
 Represents the ancestry that produced a given model.

 Recall that a model is a Transformer produced by fitting a DataFrame with a
 TransformerSpec. This FitEvent is thus included in the ancestry.

 The DataFrame used in the fitting may have been produced by transforming
 other DataFrames. So, we include all the TransformEvents that were involved in
 producing the DataFrame that was fit to produce the given model. 
 They are ordered such that the oldest TransformEvent is first and the 
 TransformEvent that produced the DataFrame used for fitting is last.

 All of the DataFrames stored in the FitEvent and the TransformEvents have
 empty schema fields. The FitEvent has an empty hyperparameters and empty
 featureColumns field. This is for performance purposes.
*/
struct ModelAncestryResponse {
  1: i32 modelId,
  2: FitEvent fitEvent,
  3: list<TransformEvent> transformEvents
}

/*
 Represents an extracted pipeline (i.e. sequence of Transformers and TransformerSpecs) that produced the DataFrame
 used to train a particular model.

 transformers: Represents the Transformers that transformed DataFrames to yield the final DataFrame.
 specs: Represents the TransformerSpecs that were used to train the models involved in transforming DataFrames.
 */
struct ExtractedPipelineResponse {
  1: list<Transformer> transformers;
  2: list<TransformerSpec> specs;
}

/*
 Thrown when a specified resource (e.g. DataFrame, Transformer) is not found.
 For example, if you try to read Transformer with ID 1, then we throw this
 exception if that Transformer does not exist.
 */
exception ResourceNotFoundException {
  1: string message
}

/*
 Thrown when field of a structure is empty or incorrect.
 For example, if you try to get the path for a Transformer, but the server
 finds that the path is empty, then this exception gets thrown.
 */
exception InvalidFieldException {
  1: string message
}

/*
 Thrown when the request is not properly constructed.
 For example, if you say that you want to find -3 similar models, then this
 exception gets thrown.
 */
exception BadRequestException {
  1: string message
}

/*
 Thrown when the server is told to perform an operation that it cannot do.
 For example, if you try to compute feature importances for a linear model
 that isn't standardized, then it is not possible to compute feature
 importance, so this exception gets thrown.
 */
exception IllegalOperationException {
  1: string message
}

/*
 Thrown when the server has thrown an exception that we did not think of.
 */
exception ServerLogicException {
  1: string message
}

/*
 Thrown when an Experiment Run ID is not defined.
 */
exception InvalidExperimentRunException {
  1: string message
}

service ModelDBService {
  /*
   Tests connection to the server. This just returns 200.
   */
  i32 testConnection(), 

  /*
   Stores a DataFrame in the database.

   df: The DataFrame.
   experimentRunId: The ID of the experiment run that contains this given DataFrame.
   */
  i32 storeDataFrame(1: DataFrame df, 2: i32 experimentRunId) 
    throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  /*
   Get the path to the file that contains a serialized version of the Transformer with the given ID.
   // TODO: This seems unnecessary because there's another method called getFilePath. Perhaps we should get rid of it.

   transformerId: The ID of a Transformer.
   */
  string pathForTransformer(1: i32 transformerId) 
    throws (1: ResourceNotFoundException rnfEx, 2: InvalidFieldException efEx, 3: ServerLogicException svEx),

  /*
   Stores a FitEvent in the database. This indicates that a TransformerSpec has been fit to a DataFrame to produce
   a Transformer.

   fe: The FitEvent.
   */
  FitEventResponse storeFitEvent(1: FitEvent fe)
   throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  /*
   Stores a MetricEvent in the database. This indicates that a Transformer was used to compute an evaluation metric
   on a DataFrame.
   */
  MetricEventResponse storeMetricEvent(1: MetricEvent me)
    throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  /*
   Gets the filepath associated with the given Transformer.

   If the Transformer exists (t.id > 0 and there's a Transformer with the
   given ID), then we will generate a filepath for it (unless a filepath
   already exists) and return the filepath. In this case, we only access the
   t.id field, so you can leave the other fields and the experimentRunId empty.

   If the Transformer does not exist (t.id > 0 and there's no Transformer
   with the given ID), then we will throw a ResourceNotFoundException. In
   this case, we only access the t.id field, so you can leave the other
   fields and the experimentRunId empty.

   If the Transformer has t.id < 0, then a new Transformer will be created,
   given a filepath, and that filepath will be returned.

   You can specify a filename as well. This will be ignored if the Transformer
   already has a filename. Otherwise, your Transformer will be saved at this
   filename. If there's already a Transformer at the given filename, some
   random characters will be added to your filename to prevent conflict. Set
   filename to the empty string if you want a randomly generated filename.
   */
  string getFilePath(1: Transformer t, 2: i32 experimentRunId, 3: string filename)
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Stores a TransformEvent in the database. This indicates that a Transformer was used to create an output DataFrame
   from an input DataFrame.

   te: The TransformEvent.
   */
  TransformEventResponse storeTransformEvent(1: TransformEvent te)
    throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  /*
   Stores a RandomSplitEvent in the database. This indicates that a DataFrame was randomly split into many pieces.

   rse: The RandomSplitEvent.
   */
  RandomSplitEventResponse storeRandomSplitEvent(1: RandomSplitEvent rse)
    throws (1: ServerLogicException svEx),

  /*
   Stores a PipelineEvent in the database. This indicates that a pipeline model was created by passing through
   a linear chain of Transformers and TransformerSpecs, where Transformers transform their input and feed it to the next
   step of the pipeline and where TransformerSpecs are trained on their input DataFrame and their resulting output
   Transformer transforms the input DataFrame and passes it through.

   pipelineEvent: The PipelineEvent.
   */
  PipelineEventResponse storePipelineEvent(1: PipelineEvent pipelineEvent)
    throws (1: ServerLogicException svEx),

  /*
   Stores a CrossValidationEvent in the database. This indicates that a hyperparameter configuration was evaluated
   on a given input DataFrame using cross validation.

   cve: The CrossValidationEvent.
   */
  CrossValidationEventResponse storeCrossValidationEvent(1: CrossValidationEvent cve)
    throws (1: ServerLogicException svEx),

  /*
   Stores a GridSearchCrossValidationEvent in the database. This indicates that a number of hyperparameter
   configurations were evaluated on a given input DataFrame using cross validation and the best one was used to
   train a Transformer on the input DataFrame.

    gscve: The GridSearchCrossValidationEvent.
   */
  GridSearchCrossValidationEventResponse storeGridSearchCrossValidationEvent(1: GridSearchCrossValidationEvent gscve)
    throws (1: ServerLogicException svEx),

  /*
   Stores an AnnotationEvent in the database. This indicates that some primitives (i.e. DataFrame, Transformer, or
   TransformerSpec) have been marked (perhaps with text messages) with an annotation.

   ae: The AnnotationEvent.
   */
  AnnotationEventResponse storeAnnotationEvent(1: AnnotationEvent ae)
    throws (1: ServerLogicException svEx),

  /*
   Stores a ProjectEvent in the database. This indicates that a new project was created and stored in the database.

   pr: The ProjectEvent.
   */
  ProjectEventResponse storeProjectEvent(1: ProjectEvent pr)
    throws (1: ServerLogicException svEx),

  /*
   Stores an ExperimentEvent in the database. This indicates that a new experiment was created and stored under a
   given project.

   er: The ExperimentEvent.
   */
  ExperimentEventResponse storeExperimentEvent(1: ExperimentEvent er)
    throws (1: ServerLogicException svEx),

  /*
   Stores an ExperimentRunEvent in the database. This indicates that a new experiment run was created and stored under
   a given experiment.

   er: The ExperimentRunEvent.
   */
  ExperimentRunEventResponse storeExperimentRunEvent(1: ExperimentRunEvent er)
    throws (1: ServerLogicException svEx),

  /*
   Associate LinearModel metadata with a Transformer with the given ID. Returns a boolean indicating whether the
   metadata was correctly stored.

   modelId: The ID of a Transformer.
   model: The LinearModel metadata to associate with the Transformer with ID modelId.
   */
  bool storeLinearModel(1: i32 modelId, 2: LinearModel model) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Retrieves the ancestry of a given DataFrame. This is the ordered sequence of DataFrames such that the (i+1)^st
   DataFrame is derived, via TransformEvent, from the i^th DataFrame.

   dataFrameId: The ID of the DataFrame whose ancestry we seek.
   */
  DataFrameAncestry getDataFrameAncestry(1: i32 dataFrameId) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Retrieves the common ancestor DataFrame from which the two given DataFrames are derived. That is, we compute the
   ancestry of the DataFrames with IDs dfId1 and dfId2. Then, we find the first (in terms of order in the ancestries)
   DataFrame that appears in both the ancestries and return that.

   dfId1: The ID of a DataFrame.
   dfId2: The ID of another DataFrame.
   */
  CommonAncestor getCommonAncestor(1: i32 dfId1, 2: i32 dfId2) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Get the common ancestor DataFrame for two models. This basically find the DataFrames that were used (in FitEvents)
   to create the Transformers with IDs modelId1 and modelId2 and then calls getCommonAncestor on those two DataFrames.

   modelId1: The ID of a model.
   modelId2: The ID of another model.
   */
  CommonAncestor getCommonAncestorForModels(1: i32 modelId1, 2: i32 modelId2) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Get the number of rows in the DataFrame used to produce the Transformer with the given ID.

   modelId: The ID of a model.
   */
  i32 getTrainingRowsCount(1: i32 modelId) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Returns the number of training rows in each model. If a model cannot be found, we mark that it has -1 training rows.

   modelIds: The IDs of models.
   */
  list<i32> getTrainingRowsCounts(1: list<i32> modelIds) 
    throws (1: ServerLogicException svEx),

  /*
   Compares the hyperparameters of the TransformerSpecs that trained the two given models.

   modelId1: The ID of a model.
   modelId2: The ID of another model.
   */
  CompareHyperParametersResponse compareHyperparameters(1: i32 modelId1, 2: i32 modelId2) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Compares the features used by the two given models.

   modelId1: The ID of a model.
   modelId2: The ID of another model.
   */
  CompareFeaturesResponse compareFeatures(1: i32 modelId1, 2: i32 modelId2) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Given a list of model IDs, group them by problem type. Returns a map that goes from problem type to the list of IDs
   of the models that have the given problem type. If any of the given model IDs do not appear in the database, then
   they will be left out of the returned map.

   modelIds: The IDs of models.
   */
  map<ProblemType, list<i32>> groupByProblemType(1: list<i32> modelIds)
    throws (1: ServerLogicException svEx),

  /*
   Gets the list of IDs of models (with the most similar first) that are similar to the given model according to the
   given comparison metrics.

   modelId: The ID of a model.
   compMetrics: The comparison metrics for similarity. The first comparison metric will be applied to select and rank
    similar models. The successive metrics will be applied after that to break ties.
   numModels: The maximum number of similar models to find.
   */
  list<i32> similarModels(1: i32 modelId, 2: list<ModelCompMetric> compMetrics, 3: i32 numModels) 
    throws (1: ResourceNotFoundException rnfEx, 2: BadRequestException brEx, 3: ServerLogicException svEx),

  /*
   Get the names of the features, ordered by importance (most important first), for the model with the given ID.
   The model must be a linear model and its features must be standardized (i.e. it must have been trained with
   a hyperparameter called "standardization" set to "true").

   modelId: The ID of a model.
   */
  list<string> linearModelFeatureImportances(1: i32 modelId) 
    throws (1: ResourceNotFoundException rnfEx, 2: IllegalOperationException ioEx, 3: ServerLogicException svEx),

  /*
   Compare the feature importances of two given models. Returns a list of comparisons (one for each feature).

   modelId1: The ID of a model.
   modelId2: The ID of another model.
   */
  list<FeatureImportanceComparison> compareLinearModelFeatureImportances(1: i32 model1Id, 2: i32 model2Id) 
    throws (1: ResourceNotFoundException rnfEx, 2: IllegalOperationException ioEx, 3: ServerLogicException svEx),

  /*
   Count the number of iterations that each model took to converge to its parameter values during training. The value -1
   is used if the number of iterations until convergence is unknown.

   modelIds: The IDs of the models.
   tolerance: The tolerance level used to measure convergence. If the objective function takes on value v1 in iteration
   i and takes on value v2 in iteration i+1, then we say that the model has converged if abs(v1 - v2) <= tolerance.
   This API method returns the smallest i for which each model has converged.
   */
  list<i32> iterationsUntilConvergence(1: list<i32> modelIds, 2: double tolerance)
    throws (1: ServerLogicException svEx),

  /*
   Rank the given models according to some metric. The returned list will contain
   the models ordered by highest metric to lowest metric. If we cannot
   find the corresponding metric value for a given model, its id will be
   omitted from the returned list.

   modelIds: The IDs of models.
   metric: The metric used to rank the models.
   */
  list<i32> rankModels(1: list<i32> modelIds, 2: ModelRankMetric metric)
    throws (1: ServerLogicException svEx),

  /*
   Compute the t-statistic based confidence interval, at the given significance level, for the model with the given ID.

   modelId: The ID of the model for which we would like to compute confidence intervals.
   sigLevel: The significance level for which we would like to compute confidence intervals.
   */
  list<ConfidenceInterval> confidenceIntervals(1: i32 modelId, 2: double sigLevel) 
    throws (1: ResourceNotFoundException rnfEx, 2: IllegalOperationException ioEx, 3: BadRequestException brEx, 4: ServerLogicException svEx),

  /*
   Find the IDs of the models that use all of the given features.

   featureNames: The names of features.
   */
  list<i32> modelsWithFeatures(1: list<string> featureNames)
    throws (1: ServerLogicException svEx),

  // Get the IDs of the models that are derived from the DataFrame with the 
  // given ID, or one of its descendent DataFrames. This will only consider
  // models and DataFrames in the same project as the given dfId.
  /*
   Get the IDs of all models derived from the DataFrame with the given ID.

   dfId: The ID of a DataFrame.
   */
  list<i32> modelsDerivedFromDataFrame(1: i32 dfId) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Get the IDs of all the projects that match the specified key-value pairs.

   keyValuePairs: The map containing key-value pairs to match,
    where key is not case-sensitive and value is case-sensitive
   */
  list<i32> getProjectIds(1: map<string, string> keyValuePairs)
    throws (1: ServerLogicException svEx),

  /*
   Get the IDs of all the models that match the specified key-value pairs.

   keyValuePairs: The map containing key-value pairs to match
   */
  list<i32> getModelIds(1: map<string, string> keyValuePairs)
    throws (1: ServerLogicException svEx),

  /*
    Update the given field of the project of the given ID with the given value.
    The field must be an existing field of the project.

    projectId: The ID of the project
    key: The field to update (not case-sensitive)
    value: The value for the field (case-sensitive)
   */
  bool updateProject(1: i32 projectId, 2: string key, 3: string value)
    throws (1: ServerLogicException svEx),

  /*
    Update the given field of the model of the given ID with the given value.
    If key exists, update it with value. If not, add the key-value pair to the model.
    Return a boolean indicating if the key was updated or not.

    modelId: The ID of the model
    key: The field to update, which follows MongoDB's dot notation
    value: The value for the field, where datetime values follow the format given at
      http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateTimeParser()
    valueType: The type of the value (string, int, double, long, datetime, or bool)
   */
  bool createOrUpdateScalarField(1: i32 modelId, 2: string key, 3: string value, 4: string valueType)
    throws (1: ServerLogicException svEx),

  /*
   Create a vector field with the given name inside the model with the given ID.
   The vector field is configured with the given vector config.
   Do nothing if the vector field with the given name already exists.
   Return a boolean indicating if the vector was created or not.

   modelId: The ID of the model
   vectorName: The name of the vector field, which follows MongoDB's dot notation
   vectorConfig: The map containing config information for the vector field
   */
  bool createVectorField(1: i32 modelId, 2: string vectorName, 3: map<string, string> vectorConfig)
    throws (1: ServerLogicException svEx),

  /*
    Update the given vector field of the model of the given ID with the given value at the specified index.
    The specified field must exist and must be a vector.
    Return a boolean indicating if the value was updated or not.

    modelId: The ID of the model
    key: The field to update, which follows MongoDB's dot notation
    valueIndex: The index of the value to update (0-indexed)
    value: The value for the field, where datetime values follow the format given at
      http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateTimeParser()
    valueType: The type of the value (string, int, double, long, datetime, or bool)
   */
  bool updateVectorField(1: i32 modelId, 2: string key, 3: i32 valueIndex, 4: string value, 5: string valueType)
    throws (1: ServerLogicException svEx),

  /*
   Add a new value to the vector field with the given name in the model with the given ID.
   Return a boolean indicating if the value was added or not.

   modelId: The ID of the model
   vectorName: The name of the vector field to update, which follows MongoDB's dot notation
   value: The value to be added, where datetime values follow the format given at
      http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateTimeParser()
    valueType: The type of the value (string, int, double, long, datetime, or bool)
   */
  bool appendToVectorField(1: i32 modelId, 2: string vectorName, 3: string value, 4: string valueType)
    throws (1: ServerLogicException svEx),

  /*
   Get information about the model with the given ID.

   modelId: The ID of a model.
   */
  ModelResponse getModel(1: i32 modelId) throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Get information about all the experiment runs in a given experiment.

   experimentId: The ID of an experiment.
   */
  list<ExperimentRun> getRunsInExperiment(1: i32 experimentId) throws (1: ServerLogicException svEx),

  /*
   Get information about a project and the experiments/experiment runs that it contains.

   projId: The ID of a project.
   */
  ProjectExperimentsAndRuns getRunsAndExperimentsInProject(1: i32 projId) throws (1: ServerLogicException svEx),

  /*
   Get information about of all the projects in the database.
   */
  list<ProjectOverviewResponse> getProjectOverviews() throws (1: ServerLogicException svEx),

  /*
   Get information about a given experiment run.

   experimentRunId: The ID of an experiment run.
   */
  ExperimentRunDetailsResponse getExperimentRunDetails(1: i32 experimentRunId) throws (1: ServerLogicException svEx, 2: ResourceNotFoundException rnfEx),

  /*
   Get the list of the original features used by the model with the given ID.
   The list of original feature names. For example, suppose we begin with a DataFrame that has a column "age"
   and do a TransformEvent to produce a DataFrame with column "ageInDays". Then, suppose we train a model on the
   "ageInDays" column. Then, the original feature-set of the model is simply "age".

   modelId: The ID of a model.
   */
  list<string> originalFeatures(1: i32 modelId) throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Associate TreeModel metadata with an already stored model
   (i.e. Transformer) with the given id. Returns a boolean indicating
   whether the metadata was correctly stored.

   modelId: The ID of a model.
   mode: The TreeModel information to associate with the model with the given ID.
   */
  bool storeTreeModel(1: i32 modelId, 2: TreeModel model) 
    throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
    An N stage pipeline will store N TransformEvents when it transforms a 
    DataFrame. So, the client can make N calls to storeTransformEvent. However,
    N can be very large. For example, consider a pre-processing pipeline that
    does string indexing and one-hot encoding for 20 features. This would result
    in N = 2*20 = 40, which means the client has to make 40 calls to 
    storeTransformEvent. This means the client has to wait for 40 round trip
    times at least. Notice that this operation cannot be parallelized either,
    because the first stage's TransformEvent must be stored in order to store
    the second stage's TransformEvent (because the ID of the DataFrame output
    by the first stage must equal the ID of the DataFrame input to the second
    stage).

    To mitigate the performance issue described above, 
    storePipelineTransformEvent allows the client to store all N stages of 
    transformation at once.

    te: The transform events that are involved in this overall pipeline transform event.
  */
  list<TransformEventResponse> storePipelineTransformEvent(1: list<TransformEvent> te)
    throws (1: InvalidExperimentRunException ierEx, 2: ServerLogicException svEx),

  /*
   Compute the ancestry (i.e. the FitEvent that created the model and the DataFrame ancestry of the DataFrame that the
   model was trained on) for the model with the given ID.

   modelId: The ID of a model.
   */
  ModelAncestryResponse computeModelAncestry(1: i32 modelId) throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

  /*
   Extract a pipeline (i.e. seqeuence of Transformers and TransformerSpecs that were used to generate the DataFrame
   used to train a given model) for a given model.

   modelId: The ID of a model.
   */
  ExtractedPipelineResponse extractPipeline(1: i32 modelId) throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx)
}

