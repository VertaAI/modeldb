#!/usr/local/bin/python

import argparse
import yaml
import sys
import os, os.path
import mdb_code_versioning
import constants
from modeldb.basic.ModelDbSyncerBase import *
from modeldb.utils.ConfigUtils import ConfigReader

# parse command line arguments
parser = argparse.ArgumentParser(description='ModelDB command line entry point')
parser.add_argument('--config', nargs='?', default='.mdb_config', 
    help='Configuration File. Default at .mdb_config')
parser.add_argument('--expt', nargs='?', 
    help='Name of an experiment defined in config, otherwise default used')
parser.add_argument('script', nargs='+', 
    help='Script to be invoked to run the experiment')
args = parser.parse_args()

config = ConfigReader(args.config)
versioning_info = config.get_versioning_information()
sha = None
if versioning_info:
    sha = mdb_code_versioning.version(versioning_info)
    if not sha:
        print "Unable to version code. See errors above."

syncer = Syncer.create_syncer_from_config(args.config, args.expt)


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


