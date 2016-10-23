#!/usr/local/bin/python

import argparse
import yaml
import sys
import os, os.path
import mdb_code_versioning
import constants

# parse command line arguments
parser = argparse.ArgumentParser(description='ModelDB command line entry point')
parser.add_argument('--config', nargs='?', default='.mdb_config', 
    help='Configuration File. Default at .mdb_config')
parser.add_argument('--expt', nargs='?', 
    help='Name of an experiment defined in config, otherwise default used')
parser.add_argument('script', nargs='+', 
    help='Script to be invoked to run the experiment')
args = parser.parse_args()

# read in the config file. Config file is specified in yaml
config = yaml.load(file(args.config, 'r'))
print config

project = None
experiment = None
# A project must be defined in the config
if constants.PROJECT_KEY not in config:
    print "Project must be defined in config file. Quitting."
    sys.exit(-1)
else:
    project = config[constants.PROJECT_KEY]
print "Project:", project

if args.expt is not None:
    if constants.EXPT_KEY not in config:
        print "No experiments defined in config file. Quitting."
        sys.exit(0)
    else:
        experiments = config[constants.EXPT_KEY]
        for _experiment in experiments:
            if _experiment[constants.NAME_KEY] == args.expt:
                experiment = _experiment
        if experiment is None:
            print "Experiment '%s' not defined in config file. Quitting." % args.expt
            sys.exit(0)
else:
    print "Using default experiment"
    experiment = {
      constants.NAME_KEY : 'default_expt', 
      constants.DESCRIPTION_KEY : 'default_expt description'}
print "Experiment:", experiment

sha = None
if config[constants.VERSION_CODE_KEY]:
    sha = mdb_code_versioning.version(config)
    print sha
    if not sha:
        print "Unable to version the code. See errors above."
        print "Exiting."
        sys.exit(-1)
else:
    print "Not versioning code."


# connect to modeldb and create an experiment run
expt_run_id = -1

# expt_run_id = modeldb.createExperimentRun({})
# create a connection
# create a project
# create an experiment
# create an experiment run

# call the remainder of the script with the additional information
script_cmd = ' '.join(args.script + ['--mdb_expt_run_id=' + str(expt_run_id)])
os.system(script_cmd)

sys.exit(0) 


