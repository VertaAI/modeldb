-- Top level grouping mechanism for machine learning events.
-- ModelDB stores multiple projects, each of which contains multiple
-- experiments, each of which contains multiple experiment runs.
-- Each event and primitive (DataFrame, Transformer, TransformerSpec) is
-- associated with an ExperimentRun.
DROP TABLE IF EXISTS Project;
CREATE TABLE Project (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- A descriptive name for the project.
  name TEXT,
  -- The name of the project author.
  author TEXT,
  -- A description of the project and its goals.
  description TEXT,
  -- The timestamp at which the project was created.
  created TIMESTAMP NOT NULL
);

-- The second level in the hierarchy of grouping. Many experiments
-- Can be contained in a single project. Each experiment has multiple runs.
DROP TABLE IF EXISTS Experiment;
CREATE TABLE Experiment (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The project that contains this experiment
  project INTEGER REFERENCES Project NOT NULL,
  -- The name of this particular experiment
  name TEXT NOT NULL,
  -- A description of the experiment and the purpose of the experiment
  description TEXT,
  -- A timestamp at which the experiment was created.
  created TIMESTAMP NOT NULL
);

-- Each experiment contains many experiment runs. In experiment runs,
-- you will find the actual machine learning events
DROP TABLE IF EXISTS ExperimentRun;
CREATE TABLE ExperimentRun (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The experiment which contains this run.
  experiment INTEGER REFERENCES Experiment NOT NULL,
  -- A description of this particular run, with the goals and parameters it used.
  description TEXT,
  -- A timestamp indicating the time at which this experiment run was created.
  sha TEXT,
  -- Commit hash of the code for this run
  created TIMESTAMP NOT NULL
);

-- Metadata information
DROP TABLE IF EXISTS MetadataKV;
CREATE TABLE MetadataKV (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- key name for this piece of metadata
  key TEXT NOT NULL,
  -- value of this metadata piece
  value TEXT NOT NULL,
  -- The type of the value
  valueType TEXT NOT NULL
);

-- A DataFrame is a machine learning primitive. It is a table
-- of data with named and typed columns.
DROP TABLE IF EXISTS DataFrame;
CREATE TABLE DataFrame (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- User assigned content associated with the data frame
  tag TEXT,
  --  The number of rows (elements) stored within this DataFrame.
  numRows INTEGER NOT NULL,
  -- The ExperimentRun that contains this DataFrame
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL,
  -- A path to the file where this DataFrame is stored
  filepath TEXT
);

-- A single column in a DataFrame
-- Each column has a unique name and can only contain a single type.
DROP TABLE IF EXISTS DataFrameColumn;
CREATE TABLE DataFrameColumn (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The ID of the DataFrame that has this column
  dfId INTEGER REFERENCES DataFrame NOT NULL,
  -- The name of the column
  name TEXT NOT NULL,
  -- The type of data that is stored in this column: e.g: Integer, String
  type TEXT NOT NULL
  -- TODO: Should we store the index of each column in a DataFrame?
);
CREATE INDEX IF NOT EXISTS DataFrameColumnIndexDfId ON DataFrameColumn(dfId);

-- Table associating metadata with dataframes
DROP TABLE IF EXISTS DataFrameMetadata;
CREATE TABLE DataFrameMetadata (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- id of the dataframe
  dfId INTEGER REFERENCES DataFrame NOT NULL,
  -- id of the metadatakv
  metadataKvId INTEGER REFERENCES MetadataKV NOT NULL
);

-- A Random Split event represents breaking a DataFrame into
-- smaller DataFrames randomly according to a weight vector that
-- specifies the relative sizes of the smaller DataFrames.
DROP TABLE IF EXISTS RandomSplitEvent;
CREATE TABLE RandomSplitEvent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The DataFrame to split
  inputDataFrameId INTEGER REFERENCES DataFrame NOT NULL,
  -- A seed to use to randomize the splitting process
  randomSeed BIGINT NOT NULL,
  -- The experiment run that contains RandomSplitEvent
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- TODO: This is not a great name, try something closer in meaning to "portion" or "part"
-- Something like DataPiece? Or just SplitDataFrame?
-- A DataFrameSplit represents a portion of a data frame produced by a Random Split Event
-- For example, if you split a DataFrame into pieces with weights of 0.3 and 0.7,
-- You would have two entries in the DataFrameSplit table, one for the 0.3 and one for the 0.7
DROP TABLE IF EXISTS DataFrameSplit;
CREATE TABLE DataFrameSplit (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The random split event that produced this piece (DataFrameSplit)
  splitEventId INTEGER REFERENCES RandomSplitEvent NOT NULL,
  -- The weight (relative size) of this piece (DataFrameSplit)
  weight FLOAT NOT NULL,
  -- The produced DataFrame
  dataFrameId INTEGER REFERENCES DataFrame NOT NULL,
  -- The experiment run that contains this piece (DataFrameSplit)
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- A TransformerSpec is a machine learning primitive that describes
-- the hyperparameters used to create a model (A Transformer produced
-- by fitting a TransformerSpec to a DataFrame).
DROP TABLE IF EXISTS TransformerSpec;
CREATE TABLE TransformerSpec (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The kind of Transformer that this spec describes (e.g. linear regression)
  transformerType TEXT NOT NULL,
  -- User assigned content about this spec
  tag TEXT,
  -- The experiment run in which this spec is contained
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- A hyperparameter helps guide the fitting of a model.
-- e.g. Number of trees in a random forest,
--      number of nuerons in a nueral network
DROP TABLE IF EXISTS HyperParameter;
CREATE TABLE HyperParameter (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The TransformerSpec that contains this hyperparameter
  spec INTEGER REFERENCES TransformerSpec NOT NULL,
  -- The name of this hyperparameter
  paramName TEXT NOT NULL,
  -- The type of the hyperparameter (e.g. String, Integer)
  paramType VARCHAR(40) NOT NULL,
  -- The value assigned to this hyperparameter
  paramValue TEXT NOT NULL,
  -- The minimum value allowed to be assigned to this hyperparameter
  -- Leave Min and Max NULL for non-numerical hyperparameters
  paramMinValue FLOAT,
  -- The maximum value allowed for this hyperparameter
  paramMaxValue FLOAT,
  -- The ExperimentRun that contains this hyperparameter
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- Transformers are machine learning primitives that take an input
-- DataFrame and produce an output DataFrame
DROP TABLE IF EXISTS Transformer;
CREATE TABLE Transformer (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  --  The kind of Transformer (e.g. Linear Regression Model, One-Hot Encoder)
  transformerType TEXT NOT NULL,
  --  User assigned text to describe this Transformer
  tag TEXT,
  -- The ExperimentRun that contains this Transformer
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL,
  --  The path to the serialized Transformer
  filepath TEXT
);

-- Metadata associated with a linear regression, or logistic regression model.
DROP TABLE IF EXISTS LinearModel;
CREATE TABLE LinearModel (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    -- This is the linear model.
    model INTEGER REFERENCES Transformer,
    -- The root mean squared error.
    rmse DOUBLE,
    -- The variance explained by the model.
    explainedVariance DOUBLE,
    -- The R^2 value (coefficient of determiniation).
    r2 DOUBLE
);


-- The data associated with each term (one term per feature and an optional intercept term).
DROP TABLE IF EXISTS LinearModelTerm;
CREATE TABLE LinearModelTerm (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    -- This is the linear model.
    model INTEGER REFERENCES Transformer,
    -- The index of the term. If this is 0, it's the intercept term.
    termIndex INTEGER NOT NULL,
    -- The coefficient associated with the term.
    coefficient DOUBLE NOT NULL,
    -- The t-statistic for the term.
    tStat DOUBLE,
    -- The standard error for the term.
    stdErr DOUBLE,
    -- The p-value for the term.
    pValue DOUBLE
);

-- The value of the objective function during the training of a model.
DROP TABLE IF EXISTS ModelObjectiveHistory;
CREATE TABLE ModelObjectiveHistory (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    -- This is the linear model.
    model INTEGER REFERENCES Transformer,
    -- The iteration number.
    iteration INTEGER NOT NULL,
    -- The value of the objective function at this iteration.
    objectiveValue DOUBLE NOT NULL
);

-- Describes a Fit Event - Fitting a Transformer Spec to a DataFrame to
-- produce a model (Transformer)
DROP TABLE IF EXISTS FitEvent;
CREATE TABLE FitEvent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The TransformerSpec guiding the fitting
  transformerSpec INTEGER REFERENCES TransformerSpec NOT NULL,
  -- The model (fitted Transformer) produced by the fitting
  transformer INTEGER REFERENCES Transformer NOT NULL,
  -- The DataFrame that the Spec is being fitted to
  df INTEGER REFERENCES DataFrame NOT NULL,
  -- The names of the output columns that will contain the model's predictions
  -- There may be multiple columns produced - one predicting the actual data, and the others
  -- describing additional information, such as confidence
  predictionColumns TEXT NOT NULL, -- Should be comma-separated, no spaces, alphabetical.
  -- The name of the columns in the DataFrame whose values this Transformer is supposed to predict. We support
  -- multiple label columns.
  labelColumns TEXT NOT NULL,
  -- The ExperimentRun that contains this event.
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL,
  -- The type of problem that the FitEvent is solving (e.g. Regression,
  -- Classification, Clustering, Recommendation, Undefined)
  problemType TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS FitEventIndexTransformer ON FitEvent(transformer);

-- Describes a feature in the DataFrame - an attribute to consider when
-- creating a Transformer from a DataFrame via a FitEvent.
DROP TABLE IF EXISTS Feature;
CREATE TABLE Feature (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The name of the feature
  name TEXT NOT NULL,
  -- The index of this feature in the feature vector
  featureIndex INTEGER NOT NULL,
  -- The importance to assign to this feature compared to the others
  -- (Depends on transformer type)
  importance DOUBLE NOT NULL,
  -- The transformer that should utilize this feature
  transformer INTEGER REFERENCES TRANSFORMER
);
CREATE INDEX IF NOT EXISTS FeatureIndexTransformer ON Feature(transformer);
CREATE INDEX IF NOT EXISTS FeatureIndexName ON Feature(name);

-- A TransformEvent describes using a Transformer to produce an output
-- DataFrame from an input DataFrame
DROP TABLE IF EXISTS TransformEvent;
CREATE TABLE TransformEvent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The original DataFrame that is input into the Transformer
  oldDf INTEGER REFERENCES DataFrame NOT NULL,
  -- The output DataFrame of the Transformer
  newDf INTEGER REFERENCES DataFrame NOT NULL,
  -- The Transformer used to perform this transformation
  transformer INTEGER REFERENCES Transformer NOT NULL,
  -- The columns in the input DataFrame that are used by the transformer
  inputColumns TEXT NOT NULL, -- Should be comma-separated, no spaces, alphabetical.
  -- The columns outputted by the Transformer
  outputColumns TEXT NOT NULL, -- Should be comma-separated, no spaces, alphabetical.
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);
CREATE INDEX IF NOT EXISTS TransformEventIndexNewDf ON TransformEvent(newDf);
CREATE INDEX IF NOT EXISTS TransformEventIndexExperimentRun ON TransformEvent(experimentRun);

--TODO
DROP TABLE IF EXISTS TreeNode;
CREATE TABLE TreeNode (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  isLeaf INTEGER NOT NULL, -- 1 if node is leaf, 0 if node is internal
  prediction DOUBLE NOT NULL, -- Internal nodes obviously do not use their predictions
  impurity DOUBLE NOT NULL, -- Impurity of node.
  gain DOUBLE, -- Information gain at node. NULL for leaf nodes.
  splitIndex INTEGER, -- Index of feature that the internal node is splitting. NULL if this is a leaf node.
  rootNode INTEGER REFERENCES TreeNode -- NULL for the root node
);

DROP TABLE IF EXISTS TreeLink;
CREATE TABLE TreeLink (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  parent INTEGER REFERENCES TreeNode NOT NULL,
  child INTEGER REFERENCES TreeNode NOT NULL,
  isLeft INTEGER NOT NULL -- 1 if the child is a left child and 0 if the child is a right child.
);

DROP TABLE IF EXISTS TreeModel;
CREATE TABLE TreeModel (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  model INTEGER REFERENCES Transformer NOT NULL,
  modelType TEXT NOT NULL -- Should be "Decision Tree", "GBT", or "Random Forest"
);

-- This represents the components of a tree ensemble (gradient boosted tree or random forest).
-- Note that we can also represent a decision tree as an ensemble with a single component that has weight 1.0.
DROP TABLE IF EXISTS TreeModelComponent;
CREATE TABLE TreeModelComponent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  model INTEGER REFERENCES Transformer,
  componentIndex INTEGER NOT NULL,
  componentWeight DOUBLE NOT NULL,
  rootNode INTEGER REFERENCES TreeNode
);

-- An event that represents the evaluation of a model given a DataFrame
DROP TABLE IF EXISTS MetricEvent;
CREATE TABLE MetricEvent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The model (Transformer) being evaluated
  transformer INTEGER REFERENCES Transformer NOT NULL,
  -- The DataFrame that the model is being evaluated on
  df INTEGER REFERENCES DataFrame NOT NULL,
  -- The type of Metric being measured (e.g. Squared Error, Accuracy, f1)
  metricType TEXT NOT NULL,
  -- The value of the measured Metric
  metricValue REAL NOT NULL,
  -- The Experiment Run that contains this Metric
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- A generic Event that can represent anything
DROP TABLE IF EXISTS Event;
CREATE TABLE Event (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  -- The type of the event that this entry represents
  eventType TEXT NOT NULL,
  -- The id of the event within its respective table
  eventId INTEGER NOT NULL, -- references the actual event in the table
  -- The Experiment Run that contains this Event
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- Represents a transform event or fit event that was part of the creation of a pipeline model
-- A pipeline model is a sequence of transformers, some of which may have been created by
-- Fit Events, in which each transformer transforms its input and passes its output to the next
-- Transformer
DROP TABLE IF EXISTS PipelineStage;
CREATE TABLE PipelineStage (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  pipelineFitEvent INTEGER REFERENCES FitEvent NOT NULL,
  transformOrFitEvent INTEGER REFERENCES Event NOT NULL,
  isFit INTEGER NOT NULL, -- 0 if this is a Transform stage and 1 if this is a Fit stage.
  stageNumber INTEGER NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

DROP TABLE IF EXISTS CrossValidationEvent;
CREATE TABLE CrossValidationEvent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  df INTEGER REFERENCES DataFrame NOT NULL,
  spec INTEGER REFERENCES TransformerSpec NOT NULL,
  numFolds INTEGER NOT NULL,
  randomSeed BIGINT NOT NULL,
  evaluator TEXT NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

DROP TABLE IF EXISTS CrossValidationFold;
CREATE TABLE CrossValidationFold (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  metric INTEGER REFERENCES MetricEvent NOT NULL,
  event INTEGER REFERENCES CrossValidationEvent NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

DROP TABLE IF EXISTS GridSearchCrossValidationEvent;
CREATE TABLE GridSearchCrossValidationEvent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  numFolds INTEGER NOT NULL,
  best INTEGER REFERENCES FitEvent NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

DROP TABLE IF EXISTS GridCellCrossValidation;
CREATE TABLE GridCellCrossValidation (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  gridSearch INTEGER REFERENCES GridSearchCrossValidationEvent NOT NULL,
  crossValidation INTEGER REFERENCES CrossValidationEvent NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- An annotation is a user-specified note that is posted to the server.
-- It consists of an ordered sequence of AnnotationFragments.
DROP TABLE IF EXISTS Annotation;
CREATE TABLE Annotation (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  posted TIMESTAMP NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- An AnnotationFragment is part of an Annotation. For example, consider the annotation:
-- ("I'm having issues with"), (model 1), ("it seems that it was trained on an erroneous dataset"), (DataFrame 2).
-- This annotation has four fragments (in parentheses). We let an AnnotationFragment to represent one of the following:
--  message: A string
--  spec: A reference to a TransformerSpec
--   Transformer: A references to a Transformer
--  DataFrame: A reference to a DataFrame
-- We indicate which of these four types the AnnotationFragment is by using the 'type' column.
-- The 'index' column represents the position of the fragment in the Annotation. In our example annotation above, the
-- (DataFrame 2) fragment would have index 3 while the ("I'm having issues with") fragment would have index 0.
DROP TABLE IF EXISTS AnnotationFragment;
CREATE TABLE AnnotationFragment (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  annotation INTEGER REFERENCES Annotation NOT NULL,
  fragmentIndex INTEGER NOT NULL,
  type TEXT NOT NULL,
  transformer INTEGER REFERENCES Transformer,
  DataFrame INTEGER REFERENCES DataFrame,
  spec INTEGER REFERENCES TransformerSpec,
  message TEXT,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

--  Create a view for models (i.e. the Transformers that have an associated FitEvent).
DROP VIEW IF EXISTS model_view;
CREATE VIEW model_view AS
  SELECT fe.id as fe_id, ts.transformertype as model_type, fe.transformer as model, fe.transformerspec as spec_id, fe.df as train_df
  FROM fitevent fe, transformerspec ts
  WHERE ts.id = fe.transformerspec order by fe.id;

--  Create a view for Transformers which are not models
DROP VIEW IF EXISTS transformer_view;
CREATE VIEW transformer_view AS
  SELECT te.id as te_id, t.transformertype as transformer_type, te.transformer as transformer, te.olddf as input_df, te.newdf as output_df
  FROM transformevent te, transformer t
  WHERE te.transformer = t.id order by te.id;

-- Create a view for pipeline structure
DROP VIEW IF EXISTS pipeline_view;
CREATE VIEW pipeline_view AS
  SELECT pipelinefitevent, stagenumber, e.id as event_id, e.eventtype, e.eventid
  FROM pipelinestage ps, event e
  WHERE ps.transformorfitevent = e.id order by stagenumber, eventtype;

-- Create a view that shows experimentrun, experiment, and projectid in one table.
DROP VIEW IF EXISTS experiment_run_view;
CREATE VIEW experiment_run_view AS
    SELECT er.id AS experimentRunId, e.id AS experimentId, p.id AS projectId
    FROM ExperimentRun er, Experiment e, Project p
    WHERE er.experiment = e.id
    AND e.project = p.id;
