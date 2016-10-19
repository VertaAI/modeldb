#!/usr/bin/python
import numpy as np
import pandas as pd
import sys
sys.path.append('./thrift/gen-py')
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

import SyncableFitEvent
import SyncableTransformEvent
import SyncableMetricEvent
import SyncableRandomSplitEvent
import SyncablePipelineEvent
import GridCrossValidation
import SyncableGridSearchCV
import SyncableProjectEvent
import SyncableExperimentRunEvent
from modeldb import ModelDBService
from modeldb.ttypes import *
from sklearn.linear_model import *
from sklearn.preprocessing import *
from sklearn.pipeline import Pipeline
from sklearn.grid_search import GridSearchCV
import sklearn.metrics

#Overrides the fit function for all models except for Pipeline and GridSearch Cross Validation, which have their own functions.
def fitFn(self,X,y=None):
    df = X
    #Certain fit functions only accept one argument
    if y is None:
        models = self.fit(X)
    else:
        models = self.fit(X,y)
        yDf = pd.DataFrame(y)
        if type(X) is pd.DataFrame:
            df = X.join(yDf)
        else:
            #if X does not have column-names, we cannot perform a join, and must instead add a new column.
            df = pd.DataFrame(X)
            df[outputColumn] = y
    #Calls SyncFitEvent in other class and adds to buffer 
    fitEvent = SyncableFitEvent.SyncFitEvent(models, self, df, Syncer.instance.experimentRun.id)
    Syncer.instance.addToBuffer(fitEvent)

#Overrides the predict function for models, provided that the predict function takes in one argument
def predictFn(self, X):
    predictArray = self.predict(X)
    predictDf = pd.DataFrame(predictArray)
    newDf = X.join(predictDf)
    predictEvent = SyncableTransformEvent.SyncTransformEvent(X, newDf, self, Syncer.instance.experimentRun.id)
    Syncer.instance.addToBuffer(predictEvent)
    return predictArray

#Overrides the transform function for models, provided that the transform function takes in one argument
def transformFn(self, X):
    transformedOutput = self.transform(X)
    if type(transformedOutput) is np.ndarray:
        newDf = pd.DataFrame(transformedOutput)
    else:
        newDf = pd.DataFrame(transformedOutput.toarray())
    transformEvent = SyncableTransformEvent.SyncTransformEvent(X, newDf, self, Syncer.instance.experimentRun.id)
    Syncer.instance.addToBuffer(transformEvent)
    return transformedOutput

# Stores object with associated tagName
def addTagObject(self, tagName):
    Syncer.instance.storeTagObject(id(self), tagName)

class Syncer(object):
    class __Syncer:
        def __init__(self, projectConfig):
            self.idForObject = {}
            self.objectForId = {}
            self.tagForObject = {}
            self.objectForTag = {}
            self.bufferList = []
            self.client = self.initializeThriftClient()
            self.enableSyncFunctions()
            self.project = self.createProject(projectConfig)
            self.experimentRun = None
            self.experimentRunEvent = None
            self.addTags()

        def __str__(self):
            return `self` + self.val

        def startExperiment(self, description):
            self.experimentRun = ExperimentRun(-1, self.project.id, str(description))
            self.experimentRunEvent = SyncableExperimentRunEvent.SyncExperimentRunEvent(self.experimentRun)
            self.experimentRunEvent.sync()

        def endExperiment(self):
            self.experimentRun = None
            self.experimentRunEvent = None

        def storeObject(self, obj, Id):
            self.idForObject[obj] = Id
            self.objectForId[Id] = obj

        def storeTagObject(self, obj, tag):
            self.tagForObject[obj] = tag
            self.objectForTag[tag] = obj

        def addToBuffer(self, fitEvent):
            self.bufferList.append(fitEvent)

        def sync(self):
            for b in self.bufferList:
                b.sync()

        def setColumns(self,df):
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
            dataFrameCols = []
            columns = self.setColumns(df)
            for i in range(0, len(columns)):
                columnName = str(columns[i])
                dfc = DataFrameColumn(columnName, str(df.dtypes[i]))
                dataFrameCols.append(dfc)
            return dataFrameCols

        def convertModeltoThrift(self, model):
            tid = -1
            tag = ""
            if model in self.idForObject:
                tid = self.idForObject[model]
            if id(model) in self.tagForObject:
                tag = self.tagForObject[id(model)]
            transformerType = model.__class__.__name__
            t = Transformer(tid, [0.0], transformerType, tag)
            return t

        def convertDftoThrift(self, df):
            tid = -1
            tag = ""
            dfImm = id(df)
            if dfImm in self.idForObject:
                tid = self.idForObject[dfImm]
            if dfImm in self.tagForObject:
                tag = self.tagForObject[dfImm]
            dataFrameColumns = self.setDataFrameSchema(df)
            modeldbDf = DataFrame(tid, dataFrameColumns, df.shape[0], tag)
            return modeldbDf

        def convertSpectoThrift(self, spec, df):
            tid = -1
            tag = ""
            if spec in self.idForObject:
                tid = self.idForObject[spec]
            if id(spec) in self.tagForObject:
                tag = self.tagForObject[id(spec)]
            columns = self.setColumns(df)
            hyperparams = []
            params = spec.get_params()
            for param in params:
                hp = HyperParameter(param, str(params[param]), type(params[param]).__name__, sys.float_info.min, sys.float_info.max)
                hyperparams.append(hp)
            ts = TransformerSpec(tid, spec.__class__.__name__, columns, hyperparams, tag)
            return ts

        def createProject(self, projectConfig):
            if type(projectConfig[0]) is int:
                # There is an existing project
                self.project = Project(projectConfig[0], "", "", "")
            else:
                # Create new project
                [name, author, description] = projectConfig
                self.project = Project(-1, name, author, description)
                projectEvent = SyncableProjectEvent.SyncProjectEvent(self.project)
                self.addToBuffer(projectEvent)
            return self.project

        def initializeThriftClient(self, host="localhost", port=6543):
            # Make socket
            transport = TSocket.TSocket(host, port)

            # Buffering is critical. Raw sockets are very slow
            transport = TTransport.TFramedTransport(transport)

            # Wrap in a protocol
            protocol = TBinaryProtocol.TBinaryProtocol(transport)

            # Create a client to use the protocol encoder
            client = ModelDBService.Client(protocol)
            transport.open()
            result = client.testConnection()
            return client

        # Adds tag as a method to objects, allowing users to tag objects with their own description
        def addTags(self):
            setattr(pd.DataFrame, "tag", addTagObject)
            models = [LogisticRegression, LinearRegression, LabelEncoder, OneHotEncoder,
                            Pipeline, GridSearchCV]
            for class_name in models:
                setattr(class_name, "tag", addTagObject)

        #This function extends the scikit classes to implement custom *Sync versions of methods. (i.e. fitSync() for fit())
        #Users can easily add more models to this function.
        def enableSyncFunctions(self):
            #Linear Models (transform has been deprecated)
            for class_name in [LogisticRegression, LinearRegression]:
                setattr(class_name, "fitSync", fitFn)
                setattr(class_name, "predictSync", predictFn)

            #Preprocessing models
            for class_name in [LabelEncoder, OneHotEncoder]:
                setattr(class_name, "fitSync", fitFn)
                setattr(class_name, "transformSync", transformFn)

            #Pipeline model
            for class_name in [Pipeline]:
                setattr(class_name, "fitSync", SyncablePipelineEvent.fitFnPipeline)

            #Grid-Search Cross Validation model
            for class_name in [GridSearchCV]:
                setattr(class_name, "fitSync",  SyncableGridSearchCV.fitFnGridSearch)

    instance = None
    def __new__(cls, projectConfig): # __new__ always a classmethod
        if not type(projectConfig) is list:
                raise ValueError("Project parameters must be defined as "
                                    " a list [name, author, description] or as [id]")
        if not Syncer.instance:
            Syncer.instance = Syncer.__Syncer(projectConfig)
        return Syncer.instance

    def __getattr__(self, name):
        return getattr(self.instance, name)

    def __setattr__(self, name):
        return setattr(self.instance, name)
