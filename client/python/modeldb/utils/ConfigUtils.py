import sys
import yaml
import ConfigConstants as constants

class ConfigReader(object):
    def __init__(self, filename):
        # TODO: need to deal with errors here
        self.config =  yaml.load(file(filename, 'r'))
        self.validate_config()

    def validate_config(self):
        # TODO: what all do we expect to be here?
        if constants.PROJECT_KEY not in self.config:
            print "Project must be defined in config file. Cannot proceed."
            sys.exit(-1)
        if constants.GIT_USERNAME_KEY not in self.config:
            print "Username must be defined in config file. Cannot proceed."
            sys.exit(-1)

    def get_project(self):
        project = self.config[constants.PROJECT_KEY]
        return {"name" : project[constants.NAME_KEY], 
            "username" : self.get_username(), 
            "description" : project[constants.DESCRIPTION_KEY]}

    def get_experiment(self, expt_name=None):
        if not expt_name:
            return None
        
        if constants.EXPT_KEY not in self.config:
            print "Experiment '%s' not defined in config file. Using default." \
                % expt_name
            return None
        
        experiment = None
        experiments = config[constants.EXPT_KEY]
        for _experiment in experiments:
            if _experiment[constants.NAME_KEY] == expt_name:
                experiment = _experiment

        if experiment is None:
            print "Experiment '%s' not defined in config file. Using default." \
                % expt_name
            return None

        # create an experiment from the config data
        return {"name" : experiment[constants.NAME_KEY],
            "description" : experiment[constants.DESCRIPTION_KEY]}

    def get_mdb_server_info(self):
        host = self.config[constants.MDB_SERVER_HOST_KEY] \
            if MDB_SERVER_HOST_KEY in self.config else "localhost"
        port = int(self.config[constants.MDB_SERVER_HOST_KEY]) \
            if MDB_SERVER_HOST_KEY in self.config else 6543
        return { "host" : host, "port" : port }

    def get_username(self):
        return self.config[constants.GIT_USERNAME_KEY]

    def get_versioning_information(self):
        if not config[constants.VERSION_CODE_KEY]:
            return {}
        else:
            return {
                "username" : self.get_username(),
                "repo_name" : self.config[constants.GIT_REPO_KEY],
                "access_token" : self.config[constants.ACCESS_TOKEN_KEY],
                "expt_dir" : self.config[constants.EXPT_DIR_KEY],
                "repo_dir" : self.config[constants.GIT_REPO_DIR_KEY],
            }