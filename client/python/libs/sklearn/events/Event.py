import numpy as np
import pandas as pd
import sys
sys.path.append('./thrift/gen-py')
import modeldb.ttypes as modeldb_types

class Event:
    def __init__(self, experimentRun):
        self.experimentRun = experimentRun
    def sync(self, syncer):
        pass