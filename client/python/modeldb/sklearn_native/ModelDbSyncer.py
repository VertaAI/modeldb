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



def fit_Fn(self, X, y=None, sample_weight=None):
    """
    Overrides the fit function for all models except for Pipeline and GridSearch
    Cross Validation, which have their own functions.
    """
    df = X
    #Certain fit functions only accept one argument
    if y is None:
        models = self.fit(X)
    else:
        models = self.fit(X, y)
        yDf = pd.DataFrame(y)
        if type(X) is pd.DataFrame:
            df = X.join(yDf)
        else:
            #if X does not have column-names, we cannot perform a join, and
            #must instead add a new column.
            df = pd.DataFrame(X)
            df['outputColumn'] = y
    #Calls FitEvent in other class and adds to buffer
    fitEvent = FitEvent(models, self, df)
    if hasattr(X, 'tag') and X.tag != "":
        addTagObject(df, X.tag)
    Syncer.instance.addToBuffer(fitEvent)

def predictFn(self, X):
    """
    Overrides the predict function for models, provided that the predict
    function takes in one argument.
    """
    predictArray = self.predict(X)
    predictDf = pd.DataFrame(predictArray)
    newDf = X.join(predictDf)
    predictEvent = TransformEvent(X, newDf, self)
    Syncer.instance.addToBuffer(predictEvent)
    return predictArray

def transformFn(self, X):
    """
    Overrides the transform function for models, provided that the
    transform function takes in one argument.
    """
    transformedOutput = self.transform(X)
    if type(transformedOutput) is np.ndarray:
        newDf = pd.DataFrame(transformedOutput)
    else:
        newDf = pd.DataFrame(transformedOutput.toarray())
    transformEvent = TransformEvent(X, newDf, self)
    Syncer.instance.addToBuffer(transformEvent)
    return transformedOutput

def fitFnPipeline(self, X, y):
    """
    Overrides the Pipeline model's fit function
    """
    #Check if pipeline contains valid estimators and transformers
    checkValidPipeline(self.steps)

    #Make Fit Event for overall pipeline
    pipelineModel = self.fit(X, y)
    pipelineFit = FitEvent(pipelineModel, self, X)

    #Extract all the estimators from pipeline
    #All estimators call 'fit' and 'transform' except the last estimator (which only calls 'fit')
    names, sk_estimators = zip(*self.steps)
    estimators = sk_estimators[:-1]
    lastEstimator = sk_estimators[-1]

    transformStages = []
    fitStages = []
    curDataset = X

    for index, estimator in enumerate(estimators):
        oldDf = curDataset
        model = estimator.fit(oldDf, y)
        transformedOutput = model.transform(oldDf)

        #Convert transformed output into a proper pandas DataFrame object
        if type(transformedOutput) is np.ndarray:
            newDf = pd.DataFrame(transformedOutput)
        else:
            newDf = pd.DataFrame(transformedOutput.toarray())

        curDataset = transformedOutput

        #populate the stages
        transformEvent = TransformEvent(oldDf, newDf, model)
        transformStages.append((index, transformEvent))
        fitEvent = FitEvent(model, estimator, oldDf)
        fitStages.append((index, fitEvent))

    #Handle last estimator, which has a fit method (and may not have transform)
    oldDf = curDataset
    model = lastEstimator.fit(oldDf, y)
    fitEvent = FitEvent(model, lastEstimator, oldDf)
    fitStages.append((index+1, fitEvent))

    #Create the pipeline event with all components
    pipelineEvent = PipelineEvent(pipelineFit, transformStages, fitStages)

    Syncer.instance.addToBuffer(pipelineEvent)

def checkValidPipeline(steps):
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

def fitFnGridSearch(self, X, y):
    """
    Overrides GridSearch Cross Validation's fit function
    """
    GridCrossValidation.fit(self, X, y)
    [inputDataFrame, crossValidations, seed, evaluator, bestModel,
     bestEstimator, numFolds] = self.gridCVevent

    #Calls SyncGridCVEvent and adds to buffer.
    gridEvent = GridSearchCVEvent(inputDataFrame, crossValidations,
                                  seed, evaluator, bestModel, bestEstimator, numFolds)
    Syncer.instance.addToBuffer(gridEvent)

def addTagObject(self, tagName):
    """
    Stores object with associated tagName
    """
    if type(tagName) is str:
        self.tag = tagName
        Syncer.instance.storeTagObject(id(self), tagName)

def storeDfPath(filepath_or_buffer, **kwargs):
    """
    Stores the filepath for a dataframe
    """
    df = pd.read_csv(filepath_or_buffer, **kwargs)
    Syncer.instance.pathForDf[id(df)] = filepath_or_buffer
    return df

class Syncer(ModelDbSyncerBase.Syncer):
    """
    This is the Syncer class for sklearn functionality, responsible for
    storing events in the ModelDB.
    """
    def __init__(self, projectConfig, experimentConfig, experimentRunConfig):
        super(Syncer, self).__init__(projectConfig, experimentConfig, experimentRunConfig)
        self.idForObject = {}
        self.objectForId = {}
        self.tagForObject = {}
        self.objectForTag = {}
        self.pathForDf = {}
        self.enableSyncFunctions()
        self.addTags()
        self.add_dataframe_path()

    def __str__(self):
        return "SklearnSyncer"

    def storeObject(self, obj, Id):
        """
        Stores mapping between objects and their IDs.
        """
        self.idForObject[obj] = Id
        self.objectForId[Id] = obj

    def storeTagObject(self, obj, tag):
        """
        Stores mapping between objects and their tags.
        Tags are short, user-generated names.
        """
        self.tagForObject[obj] = tag
        self.objectForTag[tag] = obj

    def addToBuffer(self, event):
        """
        As events are generated, they are added to this buffer.
        """
        self.bufferList.append(event)

    def sync(self):
        """
        When this function is called, all events in the buffer are written to the database.
        """
        for b in self.bufferList:
            b.sync(self)
        self.clearBuffer()

    def setColumns(self, df):
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
        dataFrameCols = []
        columns = self.setColumns(df)
        for i in range(0, len(columns)):
            columnName = str(columns[i])
            dfc = modeldb_types.DataFrameColumn(columnName, str(df.dtypes[i]))
            dataFrameCols.append(dfc)
        return dataFrameCols

    def convertModeltoThrift(self, model):
        """
        Converts a model into a Thrift object with appropriate fields.
        """
        tid = -1
        tag = ""
        if model in self.idForObject:
            tid = self.idForObject[model]
        if id(model) in self.tagForObject:
            tag = self.tagForObject[id(model)]
        transformerType = model.__class__.__name__
        t = modeldb_types.Transformer(tid, transformerType, tag)
        return t

    def convertDftoThrift(self, df):
        """
        Converts a dataframe into a Thrift object with appropriate fields.
        """
        tid = -1
        tag = ""
        filePath = ""
        dfImm = id(df)
        if dfImm in self.idForObject:
            tid = self.idForObject[dfImm]
        if dfImm in self.tagForObject:
            tag = self.tagForObject[dfImm]
        dataFrameColumns = self.setDataFrameSchema(df)
        if dfImm in self.pathForDf:
            filePath = self.pathForDf[dfImm]
        modeldbDf = modeldb_types.DataFrame(tid, dataFrameColumns, df.shape[0], tag, filePath)
        return modeldbDf

    def convertSpectoThrift(self, spec, df):
        """
        Converts a TransformerSpec object into a Thrift object with appropriate fields.
        """
        tid = -1
        tag = ""
        if spec in self.idForObject:
            tid = self.idForObject[spec]
        if id(spec) in self.tagForObject:
            tag = self.tagForObject[id(spec)]
        hyperparams = []
        params = spec.get_params()
        for param in params:
            hp = modeldb_types.HyperParameter(param, str(params[param]),
                                              type(params[param]).__name__,
                                              sys.float_info.min, sys.float_info.max)
            hyperparams.append(hp)
        ts = modeldb_types.TransformerSpec(tid, spec.__class__.__name__, hyperparams, tag)
        return ts

    def addTags(self):
        """
        Adds tag as a method to objects, allowing users to tag objects with their own description
        """
        setattr(pd.DataFrame, "tag", addTagObject)
        models = [LogisticRegression, LinearRegression, LabelEncoder, OneHotEncoder,
                  Pipeline, GridSearchCV, PCA]
        for class_name in models:
            setattr(class_name, "tag", addTagObject)

    def add_dataframe_path(self):
        """
        Adds the read_csv_sync function, allowing users to automatically track dataframe location
        """
        setattr(pd, "read_csv_sync", storeDfPath)

    def enableSyncFunctions(self):
        """
        This function extends the scikit classes to implement custom
        *Sync versions of methods. (i.e. fitSync() for fit())
        Users can easily add more models to this function.
        """
        #Linear Models (transform has been deprecated)
        for class_name in [LogisticRegression, LinearRegression]:
            setattr(class_name, "fitSync", fit_Fn)
            setattr(class_name, "predictSync", predictFn)

        #Preprocessing models
        for class_name in [LabelEncoder, OneHotEncoder]:
            setattr(class_name, "fitSync", fit_Fn)
            setattr(class_name, "transformSync", transformFn)

        #Pipeline model
        for class_name in [Pipeline]:
            setattr(class_name, "fitSync", fitFnPipeline)

        #Grid-Search Cross Validation model
        for class_name in [GridSearchCV]:
            setattr(class_name, "fitSync", fitFnGridSearch)
