import sys
import json
import ConfigConstants as constants
from ..basic.Structs import NewOrExistingProject, ExistingProject, \
    NewOrExistingExperiment, ExistingExperiment, DefaultExperiment, \
    NewExperimentRun, ExistingExperimentRun


class ConfigReader(object):

    def __init__(self, filename):
        # TODO: need to deal with errors here
        self.config = json.load(file(filename, 'r'))
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
    # TODO: have get_mdb_server_info return a struct
    # TODO: have get_versioning_info return a struct

    def get_mdb_server_info(self):
        thrift = self.config.get('thrift', False)

        host = thrift['host'] or 'localhost'
        port = int(thrift['port']) or 6432

        return {constants.MDB_SERVER_HOST_KEY: host,
                constants.MDB_SERVER_PORT_KEY: port}

    def get_versioning_information(self):
        git = self.config.get('git', None)
        if not git.get('versionCode', False):
            return None
        else:
            return {
                constants.GIT_USERNAME_KEY: git.get('username', None),
                constants.GIT_REPO_KEY: git.get('repo', None),
                constants.ACCESS_TOKEN_KEY: git.get('accessToken', None),
                constants.EXPT_DIR_KEY: git.get('exptDir', None),
                constants.GIT_REPO_DIR_KEY: git.get('repoDir', None),
            }


def safe_get(dct, *keys):
    for key in keys:
        try:
            dct = dct[key]
        except KeyError:
            return None
    return dct
