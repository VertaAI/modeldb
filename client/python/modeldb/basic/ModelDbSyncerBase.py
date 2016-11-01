import sys
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

from ..events import *

from ..thrift.modeldb import ModelDBService
from ..thrift.modeldb import ttypes as modeldb_types

FMIN = sys.float_info.min
FMAX = sys.float_info.max

class NewOrExistingProject:
    def __init__(self, name, author, description):
        self.name = name
        self.author = author
        self.description = description

    def toThrift(self):
        return modeldb_types.Project(-1, self.name, self.author, self.description)

class ExistingProject:
    def __init__(self, id):
        self.id = id

    def toThrift(self):
        return modeldb_types.Project(self.id, "", "", "")

class ExistingExperiment:
    def __init__(self, id):
        self.id = id

    def toThrift(self):
        return modeldb_types.Experiment(self.id, -1, "", "", False)

class DefaultExperiment:
    def toThrift(self):
        return modeldb_types.Experiment(-1, -1, "", "", True)

class NewOrExistingExperiment:
    def __init__(self, name, description):
        self.name = name
        self.description = description

    def toThrift(self):
        return modeldb_types.Experiment(-1, -1, self.name, self.description, False)

class NewExperimentRun:
    def __init__(self, description=""):
        self.description = description

    def toThrift(self):
        return modeldb_types.ExperimentRun(-1, -1, self.description)

class ExistingExperimentRun:
    def __init__(self, id):
        self.id = id

    def toThrift(self):
        return modeldb_types.ExperimentRun(self.id, -1, "")

class Syncer(object):
    instance = None
    def __new__(cls, projectConfig, experimentConfig, experimentRunConfig): # __new__ always a classmethod
        # This will break if cls is some random class.
        if not cls.instance:
            cls.instance = object.__new__(cls, projectConfig, experimentConfig, experimentRunConfig)
        return cls.instance

    def __init__(self, projectConfig, experimentConfig, experimentRunConfig):
        self.bufferList = []
        self.idForObject = {}
        self.objectForId = {}
        self.initializeThriftClient()
        self.setup(projectConfig, experimentConfig, experimentRunConfig)

    def setup(self, projectConfig, experimentConfig, experimentRunConfig):
        if isinstance(experimentRunConfig, ExistingExperimentRun):
            self.experimentRun = experimentRunConfig.toThrift()
            self.project = None
            self.experiment = None
        elif not projectConfig or not experimentConfig:
            # TODO: fix this error message
            print "Either (projectConfig and experimentConfig) need to be " \
                "specified or ExistingExperimentRunConfig needs to be specified"
            sys.exit(-1)
        else:
            self.setProject(projectConfig)
            self.setExperiment(experimentConfig)
            self.setExperimentRun(experimentRunConfig)

    def __str__(self):
        return "BaseSyncer"

    def setProject(self, projectConfig):
        self.project = projectConfig.toThrift()
        projectEvent = ProjectEvent(self.project)
        self.bufferList.append(projectEvent)
        self.sync()

    def setExperiment(self, experimentConfig):
        self.experiment = experimentConfig.toThrift()
        self.experiment.projectId = self.project.id
        experimentEvent = ExperimentEvent(self.experiment)
        self.bufferList.append(experimentEvent)
        self.sync()

    def setExperimentRun(self, experimentRunConfig):
        self.experimentRun = experimentRunConfig.toThrift()
        self.experimentRun.experimentId = self.experiment.id
        experimentRunEvent = ExperimentRunEvent(self.experimentRun)
        self.bufferList.append(experimentRunEvent)
        self.sync()

    def addToBuffer(self, event):
        self.bufferList.append(event)

    def storeObject(self, obj, id):
        self.idForObject[obj] = id
        self.objectForId[id] = obj

    def sync(self):
        for b in self.bufferList:
            b.sync(self)
        self.clearBuffer()

    def clearBuffer(self):
        self.bufferList = []

    def initializeThriftClient(self, host="localhost", port=6543):
        # Make socket
        self.transport = TSocket.TSocket(host, port)

        # Buffering is critical. Raw sockets are very slow
        self.transport = TTransport.TFramedTransport(self.transport)

        # Wrap in a protocol
        protocol = TBinaryProtocol.TBinaryProtocol(self.transport)

        # Create a client to use the protocol encoder
        self.client = ModelDBService.Client(protocol)
        self.transport.open()

    def closeThriftClient(self):
        self.transport.close()
        self.client = None

    def convertModeltoThrift(self, model):
        return model

    def convertSpectoThrift(self, spec, df):
        return spec

    def convertDftoThrift(self, df):
        return df

    def _convertModeltoThrift(self, model_type, path):
        return modeldb_types.Transformer(-1, [], model_type, "", \
            path)

    def _convertSpectoThrift(self, config, modelType, features=[]):
        hyperparameters = []
        for key in config.keys():
            hyperparameter = modeldb_types.HyperParameter(key, str(config[key]), \
                type(config[key]).__name__, FMIN, FMAX)
            hyperparameters.append(hyperparameter)
        transformerSpec = modeldb_types.TransformerSpec(-1, modelType, \
            features, hyperparameters, "")
        return transformerSpec

    def _convertDftoThrift(self, path):
        return modeldb_types.DataFrame(-1, [], -1, "", path)

    # data_dict is a dictionary of names and paths:
    # ["train" : "/path/to/train", "test" : "/path/to/test"]
    # revisit this
    def syncData(self, data_dict):
        self.data = data_dict

    def syncModel(self, trainDf, model_path, model_type, config, features=[]):
        df = self._convertDftoThrift(trainDf)
        spec = self._convertSpectoThrift(config, model_type, features)
        model = self._convertModeltoThrift(model_type, model_path)
        fe = FitEvent(model, spec, df)
        self.addToBuffer(fe)

    def syncMetrics(self, testDf, model, metrics):
        df = self._convertDftoThrift(testDf)
        for key, value in metrics:
            me = MetricEvent(df, model, "", "", key, \
                value)
            self.addToBuffer(me)