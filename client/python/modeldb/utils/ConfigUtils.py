import sys
import json
from ..basic.Structs import (
    NewOrExistingProject, ExistingProject,
    NewOrExistingExperiment, ExistingExperiment, DefaultExperiment,
    NewExperimentRun, ExistingExperimentRun, ThriftConfig, VersioningConfig)


class ConfigReader(object):

    def __init__(self, filename):
        # TODO: need to deal with errors here
        self.config = json.load(open(filename, 'r'))
        self.validate_config()

    def validate_config(self):
        # TODO: what all do we expect to be here?
        project = self.config.get('project', {})
        if project == {}:
            raise ValueError(
                '"project" must be defined in config file. Cannot proceed.')
        elif 'name' not in project:
            raise ValueError(
                '"project" in config file must contain must contain a key,'
                '"name". Cannot proceed.')
        elif 'author' not in project:
            raise ValueError(
                '"project" in config file must contain must contain a key,'
                '"author". Cannot proceed.')

        thrift = self.config.get('thrift', {})
        if thrift == {}:
            raise ValueError(
                '"thrift" must be defined in config file. Cannot proceed.')
        elif 'host' not in thrift:
            raise ValueError(
                '"thrift" in config file must contain must contain a key,'
                '"host". Cannot proceed.')
        elif 'port' not in thrift:
            raise ValueError(
                '"thrift" in config file must contain must contain a key,'
                '"port". Cannot proceed.')

    def get_project(self):
        project_name = safe_get(self.config, 'project', 'name')
        project_description = safe_get(
            self.config, 'project', 'description') or ''
        git_username = safe_get(self.config, 'git', 'username') or 'defaultuser'

        project = NewOrExistingProject(
            name=project_name, author=git_username,
            description=project_description)
        return project

    def get_experiment(self):
        experiment_name = safe_get(
            self.config, 'experiment', 'name') or 'defaultexperiment'
        experiment_description = safe_get(
            self.config, 'experiment', 'description') or ''

        experiment = NewOrExistingExperiment(
            name=experiment_name, description=experiment_description)
        return experiment

    # TODO: Define get_experiment run

    def get_mdb_server_info(self):
        thrift = self.config.get('thrift', False)

        thrift_config = ThriftConfig(
            host=thrift['host'], port=thrift['port'])

        return thrift_config

    def get_versioning_information(self):
        git = self.config.get('git', {})

        versioning_config = VersioningConfig(
            username=git.get('username', None),
            repo=git.get('repo', None),
            access_token=git.get('accessToken', None),
            export_directory=git.get('exptDir', None),
            repo_directory=git.get('repoDir', None))

        return versioning_config


def safe_get(dct, *keys):
    for key in keys:
        try:
            dct = dct[key]
        except KeyError:
            return None
    return dct
