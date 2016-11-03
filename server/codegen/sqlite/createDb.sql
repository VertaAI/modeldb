-- THIS FILE IS IN FLUX --

-- Create a database. We may want to make the database name a parameter
-- CREATE DATABASE ModelDb;

DROP TABLE IF EXISTS Project;
CREATE TABLE Project (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT, -- Can be empty.
  author TEXT, -- Can be empty.
  description TEXT, -- Can be empty.
  created TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS Experiment;
CREATE TABLE Experiment (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  project INTEGER REFERENCES Project NOT NULL,
  name TEXT NOT NULL,
  description TEXT, -- Can be empty.
  created TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS ExperimentRun;
CREATE TABLE ExperimentRun (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  experiment INTEGER REFERENCES Experiment NOT NULL,
  description TEXT, -- Can be empty.
  created TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS DataFrame;
CREATE TABLE DataFrame (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  tag TEXT, -- Can be empty.
  numRows INTEGER NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL,
  filepath TEXT
);

DROP TABLE IF EXISTS DataFrameColumn;
CREATE TABLE DataFrameColumn (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  dfId INTEGER REFERENCES DataFrame NOT NULL,
  name TEXT NOT NULL,
  type TEXT NOT NULL
);

DROP TABLE IF EXISTS RandomSplitEvent;
CREATE TABLE RandomSplitEvent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  inputDataFrameId INTEGER REFERENCES DataFrame NOT NULL,
  randomSeed BIGINT NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

DROP TABLE IF EXISTS DataFrameSplit;
CREATE TABLE DataFrameSplit (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  splitEventId INTEGER REFERENCES RandomSplitEvent NOT NULL,
  weight FLOAT NOT NULL,
  dataFrameId INTEGER REFERENCES DataFrame NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

DROP TABLE IF EXISTS TransformerSpec;
CREATE TABLE TransformerSpec (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  transformerType TEXT NOT NULL,
  tag TEXT, -- Can be empty.
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- NOTE: Our hyperparameter table is simple for now.
DROP TABLE IF EXISTS HyperParameter;
CREATE TABLE HyperParameter (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  spec INTEGER REFERENCES TransformerSpec NOT NULL,
  paramName TEXT NOT NULL,
  paramType VARCHAR(40) NOT NULL,
  paramValue TEXT NOT NULL,
  paramMinValue FLOAT, -- Can be empty.
  paramMaxValue FLOAT, -- Can be empty.
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- This is only for the demo: logreg and linreg
DROP TABLE IF EXISTS Transformer;
CREATE TABLE Transformer (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  transformerType TEXT NOT NULL,
  tag TEXT, -- Can be empty.
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL,
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


DROP TABLE IF EXISTS FitEvent;
CREATE TABLE FitEvent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  transformerSpec INTEGER REFERENCES TransformerSpec NOT NULL,
  transformer INTEGER REFERENCES Transformer NOT NULL,
  df INTEGER REFERENCES DataFrame NOT NULL,
  predictionColumns TEXT NOT NULL, -- Should be comma-separated, no spaces, alphabetical.
  labelColumn TEXT NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL,
  problemType TEXT NOT NULL
);

DROP TABLE IF EXISTS Feature;
CREATE TABLE Feature (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  featureIndex INTEGER NOT NULL,
  importance DOUBLE NOT NULL,
  transformer INTEGER REFERENCES TRANSFORMER
);

DROP TABLE IF EXISTS TransformEvent;
CREATE TABLE TransformEvent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  oldDf INTEGER REFERENCES DataFrame NOT NULL,
  newDf INTEGER REFERENCES DataFrame NOT NULL,
  transformer INTEGER REFERENCES Transformer NOT NULL,
  inputColumns TEXT NOT NULL, -- Should be comma-separated, no spaces, alphabetical.
  outputColumns TEXT NOT NULL, -- Should be comma-separated, no spaces, alphabetical.
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

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
  parent INTEGER REFERENCES TreeNode NOT NULL,
  child INTEGER REFERENCES TreeNode NOT NULL,
  isLeft INTEGER NOT NULL, -- 1 if the child is a left child and 0 if the child is a right child.
  PRIMARY KEY (parent, child, isLeft)
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
  model INTEGER REFERENCES Transformer,
  componentIndex INTEGER NOT NULL,
  componentWeight DOUBLE NOT NULL,
  rootNode INTEGER REFERENCES TreeNode
);

DROP TABLE IF EXISTS MetricEvent;
CREATE TABLE MetricEvent (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  transformer INTEGER REFERENCES Transformer NOT NULL,
  df INTEGER REFERENCES DataFrame NOT NULL,
  metricType TEXT NOT NULL,
  metricValue REAL NOT NULL,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

DROP TABLE IF EXISTS Event;
CREATE TABLE Event (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  eventType TEXT NOT NULL,
  eventId INTEGER NOT NULL, -- references the actual event in the table
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- This is how we store pipelines. We associate each transformer in the pipeline to the event representing the fit of
-- the pipeline. The stage number orders these transformers in the pipeline.
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
-- ("I'm having issues with"), (model 1), ("it seems that it was trained on an erroneous dataset"), (dataframe 2).
-- This annotation has four fragments (in parentheses). We let an AnnotationFragment to represent one of the following:
--  message: A string
--  spec: A reference to a TransformerSpec
--  transformer: A references to a Transformer
--  dataframe: A reference to a DataFrame
-- We indicate which of these four types the AnnotationFragment is by using the 'type' column.
-- The 'index' column represents the position of the fragment in the Annotation. In our example annotation above, the
-- (dataframe 2) fragment would have index 3 while the ("I'm having issues with") fragment would have index 0.
DROP TABLE IF EXISTS AnnotationFragment;
CREATE TABLE AnnotationFragment (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  annotation INTEGER REFERENCES Annotation NOT NULL,
  fragmentIndex INTEGER NOT NULL,
  type TEXT NOT NULL,
  transformer INTEGER REFERENCES Transformer,
  dataframe INTEGER REFERENCES DataFrame,
  spec INTEGER REFERENCES TransformerSpec,
  message TEXT,
  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
);

-- Create a view for models (i.e. the transformers that have an associated FitEvent).
DROP VIEW IF EXISTS model_view;
CREATE VIEW model_view AS 
  SELECT fe.id as fe_id, ts.transformertype as model_type, fe.transformer as model, fe.transformerspec as spec_id, fe.df as train_df
  FROM fitevent fe, transformerspec ts
  WHERE ts.id = fe.transformerspec order by fe.id;

-- Create a view for transformers which are not models
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
