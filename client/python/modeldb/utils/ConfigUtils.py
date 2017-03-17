import sys
import json
import ConfigConstants as constants

class ConfigReader(object):
    def __init__(self, filename):
        # TODO: need to deal with errors here
        self.config =  json.load(file(filename, 'r'))
        self.validate_config()

    def validate_config(self):
        # TODO: what all do we expect to be here?
        if 'project' not in self.config:
            print "Project must be defined in config file. Cannot proceed."
            sys.exit(-1)
        if not self.config.get('git').get('username'):
            print "Git username must be defined in config file. Cannot proceed."
            sys.exit(-1)

    def get_project(self):
        project = self.config['project']
        git = self.config['git']
        return {constants.NAME_KEY : project['name'],
            constants.GIT_USERNAME_KEY : git['username'],
            constants.DESCRIPTION_KEY : project.get('description', '')}

    def get_experiment(self, expt_name=None):
        if not expt_name:
            return None

        if 'experiments' not in self.config or len(self.config['experiments']) < 1:
            print "Experiment '%s' not defined in config file. Using default." \
                % expt_name
            return None

        experiment = None
        experiments = self.config.get('experiments', [])
        for _experiment in experiments:
            if _experiment.get('name', None) == expt_name:
                experiment = _experiment

        if experiment is None:
            print "Experiment '%s' not defined in config file. Using default " \
                "and returning None" % expt_name
            return None

        # create an experiment from the config data
        return {constants.NAME_KEY : experiment['name'],
            constants.DESCRIPTION_KEY : experiment['description']}

    def get_mdb_server_info(self):
        thrift = self.config.get('thrift', False)

        host = thrift['host'] or 'localhost';
        port = int(thrift['port']) or 6432

        return { constants.MDB_SERVER_HOST_KEY : host,
            constants.MDB_SERVER_PORT_KEY : port }

    def get_versioning_information(self):
        git = self.config.get('git', None)
        if not git.get('versionCode', False):
            return None
        else:
            return {
                constants.GIT_USERNAME_KEY : git.get('username', None),
                constants.GIT_REPO_KEY : git.get('repo', None),
                constants.ACCESS_TOKEN_KEY : git.get('accessToken', None),
                constants.EXPT_DIR_KEY : git.get('exptDir', None),
                constants.GIT_REPO_DIR_KEY : git.get('repoDir', None),
            }
