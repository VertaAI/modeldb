"""
This is the Syncer that is responsible for storing events in the ModelDB.
Contains functions for overriding basic scikit-learn functions.
"""
import sys
import numpy as np
import pandas as pd
from future.utils import with_metaclass

# sklearn imports
from sklearn.linear_model import *
from sklearn.preprocessing import *
from sklearn.decomposition import *
from sklearn.calibration import *
from sklearn.ensemble import *
from sklearn.tree import *
from sklearn.feature_selection import *
from sklearn.svm import *
from sklearn.pipeline import Pipeline
from sklearn.grid_search import GridSearchCV
from sklearn import cross_validation
import sklearn.metrics

from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

# modeldb imports
from modeldb.utils.Singleton import Singleton
from . import GridCrossValidation
from . CrossValidationScore import *
from ..basic import *
from ..events import *
from ..thrift.modeldb import ModelDBService
from ..thrift.modeldb import ttypes as modeldb_types


'''
Functions that extract relevant information from scikit-learn, pandas and
numpy calls
'''


def fit_fn(self, x, y=None, sample_weight=None):
    """
    Overrides the fit function for all models except for
    Pipeline and GridSearch, and Cross Validation,
    which have their own functions.
    """
    df = x
    # Certain fit functions only accept one argument
    if y is None:
        model = self.fit(x)
    else:
        model = self.fit(x, y)
    fit_event = FitEvent(model, self, x)
    Syncer.instance.add_to_buffer(fit_event)


def convert_prediction_to_event(model, predict_array, x):
    predict_df = pd.DataFrame(predict_array)
    # Assign names to the predicted columns.
    # This is to ensure there are no merge conflicts when joining.
    num_pred_cols = predict_df.shape[1]
    pred_col_names = []
    for i in range(0, num_pred_cols):
        pred_col_names.append('pred_' + str(i))
    predict_df.columns = pred_col_names
    if not isinstance(x, pd.DataFrame):
        x_to_df = pd.DataFrame(x)
        new_df = x_to_df.join(predict_df)
    else:
        new_df = x.join(predict_df)
    predict_event = TransformEvent(x, new_df, model)
    Syncer.instance.add_to_buffer(predict_event)
    return predict_array


def predict_fn(self, x):
    """
    Overrides the predict function for models, provided that the predict
    function takes in one argument.
    """
    predict_array = self.predict(x)
    return convert_prediction_to_event(self, predict_array, x)


def predict_proba_fn(self, x):
    """
    Overrides the predict_proba function for models.
    """
    predict_array = self.predict_proba(x)
    return convert_prediction_to_event(self, predict_array, x)


def transform_fn(self, x):
    """
    Overrides the transform function for models, provided that the
    transform function takes in one argument.
    """
    transformed_output = self.transform(x)
    if type(transformed_output) is np.ndarray:
        new_df = pd.DataFrame(transformed_output)
    else:
        new_df = pd.DataFrame(transformed_output.toarray())
    transform_event = TransformEvent(x, new_df, self)
    Syncer.instance.add_to_buffer(transform_event)
    return transformed_output


def fit_transform_fn(self, x, y=None, **fit_params):
    """
    Overrides the fit_transform function for models.
    Combines fit and transform functions.
    """
    df = x
    # Certain fit functions only accept one argument
    if y is None:
        fitted_model = self.fit(x, **fit_params)
    else:
        fitted_model = self.fit(x, y, **fit_params)
    fit_event = FitEvent(fitted_model, self, df)
    Syncer.instance.add_to_buffer(fit_event)
    transformed_output = fitted_model.transform(x)
    if type(transformed_output) is np.ndarray:
        new_df = pd.DataFrame(transformed_output)
    else:
        new_df = pd.DataFrame(transformed_output.toarray())
    transform_event = TransformEvent(x, new_df, fitted_model)
    Syncer.instance.add_to_buffer(transform_event)
    return transformed_output


def fit_fn_pipeline(self, x, y):
    """
    Overrides the Pipeline model's fit function
    """
    # Check if pipeline contains valid estimators and transformers
    check_valid_pipeline(self.steps)

    # Make Fit Event for overall pipeline
    pipeline_model = self.fit(x, y)
    pipeline_fit = FitEvent(pipeline_model, self, x)

    # Extract all the estimators from pipeline
    # All estimators call 'fit' and 'transform' except the last estimator
    # (which only calls 'fit')
    names, sk_estimators = zip(*self.steps)
    estimators = sk_estimators[:-1]
    last_estimator = sk_estimators[-1]

    transform_stages = []
    fit_stages = []
    cur_dataset = x

    for index, estimator in enumerate(estimators):
        old_df = cur_dataset
        model = estimator.fit(old_df, y)
        transformed_output = model.transform(old_df)

        # Convert transformed output into a proper pandas DataFrame object
        if type(transformed_output) is np.ndarray:
            new_df = pd.DataFrame(transformed_output)
        else:
            new_df = pd.DataFrame(transformed_output.toarray())

        cur_dataset = transformed_output

        # populate the stages
        transform_event = TransformEvent(old_df, new_df, model)
        transform_stages.append((index, transform_event))
        fit_event = FitEvent(model, estimator, old_df)
        fit_stages.append((index, fit_event))

    # Handle last estimator, which has a fit method (and may not have
    # transform)
    old_df = cur_dataset
    model = last_estimator.fit(old_df, y)
    fit_event = FitEvent(model, last_estimator, old_df)
    fit_stages.append((index + 1, fit_event))

    # Create the pipeline event with all components
    pipeline_event = PipelineEvent(pipeline_fit, transform_stages, fit_stages)

    Syncer.instance.add_to_buffer(pipeline_event)


def check_valid_pipeline(steps):
    """
    Helper function to check whether a pipeline is constructed properly. Taken
    from original sklearn pipeline source code with minor modifications,
    which are commented below.
    """
    names, estimators = zip(*steps)
    transforms = estimators[:-1]
    estimator = estimators[-1]

    for t in transforms:
        # Change from original scikit: checking for "fit" and "transform"
        # methods, rather than "fit_transform" as each event is logged
        # separately to database
        if not (hasattr(t, "fit")) and hasattr(t, "transform"):
            raise TypeError("All intermediate steps of the chain should "
                            "be transforms and implement fit and transform"
                            " '%s' (type %s) doesn't)" % (t, type(t)))

    if not hasattr(estimator, "fit"):
        raise TypeError("Last step of chain should implement fit "
                        "'%s' (type %s) doesn't)"
                        % (estimator, type(estimator)))


def fit_fn_grid_search(self, x, y):
    """
    Overrides GridSearch Cross Validation's fit function
    """
    GridCrossValidation.fit(self, x, y)
    [input_data_frame, cross_validations, seed, evaluator, best_model,
        best_estimator, num_folds] = self.grid_cv_event

    # Calls SyncGridCVEvent and adds to buffer.
    grid_event = GridSearchCVEvent(input_data_frame, cross_validations,
                                   seed, evaluator, best_model, best_estimator, num_folds)
    Syncer.instance.add_to_buffer(grid_event)


def store_df_path(filepath_or_buffer, **kwargs):
    """
    Stores the filepath for a dataframe
    """
    df = pd.read_csv(filepath_or_buffer, **kwargs)
    Syncer.instance.store_path_for_df(df, str(filepath_or_buffer))
    return df


def train_test_split_fn(*arrays, **options):
    """
    Stores the split dataframes.
    """
    split_dfs = cross_validation.train_test_split(*arrays, **options)

    # Extract the option values to create RandomSplitEvent
    test_size = options.pop('test_size', None)
    train_size = options.pop('train_size', None)
    random_state = options.pop('random_state', None)
    if test_size is None and train_size is None:
        test_size = 0.25
        train_size = 0.75
    elif test_size is None:
        test_size = 1.0 - train_size
    else:
        train_size = 1.0 - test_size
    if random_state is None:
        random_state = 1
    main_df = arrays[0]
    weights = [train_size, test_size]
    result = split_dfs[:int(len(split_dfs) / 2)]
    random_split_event = RandomSplitEvent(
        main_df, weights, random_state, result)
    Syncer.instance.add_to_buffer(random_split_event)
    return split_dfs


def drop_columns(self, labels, **kwargs):
    """
    Overrides the "drop" function of pandas dataframes
    so event can be logged as a TransformEvent.
    """
    dropped_df = self.drop(labels, **kwargs)
    drop_event = TransformEvent(self, dropped_df, 'DropColumns')
    Syncer.instance.add_to_buffer(drop_event)
    return dropped_df


'''
End functions that extract information from scikit-learn, pandas and numpy
'''


class Syncer(with_metaclass(Singleton, ModelDbSyncerBase.Syncer)):

    # The Syncer class needs to have its own pointer to the singleton instance
    # for overidden sklearn methods to reference
    instance = None
    """
    This is the Syncer class for sklearn, responsible for
    storing events in the ModelDB.
    """

    def __init__(
            self, project_config, experiment_config, experiment_run_config,
            thrift_config=None):
        self.local_id_to_path = {}
        self.enable_sklearn_sync_functions()
        self.enable_pandas_sync_functions()

        Syncer.instance = self

        super(Syncer, self).__init__(project_config, experiment_config,
                                     experiment_run_config, thrift_config)

    def __str__(self):
        return "SklearnSyncer"

    '''
    Functions that turn classes specific to this syncer into equivalent
    thrift classes
    '''

    def set_columns(self, df):
        """
        Helper function to extract column names from a dataframe.
        Pandas dataframe objects are treated differently from
        numpy arrays.
        """
        if type(df) is pd.DataFrame:
            columns = df.columns.values
            if type(columns) is np.ndarray:
                columns = np.array(columns).tolist()
            for i in range(0, len(columns)):
                columns[i] = str(columns[i])
        else:
            columns = []
        return columns

    def setDataFrameSchema(self, df):
        """
        Helper function designated to extract the column schema
        within a dataframe.
        """
        data_frame_cols = []
        columns = self.set_columns(df)
        for i in range(0, len(columns)):
            columnName = str(columns[i])
            dfc = modeldb_types.DataFrameColumn(columnName, str(df.dtypes[i]))
            data_frame_cols.append(dfc)
        return data_frame_cols

    def convert_model_to_thrift(self, model):
        """
        Converts a model into a Thrift object with appropriate fields.
        """
        tid = self.get_modeldb_id_for_object(model)
        tag = self.get_tag_for_object(model)
        transformer_type = model.__class__.__name__
        t = modeldb_types.Transformer(tid, transformer_type, tag)
        return t

    def get_path_for_df(self, df):
        local_id = self.get_local_id(df)
        if local_id in self.local_id_to_path:
            return self.local_id_to_path[local_id]
        else:
            return ""

    def store_path_for_df(self, df, path):
        local_id = self.get_local_id(df)
        self.local_id_to_path[local_id] = path
        if local_id not in self.local_id_to_object:
            self.local_id_to_object[local_id] = df

    def convert_df_to_thrift(self, df):
        """
        Converts a dataframe into a Thrift object with appropriate fields.
        """
        tid = self.get_modeldb_id_for_object(df)
        tag = self.get_tag_for_object(df)
        filepath = self.get_path_for_df(df)

        dataframe_columns = self.setDataFrameSchema(df)
        modeldb_df = modeldb_types.DataFrame(
            tid, dataframe_columns, df.shape[0], tag, filepath)
        return modeldb_df

    def convert_spec_to_thrift(self, spec):
        """
        Converts a TransformerSpec object into a Thrift object with appropriate
        fields.
        """
        tid = self.get_modeldb_id_for_object(spec)
        tag = self.get_tag_for_object(spec)
        hyperparams = []
        params = spec.get_params()
        for param in params:
            hp = modeldb_types.HyperParameter(
                param,
                str(params[param]),
                type(params[param]).__name__,
                sys.float_info.min,
                sys.float_info.max)
            hyperparams.append(hp)
        ts = modeldb_types.TransformerSpec(tid, spec.__class__.__name__,
                                           hyperparams, tag)
        return ts

    '''
    End Functions that turn classes specific to this syncer into equivalent
    thrift classes
    '''

    '''
    Enable sync functionality on various functions
    '''

    def enable_pandas_sync_functions(self):
        """
        Adds the read_csv_sync function, allowing users to automatically
        track dataframe location, and the drop_sync function, allowing users
        to track dropped columns from a dataframe.
        """
        setattr(pd, "read_csv_sync", store_df_path)
        setattr(pd.DataFrame, "drop_sync", drop_columns)

    def enable_sklearn_sync_functions(self):
        """
        This function extends the scikit classes to implement custom
        *Sync versions of methods. (i.e. fit_sync() for fit())
        Users can easily add more models to this function.
        """
        # Linear Models (transform has been deprecated)
        for class_name in [LogisticRegression, LinearRegression,
                           CalibratedClassifierCV, RandomForestClassifier,
                           BaggingClassifier]:
            setattr(class_name, "fit_sync", fit_fn)
            setattr(class_name, "predict_sync", predict_fn)
            setattr(class_name, "predict_proba_sync", predict_proba_fn)

        # Preprocessing and some Classifier models
        for class_name in [LabelEncoder, OneHotEncoder,
                           DecisionTreeClassifier]:
            setattr(class_name, "fit_sync", fit_fn)
            setattr(class_name, "transform_sync", transform_fn)
            setattr(class_name, "fit_transform_sync", fit_transform_fn)

        # Pipeline model
        for class_name in [Pipeline]:
            setattr(class_name, "fit_sync", fit_fn_pipeline)

        # Grid-Search Cross Validation model
        for class_name in [GridSearchCV]:
            setattr(class_name, "fit_sync", fit_fn_grid_search)
            setattr(class_name, "predict_sync", predict_fn)

        # Train-test split for cross_validation
        setattr(cross_validation, "train_test_split_sync", train_test_split_fn)
        setattr(cross_validation, "cross_val_score_sync", cross_val_score_fn)
