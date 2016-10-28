import sys
from ..thrift.modeldb import ttypes as modeldb_types

class Event:
    def __init__(self, experimentRun):
        self.experimentRun = experimentRun
    def sync(self, syncer):
        pass