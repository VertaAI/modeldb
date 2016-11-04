"""
This is the Syncer that is responsible for storing events in the ModelDB.
Contains functions for overriding basic scikit-learn functions.
"""
import sys
import numpy as np
import pandas as pd

# sklearn imports
from sklearn.linear_model import *
from sklearn.preprocessing import *
from sklearn.decomposition import *
from sklearn.feature_selection import *
from sklearn.svm import *
from sklearn.pipeline import Pipeline
from sklearn.grid_search import GridSearchCV
import sklearn.metrics

from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

# modeldb imports
import GridCrossValidation
from ..basic import *
from ..events import *
from ..thrift.modeldb import ModelDBService
from ..thrift.modeldb import ttypes as modeldb_types



def fit_fn(self, x, y=None, sample_weight=None):
    """
    Overrides the fit function for all models except for Pipeline and GridSearch
    Cross Validation, which have their own functions.
    """
    df = x
    #Certain fit functions only accept one argument
    if y is None:
        models = self.fit(x)
    else:
        models = self.fit(x, y)
    fit_event = FitEvent(models, self, x)
    Syncer.instance.add_to_buffer(fit_event)

def predict_fn(self, x):
    """
    Overrides the predict function for models, provided that the predict
    function takes in one argument.
    """
    predict_array = self.predict(x)
    predict_df = pd.DataFrame(predict_array)
    # Assign names to the predicted columns.
    # This is to ensure there are no merge conflicts when joining.
    num_pred_cols = predict_df.shape[1]
    pred_col_names = []
    for i in range(0, num_pred_cols):
        pred_col_names.append('pred_'+str(i))
    predict_df.columns = pred_col_names
    new_df = x.join(predict_df)
    predict_event = TransformEvent(x, new_df, self)
    Syncer.instance.add_to_buffer(predict_event)
    return predict_array

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

def fit_fn_pipeline(self, x, y):
    """
    Overrides the Pipeline model's fit function
    """
    #Check if pipeline contains valid estimators and transformers
    check_valid_pipeline(self.steps)

    #Make Fit Event for overall pipeline
    pipeline_model = self.fit(x, y)
    pipeline_fit = FitEvent(pipeline_model, self, x)

    #Extract all the estimators from pipeline
    #All estimators call 'fit' and 'transform' except the last estimator (which only calls 'fit')
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

        #Convert transformed output into a proper pandas DataFrame object
        if type(transformed_output) is np.ndarray:
            new_df = pd.DataFrame(transformed_output)
        else:
            new_df = pd.DataFrame(transformed_output.toarray())

        cur_dataset = transformed_output

        #populate the stages
        transform_event = TransformEvent(old_df, new_df, model)
        transform_stages.append((index, transform_event))
        fit_event = FitEvent(model, estimator, old_df)
        fit_stages.append((index, fit_event))

    #Handle last estimator, which has a fit method (and may not have transform)
    old_df = cur_dataset
    model = last_estimator.fit(old_df, y)
    fit_event = FitEvent(model, last_estimator, old_df)
    fit_stages.append((index+1, fit_event))

    #Create the pipeline event with all components
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
        #Change from original scikit: checking for "fit" and "transform"
        #methods, rather than "fit_transform" as each event is logged separately to database
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

    #Calls SyncGridCVEvent and adds to buffer.
    grid_event = GridSearchCVEvent(input_data_frame, cross_validations,
        seed, evaluator, best_model, best_estimator, num_folds)
    Syncer.instance.add_to_buffer(grid_event)

def store_df_path(filepath_or_buffer, **kwargs):
    """
    Stores the filepath for a dataframe
    """
    df = pd.read_csv(filepath_or_buffer, **kwargs)
    Syncer.instance.path_for_df[id(df)] = filepath_or_buffer
    return df

def drop_columns(self, labels, **kwargs):
    """
    Overrides the "drop" function of pandas dataframes
    so event can be logged as a TransformEvent.
    """
    dropped_df = self.drop(labels, **kwargs)
    drop_event = TransformEvent(self, dropped_df, 'DropColumns')
    Syncer.instance.add_to_buffer(drop_event)
    return dropped_df


class Syncer(ModelDbSyncerBase.Syncer):
    """
    This is the Syncer class for sklearn functionality, responsible for
    storing events in the ModelDB.
    """
    def __init__(self, project_config, experiment_config, experiment_run_config):
        super(Syncer, self).__init__(project_config, experiment_config, experiment_run_config)
        self.id_for_object = {}
        self.object_for_id = {}
        self.tag_for_object = {}
        self.object_for_tag = {}
        self.path_for_df = {}
        self.enable_sync_functions()
        self.add_dataframe_attr()

    def __str__(self):
        return "SklearnSyncer"

    def store_object(self, obj, Id):
        """
        Stores mapping between objects and their IDs.
        """
        self.id_for_object[obj] = Id
        self.object_for_id[Id] = obj

    def store_tag_object(self, obj, tag):
        """
        Stores mapping between objects and their tags.
        Tags are short, user-generated names.
        """
        self.tag_for_object[obj] = tag
        self.object_for_tag[tag] = obj

    def add_tag(self, obj, tag_name):
        """
        Adds tag name to object.
        """
        self.store_tag_object(id(obj), tag_name)

    def add_to_buffer(self, event):
        """
        As events are generated, they are added to this buffer.
        """
        self.buffer_list.append(event)

    def sync(self):
        """
        When this function is called, all events in the buffer are stored on server.
        """
        for b in self.buffer_list:
            b.sync(self)
        self.clear_buffer()

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
        tid = -1
        tag = ""
        if model in self.id_for_object:
            tid = self.id_for_object[model]
        if id(model) in self.tag_for_object:
            tag = self.tag_for_object[id(model)]
        transformer_type = model.__class__.__name__
        t = modeldb_types.Transformer(tid, transformer_type, tag)
        return t

    def convert_df_to_thrift(self, df):
        """
        Converts a dataframe into a Thrift object with appropriate fields.
        """
        tid = -1
        tag = ""
        filepath = ""
        dfImm = id(df)
        if dfImm in self.id_for_object:
            tid = self.id_for_object[dfImm]
        if dfImm in self.tag_for_object:
            tag = self.tag_for_object[dfImm]
        dataframe_columns = self.setDataFrameSchema(df)
        if dfImm in self.path_for_df:
            filepath = self.path_for_df[dfImm]
        modeldb_df = modeldb_types.DataFrame(
            tid, dataframe_columns, df.shape[0], tag, filepath)
        return modeldb_df

    def convert_spec_to_thrift(self, spec):
        """
        Converts a TransformerSpec object into a Thrift object with appropriate fields.
        """
        tid = -1
        tag = ""
        if spec in self.id_for_object:
            tid = self.id_for_object[spec]
        if id(spec) in self.tag_for_object:
            tag = self.tag_for_object[id(spec)]
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

    def add_dataframe_attr(self):
        """
        Adds the read_csv_sync function, allowing users to automatically
        track dataframe location, and the drop_sync function, allowing users
        to track dropped columns from a dataframe.
        """
        setattr(pd, "read_csv_sync", store_df_path)
        setattr(pd.DataFrame, "drop_sync", drop_columns)

    def enable_sync_functions(self):
        """
        This function extends the scikit classes to implement custom
        *Sync versions of methods. (i.e. fit_sync() for fit())
        Users can easily add more models to this function.
        """
        #Linear Models (transform has been deprecated)
        for class_name in [LogisticRegression, LinearRegression]:
            setattr(class_name, "fit_sync", fit_fn)
            setattr(class_name, "predict_sync", predict_fn)

        #Preprocessing models
        for class_name in [LabelEncoder, OneHotEncoder]:
            setattr(class_name, "fit_sync", fit_fn)
            setattr(class_name, "transform_sync", transform_fn)

        #Pipeline model
        for class_name in [Pipeline]:
            setattr(class_name, "fit_sync", fit_fn_pipeline)

        #Grid-Search Cross Validation model
        for class_name in [GridSearchCV]:
            setattr(class_name, "fit_sync", fit_fn_grid_search)
