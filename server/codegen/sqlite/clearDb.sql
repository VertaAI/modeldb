-- Clears tables in database modeldb_test.

DELETE FROM Project;

DELETE FROM Experiment;

DELETE FROM ExperimentRun;

DELETE FROM DataFrame;

DELETE FROM DataFrameColumn;

DELETE FROM RandomSplitEvent;

DELETE FROM DataFrameSplit;

DELETE FROM TransformerSpec;

DELETE FROM HyperParameter;

DELETE FROM Transformer;

DELETE FROM LinearModel;

DELETE FROM LinearModelTerm;

DELETE FROM ModelObjectiveHistory;

DELETE FROM FitEvent;

DELETE FROM Feature;

DELETE FROM TransformEvent;

DELETE FROM TreeNode;

DELETE FROM TreeLink;

DELETE FROM TreeModel;

DELETE FROM TreeModelComponent;

DELETE FROM MetricEvent;

DELETE FROM Event;

DELETE FROM PipelineStage;

DELETE FROM CrossValidationEvent;

DELETE FROM CrossValidationFold;

DELETE FROM GridSearchCrossValidationEvent;

DELETE FROM GridCellCrossValidation;

DELETE FROM Annotation;

DELETE FROM AnnotationFragment;