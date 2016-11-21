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

def Dataset:
    def __init__(self, filename, metadata={}):
        self.filename = filename
        self.metadata = metadata

def ModelConfig:
    def __init__(self, model_type, config, tag=None):
        self.model_type = model_type
        self.config = config
        self.tag = tag

# def make_dataset(filename, metadata):
#     return Dataset(filename, metadata)

class ExperimentRunInfo:
    def __init__(self, data, config, model, metrics):
        this.data = data
        this.config = config
        this.model = model
        this.metrics = metrics

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

    # need to deal with modeldb_types
    def sync_model_config(self, model_type, config, tag):
        hyperparameters = []
        for key in config.keys():
            hyperparameter = modeldb_types.HyperParameter(key, \
                str(config[key]), type(config[key]).__name__, FMIN, FMAX)
            hyperparameters.append(hyperparameter)
        transformer_spec = modeldb_types.TransformerSpec(-1, model_type, \
            hyperparameters, "")
        self.model_config = transformer_spec

    def sync_model_metrics(self, model, metrics, dataset="default"):
        # TODO: do I need this code?
        df = self._convert_df_to_thrift(testDf)
        for key, value in metrics:
            me = MetricEvent(df, model, "", "", key, value)
            self.addToBuffer(me)

    def sync_model(self, model, df, spec):
        if type(model) == str:
            # we only need to store the model path
        else:
            # we need to export the model object
            print 'Exporting models directly is not implemented.'
        model = modeldb_types.Transformer(-1, [], model_type, "", path)
        fe = FitEvent(model, spec, df)
        self.add_to_buffer(fe)

    def sync_datasets(self, datasets):
        '''
        Registers the data used in this experiment run.
        The input is expected to be a dictionary with three keys,
        train, test and validate. All keys are optional.

        E.g. sync_data({"train" : "/path/to/train"})
        '''
        result = {}
        if type(datasets) != dict:
            # TODO: need to capture the metadata
            result["default"] = \
                modeldb_types.DataFrame(-1, [], -1, "", datasets.filename)
        else:
            # this is a dictionary
            for key, dataset in datasets.items:
                result[key] = \
                    modeldb_types.DataFrame(-1, [], -1, "", datasets.filename)
        self.datasets = result

    def sync_model(self, data_tag, config, model):
        '''

        '''



    def sync_all(self, data, model_type, config, model, metrics):
        expt_run_info = ExperimentRunInfo(data, config, model, metrics)
        # convert the data frames to thrift
        self.sync_data(data)

        # convert config to thrift
        spec = self.sync_model_config(model_type, config)

        # convert model to thrift
        model = self._sync_model(model, data_sources.train, spec)

        # convert metrics to thrift
        metrics = self.sync_model_metrics(model, metrics)
