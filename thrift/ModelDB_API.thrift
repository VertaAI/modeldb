namespace java modeldb
namespace py modeldb

/*
 Thrown when the server has thrown an exception that we did not think of.
 */
exception ServerLogicException {
  1: string message
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
  4: string description,
  5: optional string jsonMetadata,
  6: optional string dateCreated,
  7: optional string dateLastUpdated
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
  5: bool isDefault = 0,
  6: optional string jsonMetadata
}

// TODO: allow user to move model to an experiment

/*
  Experiment runs are contained within experiments. Note that the
  experiment must be specified, even if it is the default experiment.

  id: a unique identifier for this run.
  experimentId: The id of the experiment that contains this run.
  description: User assigned text to this experiment run. Can be used to summarize
    data, method, etc.
  sha: Commit hash of the code used in the current run.
  created: Timestamp for when the ExperimentRun was created.

  TODO: can we remove this or do it differently?
*/
struct ExperimentRun {
  1: i32 id = -1,
  2: i32 experimentId,
  3: string description,
  4: optional string sha,
  5: optional string created,
  6: optional string jsonMetadata
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

// TODO: consolidate with DataFrame ?
struct DataSource {
  1: i32 id,
  2: string filepath,
  3: optional string jsonMetadata
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

struct Model {
  1: i32 id,
  2: i32 experimentRunId,
  3: i32 experimentId,
  4: i32 projectId,
  5: list<DataSource> dataSources, //TODO: split test, train, validation data sources?
  // TODO: optional metrics? register model first and add metrics later
  6: map<string, map<i32, double>> metrics, //TODO: include datasource id in metrics or create struct for metrics or split metrics for test, train?
  7: optional string filepath,
  8: optional string sha,
  9: optional string timestamp,
  10: optional list<string> tags,
  11: optional list<string> comments,
  12: optional string jsonMetadata
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

service ModelDBAPI {

  i32 createProject(1: Project project) 
    throws (1: ServerLogicException svEx),

  i32 createExperiment(1: Experiment experiment)
    throws (1: ServerLogicException svEx),

  i32 createExperimentRun(1: ExperimentRun experimentRun)
    throws (1: ServerLogicException svEx),

  i32 createModel(1: Model model)
    throws (1: ServerLogicException svEx),

  // TODO: rename functions from get*Ids to something else
  list<i32> getExperimentIds(1: map<string, string> keyValuePairs)
    throws (1: ServerLogicException svEx),

  Experiment getExperiment(1: i32 experimentId) 
    throws (1: ServerLogicException svEx, 2: ResourceNotFoundException rnfEx),

  list<i32> getExperimentRunIds(1: map<string, string> keyValuePairs)
    throws (1: ServerLogicException svEx),

  ExperimentRun getExperimentRun(1: i32 experimentRunId) 
    throws (1: ServerLogicException svEx, 2: ResourceNotFoundException rnfEx),

  /*
   Get information about all the experiment runs in a given experiment.

   experimentId: The ID of an experiment.
   */
  list<ExperimentRun> getRunsInExperiment(1: i32 experimentId) throws (1: ServerLogicException svEx, 2: ResourceNotFoundException rnfEx),

  /*
   Get information about a project and the experiments/experiment runs that it contains.

   projId: The ID of a project.
   */
  ProjectExperimentsAndRuns getRunsAndExperimentsInProject(1: i32 projId) throws (1: ServerLogicException svEx, 2: ResourceNotFoundException rnfEx),

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
   Get information about the model with the given ID.

   modelId: The ID of a model.
   */
  ModelResponse getModel(1: i32 modelId) throws (1: ResourceNotFoundException rnfEx, 2: ServerLogicException svEx),

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

    TODO(mcslao): support adding key value pairs
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
}
