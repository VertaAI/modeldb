from ..thrift.modeldb import ttypes as modeldb_types


class NewOrExistingProject:

    def __init__(self, name, author, description):
        self.name = name
        self.author = author
        self.description = description

    def to_thrift(self):
        return modeldb_types.Project(
            -1, self.name, self.author, self.description)


class ExistingProject:

    def __init__(self, id):
        self.id = id

    def to_thrift(self):
        return modeldb_types.Project(self.id, "", "", "")


class NewOrExistingExperiment:

    def __init__(self, name, description):
        self.name = name
        self.description = description

    def to_thrift(self):
        return modeldb_types.Experiment(
            -1, -1, self.name, self.description, False)


class ExistingExperiment:

    def __init__(self, id):
        self.id = id

    def to_thrift(self):
        return modeldb_types.Experiment(self.id, -1, "", "", False)


class DefaultExperiment:

    def to_thrift(self):
        return modeldb_types.Experiment(-1, -1, "", "", True)


class NewExperimentRun:

    def __init__(self, description="", sha=None):
        self.description = description
        self.sha = sha

    def to_thrift(self):
        erun = modeldb_types.ExperimentRun(-1, -1, self.description)
        if self.sha:
            erun.sha = self.sha
        return erun


class ExistingExperimentRun:

    def __init__(self, id):
        self.id = id

    def to_thrift(self):
        return modeldb_types.ExperimentRun(self.id, -1, "")


class ThriftConfig:
    def __init__(self, host="localhost", port="6543"):
        self.host = host
        self.port = port


class VersioningConfig:
    def __init__(
            self, username, repo, access_token, export_directory,
            repo_directory):
        self.username = username
        self.repo = repo
        self.access_token = access_token
        self.export_directory = export_directory
        self.repo_directory = repo_directory


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
