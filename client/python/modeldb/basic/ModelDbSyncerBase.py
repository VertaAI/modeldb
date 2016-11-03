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

    def to_thrift(self):
        return modeldb_types.Project(-1, self.name, self.author, self.description)

class ExistingProject:
    def __init__(self, id):
        self.id = id

    def to_thrift(self):
        return modeldb_types.Project(self.id, "", "", "")

class ExistingExperiment:
    def __init__(self, id):
        self.id = id

    def to_thrift(self):
        return modeldb_types.Experiment(self.id, -1, "", "", False)

class DefaultExperiment:
    def to_thrift(self):
        return modeldb_types.Experiment(-1, -1, "", "", True)

class NewOrExistingExperiment:
    def __init__(self, name, description):
        self.name = name
        self.description = description

    def to_thrift(self):
        return modeldb_types.Experiment(
            -1, -1, self.name, self.description, False)

class NewExperimentRun:
    def __init__(self, description=""):
        self.description = description

    def to_thrift(self):
        return modeldb_types.ExperimentRun(-1, -1, self.description)

class ExistingExperimentRun:
    def __init__(self, id):
        self.id = id

    def to_thrift(self):
        return modeldb_types.ExperimentRun(self.id, -1, "")

class Syncer(object):
    instance = None
    def __new__(cls, project_config, experiment_config, experiment_run_config): # __new__ always a classmethod
        # This will break if cls is some random class.
        if not cls.instance:
            cls.instance = object.__new__(
                cls, project_config, experiment_config, experiment_run_config)
        return cls.instance

    def __init__(
        self, project_config, experiment_config, experiment_run_config):
        self.buffer_list = []
        self.id_for_object = {}
        self.object_for_id = {}
        self.initialize_thrift_client()
        self.setup(project_config, experiment_config, experiment_run_config)

    def setup(self, project_config, experiment_config, experiment_run_config):
        if isinstance(experiment_run_config, ExistingExperimentRun):
            self.experiment_run = experiment_run_config.to_thrift()
            self.project = None
            self.experiment = None
        elif not project_config or not experiment_config:
            # TODO: fix this error message
            print "Either (project_config and experiment_config) need to be " \
                "specified or ExistingExperimentRunConfig needs to be specified"
            sys.exit(-1)
        else:
            self.set_project(project_config)
            self.set_experiment(experiment_config)
            self.set_experiment_run(experiment_run_config)

    def __str__(self):
        return "BaseSyncer"

    def set_project(self, project_config):
        self.project = project_config.to_thrift()
        project_event = ProjectEvent(self.project)
        self.buffer_list.append(project_event)
        self.sync()

    def set_experiment(self, experiment_config):
        self.experiment = experiment_config.to_thrift()
        self.experiment.projectId = self.project.id
        experiment_event = ExperimentEvent(self.experiment)
        self.buffer_list.append(experiment_event)
        self.sync()

    def set_experiment_run(self, experiment_run_config):
        self.experiment_run = experiment_run_config.to_thrift()
        self.experiment_run.experimentId = self.experiment.id
        experiment_run_event = ExperimentRunEvent(self.experiment_run)
        self.buffer_list.append(experiment_run_event)
        self.sync()

    def add_to_buffer(self, event):
        self.buffer_list.append(event)

    def store_object(self, obj, id):
        self.id_for_object[obj] = id
        self.object_for_id[id] = obj

    def sync(self):
        for b in self.buffer_list:
            b.sync(self)
        self.clear_buffer()

    def clear_buffer(self):
        self.buffer_list = []

    def initialize_thrift_client(self, host="localhost", port=6543):
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

    def convert_model_to_thrift(self, model):
        return model

    def convert_spec_to_thrift(self, spec):
        return spec

    def convert_df_to_thrift(self, df):
        return df

    def _convert_model_to_thrift(self, model_type, path):
        return modeldb_types.Transformer(-1, [], model_type, "", \
            path)

    def _convert_spec_to_thrift(self, config, modelType, features=[]):
        hyperparameters = []
        for key in config.keys():
            hyperparameter = modeldb_types.HyperParameter(key, str(config[key]), \
                type(config[key]).__name__, FMIN, FMAX)
            hyperparameters.append(hyperparameter)
        transformerSpec = modeldb_types.TransformerSpec(-1, modelType, \
            features, hyperparameters, "")
        return transformerSpec

    def _convert_df_to_thrift(self, path):
        return modeldb_types.DataFrame(-1, [], -1, "", path)

    # data_dict is a dictionary of names and paths:
    # ["train" : "/path/to/train", "test" : "/path/to/test"]
    # revisit this
    def syncData(self, data_dict):
        self.data = data_dict

    def syncModel(self, trainDf, model_path, model_type, config, features=[]):
        df = self._convert_df_to_thrift(trainDf)
        spec = self._convert_spec_to_thrift(config, model_type, features)
        model = self._convert_model_to_thrift(model_type, model_path)
        fe = FitEvent(model, spec, df)
        self.add_to_buffer(fe)

    def syncMetrics(self, testDf, model, metrics):
        df = self._convert_df_to_thrift(testDf)
        for key, value in metrics:
            me = MetricEvent(df, model, "", "", key, \
                value)
            self.add_to_buffer(me)