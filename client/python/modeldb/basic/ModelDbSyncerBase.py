import sys
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

from ..events import *

from ..thrift.modeldb import ModelDBService
from ..thrift.modeldb import ttypes as modeldb_types
from ..utils.ConfigUtils import ConfigReader
from ..utils import ConfigConstants as constants

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

# TODO: fix the way i'm doing tagging
class Dataset:
    def __init__(self, filename, metadata={}, tag=None):
        self.filename = filename
        self.metadata = metadata
        self.tag = tag if tag else ""

    def __str__(self):
        return self.filename + "," + self.tag

class ModelConfig:
    def __init__(self, model_type, config, tag=None):
        self.model_type = model_type
        self.config = config
        self.tag = tag if tag else ""

    def __str__(self):
        return self.model_type + "," + self.tag

class Model:
    def __init__(self, model_type, model, path=None, tag=None):
        self.model_type = model_type
        self.model = model
        self.path = path
        self.tag = tag if tag else ""

    def __str__(self):
        return self.model_type + "," + self.path + "," + self.tag

class ModelMetrics:
    def __init__(self, metrics, tag=None):
        self.metrics = metrics
        self.tag = tag if tag else ""

    def __str__(self):
        return self.metrics

# def make_dataset(filename, metadata):
#     return Dataset(filename, metadata)

class Syncer(object):
    instance = None

    @classmethod
    def create_syncer(cls, proj_name, user_name, proj_desc=None):
        """
        Create a syncer given project information. A default experiment will be
        created and a default experiment run will be used
        """
        syncer_obj = cls(
            NewOrExistingProject(proj_name, user_name, \
                proj_desc if proj_desc else ""),
            DefaultExperiment(),
            NewExperimentRun(""))
        return syncer_obj

    @classmethod
    def create_syncer_from_config(
        cls, config_file=".mdb_config", expt_name=None):
        """
        Create a syncer based on the modeldb configuration file
        """
        config_reader = ConfigReader(config_file)
        project_info = config_reader.get_project()
        experiment_info = config_reader.get_experiment(expt_name)

        project = NewOrExistingProject(
            project_info[constants.NAME_KEY],
            project_info[constants.GIT_USERNAME_KEY], 
            project_info[constants.DESCRIPTION_KEY])
        experiment = DefaultExperiment() if experiment_info == None else \
            NewOrExistingExperiment(experiment_info[constants.NAME_KEY],
                experiment_info[constants.DESCRIPTION_KEY])

        syncer_obj = cls(project, experiment, NewExperimentRun(""))
        return syncer_obj

    @classmethod
    def create_syncer_for_experiment_run(cls, experiment_run_id):
        syncer_obj = cls(None, None, ExistingExperimentRun(experiment_run_id))
        return syncer_obj


    def __new__(cls, project_config, experiment_config, experiment_run_config): # __new__ always a classmethod
        # This will break if cls is some random class.
        if not cls.instance:
            cls.instance = object.__new__(
                cls, project_config, experiment_config, experiment_run_config)
        return cls.instance

    def __init__(
        self, project_config, experiment_config, experiment_run_config):
        self.buffer_list = []
        self.local_id_to_modeldb_id = {}
        self.local_id_to_object = {}
        self.local_id_to_tag = {}
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

    def get_local_id(self, obj):
        return id(obj)

    def store_object(self, obj, modeldb_id):
        """
        Stores mapping between objects and their IDs.
        """
        local_id = self.get_local_id(obj)
        self.local_id_to_modeldb_id[local_id] = modeldb_id
        if local_id not in self.local_id_to_object:
            self.local_id_to_object[local_id] = obj

    def get_modeldb_id_for_object(self, obj):
        local_id = self.get_local_id(obj)
        if local_id in self.local_id_to_modeldb_id:
            return self.local_id_to_modeldb_id[local_id]
        else:
            return -1

    def get_tag_for_object(self, obj):
        local_id = self.get_local_id(obj)
        if local_id in self.local_id_to_tag:
            return self.local_id_to_tag[local_id]
        else:
            return ""

    def add_tag(self, obj, tag):
        """
        Stores mapping between objects and their tags.
        Tags are short, user-generated names.
        """
        local_id = self.get_local_id(obj)
        self.local_id_to_tag[local_id] = tag
        if local_id not in self.local_id_to_object:
            self.local_id_to_object[local_id] = obj

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

    def clear_buffer(self):
        '''
        Remove all events from the buffer
        '''
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

    '''
    Functions that convert ModelDBSyncerLight classes into ModelDB 
    thrift classes
    '''
    

    def convert_model_to_thrift(self, model):
        model_id = self.get_modeldb_id_for_object(model)
        if model_id != -1:
            return modeldb_types.Transformer(model_id, "", "", "")
        return modeldb_types.Transformer(-1, 
            model.model_type, model.tag, model.path)

    def convert_spec_to_thrift(self, spec):
        spec_id = self.get_modeldb_id_for_object(spec)
        if spec_id != -1:
            return modeldb_types.TransformerSpec(spec_id, "", [], "")
        hyperparameters = []
        for key, value in spec.config.items():
            hyperparameter = modeldb_types.HyperParameter(key, \
                str(value), type(value).__name__, FMIN, FMAX)
            hyperparameters.append(hyperparameter)
        transformer_spec = modeldb_types.TransformerSpec(-1, spec.model_type, \
            hyperparameters, spec.tag)
        return transformer_spec

    def set_columns(self, df):
        return []

    def convert_df_to_thrift(self, dataset):
        dataset_id = self.get_modeldb_id_for_object(dataset)
        if dataset_id != -1:
            return modeldb_types.DataFrame(dataset_id, [], -1, "", "")
        return modeldb_types.DataFrame(-1, [], -1, dataset.tag, \
            dataset.filename)
    '''
    End. Functions that convert ModelDBSyncerLight classes into ModelDB 
    thrift classes
    '''

    '''
    ModelDBSyncerLight API
    '''
    def sync_datasets(self, datasets):
        '''
        Registers the datasets used in this experiment run.
        The input is expected to be either a single dataset or a dictionary
        with keys which are local tags for the dataset and values are the
        dataset objects.
        '''
        # TODO: need to capture the metadata
        self.datasets = {}
        if type(datasets) != dict:
            self.datasets["default"] = dataset
        else:
            for key, dataset in datasets.items():
                if not dataset.tag:
                    dataset.tag = key
                self.datasets[key] = dataset

    def sync_model(self, data_tag, config, model):
        '''
        Syncs the model as having been generated from a given dataset using
        the given config
        '''
        dataset = self.get_dataset_for_tag(data_tag)
        fit_event = FitEvent(model, config, dataset)
        Syncer.instance.add_to_buffer(fit_event)

    def sync_metrics(self, data_tag, model, metrics):
        '''
        Syncs the metrics for the given model on the given data
        '''
        dataset = self.get_dataset_for_tag(data_tag)
        for metric, value in metrics.metrics.items():
            metric_event = MetricEvent(dataset, model, "label_col", \
                "prediction_col", metric, value)
            Syncer.instance.add_to_buffer(metric_event)

    def get_dataset_for_tag(self, data_tag):
        if data_tag not in self.datasets:
            if "default" not in self.datasets:
                self.datasets["default"] = Dataset("", {}) 
            print data_tag, \
                ' dataset not defined. default dataset will be used.'
            data_tag = "default"
        return self.datasets[data_tag]

    # TODO: do we want a sync all?
