#!/usr/local/bin/python

import argparse
import yaml
import sys
import os, os.path
import mdb_code_versioning

PROJECT_KEY = 'PROJECT'
EXPT_KEY = 'EXPT'
STATE_FILE = '.mdb_state'


# parse command line arguments
parser = argparse.ArgumentParser(description='ModelDB command line entry point')
parser.add_argument('--config', nargs='?', default='.mdb_config', 
    help='Configuration File. Default at .mdb_config')
# parser.add_argument('--project', nargs='?', 
#     help='Name of project. Can be specified in the config file.')
parser.add_argument('--expt', nargs='?', 
    help='Specify name of experiment defined in config')
parser.add_argument('script', nargs='+', 
    help='Script to be invoked to run the experiment')

args = parser.parse_args()
print args
print args.config


# read in mdb state file if it exists
mdb_state = None
if os.path.isfile(STATE_FILE): 
    mdb_state = yaml.load(file(STATE_FILE, 'r'))

# read in the config file. Config file is specified in yaml
config = yaml.load(file(args.config, 'r'))
print config

# check if we have a project defined. we always need a project to be defined
# TODO: allow project to be a dictionary
# TODO: we may want to allow project information to be written back
# TODO: how to create a new project?

# if args.project:
#     # override the config
#     config[PROJECT_KEY] = args.project
# elif PROJECT_KEY in config:
#     pass
# elif PROJECT_KEY in mdb_state:
#     config[PROJECT_KEY] = mdb_state[PROJECT_KEY]
# else:
#     print """Project must be specified. Either specify %s in config or
#     provide --project project_name command line flag""" % PROJECT_KEY
#     sys.exit(-1)

# TODO: add expt functionality back in
# check that we have an experiment defined. we always need an experiment
# TODO: allow experiment to be a dictionary
# if args.expt:
#     # override the config
#     config[EXPT_KEY] = args.expt
# else:
#     if EXPT_KEY not in config and :
#         print """Experiment must be specified. Either specify %s in config or
#         provide --expt expt_name command line flag""" % EXPT_KEY
#         sys.exit(-1)

print config

# TODO: verify that the config looks good

# version the code and get the commit hash
# TODO: check if code should be versioned. this is specified in the config
sha = mdb_code_versioning.version(config)
if not sha:
    print "Unable to version the code. See errors above."
    print "Exiting."
    sys.exit(-1)

# connect to modeldb and get an experiment run_id
# TODO: what if we write out state to a file and just use the run_id from it?
expt_run_id = modeldb.createExperimentRun({})

# call the remainder of the script with the additional information
script_cmd = ' '.join(args.script + ['--mdb_expt_run_id=' + str(expt_run_id)])
print script_cmd

#os.system(script_cmd)