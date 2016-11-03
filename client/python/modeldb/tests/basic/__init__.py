import sys
import modeldb.sklearn_native.ModelDbSyncer

# modeldb imports
from modeldb.events import *
from thrift.modeldb import ModelDBService
from modeldb import ttypes as modeldb_types

class SyncerTest(ModelDbSyncer.Syncer):
    instance = None
    def __new__(cls, projectConfig, experimentConfig, experimentRunConfig): # __new__ always a classmethod
        # This will break if cls is some random class.
        if not cls.instance:
            cls.instance = object.__new__(cls, projectConfig, experimentConfig, experimentRunConfig)
            ModelDbSyncer.Syncer.instance = SyncerTest.instance    
        return cls.instance
    
    def sync(self):
        events = []
        for b in self.bufferList:
            event = b.makeEvent(self)
            events.append(event)
        return events

    def clearBuffer(self):
        self.bufferList = []
