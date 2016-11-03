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

class DataSources:
    def __init__(self, data_dict):
        self.train = self.create_dataframe(data_dict["train"] or "")
        self.test = self.create_dataframe(data_dict["test"] or "")
        self.validate = self.create_dataframe(data_dict["validate"] or "")

    def create_dataframe(self, path):
        return modeldb_types.DataFrame(-1, [], -1, "", path)

class ExperimentRunInfo:
    def __init__(self, data, config, model, metrics):
        this.data = data
        this.config = config
        this.model = model
        this.metrics = metrics

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

    def storeObject(self, obj, obj_id):
        self.idForObject[obj] = obj_id

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

    def convertSpectoThrift(self, spec):
        return spec

    def convertDftoThrift(self, df):
        return df

    def setColumns(self, df):
        return []

    def _sync_model_config(self, config, modeldb_type=""):
        hyperparameters = []
        for key in config.keys():
            hyperparameter = modeldb_types.HyperParameter(key, \
                str(config[key]), type(config[key]).__name__, FMIN, FMAX)
            hyperparameters.append(hyperparameter)
        transformer_spec = modeldb_types.TransformerSpec(-1, model_type, \
            hyperparameters, "")
        return transformer_spec

    def _sync_model_metrics(self, metrics, df, model):
        df = self._convertDftoThrift(testDf)
        for key, value in metrics:
            me = MetricEvent(df, model, "", "", key, value)
            self.addToBuffer(me)

    def _sync_model(self, model, df, spec):
        if type(model) == str:
            # we only need to store the model path
        else:
            # we need to export the model object
            print 'Exporting models directly is not implemented.'

        model = modeldb_types.Transformer(-1, [], model_type, "", path)
        fe = FitEvent(model, spec, df)
        self.addToBuffer(fe)

    def _sync_data_sources(self, data):
        '''
        Registers the data used in this experiment run.
        The input is expected to be a dictionary with three keys,
        train, test and validate. All keys are optional.

        E.g. _sync_data({"train" : "/path/to/train"})
        '''
        if type(data) != dict:
            print 'Cannot sync data dictionary.'
            sys.exit(-1)
        return DataSources(data)

    def sync_all(self, data, config, model, metrics):
        expt_run_info = ExperimentRunInfo(data, config, model, metrics)
        # convert the data frames to thrift
        data_sources = self._sync_data(data)

        # convert config to thrift
        spec = self._sync_model_config(config)

        # convert model to thrift
        model = self._sync_model(model, data_sources.train, spec)

        # convert metrics to thrift
        metrics = self._sync_model_metrics(metrics, data_sources.test, model)