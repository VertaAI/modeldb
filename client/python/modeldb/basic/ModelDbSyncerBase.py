import sys
import os
import yaml
from future.utils import with_metaclass
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

from modeldb.utils.Singleton import Singleton
from ..events import (
    Event, ExperimentEvent, ExperimentRunEvent, FitEvent, GridSearchCVEvent,
    MetricEvent, PipelineEvent, ProjectEvent, RandomSplitEvent, TransformEvent)

from . Structs import (NewOrExistingProject, ExistingProject,
     NewOrExistingExperiment, ExistingExperiment, DefaultExperiment,
     NewExperimentRun, ExistingExperimentRun, ThriftConfig, VersioningConfig,
     Dataset, ModelConfig, Model, ModelMetrics)


from ..thrift.modeldb import ModelDBService
from ..thrift.modeldb import ttypes as modeldb_types
from ..utils.ConfigUtils import ConfigReader
from ..utils import MetadataConstants as metadata_constants

FMIN = sys.float_info.min
FMAX = sys.float_info.max


class Syncer(with_metaclass(Singleton, object)):

    # location of the default config file
    config_path = os.path.abspath(
        os.path.join(os.path.dirname(__file__), os.pardir, 'syncer.json'))

    @classmethod
    def create_syncer(
            cls, proj_name, user_name, proj_desc=None, host=None, port=None):
        """
        Create a syncer given project information. A default experiment will be
        created and a default experiment run will be used
        """
        project_config = NewOrExistingProject(
            proj_name, user_name, proj_desc if proj_desc else "")
        syncer_obj = cls(
            project_config=project_config,
            experiment_config=DefaultExperiment(),
            experiment_run_config=NewExperimentRun(""),
            thrift_config=ThriftConfig(host, port))
        return syncer_obj

    @classmethod
    def create_syncer_from_config(
            cls, config_file=config_path, sha=None):
        """
        Create a syncer based on the modeldb configuration file
        """
        config_reader = ConfigReader(config_file)
        project = config_reader.get_project()
        experiment = config_reader.get_experiment()
        experiment_run = NewExperimentRun("", sha)
        thrift_config = config_reader.get_mdb_server_info()

        syncer_obj = cls(
            project_config=project,
            experiment_config=experiment,
            experiment_run_config=experiment_run,
            thrift_config=thrift_config)
        return syncer_obj

    @classmethod
    def create_syncer_for_experiment_run(
            cls, experiment_run_id, host=None, port=None):
        """
        Create a syncer for this experiment run
        """
        syncer_obj = cls(
            project_config=None,
            experiment_config=None,
            experiment_run_config=ExistingExperimentRun(experiment_run_id),
            thrift_config=ThriftConfig(host, port))
        return syncer_obj

    def __init__(
            self, project_config, experiment_config, experiment_run_config,
            thrift_config=None):
        if thrift_config is None:
            thrift_config = ThriftConfig()
        self.buffer_list = []
        self.local_id_to_modeldb_id = {}
        self.local_id_to_object = {}
        self.local_id_to_tag = {}
        self.initialize_thrift_client(thrift_config)
        self.setup(project_config, experiment_config, experiment_run_config)

    def setup(self, project_config, experiment_config, experiment_run_config):
        if isinstance(experiment_run_config, ExistingExperimentRun):
            self.experiment_run = experiment_run_config.to_thrift()
            self.project = None
            self.experiment = None
        elif not project_config or not experiment_config:
            # TODO: fix this error message
            print(
                "Either (project_config and experiment_config) need to be ",
                "specified or ExistingExperimentRunConfig needs to be ",
                "specified")
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
        When this function is called,
        all events in the buffer are stored on server.
        """
        for b in self.buffer_list:
            b.sync(self)
        self.clear_buffer()

    def clear_buffer(self):
        '''
        Remove all events from the buffer
        '''
        self.buffer_list = []

    def initialize_thrift_client(self, thrift_config):
        # use defaults if thrift_config values are empty
        if not (thrift_config.port and thrift_config.host):
            config_reader = ConfigReader(Syncer.config_path)
            default_thrift = config_reader.get_mdb_server_info()
            thrift_config.host = thrift_config.host or default_thrift.host
            thrift_config.port = thrift_config.port or default_thrift.port

        # Make socket
        self.transport = TSocket.TSocket(
            thrift_config.host, thrift_config.port)

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
        return modeldb_types.Transformer(
            -1, model.model_type, model.tag, model.path)

    def convert_spec_to_thrift(self, spec):
        spec_id = self.get_modeldb_id_for_object(spec)
        if spec_id != -1:
            return modeldb_types.TransformerSpec(spec_id, "", [], "")
        hyperparameters = []
        for key, value in spec.config.items():
            hyperparameter = modeldb_types.HyperParameter(
                key, str(value), type(value).__name__, FMIN, FMAX)
            hyperparameters.append(hyperparameter)
        transformer_spec = modeldb_types.TransformerSpec(
            -1, spec.model_type, hyperparameters, spec.tag)
        return transformer_spec

    def set_columns(self, df):
        return []

    def convert_df_to_thrift(self, dataset):
        dataset_id = self.get_modeldb_id_for_object(dataset)
        if dataset_id != -1:
            return modeldb_types.DataFrame(dataset_id, [], -1, "", "", [])
        metadata = []
        for key, value in dataset.metadata.items():
            kv = modeldb_types.MetadataKV(key, str(value), str(type(value)))
            metadata.append(kv)
        return modeldb_types.DataFrame(-1, [], -1, dataset.tag,
                                       dataset.filename, metadata)
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
        self.add_to_buffer(fit_event)

    def sync_metrics(self, data_tag, model, metrics):
        '''
        Syncs the metrics for the given model on the given data
        '''
        dataset = self.get_dataset_for_tag(data_tag)
        for metric, value in metrics.metrics.items():
            metric_event = MetricEvent(dataset, model, "label_col",
                                       "prediction_col", metric, value)
            self.add_to_buffer(metric_event)

    def get_dataset_for_tag(self, data_tag):
        if data_tag not in self.datasets:
            if "default" not in self.datasets:
                self.datasets["default"] = Dataset("", {})
            print(data_tag,
                  ' dataset not defined. default dataset will be used.')
            data_tag = "default"
        return self.datasets[data_tag]

    def dataset_from_dict(self, dataset_dict):
        filename = dataset_dict[metadata_constants.DATASET_FILENAME_KEY]
        metadata = dataset_dict.get(
            metadata_constants.DATASET_METADATA_KEY, {})
        tag = dataset_dict.get(metadata_constants.DATASET_TAG_KEY, 'default')
        return Dataset(filename, metadata, tag)

    def sync_all(self, metadata_path):
        with open(metadata_path) as data_file:
            metadata = yaml.load(data_file)

        # sync datasets
        datasets = {}
        for dataset_dict in metadata[metadata_constants.DATASETS_KEY]:
            dataset = self.dataset_from_dict(dataset_dict)
            datasets[dataset.tag] = dataset
        self.sync_datasets(datasets)

        # get model details
        model_data = metadata[metadata_constants.MODEL_KEY]
        model_type = model_data[metadata_constants.TYPE_KEY]
        model_name = model_data[metadata_constants.NAME_KEY]
        model_path = model_data.get(metadata_constants.PATH_KEY, None)
        model_tag = model_data.get(metadata_constants.TAG_KEY, None)
        model = Model(model_type, model_name, model_path, model_tag)

        model_dataset = self.get_dataset_for_tag(model_tag)
        config = model_data[metadata_constants.CONFIG_KEY]
        fit_event = FitEvent(model, ModelConfig(model_type, config, model_tag),
                             model_dataset, model_data)
        self.add_to_buffer(fit_event)

        # sync metrics
        metrics_data = model_data.get(metadata_constants.METRICS_KEY, [])
        for metric in metrics_data:
            metric_type = metric[metadata_constants.METRIC_TYPE_KEY]
            metric_value = metric[metadata_constants.METRIC_VALUE_KEY]
            metric_event = MetricEvent(
                model_dataset, model, "label_col", "prediction_col",
                metric_type, metric_value)
            self.add_to_buffer(metric_event)
